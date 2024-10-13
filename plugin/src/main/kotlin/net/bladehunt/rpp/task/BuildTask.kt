package net.bladehunt.rpp.task

import net.bladehunt.rpp.RppExtension
import net.bladehunt.rpp.output.buildResourcePack
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

abstract class BuildTask : DefaultTask() {
    init {
        group = "resource pack"
        description = "Archives the resource pack and generates a hash"
    }

    @TaskAction
    fun compile() {
        val extension = project.extensions.getByName("rpp") as RppExtension
        buildResourcePack(
            logger,
            project.layout.projectDirectory.asFile.resolve(extension.sourceDirectory),
            project.layout.buildDirectory.asFile.get().resolve("rpp"),
            project.version.toString(),
            extension
        )
    }
}