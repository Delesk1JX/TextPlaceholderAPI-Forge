# Changelog

All notable changes to this Forge port. This project tracks the upstream
[Text Placeholder API](https://github.com/Patbox/TextPlaceholderAPI); entries are grouped by the
upstream release they port.

## 2.1.4 (unofficial Forge port)

First Forge release. Ports the upstream `2.1.4` Fabric build to Minecraft 1.20.1 / Forge 47.x.

### Added

- `@Mod` entrypoint (`eu.pb4.placeholders.api.PlaceholderApiForge`) so Forge loads the library and
  the built-in `%player%`, `%server%` and `%world%` placeholders register during mod construction.
- `META-INF/mods.toml` and `pack.mcmeta` in place of `fabric.mod.json`.
- `mod id` is `placeholderapi`; upstream uses `placeholder-api`.

### Changed

- Loader-specific calls routed through the new `eu.pb4.placeholders.impl.ForgePlatform`:
  - `FabricLoader#isDevelopmentEnvironment()` -> `FMLLoader.isProduction()`
  - mod metadata lookups -> `ModList.getModContainerById(id).getModInfo()`
  - `getAllMods().size()` -> `ModList.get().getMods().size()`
- `GeneralUtils.IS_LEGACY_TRANSLATION` now derives the Minecraft version from
  `SharedConstants.getCurrentVersion().getId()` instead of Fabric loader metadata.

### Fixed

- `HoverNode#applyFormatting` pins the `HoverEvent` payload per action branch; passing the node's
  own `T` does not satisfy the constructor's generic bound.

### Unchanged

- The public API, the `eu.pb4.placeholders` package and every placeholder id. Mods written against
  the upstream library need no changes.
