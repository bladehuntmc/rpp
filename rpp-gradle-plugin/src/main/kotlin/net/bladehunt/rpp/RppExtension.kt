package net.bladehunt.rpp

import net.bladehunt.rpp.build.Archive
import net.bladehunt.rpp.processor.FileProcessor
import net.bladehunt.rpp.processor.JsonProcessor
import net.bladehunt.rpp.processor.PostProcessor
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import java.net.InetSocketAddress
import javax.inject.Inject

fun Project.rpp(): RppExtension = extensions.create("rpp", RppExtension::class.java)

open class RppExtension @Inject constructor(
    objects: ObjectFactory,
) {
    var outputName: String? = null

    var sourceDirectory: String = "src/main/rpp"

    val server: RppServerHandler = objects.newInstance(RppServerHandler::class.java)

    val fileProcessors: MutableList<FileProcessor<*>> = arrayListOf()

    val outputProcessors: MutableList<PostProcessor<*>> = arrayListOf()

    val archiveProcessors: MutableList<PostProcessor<*>> = arrayListOf()

    var baseArchiveName = "resource_pack"

    fun server(action: Action<RppServerHandler>) = action.execute(server)
}

open class RppServerHandler {
    var address: InetSocketAddress = InetSocketAddress("127.0.0.1", 8000)

    var archiveId: String = Archive.DEFAULT_NAME
}