package dev.gisketch.commandpalette

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object CommandPaletteContent {
    private val ITEMS: DeferredRegister<Item> = DeferredRegister.create(BuiltInRegistries.ITEM, CommandPaletteMod.MOD_ID)

    val EXAMPLE_ITEM: DeferredHolder<Item, Item> = ITEMS.register("example_item") { -> Item(Item.Properties()) }

    fun register(modBus: IEventBus) {
        ITEMS.register(modBus)
        modBus.addListener(::addCreativeTabItems)
    }

    private fun addCreativeTabItems(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == CreativeModeTabs.INGREDIENTS) {
            event.accept(EXAMPLE_ITEM.get())
        }
    }
}