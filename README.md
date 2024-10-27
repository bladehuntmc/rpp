# Resource Pack Processor
The *best* Gradle plugin that assists in building resource packs for development.

## Plugin Overview
### Tasks
- **buildResourcePack** - Generates an output and code.
- **cleanResourcePack** - Deletes the output directory.
- **watchResourcePack** - Watches the source directory for file changes, then rebuilds the resource pack.
- **startResourcePackServer** - Hosts an HTTP server for local development. In conjunction with the client library, resource packs can be iterated upon easily.

### Configuration
```kt
plugins {
    id("net.bladehunt.rpp") version "<version>"
}

rpp {
    // Default source directory
    sourceDirectory = "src/main/rpp"
  
    // Sets the output name of the archive and file containing the hash
    baseArchiveName = "resource_pack_$version"
  
    // processJson() adds a FileProcessor that minifies Json. As a result of parsing, this also acts as a strict
    // JSON validator. This also transpiles .jsonc to .json, .mcmeta.json to .mcmeta, and .mcmeta.jsonc to .mcmeta
    processJson()
    
    // This will take a font file, then generate a corresponding class that allows for easy use.
    generateFontClasses(matcher = Regex(...) /* Optional. If you're using a custom file format, you can create a regex to match them here */)
  
    // Creates a spaces font & optional utility class.
    generateSpaces(
        resource = Resource("rpp:spaces"),
        charCount = 8, // This will generate characters for 1, 2, 4, 8, 16, 32, 64, 128, and their corresponding negatives
        `package` = "net.bladehunt.rpp.generated.spaces", // Optional: Allows for generating of the utility class for creating spaces
        className = "Spaces" // If package is defined, this must also be defined
    )
  
    server {
        address = InetSocketAddress("127.0.0.1", 8000)
        
        // If you would like to change the archive ID that is served, change it here
        // Leaving it unchanged uses "default"
        archiveId = "my_custom_id"
    }
    
    // If you would like to use custom processors, add them
    fileProcessor.add(MyFileProcessor)
    
    outputProcessor.add(MyOutputProcessor)
    
    archiveProcessors.add(MyArchiveProcessor)
}
```

### Codegen

Currently, Font generation is only done with bitmap and space provider types.

To start, place a codegen configuration in your font's definition.
```json5
{
  "codegen": {
    "package": "net.bladehunt.rpp.generated.font",
    "className": "MyFontClass"
  },
  "providers": [
    {
      "type": "bitmap",
      "file": "mygame:sprite/image.png",
      "ascent": 7,
      "height": 7,
      "name": "IMAGE", // This will generate an IMAGE field that uses the first character
      "chars": [
        "\uE000"
      ]
    }
  ]
}
```

### Processors

Processors are a huge part of RPP, hence the name Resource Pack _Processor_. Two different types of processors are supported by RPP.

* FileProcessor
  * Outputs are tracked by RPP, so subsequent builds in the same _session_ only build the necessary files.
* PostProcessor
  * These processors are executed after certain steps. In the plugin configuration, there are two different fields: outputProcessors & archiveProcessors
  * All PostProcessors in outputProcessors get executed after all files are copied & all FileProcessors have been executed, but before the default archive is generated.
  * All PostProcessors in archiveProcessors get executed after the default archive is generated.

Building your own processor is relatively simple. The recommended method is putting it in your `buildSrc` to stay organized.

Here is an example of a simple FileProcessor that copies the values in `assets/minecraft/lang/all.json` to all the defined language codes
```kt
private val LANGUAGE_CODES = arrayOf(
    "af_za",
    "ar_sa",
    // put any other language codes
)

class LanguageProcessor : FileProcessor<Nothing?> {
    override fun BuildScope.process(file: FileData) {
        // This is a "lazy" approach
        // The recommended method is processing all the outputs
        val source = file.outputs.firstOrNull() ?: return

        // Decode the JSON. Rpp provides a kotlinx.serialization.json.Json instance that ignores comments & trailing commas
        val all = try {
            source.inputStream().use { Json.decodeFromStream<Map<String, String>>(it) }
        } catch (_: SerializationException) {
            return
        }
        
        // Make sure all the fonts directory exists
        val fonts = rpp.layout.build.output.resolve("assets/minecraft/lang")
        fonts.mkdirs()

        LANGUAGE_CODES.forEach { language ->
            val langFile = fonts.resolve("$language.json")
            
            // Load the current language file or create a new one
            val output = if (langFile.exists()) {
                try {
                    val input = langFile.inputStream().use {
                        Json.decodeFromStream<MutableMap<String, String>>(it)
                    }
                    all.forEach { (k, v) ->
                        input.putIfAbsent(k, v)
                    }
                    input
                } catch (_: SerializationException) {
                    return@forEach
                }
            } else {
                langFile.createNewFile() // Create the new output
                file.outputs.add(langFile) // Track the output
                all
            }

            // Encode the output to the file
            langFile.outputStream().use { Json.encodeToStream(output, it) }
        }
        
        source.delete() // Delete the source
        file.outputs.remove(source) // Untrack it
    }

    // Checks if the processor should execute.
    // If you would like to do a more complex analysis, you can always just return in .process
    override fun BuildScope.shouldExecute(file: FileData): Boolean =
        file.source.absolutePath.endsWith("assets/minecraft/lang/all.json")

    // Contexts are shared throughout a session.
    // They can be accessed just by calling getOrCreateSession() in .process
    override fun createContext(rpp: ResourcePackProcessor): Nothing? = null
}
```

## Client Overview
The client library is not strictly necessary to use RPP, but it improves development experience. Currently, it requires SLF4J to function.

### Installation
```kt
repositories {
    maven("https://mvn.bladehunt.net/releases")
}

dependencies {
    implementation("net.bladehunt:rpp:<version>")
}
```

### Example (Minestom/Adventure)
```kt
fun main() {
    val server = MinecraftServer.init()

    val rppClient = RppClient(URI("http://127.0.0.1:8000")) { uri, hash ->
        // This assumes default RPP configuration with Minestom

        Audiences.server().sendResourcePacks(
            ResourcePackRequest.resourcePackRequest()
                .packs(
                    ResourcePackInfo.resourcePackInfo()
                        .uri(uri)
                        .id(UUID.randomUUID())
                        .hash(hash.removeSuffix("\n")) // In some cases, a newline will be at the end of the hash. This will be fixed in an upcoming release
                        .build()
                )
                .prompt(text("Please use the pack"))
                .replace(true) // Replace the packs
                .required(true)
                .build()
        )
    }

    rppClient.start() // Starts on another thread. You can call .join() on the return of .start() to block.

    server.start("127.0.0.1", 25565)
}
```
