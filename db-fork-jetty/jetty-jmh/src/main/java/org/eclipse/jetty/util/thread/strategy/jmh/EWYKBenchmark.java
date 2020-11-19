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

package org.eclipse.jetty.util.thread.strategy.jmh;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.ExecutionStrategy;
import org.eclipse.jetty.util.thread.Invocable;
import org.eclipse.jetty.util.thread.ReservedThreadExecutor;
import org.eclipse.jetty.util.thread.strategy.EatWhatYouKill;
import org.eclipse.jetty.util.thread.strategy.ProduceExecuteConsume;
import org.eclipse.jetty.util.thread.strategy.ProduceConsume;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Benchmark)
public class EWYKBenchmark 
{
    static TestServer server;
    static ReservedThreadExecutor reserved;
    static File directory;

    @Param({"PC","PEC","EWYK"})
    public static String strategyName;
    
    @Param({"true","false"})
    public static boolean sleeping;
    
    @Param({"true","false"})
    public static boolean nonBlocking;
       
    @Setup(Level.Trial)
    public static void setupServer() throws Exception
    {
        // Make a test directory
        directory = File.createTempFile("ewyk","dir");
        if (directory.exists())
            directory.delete();
        directory.mkdir();
        directory.deleteOnExit();
        
        // Make some test files
        for (int i=0;i<75;i++)
        {
            File file =new File(directory,i+".txt");
            file.createNewFile();
            file.deleteOnExit();
        }
        
        server=new TestServer(directory);
        server.start();
        reserved = new ReservedThreadExecutor(server,20);
        reserved.start();
    }
    
    @TearDown(Level.Trial)
    public static void stopServer() throws Exception
    {
        reserved.stop();
        server.stop();
    }
    
    @State(Scope.Thread)
    public static class ThreadState implements Runnable
    {
        final TestConnection connection=new TestConnection(server,sleeping);
        final ExecutionStrategy strategy;
        {
            switch(strategyName)
            {
                case "PC":
                    strategy = new ProduceConsume(connection,server);
                    break;
                    
                case "PEC":
                    strategy = new ProduceExecuteConsume(connection,server);
                    break;
                    
                case "EWYK":
                    strategy = new EatWhatYouKill(connection,server);
                    break;
                    
                default:
                    throw new IllegalStateException();
            }
            
            LifeCycle.start(strategy);
        }
        
        @Override
        public void run()
        {
            strategy.produce();
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public long testStrategy(ThreadState state) throws Exception
    {
        int r;
        switch(server.getRandom(8))
        {
            case 0:
                r = 4;
                break;
            case 1:
            case 2:
                r = 2;
                break;
            default:
                r = 1;
                break;
        }

        List<CompletableFuture<String>> results = new ArrayList<>(r);
        for (int i=0;i<r;i++)
        {
            CompletableFuture<String> result = new CompletableFuture<String>();
            results.add(result);
            state.connection.submit(result);
        }
                
        if (nonBlocking)
            Invocable.invokeNonBlocking(state);
        else
            state.run();
        
        long hash = 0;
        for (CompletableFuture<String> result : results)
            hash ^= result.get().hashCode();
        
        return hash;
    }

    public static void main(String[] args) throws RunnerException 
    {
        Options opt = new OptionsBuilder()
                .include(EWYKBenchmark.class.getSimpleName())
                .warmupIterations(2)
                .measurementIterations(3)
                .forks(1)
                .threads(400)
                // .syncIterations(true) // Don't start all threads at same time
                .warmupTime(new TimeValue(10000,TimeUnit.MILLISECONDS))
                .measurementTime(new TimeValue(10000,TimeUnit.MILLISECONDS))
                // .addProfiler(CompilerProfiler.class)
                // .addProfiler(LinuxPerfProfiler.class)
                // .addProfiler(LinuxPerfNormProfiler.class)
                // .addProfiler(LinuxPerfAsmProfiler.class)
                // .resultFormat(ResultFormatType.CSV)
                .build();
        
        new Runner(opt).run();
    }
}


