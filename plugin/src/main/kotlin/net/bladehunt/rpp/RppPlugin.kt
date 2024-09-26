package net.bladehunt.rpp

import net.bladehunt.rpp.task.BuildTask
import net.bladehunt.rpp.task.ServerTask
import net.bladehunt.rpp.task.WatchTask
import org.gradle.api.Project
import org.gradle.api.Plugin

class RppPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val rp = project.rpp()

        project.afterEvaluate {
            it.tasks.apply {
                create("buildResourcePack", BuildTask::class.java)
                create("watchResourcePack", WatchTask::class.java)
                create("startResourcePackServer", ServerTask::class.java)
            }
        }
    }
}
