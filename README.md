# Cosmetica Core

The base mod for Cosmetica clients. Authenticates users with the Cosmetica servers and shows cool cosmetics on players.

This is intended as a base for other mods, such as clients, to build upon. The official Cosmetica mod depends on this as well.

## Client Name

When developing a mod that uses Cosmetica Core, a client name must be provided. This can be provided in your
`fabric.mod.json` or `mods.toml` file, on Fabric/Forge respectively.

### Fabric

fabric.mod.json:
```json
{
  ...
  "custom": {
    "cosmetica-client": "my-client"
  }
}
```

### Forge

mods.toml:
```toml
[[mods]]
cosmetica-client="my-client"
```

## Developing Core

As core does not provide its own client string, you can provide a client string when testing core by setting `-Dcosmetica.client=core`
in your java arguments.

(TODO: make gradle automatically set this up in run configurations!)
