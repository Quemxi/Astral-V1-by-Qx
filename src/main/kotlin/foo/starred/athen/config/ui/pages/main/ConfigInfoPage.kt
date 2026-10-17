package foo.starred.athen.config.ui.pages.main

import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.config.ui.ConfigUI.right
import foo.starred.athen.utils.data
import foo.starred.cascade.animation.extension.impl.animate
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.data.PositionAnchor
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.AnchorPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive.Companion.roundedRectangle
import foo.starred.cascade.primitives.impl.TextPrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.literal
import foo.starred.snowbird.utils.open
import kotlin.time.Duration.Companion.milliseconds

object ConfigInfoPage {
    private var changelogs: List<String> = emptyList()
    private val texts = mutableListOf<TextPrimitive>()

    fun fn() {
        get()

        val links = container {
            position = FixedPositionConstraint(10f, 10f)
            size = FixedSizeConstraint(482f, 28f)

            attach(right)

            val list = emptyList<Pair<String, String>>()
            var last: IPrimitiveElement<*>? = null

            for ((k, v) in list.reversed()) {
                val last0 = last
                last = roundedRectangle {
                    position = if (last0 == null) AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER) else AnchorPositionConstraint({ last0 }, PositionAnchor.LEFT)
                    offset = if (last0 == null) CascadeGeometricOffset.ZERO else CascadeGeometricOffset(-10f, 0f)
                    size = FixedSizeConstraint(154f, 28f)
                    color = CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.35f))
                    radius = CascadeGeometricRadius(4f)

                    effect(OutlineEffect {
                        color = CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.2f))
                        inset = false
                    })

                    on<MouseEvent.Move.Enter> {
                        animate(150.milliseconds) {
                            ::color to CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.2f))
                        }
                    }

                    on<MouseEvent.Move.Exit> {
                        animate(150.milliseconds) {
                            ::color to CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.3f))
                        }
                    }

                    on<MouseEvent.Press> {
                        cancel()
                        v.open()
                    }

                    attach(this@container)
                    adopt(text {
                        wrapper = CascadeTextWrapper
                        text = k.literal()
                        textSize = 12f
                        color = CascadeGeometricColor(MochaColorScheme.Lavender.argb)
                        position = CenterPositionConstraint()
                    })
                }
            }
        }

        val card0 = roundedRectangle {
            position = AnchorPositionConstraint({ links }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 10f)
            size = FixedSizeConstraint(482f, 96f)
            color = CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.3f))
            radius = CascadeGeometricRadius(4f)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.18f))
                inset = false
            })

            attach(right)
            adopt(text {
                wrapper = CascadeTextWrapper
                text = "<bold><#FDCCDA>A<#FCDDD3>t<#FAEDCB>h<#F0E2D7>e<#E5D8E4>n<#DBCDF0> <white>Configuration".parse()
                textSize = 18f
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = FixedPositionConstraint(14f, 14f)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "- Run /astral help to view all commands".literal()
                textSize = 12f
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = FixedPositionConstraint(14f, 40f)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "- Run /astral hud to open hud editor".literal()
                textSize = 12f
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = FixedPositionConstraint(14f, 56f)
            })

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "- Donate to get cosmetics such as custom name and size along with other perks!".literal()
                textSize = 12f
                color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                position = FixedPositionConstraint(14f, 72f)
            })
        }

        roundedRectangle {
            position = AnchorPositionConstraint({ card0 }, PositionAnchor.BELOW)
            offset = CascadeGeometricOffset(0f, 10f)
            size = FixedSizeConstraint(482f, 96f)
            color = CascadeGeometricColor(MochaColorScheme.Surface0.alpha(0.3f))
            radius = CascadeGeometricRadius(4f)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Lavender.alpha(0.18f))
                inset = false
            })

            attach(right)
            adopt(text {
                wrapper = CascadeTextWrapper
                text = "<bold>Changelogs".parse()
                textSize = 18f
                color = CascadeGeometricColor(MochaColorScheme.Text.argb)
                position = FixedPositionConstraint(14f, 14f)
            })

            texts.clear()
            val list = changelogs.ifEmpty { listOf("Loading changelogs...", "Loading changelogs...", "Loading changelogs...") }
            var y0 = 40f

            for (log in list.take(3)) {
                adopt(text {
                    wrapper = CascadeTextWrapper
                    text = "- $log".literal()
                    textSize = 12f
                    color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                    position = FixedPositionConstraint(14f, y0)
                }.also { texts.add(it) })

                y0 += 16f
            }
        }
    }

    private fun get() {
        if (changelogs.isNotEmpty()) return
        "athen/changelogs.json".data.request {
            success<List<String>> {
                changelogs = it

                for ((k, v) in it.take(3).withIndex()) {
                    texts.getOrNull(k)?.text = "- $v".literal()
                }
            }
        }
    }
}
