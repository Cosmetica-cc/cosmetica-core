# Cosmetica Core

The base mod for Cosmetica clients. Exposes an API to authenticate users with the Cosmetica servers, connects players to the websocket, and shows cool cosmetics on players.

This is intended as a base for other mods, such as clients, to build upon. The [official Cosmetica mod](https://github.com/Cosmetica-cc/Cosmetica-2) depends on this as well.

## Using this in your projects

Cosmetica Core can be included from the Cosmetica Maven. Check the maven website for the latest version for your Minecraft version and platform.

Adding Cosmetica Maven to repositories:
```groovy
repositories {
    maven { url 'https://maven.cloaks.gg/' }
}
```

Adding the mod as a dependency in an Architectury project:

We highly recommend bundling this with Jar-In-Jar or Shadow as well (not shown here).
### Common
```groovy
dependencies {
    modImplementation "cc.cosmetica:cosmetica-core:${rootProject.cosmetica_core_version}-${rootProject.minecraft_version}"
}
```
### Fabric
```groovy
dependencies {
    modImplementation "cc.cosmetica:cosmetica-core-fabric:${rootProject.cosmetica_core_version}-${rootProject.minecraft_version}"
}
```
### Forge
```groovy
dependencies {
    modImplementation "cc.cosmetica:cosmetica-core-forge:${rootProject.cosmetica_core_version}-${rootProject.minecraft_version}"
}
```

### NeoForge
```groovy
dependencies {
    modImplementation "cc.cosmetica:cosmetica-core-neoforge:${rootProject.cosmetica_core_version}-${rootProject.minecraft_version}"
}
```
