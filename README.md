# Text Placeholder API — Forge port (unofficial)

> [!IMPORTANT]
> **This is an unofficial port.** It is not made, endorsed, or supported by the original author
> (Patbox). It is a community port of the Fabric-only
> [Text Placeholder API](https://modrinth.com/mod/placeholder-api) to Minecraft 1.20.1 Forge, done
> to make the library available on the Forge loader.
>
> For the official Fabric version, bug reports about the original library, and the canonical
> documentation, go to the original project:
> **<https://github.com/Patbox/TextPlaceholderAPI>**
>
> This port tracks the original `2.1.4` release. Bug reports about placeholder behaviour itself
> are best filed against the upstream project; this repository is the place for problems specific
> to the Forge port.

| | |
| --- | --- |
| Minecraft | 1.20.1 |
| Loader | Forge 47.x (built against 47.4.10) |
| Mod id | `placeholderapi` (original: `placeholder-api`) |
| Upstream version | 2.1.4 |
| Licence | LGPL-3.0-only, same as upstream |

---

## About this API

It's a small, JIJ-able API that allows creation and parsing of placeholders within strings and
Minecraft text components. Placeholder API uses a simple format of `%modid:type%` or
`%modid:type data%` (`%modid:type/data%` prior to 1.19). It also includes a simple, general usage
text format intended for simplifying user input in configs, chats, and so on.

### For users

It allows users to configure multiple mods in a similar way without losing compatibility between
them. Placeholders allow changing what and where any information is present within compatible
mods.

Additionally, the Simplified Text Format allows styling text in a readable way without the
requirement of writing JSON manually or using generators.

- [Using placeholders](https://placeholders.pb4.eu/user/general)
- [Default placeholder list](https://placeholders.pb4.eu/user/default-placeholders)
- [Mod placeholder list](https://placeholders.pb4.eu/user/mod-placeholders)
- [QuickText](https://placeholders.pb4.eu/user/quicktext)
- [Simplified Text Format](https://placeholders.pb4.eu/user/text-format)

Mods generally using this API should bundle it, but an updated version can be downloaded from here
if needed.

### For developers

Using Placeholder API is a simple way to achieve good mod compatibility without having to
implement multiple mod-specific APIs. Additionally, the placeholder parsing system can be used for
replacing your own static (or dynamic) placeholders in text created by a player or read from a
config. Combined with the Simplified Text Format this allows creating a good user/admin
experience.

- [Getting started](https://placeholders.pb4.eu/dev/getting-started)
- [Adding placeholders](https://placeholders.pb4.eu/dev/adding-placeholders)
- [Parsing placeholders](https://placeholders.pb4.eu/dev/parsing-placeholders)
- [TextNodes and NodeParsers](https://placeholders.pb4.eu/dev/text-nodes)
- [Using Simplified Text Format (TextParserV1)](https://placeholders.pb4.eu/dev/text-format)

*[JIJ]: Jar-in-Jar*

---

## What this port changes

The Java API is a faithful port. The `eu.pb4.placeholders` package, every public type, and every
placeholder id (`%player%`, `%server%`, `%world%`, ...) are unchanged, so mods written against the
original library work on Forge without modification. The differences are loader plumbing only:

| Original (Fabric) | This port (Forge) |
| --- | --- |
| `net.fabricmc.loader.api.FabricLoader` | `eu.pb4.placeholders.impl.ForgePlatform` over `net.minecraftforge.fml.ModList` |
| `FabricLoader#isDevelopmentEnvironment()` | `FMLLoader.isProduction()` |
| `getModContainer(id).getMetadata().getVersion()` / `getName()` / `getDescription()` | `ModList.getModContainerById(id).getModInfo()` (`IModInfo`) |
| `getAllMods().size()` | `ModList.get().getMods().size()` |
| Minecraft version read from the loader's mod metadata | `SharedConstants.getCurrentVersion().getId()` |
| `fabric.mod.json` | `META-INF/mods.toml` |
| mod id `placeholder-api` | mod id `placeholderapi` |

All loader-specific logic is isolated in `eu.pb4.placeholders.impl.ForgePlatform`, so porting back
to another loader means swapping that one class.

Two places needed more than a rename:

- **`HoverNode#applyFormatting`** — `HoverEvent`'s constructor is generic over the action payload,
  so the payload is pinned down per branch instead of being passed as the node's own `T`.
- **`GeneralUtils`** — the legacy-translation flag now compares version strings instead of using
  Fabric's `Version` type.

## How the port was produced

The upstream jar ships against Fabric's *intermediary* namespace, while Forge compiles against
Mojang's official names. The sources in `src/main/java` were recovered mechanically rather than
rewritten by hand:

```
placeholder-api-2.1.4+1.20.1.jar
  │  tiny-remapper         intermediary -> obfuscated   (classpath: MC remapped to intermediary)
  ▼
stage1-obf.jar
  │  ForgeAutoRenamingTool  obfuscated -> Mojang names  (map: client.txt, reversed)
  ▼
remapped-mojang.jar
  │  Vineflower decompile
  ▼
src/main/java
```

The hop through the obfuscated namespace is deliberate: it is the one namespace both Mojang and
Fabric publish complete mappings for. `--record-fix` restores record accessors such as
`LiteralContents#text()` that ProGuard does not list. See `tools/README.md` to reproduce it.

## Building

Requires a JDK 17 toolchain; Gradle provisions one if needed.

```bash
./gradlew build          # jar + sources jar land in build/libs/
./gradlew runClient      # dev client
./gradlew runServer      # dev dedicated server
```

## Publishing

Tokens are read from the environment, so nothing secret is ever committed.

```bash
export CURSEFORGE_API_TOKEN=...   # https://www.curseforge.com/account/api-tokens
export MODRINTH_API_TOKEN=...     # https://modrinth.com/settings/pats
export CHANGELOG="..."            # optional release notes
./gradlew build publishMods
```

Project ids live in `gradle.properties`.

## Licence and attribution

This is a derivative work of [Text Placeholder API](https://github.com/Patbox/TextPlaceholderAPI)
by **Patbox**, which is licensed **LGPL-3.0-only**, so this port is distributed under the same
licence. The full licence text is in [`LICENSE`](LICENSE), and it is embedded in the released jar
as `LICENSE_placeholder-api`.

If you redistribute this port, LGPL-3.0 requires you to:

1. keep the licence and all copyright notices,
2. state that you changed the code — this port is a modification, and is marked unofficial,
3. offer the corresponding source of your version.

`./gradlew build` also produces `placeholderapi-<version>-sources.jar`, which is what satisfies
point 3. This repository is the corresponding source.
