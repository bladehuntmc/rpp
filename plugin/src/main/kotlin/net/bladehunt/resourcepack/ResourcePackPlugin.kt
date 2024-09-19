package net.bladehunt.resourcepack

import org.gradle.api.Project
import org.gradle.api.Plugin

class ResourcePackPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.objects.sourceDirectorySet("resourcePack", "Resource Pack").apply {
            srcDir("src/main/resourcePack")
        }
    }
}
