@file:Suppress("ObjectPropertyName", "Unused")

package foo.starred.athen.modules.impl.render.radial

import com.google.gson.reflect.TypeToken
import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.Athen
import foo.starred.athen.annotations.Load
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.minecraft.mod.ModWrapper
import foo.starred.athen.api.minecraft.text.measurer.VanillaFontMeasurer
import foo.starred.athen.api.minecraft.text.renderer.VanillaFontRenderer
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.GameEvent
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.events.InputEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.render.radial.data.RadialSlot
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialEditor
import foo.starred.athen.modules.impl.render.radial.utils.RadialRenderState
import foo.starred.athen.utils.command
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.kbus.extensions.runWhen
import foo.starred.snowbird.api.center
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.data.Observable
import foo.starred.snowbird.api.inputs.impl.MouseInputState
import foo.starred.snowbird.api.lie
import foo.starred.snowbird.api.repeat
import foo.starred.snowbird.utils.compress
import foo.starred.snowbird.utils.decompress
import foo.starred.snowbird.utils.literal
import foo.starred.snowbird.utils.safely
import kotlin.math.hypot

@Load
object RadialMenu : Module(
    "Radial menu",
    "Shows a cool radial menu with a ton of options for customisations!",
    ConfigCategory.RENDER
) {
    private val keybind by config.keybind("Keybind", InputConstants.KEY_R)
    private val releaseClose by config.switch("Release to close", true)
    val direction by config.switch("General direction click").description("Enabling this will make your clicks be on the slot closest to the cursor.")
    val type by config.selector("Sub menu type", listOf("Full", "Mini", "Mini extended"))
    val radius1 by config.slider("Inner radius", 50f, 20f, 120f, "pixels")
    val radius2 by config.slider("Outer radius", 80f, 40f, 180f, "pixels")
    val thickness by config.slider("Sub thickness", 18f, 8f, 40f, "pixels")

    val `color$normal` by config.colorPicker("Normal color", MochaColorScheme.Surface0.alpha(0.5f))
    val `color$hover` by config.colorPicker("Hover color", MochaColorScheme.Lavender.alpha(0.5f))

    private val _unused0 by config.button("Open editor") {
        RadialEditor.open()
    }

    private val _unused1 by config.information("Command: <red>/astral radial help")

    private val json = JsonStore("features/radialMenu")
    private val stack = ArrayDeque<List<RadialSlot>>()

    val slots = mutableListOf<RadialSlot>()
    val configs = mutableMapOf<String, List<RadialSlot>>()

    val current: List<RadialSlot>
        get() = stack.lastOrNull() ?: slots

    val open = Observable(false).observe {
        if (it) return@observe

        stack.clear()
        i0 = -1
        i1 = -1
        i2 = -1
    }

    var i0 = -1
        private set

    var i1 = -1
        private set

    var i2 = -1
        private set

    var active: String by json.string("active", "Default")
        private set

    var saved: String by json.string("configs")
        private set

    init {
        command {
            "radial" {
                help()
            }

            "radial" / "help" {
                help()
            }

            "radial" / "edit" {
                RadialEditor.open()
            }

            "import" / "radial" {
                val clipboard = client.keyboardHandler.clipboard
                if (clipboard.isEmpty()) return@invoke "No data found in clipboard!".mod()

                val map: Map<String, Any> = Athen.GSON.fromJson(clipboard.decompress(), object : TypeToken<Map<String, Any>>() {}.type)
                val name = map["name"] as? String ?: return@invoke "Invalid config data!".mod()
                val data: List<RadialSlot> = Athen.GSON.fromJson(Athen.GSON.toJson(map["slots"]), object : TypeToken<List<RadialSlot>>() {}.type)

                var n = name
                var i = 1
                while (n in configs) n = "$name ${++i}"

                configs[n] = data
                load(n)
                disk()
                "Imported config '$n' with ${data.size} slots!".mod()
            }

            "export" / "radial" {
                save()
                client.keyboardHandler.clipboard = Athen.GSON.toJson(mapOf("name" to active, "slots" to slots)).compress()
                "Exported config '$active' to clipboard!".mod()
            }
        }

        on<GameEvent.Start> {
            safely {
                saved.takeIf { it.isNotBlank() }?.let { raw ->
                    val map: Map<String, List<RadialSlot>> = Athen.GSON.fromJson(raw, object : TypeToken<Map<String, List<RadialSlot>>>() {}.type)
                    configs.clear()
                    configs.putAll(map)
                }

                if (configs.isEmpty()) configs["Default"] = emptyList()
                fill(active)
            }
        }

        on<GameEvent.Stop> {
            save()
            disk()
        }

        on<InputEvent.Keyboard.Press> {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            if (client.screen != null) return@on
            if (keyEvent.key != keybind.value) return@on

            react(releaseClose || !open.value, true)
        }

        on<InputEvent.Keyboard.Release> {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            if (client.screen != null) return@on
            if (keyEvent.key != keybind.value) return@on
            if (!open.value) return@on
            if (!releaseClose) return@on

            trigger()
            react(false, bool = true)
        }

        on<InputEvent.Mouse.Press> {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            if (client.screen != null) return@on
            if (buttonInfo.button() != keybind.value) return@on
            if (open.value) return@on

            react(releaseClose || !open.value, true)
        }

        on<InputEvent.Mouse.Release> {
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            if (client.screen != null) return@on
            if (buttonInfo.button() != keybind.value) return@on
            if (!open.value) return@on
            if (!releaseClose) return@on

            trigger()
            react(false, bool = true)
        }

        on<InputEvent.Mouse.Press> {
            val x1 = client.window.guiScaledWidth / 2
            val y1 = client.window.guiScaledHeight / 2

            if (dist(x1, y1) < 15f) {
                if (type == 2 && i2 != -1) {
                    i2 = -1
                    i1 = -1
                    return@on cancel()
                }

                if (stack.isEmpty()) {
                    react(false, bool = true)
                    return@on cancel()
                }

                back()
                return@on cancel()
            }

            if (buttonInfo.button() == 1 && stack.isNotEmpty()) {
                back()
                return@on cancel()
            }

            if (i1 != -1 && i2 in current.indices) {
                current[i2].sub.getOrNull(i1)?.action?.run()
                react(false, bool = true)
                return@on cancel()
            }

            val slot = current.getOrNull(i0) ?: return@on cancel()

            if ((type == 1 || type == 2) && slot.sub.isNotEmpty()) {
                i2 = if (i2 == i0) -1 else i0
                i1 = -1
                return@on cancel()
            }

            if (type == 0 && slot.sub.isNotEmpty()) {
                stack.addLast(slot.sub)
                i0 = RadialRenderState.hit(MouseInputState.Position.Scaled.x, MouseInputState.Position.Scaled.y, x1, y1, maxOf(1, stack.last().size), radius1, radius2)
                return@on cancel()
            }

            slot.action.run()
            react(false, bool = true)
            cancel()
        }.runWhen(open)

        on<InputEvent.Mouse.Move> {
            cancel()

            val x0 = MouseInputState.Position.Scaled.x
            val y0 = MouseInputState.Position.Scaled.y

            val x1 = client.window.guiScaledWidth / 2
            val y1 = client.window.guiScaledHeight / 2

            if (dist(x1, y1) < 15f) {
                i0 = -1
                i1 = -1
                return@on
            }

            if (type == 2 && i2 in current.indices) {
                val ring = layout()
                val hit = RadialRenderState.hitRing(x0, y0, x1, y1, maxOf(1, current.size), radius2, ring.map { it.first }, direction, thickness)
                if (hit != -1) {
                    i1 = hit
                    return@on
                }
            }

            if (type == 1 && i2 in current.indices) {
                val hit = RadialRenderState.hitNested(x0, y0, x1, y1, maxOf(1, current.size), radius2, i2, current[i2].sub.size, direction, thickness)
                if (hit != -1) {
                    i0 = i2
                    i1 = hit
                    return@on
                }
            }

            i1 = -1
            i0 = RadialRenderState.hit(x0, y0, x1, y1, maxOf(1, current.size), radius1, radius2, direction || (type == 2 && i2 != -1))
        }.runWhen(open)

        on<GuiEvent.Render.Any.Post> {
            val x = graphics.guiWidth() / 2
            val y = graphics.guiHeight() / 2
            val size = maxOf(1, current.size)

            val mini = if (type == 1 && i2 in current.indices) current[i2].sub else emptyList()
            val ring = layout()
            val ringI = if (type == 2 && i1 != -1) ring.getOrNull(i1)?.first ?: -1 else -1

            graphics.guiRenderState.addGuiElement(RadialRenderState(graphics, x, y, size, mini, ring, i3 = ringI))
            graphics.guiRenderState.nextStratum()

            for (i in current.indices) {
                val (x, y) = RadialRenderState.anchor(x, y, size, radius1, radius2, i)
                graphics.item(current[i].item, x - 8, y - 8)
            }

            if (type == 1 && i2 in current.indices) {
                for (j in current[i2].sub.indices) {
                    val (x, y) = RadialRenderState.nested(x, y, size, radius2, i2, j, thickness)
                    graphics.item(current[i2].sub[j].item, x - 8, y - 8)
                }
            }

            if (type == 2) {
                for ((i, s) in ring) {
                    val (x, y) = RadialRenderState.ring(x, y, size, radius2, i, thickness)
                    graphics.item(s.item, x - 8, y - 8)
                }
            }

            val bool0 = dist(x, y) < 15f
            val bool1 = stack.isNotEmpty() || (type == 2 && i2 != -1)

            VanillaFontRenderer.extract(graphics, if (bool1) "←" else "✕", x - VanillaFontMeasurer.width(if (bool1) "←" else "✕") / 2, y - VanillaFontMeasurer.height / 2, false, if (bool0) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb)

            val hovered = when {
                i1 != -1 && i2 in current.indices -> current[i2].sub.getOrNull(i1)
                else -> current.getOrNull(i0)
            }

            val label = if (bool0) (if (bool1) "Back" else "Exit") else hovered?.name ?: return@on
            val x1 = MouseInputState.Position.Scaled.x.toInt() + 12
            val y1 = MouseInputState.Position.Scaled.y.toInt() - 4

            graphics.rectangle(x1 - 5f, y1 - 5f, VanillaFontMeasurer.width(label) + 10f, VanillaFontMeasurer.height + 10f, MochaColorScheme.Base.argb)
            VanillaFontRenderer.extract(graphics, label, x1, y1, false, MochaColorScheme.Text.argb)
        }.runWhen(open)

        on<GuiEvent.Open.Any> {
            react(false, bool = false)
        }
    }

    fun disk() {
        saved = Athen.GSON.toJson(configs, object : TypeToken<Map<String, List<RadialSlot>>>() {}.type)
    }

    fun save() {
        configs[active] = slots.toList()
    }

    fun load(name: String) {
        save()
        active = name
        fill(name)
    }

    fun add(name: String) {
        configs[name] = emptyList()
        load(name)
    }

    fun delete(name: String) {
        if (configs.size <= 1) return
        configs.remove(name)
        if (active != name) return
        active = configs.keys.first()
        fill(active)
    }

    fun rename(old: String, new: String) {
        val data = configs.remove(old) ?: return
        configs[new] = data
        if (active == old) active = new
    }

    private fun fill(name: String) {
        slots.clear()
        configs[name]?.map { it.clone() }?.let { slots.addAll(it) }
    }

    private fun dist(x1: Int, y1: Int): Double {
        return hypot((MouseInputState.Position.Scaled.x - x1).toDouble(), (MouseInputState.Position.Scaled.y - y1).toDouble())
    }

    private fun back() {
        stack.removeLast()
        i0 = -1
        i2 = -1
    }

    private fun trigger() {
        when {
            i1 != -1 && i2 in current.indices -> current[i2].sub.getOrNull(i1)?.action?.run()
            i0 in current.indices -> current[i0].action.run()
        }
    }

    private fun react(v: Boolean, bool: Boolean) {
        open.value = v
        if (v) client.mouseHandler.releaseMouse() else if (bool) client.mouseHandler.grabMouse()
    }

    private fun help() {
        val divider = ("§8§m" + "-".repeat()).literal()
        divider.lie()
        "§bRadial Menu §7[Astral]".center().lie()
        divider.lie()
        " <dark_gray>• <${MochaColorScheme.Green.argb}>/astral radial edit <gray>- Opens editor".lie()
        " <dark_gray>• <${MochaColorScheme.Green.argb}>/astral import radial <gray>- Imports config from clipboard".lie()
        " <dark_gray>• <${MochaColorScheme.Green.argb}>/astral export radial <gray>- Exports current config to clipboard".lie()
        divider.lie()
        divider.lie()
    }

    private fun layout(): List<Pair<Int, RadialSlot>> {
        if (type != 2) return emptyList()
        if (i2 !in current.indices) return emptyList()

        val sub = current[i2].sub
        val n = sub.size
        val m = maxOf(1, current.size)
        val p = i2

        return List(n) { i ->
            ((p - n / 2 + i + m * 2) % m) to sub[i]
        }
    }
}
