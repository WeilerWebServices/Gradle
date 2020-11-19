package configurations

import Gradle_Check.model.slowSubprojects
import common.Os
import jetbrains.buildServer.configs.kotlin.v2019_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import model.CIBuildModel
import model.Stage
import model.TestCoverage
import model.TestType

class FunctionalTest(
    model: CIBuildModel,
    uuid: String,
    name: String,
    description: String,
    testCoverage: TestCoverage,
    stage: Stage,
    subprojects: List<String> = listOf(),
    extraParameters: String = "",
    extraBuildSteps: BuildSteps.() -> Unit = {},
    preBuildSteps: BuildSteps.() -> Unit = {}
) : BaseGradleBuildType(model, stage = stage, init = {
    this.uuid = uuid
    this.name = name
    this.description = description
    id = AbsoluteId(uuid)
    val testTasks = getTestTaskName(testCoverage, stage, subprojects)
    val buildScanTags = listOf("FunctionalTest")
    val buildScanValues = mapOf(
        "coverageOs" to testCoverage.os.name.toLowerCase(),
        "coverageJvmVendor" to testCoverage.vendor.name,
        "coverageJvmVersion" to testCoverage.testJvmVersion.name
    )

    if (name.contains("(configuration-cache)")) {
        requirements {
            doesNotContain("teamcity.agent.name", "ec2")
            // US region agents have name "EC2-XXX"
            doesNotContain("teamcity.agent.name", "EC2")
        }
    }

    val enableTestDistribution = testCoverage.testDistribution && subprojects.intersect(slowSubprojects).isEmpty()

    applyTestDefaults(model, this, testTasks, notQuick = !testCoverage.isQuick, os = testCoverage.os,
        extraParameters = (
            listOf(""""-PtestJavaHome=${testCoverage.os.javaHome(testCoverage.testJvmVersion, testCoverage.vendor)}"""") +
                buildScanTags.map { buildScanTag(it) } +
                buildScanValues.map { buildScanCustomValue(it.key, it.value) } +
                if (enableTestDistribution) "-DenableTestDistribution=true -Dscan.tag.test-distribution -Dgradle.enterprise.url=https://e.grdev.net" else "" +
                    extraParameters
            ).filter { it.isNotBlank() }.joinToString(separator = " "),
        timeout = testCoverage.testType.timeout,
        extraSteps = extraBuildSteps,
        preSteps = preBuildSteps)

    params {
        if (enableTestDistribution) {
            param("env.GRADLE_ENTERPRISE_ACCESS_KEY", "%e.grdev.net.access.key%")
        }

        param("env.JAVA_HOME", "%${testCoverage.os.name.toLowerCase()}.${testCoverage.buildJvmVersion}.openjdk.64bit%")
        param("env.ANDROID_HOME", testCoverage.os.androidHome)
        if (testCoverage.os == Os.MACOS) {
            // Use fewer parallel forks on macOs, since the agents are not very powerful.
            param("maxParallelForks", "2")
        }

        if (testCoverage.testDistribution) {
            param("maxParallelForks", "16")
        }
    }

    if (testCoverage.testType == TestType.soak || testTasks.contains("plugins:")) {
        failureConditions {
            // JavaExecDebugIntegrationTest.debug session fails without debugger might cause JVM crash
            // Some soak tests produce OOM exceptions
            javaCrash = false
        }
    }
})

fun getTestTaskName(testCoverage: TestCoverage, stage: Stage, subprojects: List<String>): String {
    val testTaskName = "${testCoverage.testType.name}Test"
    return if (subprojects.intersect(slowSubprojects).isNotEmpty()) {
        subprojects.joinToString(" ") { "$it:$testTaskName" }
    } else if (testCoverage.testDistribution && stage.omitsSlowProjects) {
        return "$testTaskName ${slowSubprojects.joinToString(" ") { "-x $it:$testTaskName" }}"
    } else if (subprojects.isEmpty()) {
        testTaskName
    } else {
        subprojects.joinToString(" ") { "$it:$testTaskName" }
    }
}
