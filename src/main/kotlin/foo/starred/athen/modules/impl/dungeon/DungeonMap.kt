@file:Suppress("UNUSED")

package foo.starred.athen.modules.impl.dungeon

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.dungeon.DungeonAPI
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.modules.Module
import foo.starred.snowbird.api.level
import foo.starred.snowbird.api.player
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.saveddata.maps.MapItemSavedData

/**
 * Astral: shows the dungeon map from your hotbar as a movable on-screen element.
 *
 * Display only. It reads the map data the game already sends you (the map item in the last
 * hotbar slot) and draws it. It does not scan rooms or read anything hidden.
 * Move and resize it with the HUD editor (`/astral hud`).
 */
@Load
@OnlyIn(skyblock = true)
object DungeonMap : Module(
    "Dungeon map",
    "Shows the dungeon map on your screen. Move and resize it with the HUD editor.",
    ConfigCategory.DUNGEONS
) {
    private val backdrop by config.switch("Dark background", true)

    private class Rect(val x: Int, val y: Int, val w: Int, var h: Int, val argb: Int)

    // Vanilla map base colours by id (0 is transparent). Anything above the list falls back to grey.
    private val baseColors = intArrayOf(
        0x000000, 0x7FB238, 0xF7E9A3, 0xC7C7C7, 0xFF0000, 0xA0A0FF, 0xA7A7A7, 0x007C00,
        0xFFFFFF, 0xA4A8B8, 0x976D4D, 0x707070, 0x4040FF, 0x8F7748, 0xFFFCF5, 0xD87F33,
        0xB24CD8, 0x6699D8, 0xE5E533, 0x7FCC19, 0xF27FA5, 0x4C4C4C, 0x999999, 0x4C7F99,
        0x7F3FB2, 0x334CB2, 0x664C33, 0x667F33, 0x993333, 0x191919, 0xFAEE4D, 0x5CDBD5,
        0x4A80FF, 0x00D93A, 0x815631, 0x700200
    )

    // Brightness steps used by map colours: low, normal, high, lowest.
    private val shade = intArrayOf(180, 220, 255, 135)

    // Raw map byte (colour id * 4 + brightness) -> ARGB, 0 means "draw nothing".
    private val palette = IntArray(256) { raw ->
        val id = raw shr 2
        if (id == 0) {
            0
        } else {
            val rgb = baseColors.getOrElse(id) { 0x999999 }
            val m = shade[raw and 3]
            val r = ((rgb shr 16) and 0xFF) * m / 255
            val g = ((rgb shr 8) and 0xFF) * m / 255
            val b = (rgb and 0xFF) * m / 255
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    private var cachedHash = 0
    private var cachedRects: List<Rect> = emptyList()

    private val hud by config.hud("Dungeon map") {
        constrain {
            128 to 128
        }

        preview {
            drawPreview(this)
        }

        render {
            if (DungeonAPI.floor.value == null) return@render
            drawMap(this, mapData())
        }
    }

    private fun mapData(): MapItemSavedData? {
        val p = player ?: return null
        val l = level ?: return null
        val id = p.inventory.getItem(8).get(DataComponents.MAP_ID) ?: return null
        return l.getMapData(id)
    }

    private fun drawMap(g: GuiGraphicsExtractor, data: MapItemSavedData?) {
        if (backdrop) g.fill(0, 0, 128, 128, 0x80000000.toInt())
        if (data == null) return

        val colors = data.colors
        val hash = colors.contentHashCode()
        if (hash != cachedHash) {
            cachedHash = hash
            cachedRects = buildRects(colors)
        }

        for (r in cachedRects) g.fill(r.x, r.y, r.x + r.w, r.y + r.h, r.argb)
    }

    // Row-by-row runs of equal colour, merged vertically, so we draw hundreds of rectangles instead of 16,384.
    private fun buildRects(colors: ByteArray): List<Rect> {
        val done = ArrayList<Rect>()
        var open = HashMap<Triple<Int, Int, Int>, Rect>()

        for (y in 0 until 128) {
            val next = HashMap<Triple<Int, Int, Int>, Rect>()
            var x = 0
            while (x < 128) {
                val argb = palette[colors[y * 128 + x].toInt() and 0xFF]
                var end = x + 1
                while (end < 128 && palette[colors[y * 128 + end].toInt() and 0xFF] == argb) end++

                if (argb != 0) {
                    val key = Triple(x, end - x, argb)
                    val prev = open[key]
                    if (prev != null) {
                        prev.h++
                        next[key] = prev
                    } else {
                        next[key] = Rect(x, y, end - x, 1, argb)
                    }
                }
                x = end
            }

            for ((key, rect) in open) if (key !in next) done.add(rect)
            open = next
        }

        done.addAll(open.values)
        return done
    }

    // A simple fake map so the element can be positioned in the HUD editor.
    private fun drawPreview(g: GuiGraphicsExtractor) {
        g.fill(0, 0, 128, 128, 0x80000000.toInt())
        for (gx in 0 until 5) {
            for (gy in 0 until 5) {
                val colour = when ((gx + gy) % 4) {
                    0 -> 0xFF8B5A2B.toInt()
                    1 -> 0xFFE5E533.toInt()
                    2 -> 0xFF7F3FB2.toInt()
                    else -> 0xFF5CB85C.toInt()
                }
                val x = 8 + gx * 24
                val y = 8 + gy * 24
                g.fill(x, y, x + 18, y + 18, colour)
            }
        }
    }
}
