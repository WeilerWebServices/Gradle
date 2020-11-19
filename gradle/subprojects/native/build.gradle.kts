plugins {
    id("gradlebuild.distribution.api-java")
    id("gradlebuild.jmh")
}

description = "This project contains various native operating system integration utilities"

gradlebuildJava.usedInWorkers()

dependencies {
    api(project(":files"))

    implementation(project(":base-services"))

    implementation(libs.nativePlatform)
    implementation(libs.nativePlatformFileEvents)
    implementation(libs.slf4jApi)
    implementation(libs.guava)
    implementation(libs.commonsIo)
    implementation(libs.jansi)

    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":logging")))

    jmhImplementation(project(":files"))
}

jmh {
    fork = 1
    threads = 2
    warmupIterations = 10
    synchronizeIterations = false
    resultFormat = "CSV"
}

val copyJmhReport by tasks.registering(Copy::class) {
    destinationDir = file("$buildDir/reports/jmh-html")

    from("src/jmh/html")
}

val convertCSV by tasks.registering {
    val inputFile by extra(file("$buildDir/reports/jmh/results.csv"))
    val outputFile by extra(file("$buildDir/reports/jmh-html/data.csv"))
    inputs.file(inputFile)
    outputs.file(outputFile)
    doLast {
        val benchToScenarioName = mapOf(
            "org.gradle.internal.nativeintegration.filesystem.FileMetadataAccessorBenchmark.stat_existing" to "Existing",
            "org.gradle.internal.nativeintegration.filesystem.FileMetadataAccessorBenchmark.stat_directory" to "Directory",
            "org.gradle.internal.nativeintegration.filesystem.FileMetadataAccessorBenchmark.stat_missing_file" to "Missing")
        var first = true
        val benchmarks = mutableMapOf<String, MutableList<Pair<String, Int>>>().withDefault { mutableListOf() }
        inputFile.forEachLine { line ->
            if (first) {
                first = false
            } else {
                val tokens = line.replace("\"", "").split(",")

                val (benchmark, mode, threads, samples, score) = tokens.subList(0, 5)
                val (error, unit, accessor) = tokens.subList(5, tokens.size)
                val benchmarksValue = benchmarks.getValue(benchToScenarioName.getValue(benchmark))
                benchmarksValue.add(Pair(accessor.replace("FileMetadataAccessor", ""), score.toDouble().toInt()))
                benchmarks.put(benchToScenarioName.getValue(benchmark), benchmarksValue)
            }
        }
        outputFile.parentFile.mkdirs()
        val tested = mutableSetOf<String>()
        benchmarks.forEach { benchmark, values ->
            values.forEach {
                tested.add(it.first)
            }
        }
        outputFile.printWriter().use { writer ->
            writer.print("Scenario,${tested.joinToString(",")}\n")
            benchmarks.forEach { benchmark, values ->
                writer.print(benchmark)
                tested.forEach { test ->
                    writer.print(",${values.find { it.first == test }!!.second}")
                }
                writer.print("\n")
            }
        }
        println(outputFile.absolutePath)
    }
}

tasks.register("jmhReport") {
    dependsOn("jmh", copyJmhReport, convertCSV)
}
