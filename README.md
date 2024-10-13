# Resource Pack Processor
The *best* Gradle plugin that assists in building resource packs for development.

## Plugin Overview
### Tasks
- **buildResourcePack** - Generates an output and generates code based on 
- **watchResourcePack** - Watches the source directory for file changes, then rebuilds the resource pack
- **startResourcePackServer** - Hosts an HTTP server for local development. In conjunction with the client library, resource packs can be iterated upon extremely easily.

### Configuration
```kt
plugins {
    id("net.bladehunt.rpp") version "<version>"
}

rpp {
    sourceDirectory = "src/main/rpp"
  
    outputName = "resource_pack_$version"
  
    // Enables codegen. Optional configuration
    codegen()
    
    // Codegen config example
    codegen {
        priority = // Priority 
        
        // Adding custom code generators
        generators += // ...
    }
  
    processJson(transpileJsonc = true, minify = true)
  
    // Creates a spaces font. If codegen is enabled, it also allows for generating a utility
    spaces(font = Resource("rpp", "spaces"), amount = 16)
  
    server {
        address = InetSocketAddress("127.0.0.1", 8000)
    }
    
    // If you would like to use custom processors, add them
    archiveProcessors += // ...
    outputProcessors += // ...
    fileProcessors += // ...
}
```

### Codegen

To start, place a file called `codegen.jsonc` in the resource pack sources.

```json5
// Example configuration
{
  "basePackage": "org.example.generated",
  // The default package is basePackage.spaces
  "spaces": [
    {
      "font": "rpp:spaces",
      "className": "Spaces", // Required
      "packageOverride": "org.example.spaces" // Optional. Overrides the basePackage
    },
  ],
  // The default package is basePackage.font
  "fonts": [
    {
      "font": "custom_font", // This uses the default namespace, minecraft
      "className": "CustomFontName", // Optional. If not set, it will be the capitalized font name + "Font",
      "spacePrefix": "SPACE", // Optional. If a space provider is found, the generated fields will start with this value.
      "packageOverride": "org.example.other.generated" // Optional. Overrides the basePackage
    },
    {
      "font": "namespace:font"
    }
  ]
}
```

Currently, Font generation is only done with bitmap and space provider types. This may change in the future. The appropriate space font must be defined in the plugin configuration to be generated.

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
