package dev.gisketch.commandpalette

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.gisketch.commandpalette.action.PaletteActionRegistry
import dev.gisketch.commandpalette.config.PaletteStore
import dev.gisketch.commandpalette.ui.CommandPaletteScreen
import dev.gisketch.commandpalette.ui.PaletteEditorScreen
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandSourceStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.client.settings.KeyModifier
import net.neoforged.neoforge.common.NeoForge

object CommandPaletteClient {
    private const val CATEGORY = "key.category.${CommandPaletteMod.MOD_ID}.command_palette"

    val OPEN_PALETTE: KeyMapping = KeyMapping(
        "key.${CommandPaletteMod.MOD_ID}.open_palette",
        KeyConflictContext.UNIVERSAL,
        KeyModifier.CONTROL,
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_K,
        CATEGORY,
    )

    fun register(modBus: IEventBus) {
        PaletteStore.load()
        PaletteActionRegistry.refresh()
        modBus.addListener(::registerKeyMappings)
        NeoForge.EVENT_BUS.addListener(::registerClientCommands)
        NeoForge.EVENT_BUS.addListener(::onClientTick)
    }

    private fun registerKeyMappings(event: RegisterKeyMappingsEvent) {
        event.register(OPEN_PALETTE)
    }

    private fun registerClientCommands(event: RegisterClientCommandsEvent) {
        event.dispatcher.register(
            LiteralArgumentBuilder.literal<CommandSourceStack>("editPalette")
                .executes {
                    Minecraft.getInstance().execute { Minecraft.getInstance().setScreen(PaletteEditorScreen()) }
                    1
                },
        )
    }

    private fun onClientTick(event: ClientTickEvent.Post) {
        while (OPEN_PALETTE.consumeClick()) {
            Minecraft.getInstance().setScreen(CommandPaletteScreen())
        }
    }
}