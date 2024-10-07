package net.bladehunt.rpp

import net.bladehunt.rpp.spaces.Spaces
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import java.net.InetSocketAddress
import javax.inject.Inject

fun Project.rpp(): RppExtension = extensions.create("rpp", RppExtension::class.java)

open class RppExtension @Inject constructor(
    objects: ObjectFactory,
) {
    var spaces: Spaces? = null

    var minifyJson: Boolean = true

    var outputName: String? = null

    var sourceDirectory: String = "./src/main/rpp"

    val server: RppServerHandler = objects.newInstance(RppServerHandler::class.java)

    fun server(action: Action<RppServerHandler>) = action.execute(server)

    fun spaces(
        namespace: String = "rpp",
        font: String = "spaces",
        amount: Int = 8192,
        className: String? = null,
        classPackage: String? = null
    ) {
        this.spaces = Spaces(namespace, font, amount, className, classPackage)
    }
}

open class RppServerHandler {
    var address: InetSocketAddress = InetSocketAddress("127.0.0.1", 8000)
}