@file:Suppress("Unused", "ObjectPrivatePropertyName")

package foo.starred.athen.modules.impl.general

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.api.messaging.enums.MessagePrefixType
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.scheduling.Scheduler
import foo.starred.athen.api.storage.JsonStore
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.MessageEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.command
import foo.starred.kbus.extensions.runWhen
import foo.starred.snowbird.api.scheduling.scheduler.data.tasks.base.SchedulerTask
import foo.starred.snowbird.api.scheduling.scheduler.extensions.clientTicks
import foo.starred.snowbird.api.text.parser.impl.parse
import foo.starred.snowbird.utils.fromLongDuration
import foo.starred.snowbird.utils.showTitle
import foo.starred.snowbird.utils.toDurationFromMillis
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

@Load
@OnlyIn(skyblock = true)
object KatReminder : Module(
    "Kat reminder",
    "Reminds you about your pet that you gave to upgrade to Kat!",
    ConfigCategory.GENERAL
) {
    private val showTitle by config.switch("Show title", true)
    private val message by config.input("Alert message", "<red>#pet<white> is waiting for you at Kat!")
    private val _unused0 by config.variables("#pet")
    private val _unused1 by config.information("Commands: <red>/astral times kat<r>, <red>/astral clear kat")

    private val json = JsonStore("features/katReminder")
    private var pet by json.string("pet")
    private var time by json.long("time")

    private val giveRegex = Regex("^\\[NPC] Kat: I'll get your (?<pet>\\w+) upgraded to \\w+ in no time!$")
    private val durationRegex = Regex("^\\[NPC] Kat: Come back in (?<duration>.+) to pick it up!$")

    private val remindRegex = Regex("^\\[NPC] Kat: I'm currently taking care of your (?<pet>\\w+)!$")
    private val durationRemindRegex = Regex("^\\[NPC] Kat: You can pick it up in (?<duration>.+)\\.$")

    private var `warning$kat$sentOnce` = false
    private var task: SchedulerTask? = null

    init {
        fn()

        command {
            "clear" / "kat" {
                if (!`warning$kat$sentOnce`) {
                    "<red>This WILL clear your Kat time info! Only use this if you know what you're doing.".mod(MessagePrefixType.ERROR)
                    `warning$kat$sentOnce` = true
                    return@invoke
                }

                reset()
                "Kat time info was successfully reset.".mod(MessagePrefixType.SUCCESS)
                `warning$kat$sentOnce` = false
            }

            "times" / "kat" {
                if (time == 0L) return@invoke "No pet being upgraded!".mod(MessagePrefixType.ERROR)

                val d = (time - System.currentTimeMillis()).toDurationFromMillis()
                "Time until upgrade for <red>$pet<r>: <red>$d".mod()
            }
        }

        on<LocationEvent.SkyBlock.Connect> {
            if (time <= 0) return@on
            if (time > System.currentTimeMillis()) return@on

            Scheduler.schedule(10.clientTicks) {
                fn0()
            }
        }

        on<MessageEvent.Chat.Receive> {
            if ("[NPC] Kat: " !in stripped) return@on

            when (stripped) {
                "[NPC] Kat: A flower? For me? How sweet!" -> {
                    fn1(1.days)
                    return@on
                }

                "[NPC] Kat: A bouquet? For me? How sweet!" -> {
                    fn1(5.days)
                    return@on
                }

                "[NPC] Kat: If you have any other pets you'd like to upgrade, you know where to find me!" -> {
                    reset()
                    return@on
                }
            }

            giveRegex.findThenNull(stripped, "pet") { (p) ->
                pet = p
            } ?: return@on

            remindRegex.findThenNull(stripped, "pet") { (p) ->
                pet = p
            } ?: return@on

            durationRegex.findThenNull(stripped, "duration") { (d) ->
                val s = d.fromLongDuration().toLong()
                time = System.currentTimeMillis() + (s * 1000L)
                fn()
            } ?: return@on

            durationRemindRegex.findThenNull(stripped, "duration") { (d) ->
                val s = d.fromLongDuration().toLong()
                time = System.currentTimeMillis() + (s * 1000L)
                fn()
            } ?: return@on
        }.runWhen(PresetSkyBlockIsland.HUB.state)
    }

    private fun fn() {
        task?.cancel()

        val delay = (time - System.currentTimeMillis()).takeIf { it > 0 } ?: return
        task = Scheduler.schedule(delay.milliseconds) { fn0() }
    }

    private fun fn1(days: Duration) {
        if (time <= 0L) return

        time -= days.inWholeMilliseconds
        if (time > System.currentTimeMillis()) return fn()

        task?.cancel()
        fn0()
    }

    private fun fn0() {
        if (pet.isEmpty()) return

        val str = message.replace("#pet", pet).parse()

        if (showTitle) str.showTitle(fadeIn = 10, stay = 40, fadeOut = 10)
        str.mod()
    }

    private fun reset() {
        pet = ""
        time = 0
    }
}
