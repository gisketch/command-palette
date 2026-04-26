package dev.gisketch.commandpalette.ui

import dev.gisketch.commandpalette.action.PaletteAction
import dev.gisketch.commandpalette.action.PaletteActionRegistry
import dev.gisketch.commandpalette.config.PaletteStore
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.Component as OwoComponent
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import io.wispforest.owo.ui.event.MouseDown
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW

private enum class EditorMode { ACTIONS, ICONS, NAME }

class PaletteEditorScreen : BaseOwoScreen<FlowLayout>(Component.translatable("screen.gisketchs_command_palette.edit_palette")) {
    private lateinit var availableList: FlowLayout
    private lateinit var selectedList: FlowLayout
    private lateinit var detailsList: FlowLayout
    private lateinit var availableScroll: ScrollContainer<FlowLayout>
    private lateinit var selectedScroll: ScrollContainer<FlowLayout>
    private lateinit var detailsScroll: ScrollContainer<FlowLayout>
    private lateinit var queryLabel: io.wispforest.owo.ui.component.LabelComponent
    private var query = ""
    private var nameDraft = ""
    private var mode = EditorMode.ACTIONS
    private var selectedColumn = 0
    private var selectedIndex = 0
    private var availableActions: List<PaletteAction> = emptyList()
    private var selectedActions: List<PaletteAction> = emptyList()
    private var availableItems: List<Item> = emptyList()
    private var selectedPaletteActionId: String? = null

    override fun createAdapter(): OwoUIAdapter<FlowLayout> = OwoUIAdapter.create(this, Containers::verticalFlow)

    override fun build(rootComponent: FlowLayout) {
        PaletteActionRegistry.refresh()
        PaletteStore.load()
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT).alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

        availableList = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(4)
        selectedList = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(4)
        detailsList = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(4)
        queryLabel = Components.label(searchComponent()).maxWidth(620)

        val searchBar = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(24))
        searchBar.surface(Surface.PANEL_INSET)
        searchBar.padding(Insets.of(5))
        searchBar.verticalAlignment(VerticalAlignment.CENTER)
        searchBar.child(queryLabel)

        val columns = Containers.horizontalFlow(Sizing.fill(100), Sizing.expand())
        columns.gap(8)
        columns.child(availablePanel())
        columns.child(selectedPanel())

        val rootPanel = Containers.verticalFlow(Sizing.fill(98), Sizing.fill(96))
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
        if (mode == EditorMode.NAME) return handleNameKey(keyCode, scanCode, modifiers)
        return when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (query.isNotEmpty()) {
                    query = query.dropLast(1)
                    selectedIndex = 0
                    rebuildLists()
                }
                true
            }
            GLFW.GLFW_KEY_UP -> { moveSelection(-1); true }
            GLFW.GLFW_KEY_DOWN -> { moveSelection(1); true }
            GLFW.GLFW_KEY_LEFT -> { selectedColumn = 0; clampSelection(); rebuildLists(); true }
            GLFW.GLFW_KEY_RIGHT -> { selectedColumn = 1; clampSelection(); rebuildLists(); true }
            GLFW.GLFW_KEY_ESCAPE -> {
                if (mode == EditorMode.ICONS) {
                    mode = EditorMode.ACTIONS
                    query = ""
                    selectedIndex = 0
                    rebuildLists()
                    true
                } else {
                    super.keyPressed(keyCode, scanCode, modifiers)
                }
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (mode == EditorMode.ICONS) currentItem()?.let(::selectIcon) else currentAction()?.let(::selectOrAddAction)
                true
            }
            else -> super.keyPressed(keyCode, scanCode, modifiers)
        }
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (codePoint.isISOControl()) return super.charTyped(codePoint, modifiers)
        if (mode == EditorMode.NAME) nameDraft += codePoint else {
            query += codePoint
            selectedIndex = 0
        }
        rebuildLists()
        return true
    }

    private fun availablePanel(): FlowLayout {
        val panel = Containers.verticalFlow(Sizing.fill(49), Sizing.fill(100))
        panel.surface(Surface.PANEL)
        panel.padding(Insets.of(6))
        panel.gap(6)
        panel.child(Components.label(availableTitle()).shadow(true))
        availableScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), availableList)
        panel.child(availableScroll)
        return panel
    }

    private fun selectedPanel(): FlowLayout {
        val panel = Containers.verticalFlow(Sizing.fill(49), Sizing.fill(100))
        panel.surface(Surface.PANEL)
        panel.padding(Insets.of(6))
        panel.gap(6)
        panel.child(Components.label(Component.translatable("ui.gisketchs_command_palette.selected")).shadow(true))
        selectedScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(62), selectedList)
        panel.child(selectedScroll)
        panel.child(Components.label(Component.translatable("ui.gisketchs_command_palette.details")).shadow(true))
        detailsScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.expand(), detailsList)
        panel.child(detailsScroll)
        return panel
    }

    private fun rebuildLists() {
        queryLabel.text(searchComponent())
        selectedActions = PaletteActionRegistry.all().filter(PaletteStore::isPinned)
        if (selectedPaletteActionId == null || selectedActions.none { action -> action.id.toString() == selectedPaletteActionId }) {
            selectedPaletteActionId = selectedActions.firstOrNull()?.id?.toString()
        }

        availableList.clearChildren()
        selectedList.clearChildren()
        detailsList.clearChildren()

        if (mode == EditorMode.ICONS) {
            availableItems = searchItems(query).take(96)
            clampSelection()
            availableItems.forEachIndexed { index, item ->
                val row = itemRow(item, index == selectedIndex && selectedColumn == 0)
                availableList.child(row)
                if (index == selectedIndex && selectedColumn == 0) availableScroll.scrollTo(row)
            }
        } else {
            val all = PaletteActionRegistry.all().filter { action -> action.matches(query) || PaletteStore.displayName(action).contains(query, ignoreCase = true) }
            availableActions = all.filterNot(PaletteStore::isPinned).take(64)
            clampSelection()
            availableActions.forEachIndexed { index, action ->
                val row = actionRow(action, true, selectedColumn == 0 && index == selectedIndex)
                availableList.child(row)
                if (index == selectedIndex && selectedColumn == 0) availableScroll.scrollTo(row)
            }
        }

        selectedActions.forEachIndexed { index, action ->
            val row = actionRow(action, false, selectedColumn == 1 && index == selectedIndex)
            selectedList.child(row)
            if (index == selectedIndex && selectedColumn == 1) selectedScroll.scrollTo(row)
        }
        selectedAction()?.let { detailsFor(it, detailsList) } ?: detailsList.child(Components.label(Component.translatable("ui.gisketchs_command_palette.no_selection")))
    }

    private fun actionRow(action: PaletteAction, canAdd: Boolean, selected: Boolean): FlowLayout {
        val row = compactRow(selected)
        val icon = iconFor(action)
        if (!canAdd) {
            icon.mouseDown().subscribe(MouseDown { _, _, button ->
                if (button == 0) {
                    beginIconPick(action)
                    true
                } else {
                    false
                }
            })
        }
        val nameBox = Containers.horizontalFlow(Sizing.expand(), Sizing.content())
        nameBox.child(Components.label(highlight(PaletteStore.displayName(action))).maxWidth(360).shadow(true))
        row.child(icon)
        row.child(nameBox)
        if (canAdd) {
            row.child(Components.label(Component.literal("+")).shadow(true))
        } else {
            val remove = Components.label(Component.literal("x")).shadow(true)
            remove.mouseDown().subscribe(MouseDown { _, _, button ->
                if (button == 0) {
                    PaletteStore.unpin(action)
                    rebuildLists()
                    true
                } else {
                    false
                }
            })
            row.child(remove)
        }
        row.mouseDown().subscribe(MouseDown { _, _, button ->
            if (button == 0) { selectOrAddAction(action); true } else false
        })
        return row
    }

    private fun itemRow(item: Item, selected: Boolean): FlowLayout {
        val itemId = BuiltInRegistries.ITEM.getKey(item)
        val row = compactRow(selected)
        row.child(Components.item(item.defaultInstance).showOverlay(false))
        row.child(Components.label(highlight(itemId.toString())).maxWidth(420).shadow(true))
        row.mouseDown().subscribe(MouseDown { _, _, button ->
            if (button == 0) { selectIcon(item); true } else false
        })
        return row
    }

    private fun detailsFor(action: PaletteAction, target: FlowLayout) {
        val actionId = action.id.toString()
        val nameValue = if (mode == EditorMode.NAME && selectedPaletteActionId == actionId) nameDraft else PaletteStore.displayName(action)
        target.child(detailRow(Component.translatable("ui.gisketchs_command_palette.name"), Component.literal(nameValue), true) {
            selectedPaletteActionId = actionId
            mode = EditorMode.NAME
            nameDraft = PaletteStore.displayName(action)
            rebuildLists()
        })
        target.child(detailRow(Component.translatable("ui.gisketchs_command_palette.icon"), iconLabel(action), true) {
            selectedPaletteActionId = actionId
            beginIconPick(action)
        })
        target.child(detailRow(Component.translatable("ui.gisketchs_command_palette.action_id"), Component.literal(actionId), false))
        target.child(detailRow(Component.translatable("ui.gisketchs_command_palette.source"), Component.literal(action.sourceModId), false))
        target.child(detailRow(Component.translatable("ui.gisketchs_command_palette.remove"), Component.literal("-"), true) {
            PaletteStore.unpin(action)
            rebuildLists()
        })
    }

    private fun detailRow(label: Component, value: Component, clickable: Boolean, onClick: (() -> Unit)? = null): FlowLayout {
        val row = compactRow(false)
        row.child(Components.label(label).maxWidth(120).shadow(true))
        row.child(Components.label(value).maxWidth(380))
        if (clickable && onClick != null) {
            row.mouseDown().subscribe(MouseDown { _, _, button ->
                if (button == 0) { onClick(); true } else false
            })
        }
        return row
    }

    private fun compactRow(selected: Boolean): FlowLayout {
        val row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(24))
        row.surface(if (selected) Surface.flat(0x663A70C4).and(Surface.outline(0xFF8DB3FF.toInt())) else Surface.DARK_PANEL)
        row.padding(Insets.of(4))
        row.gap(6)
        row.verticalAlignment(VerticalAlignment.CENTER)
        return row
    }

    private fun handleNameKey(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = when (keyCode) {
        GLFW.GLFW_KEY_BACKSPACE -> {
            if (nameDraft.isNotEmpty()) nameDraft = nameDraft.dropLast(1)
            rebuildLists()
            true
        }
        GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
            selectedAction()?.let { action -> PaletteStore.setDisplayName(action, nameDraft) }
            mode = EditorMode.ACTIONS
            rebuildLists()
            true
        }
        GLFW.GLFW_KEY_ESCAPE -> {
            mode = EditorMode.ACTIONS
            rebuildLists()
            true
        }
        else -> super.keyPressed(keyCode, scanCode, modifiers)
    }

    private fun moveSelection(delta: Int) {
        val size = currentColumnSize()
        if (size == 0) return
        selectedIndex = (selectedIndex + delta).floorMod(size)
        rebuildLists()
    }

    private fun clampSelection() {
        selectedIndex = selectedIndex.coerceIn(0, (currentColumnSize() - 1).coerceAtLeast(0))
    }

    private fun currentColumnSize(): Int = if (mode == EditorMode.ICONS && selectedColumn == 0) availableItems.size else if (selectedColumn == 0) availableActions.size else selectedActions.size

    private fun currentAction(): PaletteAction? = if (selectedColumn == 0) availableActions.getOrNull(selectedIndex) else selectedActions.getOrNull(selectedIndex)

    private fun currentItem(): Item? = availableItems.getOrNull(selectedIndex)

    private fun selectedAction(): PaletteAction? = selectedPaletteActionId?.let { id -> selectedActions.firstOrNull { action -> action.id.toString() == id } }

    private fun selectOrAddAction(action: PaletteAction) {
        if (!PaletteStore.isPinned(action)) PaletteStore.pin(action)
        selectedPaletteActionId = action.id.toString()
        selectedColumn = 1
        selectedIndex = selectedActions.indexOfFirst { selectedAction -> selectedAction.id == action.id }.coerceAtLeast(0)
        mode = EditorMode.ACTIONS
        rebuildLists()
    }

    private fun selectIcon(item: Item) {
        selectedAction()?.let { action -> PaletteStore.setIconItem(action, BuiltInRegistries.ITEM.getKey(item)) }
        mode = EditorMode.ACTIONS
        query = ""
        rebuildLists()
    }

    private fun beginIconPick(action: PaletteAction) {
        selectedPaletteActionId = action.id.toString()
        mode = EditorMode.ICONS
        query = ""
        selectedColumn = 0
        selectedIndex = 0
        rebuildLists()
    }

    private fun iconFor(action: PaletteAction): OwoComponent {
        PaletteStore.iconStack(action)?.let { stack -> return Components.item(stack).showOverlay(false) }
        val modIcon = ModIconCache.iconFor(action.sourceModId)
        return if (modIcon != null) {
            Components.texture(modIcon.texture, 0, 0, modIcon.width, modIcon.height, modIcon.width, modIcon.height).sizing(Sizing.fixed(16), Sizing.fixed(16))
        } else {
            Components.item(Items.DIRT.defaultInstance).showOverlay(false)
        }
    }

    private fun iconLabel(action: PaletteAction): Component = PaletteStore.actionEntry(action).iconItem?.let(Component::literal) ?: Component.translatable("ui.gisketchs_command_palette.default_icon")

    private fun searchItems(search: String): List<Item> {
        val normalized = search.trim().lowercase()
        return BuiltInRegistries.ITEM.stream()
            .filter { item -> item != Items.AIR }
            .filter { item -> normalized.isBlank() || BuiltInRegistries.ITEM.getKey(item).toString().contains(normalized) || item.description.string.lowercase().contains(normalized) }
            .sorted(Comparator.comparing { item -> BuiltInRegistries.ITEM.getKey(item).toString() })
            .toList()
    }

    private fun highlight(text: String): Component {
        val ranges = query.trim().split(Regex("\\s+")).filter(String::isNotBlank).flatMap { token -> exactMatchRanges(text, token) }.sortedBy { range -> range.first }
            .fold(mutableListOf<IntRange>()) { merged, range ->
                val last = merged.lastOrNull()
                if (last == null || range.first > last.last + 1) merged.add(range) else merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
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

    private fun searchComponent(): Component = when (mode) {
        EditorMode.ACTIONS -> if (query.isBlank()) Component.translatable("ui.gisketchs_command_palette.search_empty") else Component.literal("> $query")
        EditorMode.ICONS -> if (query.isBlank()) Component.translatable("ui.gisketchs_command_palette.search_icons") else Component.literal("icon > $query")
        EditorMode.NAME -> Component.literal("name > $nameDraft")
    }

    private fun availableTitle(): Component = if (mode == EditorMode.ICONS) Component.translatable("ui.gisketchs_command_palette.available_icons") else Component.translatable("ui.gisketchs_command_palette.available")

    private fun Int.floorMod(size: Int): Int = ((this % size) + size) % size
}
