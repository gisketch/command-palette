package dev.gisketch.commandpalette.ui

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.repository.PackSource
import net.neoforged.fml.ModList
import net.neoforged.neoforge.resource.ResourcePackLoader
import java.util.Optional

data class ModIcon(val texture: ResourceLocation, val width: Int, val height: Int)

object ModIconCache {
    private val cache = mutableMapOf<String, ModIcon?>()

    fun iconFor(modId: String): ModIcon? = cache.getOrPut(modId) { loadIcon(modId) }

    private fun loadIcon(modId: String): ModIcon? {
        if (modId == ResourceLocation.DEFAULT_NAMESPACE) return null
        val modInfo = ModList.get().getMods().firstOrNull { it.modId == modId } ?: return null
        val logoFile = modInfo.logoFile.orElse(null) ?: return null
        val resourcePackSupplier = ResourcePackLoader.getPackFor(modId)
            .orElse(ResourcePackLoader.getPackFor("neoforge").orElse(null)) ?: return null

        return runCatching {
            resourcePackSupplier.openPrimary(packLocation(modId)).use { packResources ->
                readLogo(packResources, logoFile)?.let { image ->
                    val texture = DynamicTexture(image)
                    val location = Minecraft.getInstance().textureManager.register("command_palette/$modId", texture)
                    ModIcon(location, image.width, image.height)
                }
            }
        }.getOrNull()
    }

    private fun packLocation(modId: String): PackLocationInfo = PackLocationInfo(
        "mod/$modId",
        Component.empty(),
        PackSource.BUILT_IN,
        Optional.empty(),
    )

    private fun readLogo(packResources: PackResources, logoFile: String): NativeImage? {
        val supplier = packResources.getRootResource(*logoFile.split(Regex("[/\\\\]")).toTypedArray()) ?: return null
        return supplier.get().use(NativeImage::read)
    }
}