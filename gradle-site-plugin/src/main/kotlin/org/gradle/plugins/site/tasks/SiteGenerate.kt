package org.gradle.plugins.site.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.plugins.site.data.CustomData
import org.gradle.plugins.site.data.ProjectDescriptor
import org.gradle.plugins.site.data.ProjectLinksDescriptor
import org.gradle.plugins.site.generator.FreemarkerSiteGenerator
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
open class SiteGenerate @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

    /**
     * Returns the project descriptor containing the derived project information.
     *
     * @return The project descriptor.
     */
    @get:Nested
    val projectDescriptor: Property<ProjectDescriptor> = project.objects.property(ProjectDescriptor::class.java)

    /**
     * Returns the output directory for the generated web page.
     *
     * @return The output directory.
     */
    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @Option(option = "output-dir", description = "Output directory path relative to project directory")
    fun setOutputDir(path: String) {
        outputDir.set(project.layout.projectDirectory.dir(path))
    }

    /**
     * Returns the custom data to be used in the generated web page.
     *
     * @return The custom data.
     */
    @get:Nested
    val customData: CustomData = CustomData(project)

    @TaskAction
    fun generate() {
        workerExecutor.submit(SiteGeneratorRunnable::class.java) {
            val linksDescriptor = ProjectLinksDescriptor(customData.getWebsiteUrl(), customData.getVcsUrl())
            it.params(projectDescriptor.get(), linksDescriptor, outputDir.get().asFile)
        }
    }
}

class SiteGeneratorRunnable @Inject constructor(
        private val projectDescriptor: ProjectDescriptor,
        private val projectLinksDescriptor: ProjectLinksDescriptor,
        private val outputDir: File) : Runnable {

    override fun run() {
        FreemarkerSiteGenerator(outputDir).generate(projectDescriptor, projectLinksDescriptor)
    }
}
