package dev.gisketch.commandpalette.ui

import dev.gisketch.commandpalette.action.PaletteAction
import dev.gisketch.commandpalette.action.PaletteActionRegistry
import dev.gisketch.commandpalette.action.PaletteExecutionContext
import dev.gisketch.commandpalette.config.PaletteStore
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component as OwoComponent
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import io.wispforest.owo.ui.event.MouseDown
import net.minecraft.client.Minecraft
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW

class CommandPaletteScreen : BaseOwoScreen<FlowLayout>(Component.translatable("screen.gisketchs_command_palette.palette")) {
    private lateinit var list: FlowLayout
    private lateinit var queryLabel: io.wispforest.owo.ui.component.LabelComponent
    private var query = ""
    private var selectedIndex = 0
    private var displayedActions: List<PaletteAction> = emptyList()

    override fun createAdapter(): OwoUIAdapter<FlowLayout> = OwoUIAdapter.create(this, Containers::verticalFlow)

    override fun build(rootComponent: FlowLayout) {
        PaletteActionRegistry.refresh()
        PaletteStore.load()

        rootComponent
            .surface(Surface.VANILLA_TRANSLUCENT)
            .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

        list = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(4)
        queryLabel = Components.label(searchComponent()).maxWidth(520)

        val searchBar = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(24))
        searchBar.surface(Surface.PANEL_INSET)
        searchBar.padding(Insets.of(5))
        searchBar.verticalAlignment(VerticalAlignment.CENTER)
        searchBar.child(queryLabel)

        val panel = Containers.verticalFlow(Sizing.fill(82), Sizing.fill(72))
        panel.surface(Surface.DARK_PANEL)
        panel.padding(Insets.of(10))
        panel.gap(8)
        panel.horizontalAlignment(HorizontalAlignment.CENTER)

        panel.child(Components.label(Component.translatable("screen.gisketchs_command_palette.palette")).shadow(true))
        panel.child(searchBar)
        panel.child(Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), list))
        rootComponent.child(panel)

        populate()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (query.isNotEmpty()) {
                    query = query.dropLast(1)
                    selectedIndex = 0
                    populate()
                }
                true
            }
            GLFW.GLFW_KEY_UP -> {
                moveSelection(-1)
                true
            }
            GLFW.GLFW_KEY_DOWN -> {
                moveSelection(1)
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                displayedActions.getOrNull(selectedIndex)?.let(::runAction)
                true
            }
            else -> super.keyPressed(keyCode, scanCode, modifiers)
        }
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (codePoint.isISOControl()) return super.charTyped(codePoint, modifiers)
        query += codePoint
        selectedIndex = 0
        populate()
        return true
    }

    private fun moveSelection(delta: Int) {
        if (displayedActions.isEmpty()) return
        selectedIndex = (selectedIndex + delta).floorMod(displayedActions.size)
        populate()
    }

    private fun populate() {
        queryLabel.text(searchComponent())
        displayedActions = PaletteStore.pinned(PaletteActionRegistry.all())
            .filter { it.matches(query) }
            .take(14)
        list.clearChildren()
        displayedActions.forEachIndexed { index, action -> list.child(actionRow(action, index == selectedIndex)) }
    }

    private fun actionRow(action: PaletteAction, selected: Boolean): FlowLayout {
        val row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(24))
        row.surface(if (selected) Surface.flat(0x663A70C4).and(Surface.outline(0xFF8DB3FF.toInt())) else Surface.PANEL)
        row.padding(Insets.of(4))
        row.gap(6)
        row.verticalAlignment(VerticalAlignment.CENTER)
        row.child(iconFor(action))
        row.child(Components.label(highlight(PaletteStore.displayName(action))).maxWidth(540).shadow(true))
        row.mouseDown().subscribe(MouseDown { _, _, button ->
            if (button == 0) {
                runAction(action)
                true
            } else {
                false
            }
        })
        return row
    }

    private fun runAction(action: PaletteAction) {
        val context = PaletteExecutionContext(originScreen = this)
        PaletteStore.remember(action)
        Minecraft.getInstance().setScreen(null)
        action.execute(context)
    }

    private fun iconFor(action: PaletteAction): OwoComponent {
        PaletteStore.iconStack(action)?.let { return Components.item(it).showOverlay(false) }
        val modIcon = ModIconCache.iconFor(action.sourceModId)
        return if (modIcon != null) {
            Components.texture(modIcon.texture, 0, 0, modIcon.width, modIcon.height, modIcon.width, modIcon.height)
                .sizing(Sizing.fixed(16), Sizing.fixed(16))
        } else {
            Components.item(Items.DIRT.defaultInstance).showOverlay(false)
        }
    }

    private fun highlight(text: String): Component {
        val ranges = query.trim().split(Regex("\\s+")).filter(String::isNotBlank).flatMap { token ->
            exactMatchRanges(text, token)
        }.sortedBy { it.first }.fold(mutableListOf<IntRange>()) { merged, range ->
            val last = merged.lastOrNull()
            if (last == null || range.first > last.last + 1) {
                merged.add(range)
            } else {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            }
            merged
        }
        if (ranges.isEmpty()) return Component.literal(text)

        val result: MutableComponent = Component.empty()
        var cursor = 0
        ranges.forEach { range ->
            if (cursor < range.first) result.append(Component.literal(text.substring(cursor, range.first)))
            result.append(Component.literal(text.substring(range.first, range.last + 1)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            cursor = range.last + 1
        }
        if (cursor < text.length) result.append(Component.literal(text.substring(cursor)))
        return result
    }

    private fun exactMatchRanges(text: String, token: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val lowerText = text.lowercase()
        val lowerToken = token.lowercase()
        var startIndex = lowerText.indexOf(lowerToken)
        while (startIndex >= 0) {
            ranges.add(startIndex until startIndex + token.length)
            startIndex = lowerText.indexOf(lowerToken, startIndex + token.length)
        }
        return ranges
    }

    private fun searchComponent(): Component = if (query.isBlank()) {
        Component.translatable("ui.gisketchs_command_palette.search_empty")
    } else {
        Component.literal("> $query")
    }

    private fun Int.floorMod(size: Int): Int = ((this % size) + size) % size
}