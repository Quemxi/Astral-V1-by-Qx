@file:Suppress("Unused", "ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.render

import com.mojang.serialization.Codec
import foo.starred.athen.annotations.Load
import foo.starred.athen.api.messaging.enums.MessagePrefixType
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.minecraft.mod.ModWrapper
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.GameEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.command
import foo.starred.snowbird.api.*
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.api.text.replacer.AbstractTextReplacer
import foo.starred.snowbird.utils.literal
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.FormattedCharSequence

@Load
object VisualWords : Module(
    "Visual words",
    "Visually modify words!",
    ConfigCategory.RENDER
) {
    private const val SKIP = "\u0000vw_bypass"

    private val unused by config.information("Command: <red>/astral visuals help")
    private val nameChanger = config.switch("Name changer").unique("nameChanger")
    private val nickname = config.input("Nickname", "cooluser4").unique("nickname")

    private val json = JsonStore("features/visualWords")
    private var stored by json.map("words", Codec.STRING, ComponentSerialization.CODEC.xmap({ it.visualOrderText }, { seq -> seq.toComponent() }))

    @JvmField
    val words = object : AbstractTextReplacer() {}.also { it.skips = SKIP }

    init {
        observable.observe {
            words.version++
        }

        if (nameChanger.value) {
            nickname.value.fn()
        }

        nickname.state.observe {
            it.fn()
        }

        nameChanger.state.observe {
            if (it) return@observe nickname.value.fn()
            words.remove(name)
            words.build()
        }

        on<GameEvent.Start> {
            load()
        }

        on<GameEvent.Stop> {
            save()
        }

        command {
            "visuals" {
                help()
            }

            "visuals" / "help" {
                help()
            }

            "visuals" / "add" / string("word") / greedyString("replacement") {
                val a = string("word")
                val b = string("replacement").parse()
                val c = b.visualOrderText

                words.put(a, b.string, b, c)
                words.build()
                save()

                "Added the word <red>\"$a\" <gray>-> ".parse().skip().append(b).mod()
                if (!enabled) "Feature not enabled!".mod(MessagePrefixType.ERROR)
            }

            "visuals" / "set" / string("word") / greedyString("replacement") {
                val a = string("word")
                val b = string("replacement").parse()
                val c = b.visualOrderText

                words.put(a, b.string, b, c)
                words.build()
                save()

                "Set the word <red>\"$a\" <gray>-> ".parse().skip().append(b).mod()
                if (!enabled) "Feature not enabled!".mod(MessagePrefixType.ERROR)
            }

            "visuals" / "remove" / string("word") {
                val a = string("word")

                words.remove(a)
                words.build()
                save()

                "Removed the word <red>\"$a\"".parse().skip().mod()
                if (!enabled) "Feature not enabled!".mod(MessagePrefixType.ERROR)
            }

            "visuals" / "list" {
                "Replacement words list:".mod()
                for ((a, b) in words.map2) " <dark_gray>• <r>$a <gray>-> ".parse().skip().append(b.toComponent()).lie()
            }
        }
    }

    private fun String.fn() {
        if (nameChanger.value && isNotEmpty()) {
            val cmp = parse(true)
            words.put(name, cmp.string, cmp, cmp.visualOrderText)
            words.build()
            return
        }

        words.remove(name)
        words.build()
    }

    private fun help() {
        val divider = ("§8§m" + "-".repeat()).literal()
        divider.lie()
        "§bVisual Words §7[Astral]".center().lie()
        divider.lie()
        " <dark_gray>• <${MochaColorScheme.Green.argb}>/astral visuals add [word] [word, supports space]".lie()
        " <dark_gray>• <${MochaColorScheme.Green.argb}>/astral visuals set [word] [word, supports space]".lie()
        " <dark_gray>• <${MochaColorScheme.Green.argb}>/astral visuals remove [word]".lie()
        " <dark_gray>• <${MochaColorScheme.Green.argb}>/astral visuals list".lie()
        divider.lie()
        " <dark_gray>• <r>The text supports the format: ".parse().append("<hex><bold>te</bold>xt").lie()
        divider.lie()
    }

    private fun load() {
        for ((k, v) in stored) {
            val c = v.toComponent()
            words.put(k, c.string, c, v)
        }

        words.build()
    }

    private fun save() {
        stored = words.map2
        words.build()
    }

    private fun Component.skip(): MutableComponent =
        copy().withStyle(style.withInsertion(SKIP))

    private fun FormattedCharSequence.toComponent(): Component {
        val builder = EMPTY_COMPONENT.copy()

        accept { _, style, cp ->
            builder.append(Character.toString(cp).literal().withStyle(style))
            true
        }

        return builder
    }
}
