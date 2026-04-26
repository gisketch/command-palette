package dev.gisketch.commandpalette.action

import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

object KeyMappingPaletteActions {
    private val CATEGORY: ResourceLocation = PaletteActionRegistry.id("category/key_mappings")

    fun register(registry: PaletteActionRegistry) {
        Minecraft.getInstance().options.keyMappings.forEach { mapping ->
            registry.register(
                PaletteAction(
                    id = PaletteActionRegistry.id("key/${mapping.name.toResourcePath()}"),
                    title = Component.translatable(mapping.name),
                    description = Component.literal("Trigger key mapping: ${mapping.translatedKeyMessage.string}"),
                    category = CATEGORY,
                    sourceModId = mapping.category.substringAfter("key.category.").substringBefore('.'),
                    tags = setOf("keybind", mapping.category, mapping.saveString()),
                    execute = { KeyMapping.click(mapping.key) },
                ),
            )
        }
    }

    private fun String.toResourcePath(): String = lowercase()
        .replace("key.", "")
        .replace('.', '/')
        .replace(Regex("[^a-z0-9/._-]"), "_")
        .trim('/')
        .ifBlank { "unknown" }
}