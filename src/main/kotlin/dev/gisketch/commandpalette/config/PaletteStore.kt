package dev.gisketch.commandpalette.config

import com.google.gson.GsonBuilder
import dev.gisketch.commandpalette.CommandPaletteMod
import dev.gisketch.commandpalette.action.PaletteAction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.fml.loading.FMLPaths
import java.nio.file.Files
import java.nio.file.Path

data class PaletteEntry(
    var name: String? = null,
    var iconItem: String? = null,
)

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
    var entries: MutableMap<String, PaletteEntry> = mutableMapOf(),
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

    fun displayName(action: PaletteAction): String = data.entries[action.id.toString()]?.name
        ?.takeIf(String::isNotBlank)
        ?: action.title.string

    fun actionEntry(action: PaletteAction): PaletteEntry = data.entries.getOrPut(action.id.toString()) { PaletteEntry() }

    fun setDisplayName(action: PaletteAction, name: String) {
        val entry = actionEntry(action)
        entry.name = name.trim().takeIf(String::isNotBlank)
        pruneEntry(action)
        save()
    }

    fun setIconItem(action: PaletteAction, itemId: ResourceLocation) {
        actionEntry(action).iconItem = itemId.toString()
        save()
    }

    fun iconStack(action: PaletteAction): ItemStack? {
        val itemId = data.entries[action.id.toString()]?.iconItem ?: return null
        val item = runCatching { ResourceLocation.parse(itemId) }.getOrNull()
            ?.let { BuiltInRegistries.ITEM.getOptional(it).orElse(null) }
            ?: return null
        return item.takeUnless { it == Items.AIR }?.defaultInstance
    }

    fun visible(actions: List<PaletteAction>): List<PaletteAction> {
        val actionById = actions.associateBy { it.id.toString() }
        val pinned = data.pinned.mapNotNull(actionById::get)
        val rest = actions.filter { it.id.toString() !in data.pinned && it.id.toString() !in data.hidden }
        return pinned + rest
    }

    fun pinned(actions: List<PaletteAction>): List<PaletteAction> {
        val actionById = actions.associateBy { it.id.toString() }
        return data.pinned.mapNotNull(actionById::get)
    }

    fun remember(action: PaletteAction) {
        data.recent.remove(action.id.toString())
        data.recent.add(0, action.id.toString())
        while (data.recent.size > 20) data.recent.removeLast()
        save()
    }

    private fun pruneEntry(action: PaletteAction) {
        val entry = data.entries[action.id.toString()] ?: return
        if (entry.name.isNullOrBlank() && entry.iconItem.isNullOrBlank()) {
            data.entries.remove(action.id.toString())
        }
    }
}