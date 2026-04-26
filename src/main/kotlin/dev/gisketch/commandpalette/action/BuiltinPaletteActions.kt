package dev.gisketch.commandpalette.action

import dev.gisketch.commandpalette.CommandPaletteMod
import dev.gisketch.commandpalette.ui.CommandPaletteScreen
import dev.gisketch.commandpalette.ui.PaletteEditorScreen
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.gui.screens.options.controls.ControlsScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.client.gui.ModListScreen

object BuiltinPaletteActions {
    private val CATEGORY: ResourceLocation = PaletteActionRegistry.id("category/built_in")

    fun register(registry: PaletteActionRegistry) {
        registry.register(screenAction("open_palette", "Open Command Palette", "Open the searchable action palette") {
            it.minecraft.setScreen(CommandPaletteScreen())
        })
        registry.register(screenAction("edit_palette", "Edit Palette", "Choose which actions appear in the palette") {
            it.minecraft.setScreen(PaletteEditorScreen())
        })
        registry.register(screenAction("options", "Open Options", "Open Minecraft options", "minecraft") {
            it.minecraft.setScreen(OptionsScreen(parentScreen(it), it.minecraft.options))
        })
        registry.register(screenAction("controls", "Open Controls", "Edit Minecraft and mod key mappings", "minecraft") {
            it.minecraft.setScreen(ControlsScreen(parentScreen(it), it.minecraft.options))
        })
        registry.register(screenAction("mods", "Open Mods", "Open the NeoForge mod list", "neoforge") {
            it.minecraft.setScreen(ModListScreen(parentScreen(it)))
        })
        registry.register(screenAction("close_screen", "Close Current Screen", "Return to the game or previous context", "minecraft") {
            it.minecraft.setScreen(null)
        })
    }

    private fun screenAction(
        path: String,
        title: String,
        description: String,
        sourceModId: String = CommandPaletteMod.MOD_ID,
        execute: (PaletteExecutionContext) -> Unit,
    ): PaletteAction = PaletteAction(
        id = PaletteActionRegistry.id(path),
        title = Component.literal(title),
        description = Component.literal(description),
        category = CATEGORY,
        sourceModId = sourceModId,
        tags = setOf("built-in", "screen", title.lowercase()),
        execute = execute,
    )

    private fun parentScreen(context: PaletteExecutionContext): Screen = if (context.minecraft.level != null) {
        PauseScreen(true)
    } else {
        context.originScreen ?: TitleScreen()
    }
}