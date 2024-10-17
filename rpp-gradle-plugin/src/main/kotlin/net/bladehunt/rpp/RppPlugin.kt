package net.bladehunt.rpp

import net.bladehunt.rpp.task.BuildTask
import net.bladehunt.rpp.task.CleanTask
import net.bladehunt.rpp.task.ServerTask
import net.bladehunt.rpp.task.WatchTask
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.SourceSetContainer

class RppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.rpp()

        project.afterEvaluate {
            it.extensions
                .getByType(SourceSetContainer::class.java)
                .named("main") { set ->
                    set.java.srcDir("build/rpp/generated/java")
                }

            it.tasks.apply {
                create("buildResourcePack", BuildTask::class.java)
                create("cleanResourcePack", CleanTask::class.java)
                create("watchResourcePack", WatchTask::class.java)
                create("startResourcePackServer", ServerTask::class.java)
            }
        }
    }
}
