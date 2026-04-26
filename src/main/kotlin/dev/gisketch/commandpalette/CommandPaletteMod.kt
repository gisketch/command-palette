package dev.gisketch.commandpalette

import com.mojang.logging.LogUtils
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import org.slf4j.Logger

@Mod(value = CommandPaletteMod.MOD_ID, dist = [Dist.CLIENT])
class CommandPaletteMod(modBus: IEventBus, container: ModContainer) {
    init {
        LOGGER.info("Loading {}", container.modInfo.displayName)
        CommandPaletteContent.register(modBus)
        CommandPaletteClient.register(modBus)
    }

    companion object {
        const val MOD_ID = "gisketchs_command_palette"
        val LOGGER: Logger = LogUtils.getLogger()
    }
}