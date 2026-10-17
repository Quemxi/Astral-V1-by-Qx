package foo.starred.athen.config.hud.ui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.annotations.Priority
import foo.starred.athen.api.minecraft.text.measurer.VanillaFontMeasurer
import foo.starred.athen.api.minecraft.text.renderer.VanillaFontRenderer
import foo.starred.athen.config.hud.data.editor.HudEditorRuntimeCoordinateData
import foo.starred.athen.config.hud.data.editor.HudEditorRuntimeRenderData
import foo.starred.athen.config.hud.data.element.HudElement
import foo.starred.athen.config.hud.impl.HudRenderer
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.Dev
import foo.starred.snowbird.api.inputs.impl.MouseInputState
import foo.starred.snowbird.utils.literal
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import kotlin.math.roundToInt

@Priority(-1)
object HudElementEditorUI : Screen("HUD Editor [Astral]".literal()) {
    private val runtime = HudEditorRuntimeRenderData()
    private val coordinate = HudEditorRuntimeCoordinateData()
    private var dragging: HudElement? = null

    private val _act: List<HudElement>
        get() = HudRenderer.elements.values.sortedBy { it.config.name }

    private val active: HudElement?
        get() = dragging ?: _act.filter { it.render0 }.asReversed().firstOrNull { it.hovered(coordinate.x, coordinate.y) }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        HudRenderer.constrain()

        val x = MouseInputState.Position.Scaled.x / HudRenderer.scale
        val y = MouseInputState.Position.Scaled.y / HudRenderer.scale
        val x1 = x - coordinate.x
        val y1 = y - coordinate.y

        coordinate.x = x
        coordinate.y = y

        dragging?.apply {
            val x1 = x - this@HudElementEditorUI.coordinate.x0
            val y1 = y - this@HudElementEditorUI.coordinate.y0

            if (this@HudElementEditorUI.runtime.snap) {
                val padding = 4f * coordinate.scale

                coordinate.x = (((x1 - padding) / 8f).roundToInt() * 8f) + padding
                coordinate.y = (((y1 - padding) / 8f).roundToInt() * 8f) + padding
                return@apply
            }

            coordinate.x = x1
            coordinate.y = y1
        }

        graphics.pose().pushMatrix()
        graphics.pose().scale(HudRenderer.scale)
        graphics.fill(0, 0, HudRenderer.width.toInt(), HudRenderer.height.toInt(), MochaColorScheme.Lavender.alpha(0.1f))

        if (runtime.grid) {
            val color = MochaColorScheme.Surface0.alpha(0.35f)
            val width = HudRenderer.width.toInt()
            val height = HudRenderer.height.toInt()

            var x2 = 0
            while (x2 <= width) {
                graphics.fill(x2, 0, x2 + 1, height, color)
                x2 += 8
            }

            var y2 = 0
            while (y2 <= height) {
                graphics.fill(0, y2, width, y2 + 1, color)
                y2 += 8
            }
        }

        for (e in _act.filter { it.render0 }) {
            graphics.pose().pushMatrix()
            graphics.pose().translate(e.coordinate.x, e.coordinate.y)
            graphics.pose().scale(e.coordinate.scale, e.coordinate.scale)

            graphics.fill(-4, -4, e.coordinate.width.toInt() + 4, e.coordinate.height.toInt() + 4, MochaColorScheme.Base.alpha(0.5f))
            graphics.outline(-4, -4, e.coordinate.width.toInt() + 8, e.coordinate.height.toInt() + 8, MochaColorScheme.Text.argb)

            graphics.pose().pushMatrix()
            e.preview(graphics)
            graphics.pose().popMatrix()

            graphics.pose().popMatrix()
        }

        active?.let { e ->
            val text = e.config.name + if (Dev.debug) " | ${e.coordinate.x} | ${e.coordinate.y}" else ""
            val width = VanillaFontMeasurer.width(text)
            val height = VanillaFontMeasurer.height

            graphics.pose().pushMatrix()
            graphics.pose().translate(coordinate.x + 12, coordinate.y - height / 2)

            graphics.fill(-6, -6, width + 6, height + 6, MochaColorScheme.Base.alpha(0.8f))
            graphics.outline(-6, -6, width + 12, height + 12, MochaColorScheme.Text.argb)
            VanillaFontRenderer.extract(graphics, text, 0, 0, false, MochaColorScheme.Text.argb)
            graphics.pose().popMatrix()
        }

        Help.render(graphics, x1, y1)
        graphics.pose().popMatrix()

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (Help.hovered(coordinate.x, coordinate.y)) {
            Help.dragging = true
            return true
        }

        val hovered = active ?: return super.mouseClicked(event, doubleClick)
        dragging = hovered
        coordinate.x0 = coordinate.x - hovered.coordinate.x
        coordinate.y0 = coordinate.y - hovered.coordinate.y
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        dragging = null
        Help.dragging = false
        return false
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        val hovered = active ?: return super.mouseScrolled(x, y, scrollX, scrollY)

        val scaleDelta = if (scrollY > 0) 0.1f else -0.1f
        hovered.coordinate.scale = (hovered.coordinate.scale + scaleDelta).coerceIn(0.2f, 5.0f)
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == InputConstants.KEY_G) {
            if ((event.modifiers() and InputConstants.MOD_CONTROL) != 0) runtime.snap = !runtime.snap
            else runtime.grid = !runtime.grid

            return true
        }

        val step = if ((event.modifiers() and InputConstants.MOD_SHIFT) != 0) 8f else 1f

        when (event.key()) {
            InputConstants.KEY_H -> {
                val e = active ?: return false
                e.coordinate.x = (HudRenderer.width - e.coordinate.width * e.coordinate.scale) / 2f
                return true
            }

            InputConstants.KEY_V -> {
                val e = active ?: return false
                e.coordinate.y = (HudRenderer.height - e.coordinate.height * e.coordinate.scale) / 2f
                return true
            }

            InputConstants.KEY_LEFT -> {
                val e = active ?: return false
                e.coordinate.x -= step
                return true
            }

            InputConstants.KEY_RIGHT -> {
                val e = active ?: return false
                e.coordinate.x += step
                return true
            }

            InputConstants.KEY_UP -> {
                val e = active ?: return false
                e.coordinate.y -= step
                return true
            }

            InputConstants.KEY_DOWN -> {
                val e = active ?: return false
                e.coordinate.y += step
                return true
            }

            InputConstants.KEY_R -> {
                val ctrl = (event.modifiers() and InputConstants.MOD_CONTROL) != 0
                val shift = (event.modifiers() and InputConstants.MOD_SHIFT) != 0

                if (ctrl && shift) {
                    for ((coordinate) in HudRenderer.elements.values) {
                        coordinate.x = 20f
                        coordinate.y = 20f
                        coordinate.scale = 1f
                    }

                    return true
                }

                val e = active ?: return false
                e.coordinate.x = 20f
                e.coordinate.y = 20f
                e.coordinate.scale = 1f
                return true
            }
        }

        return super.keyPressed(event)
    }

    override fun onClose() {
        super.onClose()
        HudRenderer.save()
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    private object Help {
        private val lines = listOf(
            { false to "Arrow keys to move, shift maybe" },
            { false to "H = center horizontally" },
            { false to "V = center vertically" },
            { false to "R = reset" },
            { false to "Ctrl + Shift + R = reset all" },
            { runtime.grid to "G = toggle grid" },
            { runtime.snap to "Ctrl + G = toggle snap to grid" },
        )

        private var width = 0
        private var height = 0

        var dragging = false
        var t0 = 0
        var x = 400f
        var y = 400f

        init {
            fn()
        }

        fun hovered(x: Float, y: Float): Boolean {
            return x >= this.x - 4f && x <= this.x + width + 4f && y >= this.y - 4f && y <= this.y + height + 4f
        }

        fun render(graphics: GuiGraphicsExtractor, x0: Float, y0: Float): Unit = with (graphics) {
            if (width == 0 || height == 0) {
                fn()
            }

            if (dragging) {
                x += x0
                y += y0
            }

            pose().pushMatrix()
            pose().translate(x, y)

            fill(-4, -4, width + 4, height + 4, MochaColorScheme.Base.alpha(0.6f))
            outline(-4, -4, width + 8, height + 8,  MochaColorScheme.Lavender.argb)

            var y1 = 0
            for (entry in lines) {
                val (enabled, text) = entry()

                VanillaFontRenderer.extract(this, "•", 0, y1, false, if (enabled) MochaColorScheme.Green.argb else MochaColorScheme.Red.argb)
                VanillaFontRenderer.extract(this, text, t0, y1, false, MochaColorScheme.Text.argb)

                y1 += VanillaFontMeasurer.height
            }

            pose().popMatrix()
        }

        private fun fn() {
            var w = 0
            var y = 0

            t0 = VanillaFontMeasurer.width("• ")

            for (line in lines) {
                w = w.coerceAtLeast(VanillaFontMeasurer.width(line().second) + t0)
                y += VanillaFontMeasurer.height
            }

            width = w
            height = y
        }
    }
}
