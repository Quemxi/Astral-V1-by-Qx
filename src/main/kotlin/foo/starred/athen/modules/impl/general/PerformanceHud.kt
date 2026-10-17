@file:Suppress("UNUSED")

package foo.starred.athen.modules.impl.general

import foo.starred.athen.annotations.Load
import foo.starred.athen.api.minecraft.text.measurer.VanillaFontMeasurer
import foo.starred.athen.api.minecraft.text.renderer.VanillaFontRenderer
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.modules.Module
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.player
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.floor

/**
 * Astral: a clean, movable performance panel (FPS, ping, TPS and a few extras).
 *
 * Purely passive: it only reads values the game already has and draws them on screen.
 * Move it with the HUD editor (`/astral hud`) and toggle every row from the config menu.
 */
@Load
object PerformanceHud : Module(
    "Performance HUD",
    "A clean panel with FPS, ping, TPS and more. Move it with the HUD editor.",
    ConfigCategory.GENERAL,
    true
) {
    private val showFps = config.switch("Show FPS", true).unique("showFps")
    private val showPing = config.switch("Show ping", true).unique("showPing")
    private val showTps = config.switch("Show TPS", true).unique("showTps")
    private val showCoords = config.switch("Show coordinates").unique("showCoords")
    private val showFacing = config.switch("Show facing direction").unique("showFacing")
    private val showClock = config.switch("Show clock").unique("showClock")
    private val showMemory = config.switch("Show memory").unique("showMemory")

    private val palette = config.selector(
        "Colors",
        listOf("Standard", "Colorblind-friendly", "Plain white")
    ).unique("palette")

    private val textShadow = config.switch("Text shadow", true).unique("textShadow")

    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")

    // FPS: counted from rendered HUD frames
    private var frames = 0
    private var frameStart = System.nanoTime()
    private var fps = 0

    // TPS: counted from server tick signals (-1 means no data yet)
    private var serverTicks = 0
    private var tickStart = System.nanoTime()
    private var tps = -1.0

    private val hud by config.hud("Performance panel") {
        constrain {
            VanillaFontMeasurer.constrain(lines(example = true))
        }

        preview {
            VanillaFontRenderer.extract(graphics, lines(example = true), 0, 0, textShadow.value)
        }

        render {
            countFrame()
            VanillaFontRenderer.extract(graphics, lines(), 0, 0, textShadow.value)
        }
    }

    init {
        // Resize the panel in the HUD editor when rows are toggled.
        for (option in listOf(showFps, showPing, showTps, showCoords, showFacing, showClock, showMemory)) {
            option.state.observe { hud.constrain() }
        }

        on<TickEvent.Server> {
            serverTicks++
        }

        on<TickEvent.Client.End> {
            val now = System.nanoTime()
            val elapsed = now - tickStart
            if (elapsed < 2_000_000_000L) return@on

            tps = if (serverTicks == 0) -1.0 else (serverTicks * 1_000_000_000.0 / elapsed).coerceIn(0.0, 20.0)
            serverTicks = 0
            tickStart = now
        }

        on<LocationEvent.Server.Connect> {
            serverTicks = 0
            tickStart = System.nanoTime()
            tps = -1.0
        }
    }

    private fun countFrame() {
        frames++
        val now = System.nanoTime()
        val elapsed = now - frameStart
        if (elapsed < 1_000_000_000L) return

        fps = (frames * 1_000_000_000.0 / elapsed).toInt()
        frames = 0
        frameStart = now
    }

    private fun good(): String = when (palette.value) {
        1 -> "§b"
        2 -> "§f"
        else -> "§a"
    }

    private fun warn(): String = when (palette.value) {
        1 -> "§6"
        2 -> "§f"
        else -> "§e"
    }

    private fun bad(): String = when (palette.value) {
        1 -> "§d"
        2 -> "§f"
        else -> "§c"
    }

    private fun ping(): Int {
        val uuid = player?.uuid ?: return -1
        return client.connection?.getPlayerInfo(uuid)?.latency ?: -1
    }

    private fun facing(yaw: Float): String {
        val d = ((yaw % 360f) + 360f) % 360f
        return listOf("South", "West", "North", "East")[((d + 45f) / 90f).toInt() % 4]
    }

    private fun lines(example: Boolean = false): List<String> = buildList {
        if (showFps.value) {
            val v = if (example) 144 else fps
            val c = if (v >= 60) good() else if (v >= 30) warn() else bad()
            add("§7FPS: $c$v")
        }

        if (showPing.value) {
            val v = if (example) 42 else ping()
            val c = if (v < 0) "§7" else if (v <= 100) good() else if (v <= 200) warn() else bad()
            add("§7Ping: $c${if (v < 0) "--" else "${v}ms"}")
        }

        if (showTps.value) {
            val v = if (example) 20.0 else tps
            val c = if (v < 0) "§7" else if (v >= 19.0) good() else if (v >= 15.0) warn() else bad()
            add("§7TPS: $c${if (v < 0) "--" else "%.1f".format(v)}")
        }

        if (showCoords.value) {
            val p = player
            val text = if (example || p == null) "0 64 0" else "${floor(p.x).toInt()} ${floor(p.y).toInt()} ${floor(p.z).toInt()}"
            add("§7XYZ: §f$text")
        }

        if (showFacing.value) {
            val p = player
            add("§7Facing: §f${if (example || p == null) "North" else facing(p.yRot)}")
        }

        if (showClock.value) {
            add("§7Time: §f${LocalTime.now().format(clockFormat)}")
        }

        if (showMemory.value) {
            val rt = Runtime.getRuntime()
            val used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
            val max = rt.maxMemory() / (1024 * 1024)
            val ratio = if (max == 0L) 0.0 else used.toDouble() / max
            val c = if (ratio < 0.7) good() else if (ratio < 0.9) warn() else bad()
            add("§7Mem: $c$used§7/${max}MB")
        }
    }
}
