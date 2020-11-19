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

package org.eclipse.jetty.util.thread;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.statistic.CounterStatistic;
import org.eclipse.jetty.util.statistic.SampleStatistic;

/**
 * <p>A {@link QueuedThreadPool} subclass that monitors its own activity by recording queue and task statistics.</p>
 */
@ManagedObject
public class MonitoredQueuedThreadPool extends QueuedThreadPool
{
    private final CounterStatistic queueStats = new CounterStatistic();
    private final SampleStatistic queueLatencyStats = new SampleStatistic();
    private final SampleStatistic taskLatencyStats = new SampleStatistic();
    private final CounterStatistic threadStats = new CounterStatistic();

    public MonitoredQueuedThreadPool()
    {
        this(256);
    }

    public MonitoredQueuedThreadPool(int maxThreads)
    {
        super(maxThreads, maxThreads, 24 * 3600 * 1000, new BlockingArrayQueue<>(maxThreads, 256));
        addBean(queueStats);
        addBean(queueLatencyStats);
        addBean(taskLatencyStats);
        addBean(threadStats);
    }

    @Override
    public void execute(final Runnable job)
    {
        queueStats.increment();
        long begin = System.nanoTime();
        super.execute(new Runnable()
        {
            @Override
            public void run()
            {
                long queueLatency = System.nanoTime() - begin;
                queueStats.decrement();
                threadStats.increment();
                queueLatencyStats.set(queueLatency);
                long start = System.nanoTime();
                try
                {
                    job.run();
                }
                finally
                {
                    long taskLatency = System.nanoTime() - start;
                    threadStats.decrement();
                    taskLatencyStats.set(taskLatency);
                }
            }

            @Override
            public String toString()
            {
                return job.toString();
            }
        });
    }

    /**
     * Resets the statistics.
     */
    @ManagedOperation(value = "resets the statistics", impact = "ACTION")
    public void reset()
    {
        queueStats.reset();
        queueLatencyStats.reset();
        taskLatencyStats.reset();
        threadStats.reset(0);
    }

    /**
     * @return the number of tasks executed
     */
    @ManagedAttribute("the number of tasks executed")
    public long getTasks()
    {
        return taskLatencyStats.getTotal();
    }

    /**
     * @return the maximum number of busy threads
     */
    @ManagedAttribute("the maximum number of busy threads")
    public int getMaxBusyThreads()
    {
        return (int)threadStats.getMax();
    }

    /**
     * @return the maximum task queue size
     */
    @ManagedAttribute("the maximum task queue size")
    public int getMaxQueueSize()
    {
        return (int)queueStats.getMax();
    }

    /**
     * @return the average time a task remains in the queue, in nanoseconds
     */
    @ManagedAttribute("the average time a task remains in the queue, in nanoseconds")
    public long getAverageQueueLatency()
    {
        return (long)queueLatencyStats.getMean();
    }

    /**
     * @return the maximum time a task remains in the queue, in nanoseconds
     */
    @ManagedAttribute("the maximum time a task remains in the queue, in nanoseconds")
    public long getMaxQueueLatency()
    {
        return queueLatencyStats.getMax();
    }

    /**
     * @return the average task execution time, in nanoseconds
     */
    @ManagedAttribute("the average task execution time, in nanoseconds")
    public long getAverageTaskLatency()
    {
        return (long)taskLatencyStats.getMean();
    }

    /**
     * @return the maximum task execution time, in nanoseconds
     */
    @ManagedAttribute("the maximum task execution time, in nanoseconds")
    public long getMaxTaskLatency()
    {
        return taskLatencyStats.getMax();
    }
}
