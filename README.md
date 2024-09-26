# Resource Pack Processor

### Features
- [ ] Building resource pack (buildResourcePack)
  - [x] Archiving the resource pack & producing a hash
  - [x] Generating an output as a directory
  - [ ] Ignoring files
- [x] HTTP Server (intended for development only) (startResourcePackServer)
  - [x] Serves hash & resource pack
  - [x] Resource pack hash SSE stream for development
  - [x] Watches for file changes and sends event to clients
- [x] Watch mode (watchResourcePack)
  - Watches for file changes, recompiling the resource pack when updated
  - Useful for symlinking the output directory into the Minecraft resource pack directory

Using the plugin (currently not on the gradle plugin portal)
```kt
plugins {
    id("net.bladehunt.rpp") version "<version>"
}

rpp {
    sourceDirectory = "./src/main/rpp"
    outputName = "resource_pack_$version"
  
    server {
        address = InetSocketAddress("127.0.0.1", 8000)
    }
}
```