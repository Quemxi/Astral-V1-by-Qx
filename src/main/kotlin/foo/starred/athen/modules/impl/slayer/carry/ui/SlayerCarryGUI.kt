@file:Suppress("ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.slayer.carry.ui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.minecraft.text.measurer.VanillaFontMeasurer
import foo.starred.athen.api.slayers.enums.type.impl.SlayerBoss
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.slayer.carry.impl.SlayerCarryTracker
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
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive
import foo.starred.cascade.primitives.impl.ScrollablePrimitive.Companion.scrollable
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.screen.CascadeScreen
import foo.starred.snowbird.utils.brighten
import foo.starred.snowbird.utils.literal

object SlayerCarryGUI : CascadeScreen("Slayer Carries [Astral]") {
    private var filter: SlayerBoss? = null
    private var selected: Int? = null
    private var deleting: Int? = null

    private var left: ScrollablePrimitive
    private var right: ScrollablePrimitive
    private var footer: RectanglePrimitive
    private var `carry$complete`: RectanglePrimitive
    private var `carry$delete`: RectanglePrimitive
    private lateinit var `carry$complete$text`: TextPrimitive
    private lateinit var `carry$delete$text`: TextPrimitive

    private data class FilterRow(val row: RectanglePrimitive, val label: TextPrimitive)

    private val rows0 = LinkedHashMap<SlayerBoss?, FilterRow>()
    private val rows1 = LinkedHashMap<Int, RectanglePrimitive>()

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
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 300))
            position = FixedPositionConstraint(0, 0)
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

        `carry$complete` = rectangle {
            size = PercentSizeConstraint(49f, 78f)
            position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
            offset = CascadeGeometricOffset(4f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                val idx = selected ?: return@on

                SlayerCarryTracker.complete(idx)
                selected = null
                deleting = null
                list()
                footer()
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                if (selected != null) color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.9f))
            }

            on<MouseEvent.Move.Exit> {
                if (selected != null) color = CascadeGeometricColor(MochaColorScheme.Green.argb.brighten(0.8f))
            }

            attach(footer)
            adopt(text {
                text = "Complete".literal()
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
                shadow = false
                position = CenterPositionConstraint()
            }.also { `carry$complete$text` = it })
        }

        `carry$delete` = rectangle {
            size = PercentSizeConstraint(49f, 78f)
            position = AnchorPositionConstraint({ `carry$complete` }, PositionAnchor.RIGHT)
            offset = CascadeGeometricOffset(3f, 0f)
            color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                val idx = selected ?: return@on

                if (deleting != idx) {
                    deleting = idx
                    footer()
                    return@on
                }

                SlayerCarryTracker.remove(idx)
                selected = null
                deleting = null
                list()
                footer()
                cancel()
            }

            on<MouseEvent.Move.Enter> {
                if (selected != null) color = CascadeGeometricColor(MochaColorScheme.Red.argb.brighten(0.9f))
            }

            on<MouseEvent.Move.Exit> {
                if (selected != null) color = CascadeGeometricColor(MochaColorScheme.Red.argb.brighten(0.8f))
            }

            attach(footer)
            adopt(text {
                text = "Delete".literal()
                color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
                shadow = false
                position = CenterPositionConstraint()
            }.also { `carry$delete$text` = it })
        }

        filters()
        list()
    }

    override fun onClose() {
        selected = null
        deleting = null
        super.onClose()
    }

    private fun filters() {
        left.children.clear()
        rows0.clear()

        val kv = sequenceOf(null to "Global") + SlayerBoss.entries.map { it to it.display.substringBefore(" ") }
        var cy = 4

        for ((k, v) in kv) {
            val b0 = filter == k

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 20))
                position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
                offset = CascadeGeometricOffset(0f, cy.toFloat())
                color = CascadeGeometricColor(if (b0) MochaColorScheme.Surface0.argb else MochaColorScheme.Base.argb)

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                    if (filter == k) return@on

                    val p = filter
                    filter = k
                    selected = null
                    deleting = null
                    filter(p)
                    filter(k)
                    list()
                    footer()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    if (filter != k) color = CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.5f))
                }

                on<MouseEvent.Move.Exit> {
                    if (filter != k) color = CascadeGeometricColor(MochaColorScheme.Base.argb)
                }

                attach(left)
            }

            val label = text {
                text = v.literal()
                color = CascadeGeometricColor(if (b0) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(4f, 0f)
                attach(row)
            }

            rows0[k] = FilterRow(row, label)
            cy += 20
        }
    }

    private fun list() {
        right.children.clear()
        rows1.clear()

        val all = SlayerCarryTracker.tracked.value
        val kv = if (filter != null) all.mapIndexed { i, c -> i to c }.filter { it.second.type == filter } else all.mapIndexed { i, c -> i to c }

        if (kv.isEmpty()) {
            text {
                text = "No active carries".literal()
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = CenterPositionConstraint()
                attach(right)
            }

            return
        }

        var cy = 0
        for ((index, carry) in kv) {
            val b1 = selected == index

            val row = rectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 28))
                position = FixedPositionConstraint(0, cy)
                color = CascadeGeometricColor(if (b1) MochaColorScheme.Surface1.argb else MochaColorScheme.Surface0.argb)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(if (b1) MochaColorScheme.Lavender.argb else MochaColorScheme.Overlay0.argb)
                })

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                    val n = if (selected == index) null else index
                    if (selected == n) return@on

                    val previous = selected
                    selected = n
                    deleting = null
                    previous?.let(::entry)
                    n?.let(::entry)
                    footer()
                    cancel()
                }

                attach(right)
            }

            rectangle {
                val s = "${carry.type.short}${carry.tier?.let { " T${it.int}" } ?: " Any"}"
                val w = VanillaFontMeasurer.width(s) + 8

                size = FixedSizeConstraint(w, 16)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(8f, 0f)
                color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
                interact = false

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Crust.argb)
                })

                attach(row)
                adopt(text {
                    text = s.literal()
                    color = CascadeGeometricColor(MochaColorScheme.Lavender.argb)
                    position = CenterPositionConstraint()
                })

                adopt(text {
                    text = carry.name.literal()
                    color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                    position = MixedPositionConstraint(AnchorPositionConstraint({ this@rectangle }, PositionAnchor.RIGHT), CenterPositionConstraint())
                    offset = CascadeGeometricOffset(8f, 0f)
                    attach(row)
                })
            }

            var x0 = -8
            rectangle {
                size = FixedSizeConstraint(16, 16)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(x0, 0f)
                color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
                })

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                    SlayerCarryTracker.update(index, carry.name, carry.type, carry.tier, carry.max + 1, carry.done)
                    list()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
                }

                on<MouseEvent.Move.Exit> {
                    color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
                }

                attach(row)
                adopt(text {
                    text = "+".literal()
                    color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                    position = CenterPositionConstraint()
                })
            }

            x0 -= 20
            val s0 = carry.max.toString()
            text {
                text = s0.literal()
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(x0, 0f)
                attach(row)
            }

            x0 -= VanillaFontMeasurer.width(s0) + 4
            rectangle {
                size = FixedSizeConstraint(16, 16)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(x0, 0f)
                color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
                })

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                    if (carry.max <= 1) return@on

                    SlayerCarryTracker.update(index, carry.name, carry.type, carry.tier, carry.max - 1, carry.done)
                    list()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
                }

                on<MouseEvent.Move.Exit> {
                    color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
                }

                attach(row)
                adopt(text {
                    text = "-".literal()
                    color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                    position = CenterPositionConstraint()
                })
            }

            x0 -= 20
            text {
                text = "Total:".literal()
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(x0, 0f)
                attach(row)
            }

            x0 -= VanillaFontMeasurer.width("Total:") + 12
            rectangle {
                size = FixedSizeConstraint(16, 16)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(x0, 0f)
                color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
                })

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on

                    SlayerCarryTracker.update(index, carry.name, carry.type, carry.tier, carry.max, carry.done + 1)
                    list()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
                }

                on<MouseEvent.Move.Exit> {
                    color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
                }

                attach(row)
                adopt(text {
                    text = "+".literal()
                    color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                    position = CenterPositionConstraint()
                })
            }

            x0 -= 20
            val s1 = carry.done.toString()
            text {
                text = s1.literal()
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(x0, 0f)
                attach(row)
            }

            x0 -= VanillaFontMeasurer.width(s1) + 4
            rectangle {
                size = FixedSizeConstraint(16, 16)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(x0, 0f)
                color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
                })

                on<MouseEvent.Press> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                    if (carry.done <= 0) return@on

                    SlayerCarryTracker.update(index, carry.name, carry.type, carry.tier, carry.max, carry.done - 1)
                    list()
                    cancel()
                }

                on<MouseEvent.Move.Enter> {
                    color = CascadeGeometricColor(MochaColorScheme.Surface2.argb)
                }

                on<MouseEvent.Move.Exit> {
                    color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
                }

                attach(row)
                adopt(text {
                    text = "-".literal()
                    color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                    position = CenterPositionConstraint()
                })
            }

            x0 -= 20
            text {
                text = "Done:".literal()
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(x0, 0f)
                attach(row)
            }

            rows1[index] = row
            cy += 32
        }
    }

    private fun filter(boss: SlayerBoss?) {
        val entry = rows0[boss] ?: return
        val b0 = filter == boss

        entry.row.color = CascadeGeometricColor(if (b0) MochaColorScheme.Surface0.argb else MochaColorScheme.Base.argb)
        entry.label.color = CascadeGeometricColor(if (b0) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb)
    }

    private fun entry(index: Int) {
        val row = rows1[index] ?: return
        val b1 = selected == index

        row.color = CascadeGeometricColor(if (b1) MochaColorScheme.Surface1.argb else MochaColorScheme.Surface0.argb)
        row.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (b1) MochaColorScheme.Lavender.argb else MochaColorScheme.Overlay0.argb)
    }

    private fun footer() {
        val b = selected != null
        val b2 = b && deleting == selected

        `carry$complete`.color = CascadeGeometricColor(if (b) MochaColorScheme.Green.argb.brighten(0.8f) else MochaColorScheme.Surface1.argb)
        `carry$complete`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (b) MochaColorScheme.Green.argb.brighten(0.5f) else MochaColorScheme.Surface0.argb)
        `carry$complete$text`.color = CascadeGeometricColor(if (b) MochaColorScheme.Base.argb else MochaColorScheme.Overlay0.argb)

        `carry$delete`.color = CascadeGeometricColor(if (!b) MochaColorScheme.Surface1.argb else if (b2) MochaColorScheme.Red.argb.brighten(0.9f) else MochaColorScheme.Red.argb.brighten(0.8f))
        `carry$delete`.effect<OutlineEffect>()?.color = CascadeGeometricColor(if (!b) MochaColorScheme.Surface0.argb else MochaColorScheme.Red.argb.brighten(0.5f))
        `carry$delete$text`.color = CascadeGeometricColor(if (b) MochaColorScheme.Base.argb else MochaColorScheme.Overlay0.argb)
        `carry$delete$text`.text = (if (b2) "Confirm?" else "Delete").literal()
    }
}
