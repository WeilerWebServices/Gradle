package Gradle_Util_Performance.buildTypes

import common.Os
import common.applyPerformanceTestSettings
import common.buildToolGradleParameters
import common.builtInRemoteBuildCacheNode
import common.checkCleanM2
import common.gradleWrapper
import common.individualPerformanceTestArtifactRules
import common.killGradleProcessesStep
import common.performanceTestCommandLine
import common.removeSubstDirOnWindows
import common.substDirOnWindows
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay

abstract class AdHocPerformanceScenario(os: Os) : BuildType({
    val id = "Gradle_Util_Performance_AdHocPerformanceScenario${os.asName()}"
    this.uuid = id
    name = "AdHoc Performance Scenario - ${os.asName()}"
    id(id)

    applyPerformanceTestSettings(os = os, timeout = 420)
    artifactRules = individualPerformanceTestArtifactRules

    params {
        text(
            "baselines",
            "defaults",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false,
            description = "The baselines you want to compare against. Can be a Gradle version number. Use force-defaults to not use the commit distribution as a baseline on a branch other than master or release."
        )
        text(
            "testProject",
            "",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false,
            description = "The test project to use. E.g. largeJavaMultiProject"
        )
        param("channel", "adhoc")
        param("checks", "all")
        text("runs", "10", display = ParameterDisplay.PROMPT, allowEmpty = false)
        text("warmups", "3", display = ParameterDisplay.PROMPT, allowEmpty = false)
        text(
            "scenario",
            "",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false,
            description = "Which performance test to run. Should be the fully qualified class name dot (unrolled) method name. E.g. org.gradle.performance.regression.java.JavaUpToDatePerformanceTest.up-to-date assemble (parallel true)"
        )
        val profilerDescription = "Command line option for the performance test task to enable profiling. For example `--profiler jfr` and `--profiler async-profiler`."
        when (os) {
            Os.WINDOWS -> {
                text("profiler", "--profiler jprofiler", description = profilerDescription)
                param("env.JPROFILER_HOME", "C:\\Program Files\\jprofiler\\jprofiler11.1.4")
            }
            else -> {
                text("profiler", "--profiler jfr", description = profilerDescription)
                param("env.FG_HOME_DIR", "/opt/FlameGraph")
                param("env.PATH", "%env.PATH%:/opt/swift/4.2.3/usr/bin")
                param("env.HP_HOME_DIR", "/opt/honest-profiler")
            }
        }

        param("additional.gradle.parameters", "")
        param("env.ANDROID_HOME", os.androidHome)
    }

    steps {
        killGradleProcessesStep(os)
        substDirOnWindows(os, builtInRemoteBuildCacheNode)
        gradleWrapper {
            name = "GRADLE_RUNNER"
            workingDir = os.perfTestWorkingDir
            gradleParams = (
                performanceTestCommandLine(
                    "clean performance:%testProject%PerformanceAdHocTest --tests \"%scenario%\"",
                    "%baselines%",
                    """--warmups %warmups% --runs %runs% --checks %checks% --channel %channel% %profiler% %additional.gradle.parameters%""",
                    os
                ) +
                    buildToolGradleParameters(isContinue = false) +
                    builtInRemoteBuildCacheNode.gradleParameters(os)
                ).joinToString(separator = " ")
        }
        removeSubstDirOnWindows(os, builtInRemoteBuildCacheNode)
        checkCleanM2(os)
    }
})

object AdHocPerformanceScenarioLinux : AdHocPerformanceScenario(Os.LINUX)
object AdHocPerformanceScenarioWindows : AdHocPerformanceScenario(Os.WINDOWS)
object AdHocPerformanceScenarioMacOS : AdHocPerformanceScenario(Os.MACOS)
