@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.render.highlight.ui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.minecraft.text.measurer.VanillaFontMeasurer
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.render.highlight.MobHighlight
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.data.PositionAnchor
import foo.starred.cascade.constraints.impl.position.*
import foo.starred.cascade.constraints.impl.size.FillSizeConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricResolution
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive
import foo.starred.cascade.primitives.impl.ScrollablePrimitive.Companion.scrollable
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.snowbird.api.nextTick
import foo.starred.snowbird.utils.brighten
import foo.starred.snowbird.utils.literal
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType

object MobHighlightGUI : CascadeScreen("Mob Highlights [Astral]", CascadeGeometricResolution.FHD.of(2f)) {
    private var category = false
    private var deleting: Int? = null
    private var entry: Int? = null

    private var left: ScrollablePrimitive
    private var right: ScrollablePrimitive
    private var footer: RectanglePrimitive
    private var popup: MobHighlightPopUp

    private var `highlight$add`: RectanglePrimitive
    private var `highlight$edit`: RectanglePrimitive
    private var `highlight$delete`: RectanglePrimitive
    private lateinit var `highlight$text$edit`: TextPrimitive
    private lateinit var `highlight$text$delete`: TextPrimitive

    private data class CategoryRow(val row: RectanglePrimitive, val label: TextPrimitive)
    private data class EntryRow(val row: RectanglePrimitive, val swatch: RectanglePrimitive)

    private val rows0 = LinkedHashMap<Boolean, CategoryRow>()
    private val rows1 = LinkedHashMap<Int, EntryRow>()

    init {
        container {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            interact = false
            attach(scene)
        }

        val main = container {
            size = FixedSizeConstraint(576, 300)
            position = CenterPositionConstraint()
            attach(scene)
        }

        val side0 = rectangle {
            size = FixedSizeConstraint(110, 300)
            position = FixedPositionConstraint(0, 0)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            interact = false

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(main)
        }

        left = scrollable {
            size = FillSizeConstraint(4)
            position = CenterPositionConstraint()
            attach(side0)
        }

        val right0 = rectangle {
            size = FixedSizeConstraint(460, 260)
            position = FixedPositionConstraint(116, 0)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            interact = false

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(main)
        }

        right = scrollable {
            size = FillSizeConstraint(6)
            position = CenterPositionConstraint()
            attach(right0)
        }

        footer = rectangle {
            size = FixedSizeConstraint(460, 34)
            position = FixedPositionConstraint(116, 266)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)
            interact = false

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(main)
        }

        popup = MobHighlightPopUp(this) {
            popup.visible = false
            scene.focused = null
            list()
            categories()
            footer()
        }.apply {
            attach(scene)
            visible = false
        }

        `highlight$add` = rectangle {
            size = FixedSizeConstraint(120, 20)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(8f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.8f))

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.5f))
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                deleting = null
                popup.open(category)
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.9f))
            }

            on<MouseEvent.Move.Exit> {
                color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.8f))
            }

            attach(footer)
            adopt(text {
                text = "+ Add Highlight".literal()
                color = CascadeGeometricColor(MochaColorScheme.Base.argb)
                position = CenterPositionConstraint()
                shadow = false
            })
        }

        `highlight$edit` = rectangle {
            size = FixedSizeConstraint(70, 20)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(-82f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                val idx = entry ?: return@on

                deleting = null
                popup.open(category, idx)
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                if (entry != null) color = CascadeGeometricColor(MochaColorScheme.Lavender.argb.brighten(0.9f))
            }

            on<MouseEvent.Move.Exit> {
                if (entry != null) color = CascadeGeometricColor(MochaColorScheme.Lavender.argb.brighten(0.8f))
            }

            attach(footer)
            adopt(text {
                text = "Edit".literal()
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
                position = CenterPositionConstraint()
                shadow = false
            }.also { `highlight$text$edit` = it })
        }

        `highlight$delete` = rectangle {
            size = FixedSizeConstraint(70, 20)
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(-8f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                val idx = entry ?: return@on

                if (deleting != idx) {
                    deleting = idx
                    footer()
                    return@on
                }

                if (category) MobHighlight.e1.update { removeAt(idx) }
                else MobHighlight.e0.update { removeAt(idx) }

                deleting = null
                entry = null
                categories()
                list()
                footer()
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                if (entry != null) color = CascadeGeometricColor(MochaColorScheme.Red.argb.brighten(0.9f))
            }

            on<MouseEvent.Move.Exit> {
                if (entry != null) color = CascadeGeometricColor(MochaColorScheme.Red.argb.brighten(0.8f))
            }

            attach(footer)
            adopt(text {
                text = "Delete".literal()
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
                position = CenterPositionConstraint()
                shadow = false
            }.also { `highlight$text$delete` = it })
        }
    }

    fun pop(name: String?, type: EntityType<*>?, max: Int = -1) {
        open()
        nextTick {
            popup.open(name, type, max)
        }
    }

    override fun init() {
        super.init()
        entry = null
        deleting = null
        popup.visible = false
        categories()
        list()
        footer()
    }

    override fun onClose() {
        MobHighlight.json.save()
        super.onClose()
    }

    private fun categories() {
        left.children.clear()
        rows0.clear()

        val kv = listOf(false to "Named (${MobHighlight.e0.value.size})", true to "Typed (${MobHighlight.e1.value.size})")
        var cy = 4

        for ((k, v) in kv) {
            val b0 = category == k

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 20))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
                offset = CascadeGeometricOffset(0f, cy.toFloat())
                color = CascadeGeometricColor(if (b0) MochaColorScheme.Surface0.argb else MochaColorScheme.Base.argb)

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                    if (category == k) return@on

                    val p = category
                    category = k
                    entry = null
                    deleting = null
                    category(p)
                    category(k)
                    list()
                    footer()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    if (category != k) color = CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.5f))
                }

                on<MouseEvent.Move.Exit> {
                    if (category != k) color = CascadeGeometricColor(MochaColorScheme.Base.argb)
                }

                attach(left)
            }

            val label = text {
                text = v.literal()
                color = CascadeGeometricColor(if (b0) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(6f, 0f)
                attach(row)
            }

            rows0[k] = CategoryRow(row, label)
            cy += 22
        }
    }

    private fun category(key: Boolean) {
        val entry = rows0[key] ?: return
        val b0 = category == key

        entry.row.color = CascadeGeometricColor(if (b0) MochaColorScheme.Surface0.argb else MochaColorScheme.Base.argb)
        entry.label.color = CascadeGeometricColor(if (b0) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb)
    }

    private fun list() {
        right.children.clear()
        rows1.clear()

        val total = if (category) MobHighlight.e1.value.size else MobHighlight.e0.value.size
        if (total == 0) {
            text {
                text = (if (category) "No typed highlights" else "No named highlights").literal()
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = CenterPositionConstraint()
                attach(right)
            }

            return
        }

        var cy = 0
        for (i in 0 until total) {
            val bool = entry == i
            val color0: Int
            val label: String
            val max: Int

            if (category) {
                val e = MobHighlight.e1.value[i]
                color0 = e.color
                label = BuiltInRegistries.ENTITY_TYPE.getKey(e.type).toString()
                max = e.max
            } else {
                val e = MobHighlight.e0.value[i]
                color0 = e.color
                label = e.name
                max = e.max
            }

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 28))
                position = FixedPositionConstraint(0, cy)
                color = CascadeGeometricColor(if (bool) MochaColorScheme.Surface1.argb else MochaColorScheme.Surface0.argb)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(if (bool) MochaColorScheme.Lavender.argb else MochaColorScheme.Overlay0.argb)
                })

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                    val n = if (entry == i) null else i
                    if (entry == n) return@on

                    val previous = entry
                    entry = n
                    deleting = null
                    previous?.let(::entry)
                    n?.let(::entry)
                    footer()
                    cancel()
                }

                attach(right)
            }

            val swatch = rectangle {
                size = FixedSizeConstraint(14, 14)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(8f, 0f)
                color = CascadeGeometricColor(color0 or 0xFF000000.toInt())
                interact = false

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
                })

                attach(row)
            }

            text {
                text = label.literal()
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = MixedPositionConstraint(AnchorPositionConstraint({ swatch }, PositionAnchor.RIGHT), CenterPositionConstraint())
                offset = CascadeGeometricOffset(8f, 0f)
                attach(row)
            }

            rectangle {
                val hp = if (max == -1) "HP: any" else "HP: $max"
                val width = VanillaFontMeasurer.width(hp) + 8

                size = FixedSizeConstraint(width, 14)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(-8f, 0f)
                color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
                interact = false

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Crust.argb)
                })

                attach(row)
                adopt(text {
                    text = hp.literal()
                    color = CascadeGeometricColor(if (max == -1) MochaColorScheme.Subtext0.argb else MochaColorScheme.Peach.argb)
                    position = CenterPositionConstraint()
                })
            }

            rows1[i] = EntryRow(row, swatch)
            cy += 32
        }
    }

    private fun entry(index: Int) {
        val row = rows1[index] ?: return
        val bool = entry == index

        row.row.color = CascadeGeometricColor(if (bool) MochaColorScheme.Surface1.argb else MochaColorScheme.Surface0.argb)
        row.row.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (bool) MochaColorScheme.Lavender.argb else MochaColorScheme.Overlay0.argb)
    }

    private fun footer() {
        val bool0 = entry != null
        val bool1 = deleting != null

        `highlight$edit`.color = CascadeGeometricColor(if (bool0) MochaColorScheme.Lavender.argb.brighten(0.8f) else MochaColorScheme.Surface1.argb)
        `highlight$edit`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (bool0) MochaColorScheme.Lavender.argb.brighten(0.5f) else MochaColorScheme.Surface0.argb)
        `highlight$text$edit`.color = CascadeGeometricColor(if (bool0) MochaColorScheme.Base.argb else MochaColorScheme.Overlay0.argb)

        `highlight$delete`.color = CascadeGeometricColor(if (!bool0) MochaColorScheme.Surface1.argb else if (bool1) MochaColorScheme.Red.argb.brighten(0.9f) else MochaColorScheme.Red.argb.brighten(0.8f))
        `highlight$delete`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (!bool0) MochaColorScheme.Surface0.argb else MochaColorScheme.Red.argb.brighten(0.5f))
        `highlight$text$delete`.color = CascadeGeometricColor(if (bool0) MochaColorScheme.Base.argb else MochaColorScheme.Overlay0.argb)
        `highlight$text$delete`.text = (if (bool1) "✔" else "Delete").literal()
    }
}
