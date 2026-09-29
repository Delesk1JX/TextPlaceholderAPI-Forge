# Remapping pipeline

The upstream Fabric jar ships against Fabric's **intermediary** namespace, while Forge compiles
against Mojang's official names. `tools/` holds the one-time pipeline used to bridge the two and
recover readable sources. You only need it if you want to re-derive the port; a normal
`./gradlew build` does not use any of it.

## What it does

```
placeholder-api-2.1.4+1.20.1.jar
  │  tiny-remapper   intermediary -> obfuscated   (classpath: MC remapped to intermediary)
  ▼
stage1-obf.jar
  │  ForgeAutoRenamingTool  obfuscated -> mojang  (map: client.txt, reversed)
  ▼
remapped-mojang.jar
  │  Vineflower decompile
  ▼
decompiled/   ->   src/main/java
```

The two-step hop through the obfuscated namespace is deliberate: it is the one namespace both
Mojang and Fabric publish complete mappings for, so neither remapper has to be taught to speak
two mapping formats at once.

Record accessors (for example `LiteralContents#text()`) survive because the ART step runs with
`--record-fix`; ProGuard only lists the backing field, not the accessor the compiler synthesises.

## Reproducing it

```powershell
$jdk = "C:\Program Files\Java\jdk-21.0.11\bin"
$w   = "<this project>"

# Inputs
#   tools/mappings/client-1.20.1.txt     Mojang client mappings (piston-meta)
#   tools/mappings/intermediary.tiny     Fabric intermediary mapping (maven.fabricmc.net)
#   tools/libs/mc-client-1.20.1-obf.jar  Mojang client jar, as the obfuscated classpath

# Build an intermediary-named MC jar so tiny-remapper can resolve the class hierarchy
& "$jdk\java.exe" -Xmx4G -jar tools\libs\tiny-remapper-0.14.1-fat.jar `
    tools\libs\mc-client-1.20.1-obf.jar tools\libs\mc-1.20.1-intermediary.jar `
    tools\mappings\intermediary.tiny official intermediary tools\libs\mc-client-1.20.1-obf.jar

# intermediary -> obfuscated
& "$jdk\java.exe" -Xmx4G -jar tools\libs\tiny-remapper-0.14.1-fat.jar `
    ..\placeholder-api-2.1.4+1.20.1.jar stage1-obf.jar `
    tools\mappings\intermediary.tiny intermediary official tools\libs\mc-1.20.1-intermediary.jar

# obfuscated -> Mojang names
& "$jdk\java.exe" -Xmx4G -jar tools\libs\ForgeAutoRenamingTool-1.1.2-all.jar `
    --input stage1-obf.jar --output remapped-mojang.jar `
    --map tools\mappings\client-1.20.1.txt --lib tools\libs\mc-client-1.20.1-obf.jar `
    --reverse --record-fix --strip-sigs
```

`ProguardToTiny.java` in this directory converts `client.txt` to tiny. It is **not** part of the
build any more - ForgeAutoRenamingTool reads ProGuard mappings directly, which is the path worth
using. It is kept only because it is what was used to diagnose tiny-remapper's descriptor-namespace
requirement (tiny member descriptors must be written in the namespace of the input classes).
