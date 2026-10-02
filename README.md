# Text Placeholder API — Forge / NeoForge port (unofficial)

> [!IMPORTANT]
> **This is an unofficial port.** It is not made, endorsed, or supported by the original author
> (Patbox). It is a community port of the Fabric-only
> [Text Placeholder API](https://modrinth.com/mod/placeholder-api) to the Forge and NeoForge
> loaders, done to make the library available outside Fabric.
>
> For the official Fabric version, bug reports about the original library, and the canonical
> documentation, go to the original project:
> **<https://github.com/Patbox/TextPlaceholderAPI>**
>
> This port tracks the original `2.1.4` release. Bug reports about placeholder behaviour itself
> are best filed against the upstream project; this repository is the place for problems specific
> to the ports.

## Supported versions

Each target is a self-contained Gradle project so that the build stays as close to the vanilla
toolchain as possible.

| Minecraft | Loader | Project directory | Build | Runtime check |
| --- | --- | --- | --- | --- |
| 1.20.1 | Forge 47.x (47.4.10) | `.` (repo root) | ForgeGradle 6 | server reached `Done` |
| 1.21.1 | NeoForge 21.1.x (21.1.252) | [`port-1.21.1-neoforge/`](port-1.21.1-neoforge) | ModDevGradle 2.0.148, JDK 21 | server reached `Done` |
| 26.1.2 | NeoForge 26.1.2.x (26.1.2.112) | [`port-26.1.2-neoforge/`](port-26.1.2-neoforge) | ModDevGradle 2.0.148, JDK 25 | server reached `Done` |

All three register the same 59 built-in placeholders at load time, and all three were verified by
booting a dedicated server and confirming the registry and parser work.

Toolchain notes, if you are porting further:

* 1.21.1+ uses **ModDevGradle**, not ForgeGradle.
* NeoForge requires the metadata file to be named `META-INF/neoforge.mods.toml`. With
  `META-INF/mods.toml` the loader silently refuses to see the mod.
* 26.1.2 requires a **Java 25** toolchain; 1.21.1 is fine on 17 or 21.

| | |
| --- | --- |
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

| Original (Fabric) | This port (Forge / NeoForge) |
| --- | --- |
| `net.fabricmc.loader.api.FabricLoader` | `eu.pb4.placeholders.impl.ForgePlatform` over `ModList` |
| `FabricLoader#isDevelopmentEnvironment()` | `FMLLoader.isProduction()` on 1.20.1, `FMLEnvironment.isProduction()` on 26.1.2 |
| `getModContainer(id).getMetadata().getVersion()` / `getName()` / `getDescription()` | `ModList.getModContainerById(id).getModInfo()` (`IModInfo`) |
| `getAllMods().size()` | `ModList.get().getMods().size()` |
| Minecraft version read from the loader's mod metadata | `SharedConstants.getCurrentVersion().getId()`, `DetectedVersion.tryDetectVersion().name()` on 26.1.2 |
| `fabric.mod.json` | `META-INF/mods.toml` (Forge), `META-INF/neoforge.mods.toml` (NeoForge) |
| mod id `placeholder-api` | mod id `placeholderapi` |

All loader-specific logic is isolated in `eu.pb4.placeholders.impl.ForgePlatform`, so porting back
to another loader means swapping that one class.

Beyond that, the three targets diverge where Mojang's own API diverged. The changes that are not
mechanical renames:

- **`HoverNode#applyFormatting`** — `HoverEvent` is generic over the action payload on 1.20.1, and
  by 26.1.2 it is a sealed interface with one record per action (`ShowText`, `ShowItem`,
  `ShowEntity`, the last wrapping a new `ItemStackTemplate`). The payload is therefore chosen per
  branch on every target.
- **`ClickEvent`** — a single `(Action, String)` record on 1.20.1, a sealed interface with one
  record per action by 26.1.2, where payloads are typed (`URI`, `int`, a registry holder). Both
  directions go through `GeneralUtils.createClickEvent` / `clickEventValue`.
- **`GeneralUtils`** — the legacy-translation flag compares version strings rather than using
  Fabric's `Version` type, and the tag parser is fed a registry lookup because 26.1 requires a
  `HolderLookup.Provider` for several serialisation entry points.
- **Text contents** — the `nbt` and `selector` node payloads, and the block/entity data sources,
  became `CompilableString` in 26.1, and `TagParser#parseTag` and `ItemStack#parse` are gone. The
  node records keep their original `String` shape and compile on the way out, so the public API is
  unchanged.


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

Each project directory builds independently and produces the same-shaped artifacts.

```bash
# 1.20.1 Forge  (this directory)      - needs a JDK 17 toolchain
./gradlew build && ./gradlew runServer

# 1.21.1 NeoForge
cd port-1.21.1-neoforge && ./gradlew build && ./gradlew runServer

# 26.1.2 NeoForge                     - needs a JDK 25 toolchain
cd port-26.1.2-neoforge && ./gradlew build && ./gradlew runServer
```

Gradle provisions a toolchain if the one you ask for is not installed.

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
