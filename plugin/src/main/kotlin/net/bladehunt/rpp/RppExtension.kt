package net.bladehunt.rpp

import net.bladehunt.rpp.pack.Font
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import java.net.InetSocketAddress
import javax.inject.Inject

fun Project.rpp(): RppExtension = extensions.create("rpp", RppExtension::class.java)

open class RppExtension @Inject constructor(
    objects: ObjectFactory,
) {
    var minifyJson: Boolean = true

    var outputName: String? = null

    var sourceDirectory: String = "./src/main/rpp"

    val server: RppServerHandler = objects.newInstance(RppServerHandler::class.java)

    fun server(action: Action<RppServerHandler>) = action.execute(server)

    val codegen: RppCodegenHandler = objects.newInstance(RppCodegenHandler::class.java)

    fun codegen(action: Action<RppCodegenHandler>) = action.execute(codegen)
}

open class RppServerHandler {
    var address: InetSocketAddress = InetSocketAddress("127.0.0.1", 8000)
}

open class RppCodegenHandler @Inject constructor(
    objects: ObjectFactory
) {
    var enable = false

    val fonts: RppCodegenFontHandler = objects.newInstance(RppCodegenFontHandler::class.java)

    fun fonts(action: Action<RppCodegenFontHandler>) = action.execute(fonts)
}

open class RppCodegenFontHandler {
    var `package` = "net.bladehunt.rpp.generated"

    var classPrefix = ""

    var classSuffix = "Font"

    internal val fonts: MutableSet<Font> = mutableSetOf()

    fun include(font: Font) {
        fonts.add(font)
    }

    fun include(namespace: String, font: String) = include(Font(namespace, font))
}