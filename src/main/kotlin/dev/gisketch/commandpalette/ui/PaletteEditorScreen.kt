package dev.gisketch.commandpalette.ui

import dev.gisketch.commandpalette.action.PaletteAction
import dev.gisketch.commandpalette.action.PaletteActionRegistry
import dev.gisketch.commandpalette.config.PaletteStore
import net.minecraft.ChatFormatting
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
import net.minecraft.network.chat.Component
import io.wispforest.owo.ui.event.MouseDown
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW

class PaletteEditorScreen : BaseOwoScreen<FlowLayout>(Component.translatable("screen.gisketchs_command_palette.edit_palette")) {
    private lateinit var availableList: FlowLayout
    private lateinit var selectedList: FlowLayout
    private lateinit var queryLabel: io.wispforest.owo.ui.component.LabelComponent
    private var query: String = ""
    private var selectedColumn = 0
    private var selectedIndex = 0
    private var availableActions: List<PaletteAction> = emptyList()
    private var selectedActions: List<PaletteAction> = emptyList()

    override fun createAdapter(): OwoUIAdapter<FlowLayout> = OwoUIAdapter.create(this, Containers::verticalFlow)

    override fun build(rootComponent: FlowLayout) {
        PaletteActionRegistry.refresh()
        PaletteStore.load()

        rootComponent
            .surface(Surface.VANILLA_TRANSLUCENT)
            .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

        availableList = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(4)
        selectedList = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(4)

        queryLabel = Components.label(searchComponent()).maxWidth(620)

        val searchBar = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(24))
        searchBar.surface(Surface.PANEL_INSET)
        searchBar.padding(Insets.of(5))
        searchBar.verticalAlignment(VerticalAlignment.CENTER)
        searchBar.child(queryLabel)

        val columns = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand())
        columns.gap(8)
        columns.child(panel(Component.translatable("ui.gisketchs_command_palette.available"), availableList))
        columns.child(panel(Component.translatable("ui.gisketchs_command_palette.selected"), selectedList))

        val rootPanel = Containers.verticalFlow(Sizing.fill(90), Sizing.fill(82))
        rootPanel.surface(Surface.DARK_PANEL)
        rootPanel.padding(Insets.of(10))
        rootPanel.gap(8)
        rootPanel.child(Components.label(Component.translatable("screen.gisketchs_command_palette.edit_palette")).shadow(true))
        rootPanel.child(searchBar)
        rootPanel.child(columns)

        rootComponent.child(rootPanel)
        rebuildLists()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (query.isNotEmpty()) {
                    query = query.dropLast(1)
                    selectedIndex = 0
                    rebuildLists()
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
            GLFW.GLFW_KEY_LEFT -> {
                selectedColumn = 0
                selectedIndex = selectedIndex.coerceIn(0, (currentColumnSize() - 1).coerceAtLeast(0))
                rebuildLists()
                true
            }
            GLFW.GLFW_KEY_RIGHT -> {
                selectedColumn = 1
                selectedIndex = selectedIndex.coerceIn(0, (currentColumnSize() - 1).coerceAtLeast(0))
                rebuildLists()
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                currentAction()?.let(::toggleAction)
                true
            }
            else -> super.keyPressed(keyCode, scanCode, modifiers)
        }
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (codePoint.isISOControl()) return super.charTyped(codePoint, modifiers)
        query += codePoint
        selectedIndex = 0
        rebuildLists()
        return true
    }

    private fun panel(title: Component, list: FlowLayout): FlowLayout {
        val panel = Containers.verticalFlow(Sizing.expand(), Sizing.fill(100))
        panel.surface(Surface.PANEL)
        panel.padding(Insets.of(6))
        panel.gap(6)
        panel.child(Components.label(title).shadow(true))
        panel.child(Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), list))
        return panel
    }

    private fun rebuildLists() {
        queryLabel.text(searchComponent())
        val all = PaletteActionRegistry.all().filter { it.matches(query) }
        availableActions = all.filterNot(PaletteStore::isPinned).take(64)
        selectedActions = all.filter(PaletteStore::isPinned)
        selectedIndex = selectedIndex.coerceIn(0, (currentColumnSize() - 1).coerceAtLeast(0))

        availableList.clearChildren()
        selectedList.clearChildren()

        availableActions.forEachIndexed { index, action -> availableList.child(editorRow(action, true, selectedColumn == 0 && index == selectedIndex)) }
        selectedActions.forEachIndexed { index, action -> selectedList.child(editorRow(action, false, selectedColumn == 1 && index == selectedIndex)) }
    }

    private fun editorRow(action: PaletteAction, canAdd: Boolean, selected: Boolean): FlowLayout {
        val buttonText = if (canAdd) {
            Component.translatable("ui.gisketchs_command_palette.add")
        } else {
            Component.translatable("ui.gisketchs_command_palette.remove")
        }

        val affordance = Components.label(buttonText).shadow(true)

        val row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(24))
        row.surface(if (selected) Surface.flat(0x663A70C4).and(Surface.outline(0xFF8DB3FF.toInt())) else Surface.DARK_PANEL)
        row.padding(Insets.of(4))
        row.gap(6)
        row.verticalAlignment(VerticalAlignment.CENTER)
        row.child(iconFor(action))
        row.child(Components.label(highlight(action.title.string)).maxWidth(360).shadow(true))
        row.child(affordance)
        row.mouseDown().subscribe(MouseDown { _, _, button ->
            if (button == 0) {
                toggleAction(action)
                true
            } else {
                false
            }
        })
        return row
    }

    private fun moveSelection(delta: Int) {
        val size = currentColumnSize()
        if (size == 0) return
        selectedIndex = (selectedIndex + delta).floorMod(size)
        rebuildLists()
    }

    private fun currentColumnSize(): Int = if (selectedColumn == 0) availableActions.size else selectedActions.size

    private fun currentAction(): PaletteAction? = if (selectedColumn == 0) {
        availableActions.getOrNull(selectedIndex)
    } else {
        selectedActions.getOrNull(selectedIndex)
    }

    private fun toggleAction(action: PaletteAction) {
        if (PaletteStore.isPinned(action)) PaletteStore.unpin(action) else PaletteStore.pin(action)
        rebuildLists()
    }

    private fun iconFor(action: PaletteAction): OwoComponent {
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