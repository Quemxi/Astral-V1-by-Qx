@file:Suppress("ObjectPrivatePropertyName", "Unused")

package foo.starred.athen.modules.impl.dungeon.carry

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.dungeon.DungeonAPI
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.minecraft.text.measurer.VanillaFontMeasurer
import foo.starred.athen.api.minecraft.text.renderer.VanillaFontRenderer
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.api.scheduling.Ticking
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.DungeonEvent
import foo.starred.athen.events.WorldRenderEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.dungeon.carry.DungeonCarryStateTracker.tracked
import foo.starred.athen.utils.command
import foo.starred.athen.utils.render.fcs
import foo.starred.athen.utils.render.renderBoundingBox
import foo.starred.kbus.extensions.runWhen
import foo.starred.parallax.api.primitives.ParallaxBox
import foo.starred.snowbird.api.center
import foo.starred.snowbird.api.command
import foo.starred.snowbird.api.lie
import foo.starred.snowbird.api.network.data.HttpRequest
import foo.starred.snowbird.api.repeat
import foo.starred.snowbird.utils.literal
import foo.starred.snowbird.utils.toDuration
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.helpers.McClient

@Load
@OnlyIn(skyblock = true)
object DungeonCarryTracker : Module(
    "Dungeon carry tracker",
    "Track dungeon carries and display progress.",
    ConfigCategory.DUNGEONS
) {
    private val announceInParty by config.switch("Announce in party", true)
    private val showStartMessage by config.switch("Show start message", true)

    private val _webhook by config.group("Discord webhook")
    private val webhook by _webhook.switch("Send to webhook")
    private val webhookEach by _webhook.switch("Send on each kill", true)
    private val webhookUrl by _webhook.input("Webhook URL").description("Requires you to add your own webhook URL!")

    private val highlights by config.group("Highlights")
    private val highlightPlayer by highlights.switch("Highlight player", true)
    private val playerColor by highlights.colorPicker("Player color", MochaColorScheme.Blue.argb)
    private val playerLineWidth by highlights.slider("Player line width", 2f, 0f, 10f)

    private val hud by config.hud("Dungeon carry display") {
        val example = listOf("§f§lDungeon Carries:", "§7> §bExample §8[§7M7§8]§f: §b3§f/§b10 §7(5m 30s | 12/hr)").fcs

        constrain {
            VanillaFontMeasurer.constrain(example)
        }

        preview {
            VanillaFontRenderer.extract(graphics, example, 0, 0)
        }

        render {
            VanillaFontRenderer.extract(graphics, display.value ?: return@render, 0, 0)
        }
    }

    private val `hud$dungeon` by config.switch("Only in dungeons", true)

    private val floorMap = mapOf(
        "e" to DungeonFloor.E,
        "f1" to DungeonFloor.F1,
        "f2" to DungeonFloor.F2,
        "f3" to DungeonFloor.F3,
        "f4" to DungeonFloor.F4,
        "f5" to DungeonFloor.F5,
        "f6" to DungeonFloor.F6,
        "f7" to DungeonFloor.F7,
        "m1" to DungeonFloor.M1,
        "m2" to DungeonFloor.M2,
        "m3" to DungeonFloor.M3,
        "m4" to DungeonFloor.M4,
        "m5" to DungeonFloor.M5,
        "m6" to DungeonFloor.M6,
        "m7" to DungeonFloor.M7
    )

    private val display = Ticking {
        if (tracked.isEmpty()) return@Ticking null
        if (`hud$dungeon` && !PresetSkyBlockIsland.THE_CATACOMBS.state.value) return@Ticking null

        buildString {
            append("§f§lDungeon Carries:")
            for (i in tracked.values) append("\n${i.str()}")
        }.split("\n").fcs
    }

    init {
        command {
            "dcarry" / "add" / word("player").suggests { McClient.players.map { it.profile.name } } / int("amount", 1).suggests { listOf("1", "5", "10", "20") } / word("floor") {
                val player = string("player")
                val floor = string("floor")
                val amount = int("amount")

                val floor0 = floorMap[floor.lowercase()] ?: return@word "Invalid floor. Use: e, f1-f7, m1-m7.".mod()

                DungeonCarryStateTracker.addCarry(player, amount, floor0)
            }.suggests { floorMap.keys }

            "dcarry" / "remove" / word("player") {
                DungeonCarryStateTracker.removeCarry(string("player"))
            }.suggests { tracked.keys }

            "dcarry" / "list" {
                DungeonCarryStateTracker.listCarries()
            }

            "dcarry" / "list" / "clear" {
                DungeonCarryStateTracker.clearCarries()
            }

            "dcarry" / "history" {
                DungeonCarryStateTracker.displayHistory(1)
            }

            "dcarry" / "history" / int("page", 1) {
                DungeonCarryStateTracker.displayHistory(int("page"))
            }.suggests { listOf("1", "2", "3", "4", "5") }

            "dcarry" / "help" {
                showHelp()
            }

            "dcarry" / "gui" {
                DungeonCarryGUI.open()
            }

            "dcarry" {
                DungeonCarryGUI.open()
            }
        }

        on<DungeonEvent.Start> {
            val floor = DungeonAPI.floor.value ?: return@on

            for (teammate in DungeonAPI.teammates) {
                val carry = tracked[teammate.name] ?: continue
                if (carry.floor != floor) continue

                if (showStartMessage) "Dungeon started for <aqua>${teammate.name}<gray> [${floor.name}]".mod()
            }
        }

        on<DungeonEvent.End> {
            val floor = DungeonAPI.floor.value ?: return@on

            for (teammate in DungeonAPI.teammates) {
                val carry = tracked[teammate.name] ?: continue
                if (carry.floor != floor) continue

                val result = carry.onCompletion()

                "Completed run for <aqua>${teammate.name}".mod()
                if (announceInParty) "pc ${teammate.name}: ${result.current}/${result.total}".command(false)
                if (webhookEach && webhook) {
                    webhookUrl.request(HttpRequest.POST) {
                        body(mapOf("content" to "Completed ${result.amount}/${result.total} ${floor.name} carries for ${teammate.name}"))
                    }
                }

                if (result.completed) {
                    val time = result.totalTime.toDuration()
                    "<${MochaColorScheme.Green.argb}>Completed carries for <aqua>${teammate.name} <gray>[${floor.name}] <r>in <yellow>$time".mod()

                    if (webhook) {
                        webhookUrl.request(HttpRequest.POST) {
                            body(mapOf("content" to "Completed ${result.amount}x ${floor.name} carries for ${teammate.name} ($time)"))
                        }
                    }

                    DungeonCarryStateTracker.add(teammate.name, result.amount, carry.type)
                    tracked.remove(teammate.name)
                }

                DungeonCarryStateTracker.persist()
            }
        }

        on<WorldRenderEvent.Extract> {
            if (!highlightPlayer) return@on
            if (tracked.isEmpty()) return@on

            for (teammate in DungeonAPI.teammates) {
                if (teammate.name !in tracked) continue
                val e = teammate.entity ?: continue
                ParallaxBox.frame(e.renderBoundingBox, playerColor, playerLineWidth, false)
            }
        }.runWhen(PresetSkyBlockIsland.THE_CATACOMBS.state)
    }

    private fun showHelp() {
        val commands = listOf(
            "/astral dcarry" to "Open the dungeon carry tracker GUI",
            "/astral dcarry add <player> <amount> <floor>" to "Add dungeon carries to track",
            "/astral dcarry remove <player>" to "Remove a tracked player",
            "/astral dcarry list" to "List players being tracked",
            "/astral dcarry list clear" to "Clear the active list",
            "/astral dcarry history [page=1]" to "Show tracked history"
        )

        val divider = ("§8§m" + ("-".repeat())).literal()

        divider.lie()
        "§bAstral Dungeon Carry Commands".center().lie()
        divider.lie()

        for ((c, d) in commands) "  <${MochaColorScheme.Green.argb}>$c <dark_gray>- <gray>$d".lie()

        divider.lie()
    }
}
