# gisketch's Command Palette

A NeoForge 1.21.1 client mod that adds a searchable command palette for Minecraft screens, keybind actions, and future mod integrations.

## What is included

- Minecraft `1.21.1`
- NeoForge `21.1.228`
- Kotlin JVM `2.3.0`
- Kotlin For Forge NeoForge `5.11.0`
- Architectury API NeoForge `13.0.8`
- owo-lib `0.12.15.5-beta.1+1.21`
- Java toolchain `21`
- Gradle Kotlin DSL
- Ctrl+K default keybind, editable in Minecraft controls
- `/editPalette` client command for selecting palette actions
- JSON palette store in `config/gisketchs_command_palette/palette.json`
- Public `RegisterPaletteActionsEvent` for adding actions from other code

## Use

Press Ctrl+K in game to open the palette. Run `/editPalette` to choose which discovered actions appear first in the palette.

## Build and run

```bash
./gradlew build
./gradlew runClient
```

On Linux ARM64, use:

```bash
./scripts/run-client.sh
```

First Gradle run downloads Minecraft, NeoForge, mappings, and dependencies.