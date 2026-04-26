package dev.gisketch.commandpalette.action

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

enum class PaletteContext {
    CLIENT,
    IN_GAME,
    IN_SCREEN,
}

data class PaletteExecutionContext(
    val minecraft: Minecraft = Minecraft.getInstance(),
    val originScreen: Screen? = Minecraft.getInstance().screen,
)

data class PaletteAction(
    val id: ResourceLocation,
    val title: Component,
    val category: ResourceLocation,
    val execute: (PaletteExecutionContext) -> Unit,
    val description: Component = Component.empty(),
    val sourceModId: String = id.namespace,
    val tags: Set<String> = emptySet(),
    val contexts: Set<PaletteContext> = setOf(PaletteContext.CLIENT),
) {
    fun matches(query: String): Boolean {
        val haystacks = listOf(id.toString(), title.string, description.string, category.toString(), sourceModId) + tags
        return PaletteSearch.matches(query, haystacks)
    }
}