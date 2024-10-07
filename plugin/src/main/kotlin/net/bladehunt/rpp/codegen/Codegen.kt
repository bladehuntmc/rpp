package net.bladehunt.rpp.codegen

import kotlinx.serialization.json.decodeFromStream
import net.bladehunt.rpp.Json
import net.bladehunt.rpp.spaces.Spaces
import java.io.File
import java.util.*

internal fun generateCode(
    config: CodegenConfig,
    spaces: Spaces?,
    assets: File,
    javaOutput: File
) {
    if (!javaOutput.deleteRecursively()) throw IllegalStateException("Failed to clean codegen output")

    if (spaces != null) {
        val className = requireNotNull(spaces.className)
        val pkg = requireNotNull(spaces.classPackage)

        val packageDir = javaOutput.resolve(pkg.replace('.', '/'))
        packageDir.mkdirs()

        val fontClass = packageDir.resolve("$className.java")

        fontClass.createNewFile()

        fontClass.outputStream().bufferedWriter().use {
            it.write(createSpaceClass(className, pkg, spaces.namespace, spaces.font, spaces.amount))
        }
    }

    config.forEach { (namespace, nsConfig) ->
        nsConfig.fonts?.forEach { (font, fontConfig) ->
            val asset = assets.resolve("$namespace/font/$font.json")

            val `package` = fontConfig.packageOverride ?: (nsConfig.basePackage + ".font")
            val className = fontConfig.className ?:
                (font.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + "Font")

            val packageDir = javaOutput.resolve(`package`.replace('.', '/'))
            packageDir.mkdirs()

            val fontClass = packageDir.resolve("$className.java")

            fontClass.createNewFile()

            fontClass.outputStream().bufferedWriter().use { writer ->
                generateFontClass(
                    `package`,
                    className,
                    namespace,
                    font,
                    asset.inputStream().use { Json.decodeFromStream(it) },
                    fontConfig.spacePrefix,
                    writer
                )
            }
        }
    }
}