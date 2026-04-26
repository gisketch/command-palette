package dev.gisketch.commandpalette.config

import com.google.gson.GsonBuilder
import dev.gisketch.commandpalette.CommandPaletteMod
import dev.gisketch.commandpalette.action.PaletteAction
import net.neoforged.fml.loading.FMLPaths
import java.nio.file.Files
import java.nio.file.Path

data class PaletteData(
    var schemaVersion: Int = 1,
    var pinned: MutableList<String> = mutableListOf(
        "gisketchs_command_palette:open_palette",
        "gisketchs_command_palette:edit_palette",
        "gisketchs_command_palette:options",
        "gisketchs_command_palette:controls",
        "gisketchs_command_palette:mods",
    ),
    var hidden: MutableList<String> = mutableListOf(),
    var aliases: MutableMap<String, MutableList<String>> = mutableMapOf(),
    var recent: MutableList<String> = mutableListOf(),
)

object PaletteStore {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configDir: Path = FMLPaths.CONFIGDIR.get().resolve(CommandPaletteMod.MOD_ID)
    private val paletteFile: Path = configDir.resolve("palette.json")

    var data: PaletteData = PaletteData()
        private set

    fun load() {
        Files.createDirectories(configDir)
        data = if (Files.exists(paletteFile)) {
            runCatching { gson.fromJson(Files.readString(paletteFile), PaletteData::class.java) }.getOrElse { PaletteData() }
        } else {
            PaletteData()
        }
        save()
    }

    fun save() {
        Files.createDirectories(configDir)
        Files.writeString(paletteFile, gson.toJson(data))
    }

    fun isPinned(action: PaletteAction): Boolean = action.id.toString() in data.pinned

    fun pin(action: PaletteAction) {
        data.hidden.remove(action.id.toString())
        if (!isPinned(action)) data.pinned.add(action.id.toString())
        save()
    }

    fun unpin(action: PaletteAction) {
        data.pinned.remove(action.id.toString())
        save()
    }

    fun visible(actions: List<PaletteAction>): List<PaletteAction> {
        val actionById = actions.associateBy { it.id.toString() }
        val pinned = data.pinned.mapNotNull(actionById::get)
        val rest = actions.filter { it.id.toString() !in data.pinned && it.id.toString() !in data.hidden }
        return pinned + rest
    }

    fun remember(action: PaletteAction) {
        data.recent.remove(action.id.toString())
        data.recent.add(0, action.id.toString())
        while (data.recent.size > 20) data.recent.removeLast()
        save()
    }
}