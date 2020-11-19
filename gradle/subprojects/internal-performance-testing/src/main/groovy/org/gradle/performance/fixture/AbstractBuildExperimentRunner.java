/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.performance.fixture;

import groovy.transform.CompileStatic;
import joptsimple.OptionParser;
import org.apache.commons.io.FileUtils;
import org.gradle.performance.measure.Duration;
import org.gradle.performance.measure.MeasuredOperation;
import org.gradle.performance.results.GradleProfilerReporter;
import org.gradle.performance.results.MeasuredOperationList;
import org.gradle.profiler.BenchmarkResultCollector;
import org.gradle.profiler.BuildMutator;
import org.gradle.profiler.InvocationSettings;
import org.gradle.profiler.Profiler;
import org.gradle.profiler.ProfilerFactory;
import org.gradle.profiler.ScenarioDefinition;
import org.gradle.profiler.result.BuildInvocationResult;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Runs a single build experiment.
 *
 * As part of a performance scenario, multiple experiments need to be run and compared.
 * For example for a cross-version scenario, experiments for each version will be run.
 */
@CompileStatic
public abstract class AbstractBuildExperimentRunner implements BuildExperimentRunner {
    private static final String PROFILER_TARGET_DIR_KEY = "org.gradle.performance.flameGraphTargetDir";
    private static final String PROFILER_KEY = "org.gradle.performance.profiler";

    private final ProfilerFlameGraphGenerator flameGraphGenerator;
    private final GradleProfilerReporter gradleProfilerReporter;
    private final Profiler profiler;

    public static String getProfilerTargetDir() {
        return System.getProperty(PROFILER_TARGET_DIR_KEY);
    }

    public AbstractBuildExperimentRunner(GradleProfilerReporter gradleProfilerReporter) {
        String profilerName = System.getProperty(PROFILER_KEY);
        boolean profile = profilerName != null && !profilerName.isEmpty();
        String profilerTargetDir = getProfilerTargetDir();
        this.flameGraphGenerator = profile
            ? new JfrDifferentialFlameGraphGenerator(new File(profilerTargetDir))
            : ProfilerFlameGraphGenerator.NOOP;
        this.profiler = profile ? createProfiler(profilerName) : Profiler.NONE;
        this.gradleProfilerReporter = gradleProfilerReporter;
    }

    private Profiler createProfiler(String profilerName) {
        OptionParser optionParser = new OptionParser();
        optionParser.accepts("profiler");
        ProfilerFactory.configureParser(optionParser);
        ProfilerFactory profilerFactory = ProfilerFactory.of(Collections.singletonList(profilerName));
        String[] options = profilerName.equals("jprofiler")
            ? new String[] {"--profile", "jprofiler", "--jprofiler-home", System.getenv("JPROFILER_HOME")}
            : new String[] {};
        return profilerFactory.createFromOptions(optionParser.parse(options));
    }

    protected ProfilerFlameGraphGenerator getFlameGraphGenerator() {
        return flameGraphGenerator;
    }

    protected BenchmarkResultCollector getResultCollector() {
        return gradleProfilerReporter.getResultCollector();
    }

    protected Profiler getProfiler() {
        return profiler;
    }

    @Override
    public void run(BuildExperimentSpec experiment, MeasuredOperationList results) {
        System.out.println();
        System.out.printf("%s ...%n", experiment.getDisplayName());
        System.out.println();

        InvocationSpec invocationSpec = experiment.getInvocation();
        File workingDirectory = invocationSpec.getWorkingDirectory();
        workingDirectory.mkdirs();
        copyTemplateTo(experiment, workingDirectory);

        doRun(experiment, results);
    }

    protected abstract void doRun(BuildExperimentSpec experiment, MeasuredOperationList results);

    private static void copyTemplateTo(BuildExperimentSpec experiment, File workingDir) {
        try {
            File templateDir = TestProjectLocator.findProjectDir(experiment.getProjectName());
            FileUtils.cleanDirectory(workingDir);
            FileUtils.copyDirectory(templateDir, workingDir);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String getExperimentOverride(String key) {
        String value = System.getProperty("org.gradle.performance.execution." + key);
        if (value != null && !"defaults".equals(value)) {
            return value;
        }
        return null;
    }

    protected static Integer invocationsForExperiment(BuildExperimentSpec experiment) {
        String overriddenInvocationCount = getExperimentOverride("runs");
        if (overriddenInvocationCount != null) {
            return Integer.valueOf(overriddenInvocationCount);
        }
        if (experiment.getInvocationCount() != null) {
            return experiment.getInvocationCount();
        }
        return 40;
    }

    protected static int warmupsForExperiment(BuildExperimentSpec experiment) {
        String overriddenWarmUpCount = getExperimentOverride("warmups");
        if (overriddenWarmUpCount != null) {
            return Integer.parseInt(overriddenWarmUpCount);
        }
        if (experiment.getWarmUpCount() != null) {
            return experiment.getWarmUpCount();
        }
        if (usesDaemon(experiment)) {
            return 10;
        } else {
            return 1;
        }
    }

    private static boolean usesDaemon(BuildExperimentSpec experiment) {
        InvocationSpec invocation = experiment.getInvocation();
        if (invocation instanceof GradleInvocationSpec) {
            return ((GradleInvocationSpec) invocation).getBuildWillRunInDaemon();
        }
        return false;
    }

    protected <T extends BuildInvocationResult> Consumer<T> consumerFor(ScenarioDefinition scenarioDefinition,
                                                                        AtomicInteger iterationCount,
                                                                        MeasuredOperationList results,
                                                                        Consumer<T> scenarioReporter) {
        return invocationResult -> {
            int currentIteration = iterationCount.incrementAndGet();
            if (currentIteration > scenarioDefinition.getWarmUpCount()) {
                MeasuredOperation measuredOperation = new MeasuredOperation();
                measuredOperation.setTotalTime(Duration.millis(invocationResult.getExecutionTime().toMillis()));
                results.add(measuredOperation);
            }
            scenarioReporter.accept(invocationResult);
        };
    }

    protected static Supplier<BuildMutator> toMutatorSupplierForSettings(InvocationSettings invocationSettings, Function<InvocationSettings, BuildMutator> mutatorFunction) {
        return () -> mutatorFunction.apply(invocationSettings);
    }
}
