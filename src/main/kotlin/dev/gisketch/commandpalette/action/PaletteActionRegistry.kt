package dev.gisketch.commandpalette.action

import dev.gisketch.commandpalette.CommandPaletteMod
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event
import net.neoforged.neoforge.common.NeoForge

class RegisterPaletteActionsEvent(private val registry: PaletteActionRegistry) : Event() {
    fun register(action: PaletteAction) = registry.register(action)
}

object PaletteActionRegistry {
    private val actions = linkedMapOf<ResourceLocation, PaletteAction>()

    fun refresh() {
        actions.clear()
        BuiltinPaletteActions.register(this)
        KeyMappingPaletteActions.register(this)
        NeoForge.EVENT_BUS.post(RegisterPaletteActionsEvent(this))
    }

    fun register(action: PaletteAction) {
        actions[action.id] = action
    }

    fun all(): List<PaletteAction> = actions.values.sortedWith(compareBy({ it.category.toString() }, { it.title.string }))

    fun search(query: String, limit: Int = 24): List<PaletteAction> = all()
        .asSequence()
        .filter { it.matches(query) }
        .take(limit)
        .toList()

    fun byId(id: String): PaletteAction? = runCatching { ResourceLocation.parse(id) }.getOrNull()?.let(actions::get)

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(CommandPaletteMod.MOD_ID, path)
}