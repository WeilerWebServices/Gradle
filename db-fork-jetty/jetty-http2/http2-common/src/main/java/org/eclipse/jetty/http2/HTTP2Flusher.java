//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.eclipse.jetty.http2.frames.Frame;
import org.eclipse.jetty.http2.frames.WindowUpdateFrame;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IteratingCallback;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HTTP2Flusher extends IteratingCallback implements Dumpable
{
    private static final Logger LOG = Log.getLogger(HTTP2Flusher.class);
    private static final ByteBuffer[] EMPTY_BYTE_BUFFERS = new ByteBuffer[0];

    private final Queue<WindowEntry> windows = new ArrayDeque<>();
    private final Deque<Entry> entries = new ArrayDeque<>();
    private final Queue<Entry> pendingEntries = new ArrayDeque<>();
    private final Set<Entry> processedEntries = new HashSet<>();
    private final HTTP2Session session;
    private final ByteBufferPool.Lease lease;
    private Throwable terminated;
    private Entry stalledEntry;

    public HTTP2Flusher(HTTP2Session session)
    {
        this.session = session;
        this.lease = new ByteBufferPool.Lease(session.getGenerator().getByteBufferPool());
    }

    public void window(IStream stream, WindowUpdateFrame frame)
    {
        Throwable closed;
        synchronized (this)
        {
            closed = terminated;
            if (closed == null)
                windows.offer(new WindowEntry(stream, frame));
        }
        // Flush stalled data.
        if (closed == null)
            iterate();
    }

    public boolean prepend(Entry entry)
    {
        Throwable closed;
        synchronized (this)
        {
            closed = terminated;
            if (closed == null)
            {
                entries.offerFirst(entry);
                if (LOG.isDebugEnabled())
                    LOG.debug("Prepended {}, entries={}", entry, entries.size());
            }
        }
        if (closed == null)
            return true;
        closed(entry, closed);
        return false;
    }

    public boolean append(Entry entry)
    {
        Throwable closed;
        synchronized (this)
        {
            closed = terminated;
            if (closed == null)
            {
                entries.offer(entry);
                if (LOG.isDebugEnabled())
                    LOG.debug("Appended {}, entries={}", entry, entries.size());
            }
        }
        if (closed == null)
            return true;
        closed(entry, closed);
        return false;
    }

    private int getWindowQueueSize()
    {
        synchronized (this)
        {
            return windows.size();
        }
    }

    public int getFrameQueueSize()
    {
        synchronized (this)
        {
            return entries.size();
        }
    }

    @Override
    protected Action process() throws Throwable
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Flushing {}", session);

        synchronized (this)
        {
            if (terminated != null)
                throw terminated;

            WindowEntry windowEntry;
            while ((windowEntry = windows.poll()) != null)
                windowEntry.perform();

            Entry entry;
            while ((entry = entries.poll()) != null)
                pendingEntries.offer(entry);
        }

        if (pendingEntries.isEmpty())
        {
            if (LOG.isDebugEnabled())
                LOG.debug("Flushed {}", session);
            return Action.IDLE;
        }

        while (true)
        {
            boolean progress = false;

            if (pendingEntries.isEmpty())
                break;

            Iterator<Entry> pending = pendingEntries.iterator();
            while (pending.hasNext())
            {
                Entry entry = pending.next();
                if (LOG.isDebugEnabled())
                    LOG.debug("Processing {}", entry);

                // If the stream has been reset or removed,
                // don't send the frame and fail it here.
                if (entry.isStale())
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Stale {}", entry);
                    entry.failed(new EofException("reset"));
                    pending.remove();
                    continue;
                }

                try
                {
                    if (entry.generate(lease))
                    {
                        if (LOG.isDebugEnabled())
                            LOG.debug("Generated {} frame bytes for {}", entry.getFrameBytesGenerated(), entry);

                        progress = true;

                        processedEntries.add(entry);

                        if (entry.getDataBytesRemaining() == 0)
                            pending.remove();
                    }
                    else
                    {
                        if (session.getSendWindow() <= 0 && stalledEntry == null)
                        {
                            stalledEntry = entry;
                            if (LOG.isDebugEnabled())
                                LOG.debug("Flow control stalled at {}", entry);
                            // Continue to process control frames.
                        }
                    }
                }
                catch (Throwable failure)
                {
                    // Failure to generate the entry is catastrophic.
                    if (LOG.isDebugEnabled())
                        LOG.debug("Failure generating " + entry, failure);
                    failed(failure);
                    return Action.SUCCEEDED;
                }
            }

            if (!progress)
                break;

            if (stalledEntry != null)
                break;

            int writeThreshold = session.getWriteThreshold();
            if (lease.getTotalLength() >= writeThreshold)
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("Write threshold {} exceeded", writeThreshold);
                break;
            }
        }

        List<ByteBuffer> byteBuffers = lease.getByteBuffers();
        if (byteBuffers.isEmpty())
        {
            finish();
            return Action.IDLE;
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Writing {} buffers ({} bytes) - entries processed/pending {}/{}: {}/{}",
                    byteBuffers.size(),
                    lease.getTotalLength(),
                    processedEntries.size(),
                    pendingEntries.size(),
                    processedEntries,
                    pendingEntries);

        session.getEndPoint().write(this, byteBuffers.toArray(EMPTY_BYTE_BUFFERS));
        return Action.SCHEDULED;
    }

    void onFlushed(long bytes) throws IOException
    {
        // A single EndPoint write may be flushed multiple times (for example with SSL).
        for (Entry entry : processedEntries)
            bytes = entry.onFlushed(bytes);
    }

    @Override
    public void succeeded()
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Written {} buffers - entries processed/pending {}/{}: {}/{}",
                    lease.getByteBuffers().size(),
                    processedEntries.size(),
                    pendingEntries.size(),
                    processedEntries,
                    pendingEntries);
        finish();
        super.succeeded();
    }

    private void finish()
    {
        lease.recycle();

        processedEntries.forEach(Entry::succeeded);
        processedEntries.clear();

        if (stalledEntry != null)
        {
            int size = pendingEntries.size();
            for (int i = 0; i < size; ++i)
            {
                Entry entry = pendingEntries.peek();
                if (entry == stalledEntry)
                    break;
                pendingEntries.poll();
                pendingEntries.offer(entry);
            }
            stalledEntry = null;
        }
    }

    @Override
    protected void onCompleteSuccess()
    {
        throw new IllegalStateException();
    }

    @Override
    protected void onCompleteFailure(Throwable x)
    {
        lease.recycle();

        Throwable closed;
        Set<Entry> allEntries;
        synchronized (this)
        {
            closed = terminated;
            terminated = x;
            if (LOG.isDebugEnabled())
                LOG.debug(String.format("%s, entries processed/pending/queued=%d/%d/%d",
                        closed != null ? "Closing" : "Failing",
                        processedEntries.size(),
                        pendingEntries.size(),
                        entries.size()), x);
            allEntries = new HashSet<>(entries);
            entries.clear();
        }

        allEntries.addAll(processedEntries);
        processedEntries.clear();
        allEntries.addAll(pendingEntries);
        pendingEntries.clear();
        allEntries.forEach(entry -> entry.failed(x));

        // If the failure came from within the
        // flusher, we need to close the connection.
        if (closed == null)
            session.abort(x);
    }

    void terminate(Throwable cause)
    {
        Throwable closed;
        synchronized (this)
        {
            closed = terminated;
            terminated = cause;
            if (LOG.isDebugEnabled())
                LOG.debug("{}", closed != null ? "Terminated" : "Terminating");
        }
        if (closed == null)
            iterate();
    }

    private void closed(Entry entry, Throwable failure)
    {
        entry.failed(failure);
    }

    @Override
    public String dump()
    {
        return Dumpable.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException
    {
        out.append(toString()).append(System.lineSeparator());
    }

    @Override
    public String toString()
    {
        return String.format("%s[window_queue=%d,frame_queue=%d,processed/pending=%d/%d]",
                super.toString(),
                getWindowQueueSize(),
                getFrameQueueSize(),
                processedEntries.size(),
                pendingEntries.size());
    }

    public static abstract class Entry extends Callback.Nested
    {
        protected final Frame frame;
        protected final IStream stream;

        protected Entry(Frame frame, IStream stream, Callback callback)
        {
            super(callback);
            this.frame = frame;
            this.stream = stream;
        }

        public abstract int getFrameBytesGenerated();

        public int getDataBytesRemaining()
        {
            return 0;
        }

        protected abstract boolean generate(ByteBufferPool.Lease lease);

        public abstract long onFlushed(long bytes) throws IOException;

        @Override
        public void failed(Throwable x)
        {
            if (stream != null)
            {
                stream.close();
                stream.getSession().removeStream(stream);
            }
            super.failed(x);
        }

        private boolean isStale()
        {
            return !isProtocol() && stream != null && stream.isReset();
        }

        private boolean isProtocol()
        {
            switch (frame.getType())
            {
                case DATA:
                case HEADERS:
                case PUSH_PROMISE:
                case CONTINUATION:
                    return false;
                case PRIORITY:
                case RST_STREAM:
                case SETTINGS:
                case PING:
                case GO_AWAY:
                case WINDOW_UPDATE:
                case PREFACE:
                case DISCONNECT:
                    return true;
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public String toString()
        {
            return frame.toString();
        }
    }

    private class WindowEntry
    {
        private final IStream stream;
        private final WindowUpdateFrame frame;

        public WindowEntry(IStream stream, WindowUpdateFrame frame)
        {
            this.stream = stream;
            this.frame = frame;
        }

        public void perform()
        {
            FlowControlStrategy flowControl = session.getFlowControlStrategy();
            flowControl.onWindowUpdate(session, stream, frame);
        }
    }
}
