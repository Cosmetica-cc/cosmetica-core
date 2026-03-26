# Cosmetica Core

The base mod for Cosmetica clients. Exposes an API to authenticate users with the Cosmetica servers, connects players to the websocket, and shows cool cosmetics on players.

This is intended as a base for other mods, such as clients, to build upon. The [official Cosmetica mod](https://github.com/Cosmetica-cc/Cosmetica-2) depends on this as well.

## Using this in your projects

Cosmetica Core can be included from the Cosmetica Maven. Check the maven website for the latest version for your Minecraft version and platform.

Adding Cosmetica Maven to repositories:
```groovy
repositories {
    maven { url 'https://maven.cosmetica.cc/' }
}
```

Adding the mod as a dependency in an Architectury project:

We highly recommend bundling this with Jar-In-Jar or Shadow as well (not shown here).
### Common
```groovy
dependencies {
    implementation "cc.cosmetica:kupe:${rootProject.kupe_version}-${rootProject.minecraft_version}"
}
```
### Fabric
```groovy
dependencies {
    implementation "cc.cosmetica:kupe-fabric:${rootProject.kupe_version}-${rootProject.minecraft_version}"
}
```
### Forge
```groovy
dependencies {
    implementation "cc.cosmetica:kupe-forge:${rootProject.kupe_version}-${rootProject.minecraft_version}"
}
```

### NeoForge
```groovy
dependencies {
    implementation "cc.cosmetica:kupe-neoforge:${rootProject.kupe_version}-${rootProject.minecraft_version}"
}
```