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

package org.eclipse.jetty.http2.parser;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.ErrorCode;
import org.eclipse.jetty.http2.hpack.HpackDecoder;
import org.eclipse.jetty.http2.hpack.HpackException;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HeaderBlockParser
{
    public static final MetaData STREAM_FAILURE = new MetaData(HttpVersion.HTTP_2, null);
    public static final MetaData SESSION_FAILURE = new MetaData(HttpVersion.HTTP_2, null);
    private static final Logger LOG = Log.getLogger(HeaderBlockParser.class);

    private final HeaderParser headerParser;
    private final ByteBufferPool byteBufferPool;
    private final HpackDecoder hpackDecoder;
    private final BodyParser notifier;
    private ByteBuffer blockBuffer;

    public HeaderBlockParser(HeaderParser headerParser, ByteBufferPool byteBufferPool, HpackDecoder hpackDecoder, BodyParser notifier)
    {
        this.headerParser = headerParser;
        this.byteBufferPool = byteBufferPool;
        this.hpackDecoder = hpackDecoder;
        this.notifier = notifier;
    }

    /**
     * Parses @{code blockLength} HPACK bytes from the given {@code buffer}.
     *
     * @param buffer      the buffer to parse
     * @param blockLength the length of the HPACK block
     * @return null, if the buffer contains less than {@code blockLength} bytes;
     * {@link #STREAM_FAILURE} if parsing the HPACK block produced a stream failure;
     * {@link #SESSION_FAILURE} if parsing the HPACK block produced a session failure;
     * a valid MetaData object if the parsing was successful.
     */
    public MetaData parse(ByteBuffer buffer, int blockLength)
    {
        // We must wait for the all the bytes of the header block to arrive.
        // If they are not all available, accumulate them.
        // When all are available, decode them.

        int accumulated = blockBuffer == null ? 0 : blockBuffer.position();
        int remaining = blockLength - accumulated;

        if (buffer.remaining() < remaining)
        {
            if (blockBuffer == null)
            {
                blockBuffer = byteBufferPool.acquire(blockLength, false);
                BufferUtil.clearToFill(blockBuffer);
            }
            blockBuffer.put(buffer);
            return null;
        }
        else
        {
            int limit = buffer.limit();
            buffer.limit(buffer.position() + remaining);
            ByteBuffer toDecode;
            if (blockBuffer != null)
            {
                blockBuffer.put(buffer);
                BufferUtil.flipToFlush(blockBuffer, 0);
                toDecode = blockBuffer;
            }
            else
            {
                toDecode = buffer;
            }

            try
            {
                return hpackDecoder.decode(toDecode);
            }
            catch (HpackException.StreamException x)
            {
                if (LOG.isDebugEnabled())
                    LOG.debug(x);
                notifier.streamFailure(headerParser.getStreamId(), ErrorCode.PROTOCOL_ERROR.code, "invalid_hpack_block");
                return STREAM_FAILURE;
            }
            catch (HpackException.CompressionException x)
            {
                if (LOG.isDebugEnabled())
                    LOG.debug(x);
                notifier.connectionFailure(buffer, ErrorCode.COMPRESSION_ERROR.code, "invalid_hpack_block");
                return SESSION_FAILURE;
            }
            catch (HpackException.SessionException x)
            {
                if (LOG.isDebugEnabled())
                    LOG.debug(x);
                notifier.connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_hpack_block");
                return SESSION_FAILURE;
            }
            finally
            {
                buffer.limit(limit);

                if (blockBuffer != null)
                {
                    byteBufferPool.release(blockBuffer);
                    blockBuffer = null;
                }
            }
        }
    }
}
