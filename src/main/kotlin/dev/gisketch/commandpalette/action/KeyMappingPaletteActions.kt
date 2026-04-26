package dev.gisketch.commandpalette.action

import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

object KeyMappingPaletteActions {
    private val CATEGORY: ResourceLocation = PaletteActionRegistry.id("category/key_mappings")
    private val categoryModIdPattern = Regex("""key\.categor(?:y|ies)\.([a-z0-9_\-.]+)""")
    private val nameModIdPattern = Regex("""key\.([a-z0-9_\-.]+)\.""")

    fun register(registry: PaletteActionRegistry) {
        allMappings().forEach { mapping ->
            registry.register(
                PaletteAction(
                    id = PaletteActionRegistry.id("key/${mapping.name.toResourcePath()}"),
                    title = Component.translatable(mapping.name),
                    description = Component.literal("Trigger key mapping: ${mapping.translatedKeyMessage.string}"),
                    category = CATEGORY,
                    sourceModId = sourceModId(mapping),
                    tags = setOf("keybind", mapping.category, mapping.name, mapping.saveString()),
                    execute = { KeyMapping.click(mapping.key) },
                ),
            )
        }
    }

    private fun allMappings(): List<KeyMapping> {
        val optionMappings = Minecraft.getInstance().options.keyMappings.toList()
        val registeredMappings = runCatching {
            val field = KeyMapping::class.java.getDeclaredField("ALL")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (field.get(null) as Map<String, KeyMapping>).values.toList()
        }.getOrDefault(emptyList())

        return (optionMappings + registeredMappings)
            .distinctBy(KeyMapping::getName)
            .sortedBy(KeyMapping::getName)
    }

    private fun sourceModId(mapping: KeyMapping): String {
        val candidates = listOf(mapping.category, mapping.name)
        candidates.forEach { candidate ->
            categoryModIdPattern.find(candidate)?.groupValues?.getOrNull(1)
                ?.substringBefore('.')
                ?.takeUnless(::isVanillaCategory)
                ?.let { return it }
            nameModIdPattern.find(candidate)?.groupValues?.getOrNull(1)
                ?.substringBefore('.')
                ?.takeUnless(::isVanillaCategory)
                ?.let { return it }
        }
        return "minecraft"
    }

    private fun isVanillaCategory(modId: String): Boolean = modId in setOf(
        "categories",
        "movement",
        "gameplay",
        "inventory",
        "creative",
        "multiplayer",
        "ui",
        "misc",
    )

    private fun String.toResourcePath(): String = lowercase()
        .replace("key.", "")
        .replace('.', '/')
        .replace(Regex("[^a-z0-9/._-]"), "_")
        .trim('/')
        .ifBlank { "unknown" }
}