# Resource Pack Processor

### Features
- [x] Building resource pack (buildResourcePack)
  - [x] Archiving the resource pack & producing a hash
  - [x] Generating an output as a directory
  - [x] Ignoring files (.rppignore)
    - [x] Global exclusions
    - [x] Relative exclusions
  - [x] JSON minification
  - [ ] Sound/image compression
- [x] HTTP Server (intended for development only) (startResourcePackServer)
  - [x] Serves hash & resource pack
  - [x] Real-time update stream (SSE implementation)
  - [x] Watches for file changes and sends event to clients
- [x] Watch mode (watchResourcePack)
  - Watches for file changes, recompiling the resource pack when updated
  - Useful for symlinking the output directory into the Minecraft resource pack directory
- [x] Client library for interacting with the SSE stream

### Plugin Configuration
```kt
plugins {
    id("net.bladehunt.rpp") version "<version>"
}

rpp {
    minifyJson = true
    sourceDirectory = "./src/main/rpp"
    outputName = "resource_pack_$version"
  
    server {
        address = InetSocketAddress("127.0.0.1", 8000)
    }
}
```

### Client Usage
```kt
// This assumes a default RPP configuration with Minestom
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
