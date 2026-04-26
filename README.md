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

## Resource-pack UI skinning

The palette currently uses owo-lib surfaces in code (`Surface.VANILLA_TRANSLUCENT`, `Surface.DARK_PANEL`, `Surface.PANEL`, and `Surface.PANEL_INSET`) rather than hard-coded image files. To make the dialog or background fully resource-packable, replace the relevant surface call in `CommandPaletteScreen.kt` and `PaletteEditorScreen.kt` with `Surface.tiled(ResourceLocation.fromNamespaceAndPath("gisketchs_command_palette", "textures/gui/<name>.png"), width, height)` or a texture component, then ship a default texture in the mod. The background hook is the root component surface; the dialog hook is the main panel surface.

A resource pack can override those assets at the same paths, for example:

```text
resource-pack/
	pack.mcmeta
	assets/
		gisketchs_command_palette/
			textures/
				gui/
					palette_background.png
					palette_dialog.png
					palette_panel.png
```

For Minecraft 1.21.1, a minimal `pack.mcmeta` looks like:

```json
{
	"pack": {
		"pack_format": 34,
		"description": "Command Palette UI skin"
	}
}
```

Use the vanilla client jar as the reference source for Minecraft UI textures. In a Gradle workspace, look under `~/.gradle/caches` for the extracted Minecraft client or open the downloaded client jar and inspect `assets/minecraft/textures/gui/`. Good references for this style are the options/menu background textures, widget textures, and any nine-slice-like panel art you want to match. Copy a reference texture into your resource pack, edit it in an image editor, keep the same canvas size or update the `Surface.tiled(...)` tile dimensions, and reload resources in-game with F3+T.