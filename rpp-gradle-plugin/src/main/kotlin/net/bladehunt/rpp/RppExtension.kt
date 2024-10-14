package net.bladehunt.rpp

import net.bladehunt.rpp.model.Resource
import net.bladehunt.rpp.processor.codegen.CodegenProcessorBuilder
import net.bladehunt.rpp.processor.file.FileProcessor
import net.bladehunt.rpp.processor.file.JsonProcessor
import net.bladehunt.rpp.processor.Processor
import net.bladehunt.rpp.processor.file.McmetaProcessor
import net.bladehunt.rpp.processor.output.SpacesProcessor
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

    val fileProcessors: MutableList<FileProcessor> = arrayListOf()

    val outputProcessors: MutableList<Processor> = arrayListOf()

    val archiveProcessors: MutableList<Processor> = arrayListOf()

    fun server(action: Action<RppServerHandler>) = action.execute(server)

    fun processMcmeta(priority: Int = -1) {
        fileProcessors.add(McmetaProcessor(priority))
    }

    fun processJson(
        transpileJsonc: Boolean = true,
        minify: Boolean = true,
        priority: Int = 0
    ) {
        fileProcessors.add(JsonProcessor(transpileJsonc, minify, priority))
    }

    fun spaces(
        font: Resource,
        amount: Int = 16,
        priority: Int = Int.MIN_VALUE
    ) {
        outputProcessors.add(SpacesProcessor(font, amount, priority))
    }

    fun codegen(action: Action<CodegenProcessorBuilder>? = null) {
        val builder = CodegenProcessorBuilder()
        action?.execute(builder)
        outputProcessors.add(builder.build())
    }
}

open class RppServerHandler {
    var address: InetSocketAddress = InetSocketAddress("127.0.0.1", 8000)
}