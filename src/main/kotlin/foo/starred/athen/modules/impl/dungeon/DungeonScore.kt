@file:Suppress("UNUSED")

package foo.starred.athen.modules.impl.dungeon

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.dungeon.DungeonAPI
import foo.starred.athen.api.location.island.impl.PresetSkyBlockIsland
import foo.starred.athen.api.minecraft.text.measurer.VanillaFontMeasurer
import foo.starred.athen.api.minecraft.text.renderer.VanillaFontRenderer
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.MessageEvent
import foo.starred.athen.events.ScoreboardEvent
import foo.starred.athen.events.TabListEvent
import foo.starred.athen.modules.Module
import foo.starred.kbus.extensions.runWhen
import foo.starred.snowbird.utils.stripped
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Astral: shows your dungeon secrets and an estimated score as a movable on-screen element.
 *
 * Passive and display-only. Every input is something the game already shows you:
 *  - tab list: Secrets Found (count and %), Crypts, Completed Rooms, Puzzles
 *  - sidebar:  Cleared %, Time Elapsed
 *  - chat:     deaths (via DungeonAPI), "A Prince falls", and party chat that says the mimic is dead
 *
 * It does NOT scan the world or rooms, does NOT use the network, and does NOT send chat or commands.
 * The total secret count is derived from the two tab-list numbers (found / found-%), so it is an estimate
 * that sharpens as you find more secrets. The score is an estimate too: the game's own result screen wins.
 */
@Load
@OnlyIn(skyblock = true)
object DungeonScore : Module(
    "Dungeon score",
    "Shows secrets found and an estimated score. Uses only information the game already shows you.",
    ConfigCategory.DUNGEONS
) {
    private val showSecrets = config.switch("Show secrets", true).unique("showSecrets")
    private val showScore = config.switch("Show score", true).unique("showScore")
    private val showNeeded = config.switch("Show secrets needed for S / S+", true).unique("showNeeded")
    private val showCrypts = config.switch("Show crypts", false).unique("showCrypts")

    private val paul = config.switch("Paul (EZPZ) active", false)
        .description("Adds the +10 bonus score if the Paul perk is active. Astral can't read the mayor, so set this yourself.")
        .unique("paul")
    private val spirit = config.switch("Spirit pet equipped", true)
        .description("The first death costs 1 point instead of 2 with a Spirit pet.")
        .unique("spirit")
    private val mimic = config.switch("Mimic killed (manual)", false)
        .description("Counts the +2 mimic bonus on floors 6 and 7. Also turns on by itself if a party member says the mimic is dead.")
        .unique("mimic")

    private val textShadow = config.switch("Text shadow", true).unique("textShadow")

    // Rank thresholds, highest first.
    private val ranks = listOf(300 to "S+", 270 to "S", 230 to "A", 160 to "B", 100 to "C", 0 to "D")

    // Share of secrets the floor requires for the full 40 secret points.
    private val requiredSecrets = mapOf(
        "E" to 0.3, "F1" to 0.3, "F2" to 0.4, "F3" to 0.5, "F4" to 0.6, "F5" to 0.7, "F6" to 0.85, "F7" to 1.0
    )

    // Seconds allowed before speed points start dropping.
    private val timeLimits = mapOf(
        "E" to 600, "F1" to 600, "F2" to 600, "F3" to 600, "F4" to 720, "F5" to 600, "F6" to 720, "F7" to 840,
        "M1" to 480, "M2" to 480, "M3" to 480, "M4" to 480, "M5" to 480, "M6" to 600, "M7" to 840
    )

    private val formatCodes = Regex("§.")
    private val secretsCountRegex = Regex("^Secrets Found:\\s*(\\d+)$")
    private val secretsPercentRegex = Regex("^Secrets Found:\\s*([\\d.]+)%$")
    private val cryptsRegex = Regex("^Crypts:\\s*(\\d+)$")
    private val roomsRegex = Regex("^Completed Rooms:\\s*(\\d+)$")
    private val puzzleCountRegex = Regex("^Puzzles:\\s*\\((\\d+)\\)$")
    private val puzzleRegex = Regex("^(.+?):\\s*\\[([✦✔✖])")
    private val clearedRegex = Regex("Cleared:\\s*(\\d+)%")
    private val elapsedRegex = Regex("Time Elapsed:\\s*(?:(\\d+)h\\s*)?(?:(\\d+)m\\s*)?(?:(\\d+)s)?")
    private val princeRegex = Regex("^A Prince falls\\. \\+1 Bonus Score", RegexOption.IGNORE_CASE)
    private val partyChatRegex = Regex("^Party > (?:\\[[^\\]]+] )*\\w{1,16}: (.+)$")
    private val mimicCalls = setOf("mimic dead", "mimic killed", "mimic dead!", "mimic killed!")

    // State read from the game.
    private var secretsFound = 0
    private var secretsPercent = 0.0
    private var crypts = 0
    private var completedRooms = 0
    private var clearedPercent = 0
    private var secondsElapsed = 0
    private var puzzleTotal = 0
    private val puzzleSolved = HashSet<String>()
    private var princeKilled = false
    private var mimicSeen = false

    private class Result(val score: Int, val secretsScore: Int)

    private val hud by config.hud("Dungeon score") {
        constrain {
            VanillaFontMeasurer.constrain(lines(example = true))
        }

        preview {
            VanillaFontRenderer.extract(graphics, lines(example = true), 0, 0, textShadow.value)
        }

        render {
            if (DungeonAPI.floor.value == null) return@render
            val lines = lines()
            if (lines.isEmpty()) return@render
            VanillaFontRenderer.extract(graphics, lines, 0, 0, textShadow.value)
        }
    }

    init {
        for (option in listOf(showSecrets, showScore, showNeeded, showCrypts)) {
            option.state.observe { hud.constrain() }
        }

        on<LocationEvent.SkyBlock.Island> {
            secretsFound = 0
            secretsPercent = 0.0
            crypts = 0
            completedRooms = 0
            clearedPercent = 0
            secondsElapsed = 0
            puzzleTotal = 0
            puzzleSolved.clear()
            princeKilled = false
            mimicSeen = false
        }

        on<TabListEvent.Change> {
            for (column in new) for (component in column) readTabLine(component.stripped().trim())
        }.runWhen(PresetSkyBlockIsland.THE_CATACOMBS.state)

        on<ScoreboardEvent.Update> {
            for (raw in new) readSidebarLine(raw.replace(formatCodes, "").trim())
        }.runWhen(PresetSkyBlockIsland.THE_CATACOMBS.state)

        on<MessageEvent.Chat.Receive> {
            if (princeRegex.containsMatchIn(stripped)) princeKilled = true

            val said = partyChatRegex.find(stripped)?.groupValues?.get(1)?.trim()?.lowercase()
            if (said != null && said in mimicCalls) mimicSeen = true
        }.runWhen(PresetSkyBlockIsland.THE_CATACOMBS.state)
    }

    private fun readTabLine(line: String) {
        secretsCountRegex.find(line)?.let { secretsFound = it.groupValues[1].toIntOrNull() ?: secretsFound; return }
        secretsPercentRegex.find(line)?.let { secretsPercent = it.groupValues[1].toDoubleOrNull() ?: secretsPercent; return }
        cryptsRegex.find(line)?.let { crypts = it.groupValues[1].toIntOrNull() ?: crypts; return }
        roomsRegex.find(line)?.let { completedRooms = it.groupValues[1].toIntOrNull() ?: completedRooms; return }
        puzzleCountRegex.find(line)?.let { puzzleTotal = it.groupValues[1].toIntOrNull() ?: puzzleTotal; return }

        puzzleRegex.find(line)?.let {
            val name = it.groupValues[1]
            if (it.groupValues[2] == "✔") puzzleSolved.add(name) else puzzleSolved.remove(name)
        }
    }

    private fun readSidebarLine(line: String) {
        clearedRegex.find(line)?.let { clearedPercent = it.groupValues[1].toIntOrNull() ?: clearedPercent; return }

        elapsedRegex.find(line)?.let {
            val h = it.groupValues[1].toIntOrNull() ?: 0
            val m = it.groupValues[2].toIntOrNull() ?: 0
            val s = it.groupValues[3].toIntOrNull() ?: 0
            secondsElapsed = h * 3600 + m * 60 + s
        }
    }

    // Total secrets in the run, from "found" and "found as a percentage". Null until both are known.
    private fun estimatedTotal(): Int? {
        if (secretsFound <= 0 || secretsPercent <= 0.0) return null
        return (secretsFound * 100.0 / secretsPercent).roundToInt().coerceAtLeast(secretsFound)
    }

    private fun calculate(): Result? {
        val dungeonFloor = DungeonAPI.floor.value ?: return null
        val name = dungeonFloor.name
        val number = dungeonFloor.floorNumber
        val required = requiredSecrets[name] ?: 1.0

        // Without both numbers we can't estimate how many rooms the run has.
        if (completedRooms <= 0 || clearedPercent <= 0) return null
        val totalRooms = floor(completedRooms / (clearedPercent / 100.0) + 0.4)
        if (totalRooms <= 0.0) return null

        // The blood room and the boss room are counted as done when the run finishes.
        val effective = completedRooms + (if (DungeonAPI.bloodKilledAll.value) 0 else 1) + (if (DungeonAPI.inBoss.value) 0 else 1)
        val roomShare = (effective / totalRooms).coerceIn(0.0, 1.0)

        val secretsScore = floor((secretsPercent / required) / 100.0 * 40.0).coerceIn(0.0, 40.0).toInt()
        val roomScore = (roomShare * 60.0).toInt().coerceIn(0, 60)
        val skillRooms = floor(roomShare * 80.0).toInt().coerceIn(0, 80)

        val unsolved = (puzzleTotal - puzzleSolved.size).coerceAtLeast(0)
        val deaths = DungeonAPI.teammates.sumOf { it.deaths }
        val deathPenalty = if (deaths == 0) 0 else deaths * 2 - (if (spirit.value) 1 else 0)
        val skill = (20 + skillRooms - unsolved * 10 - deathPenalty).coerceIn(20, 100)

        var bonus = crypts.coerceAtMost(5)
        if (princeKilled) bonus += 1
        if ((mimic.value || mimicSeen) && number > 5) bonus += 2
        if (paul.value) bonus += 10

        return Result(secretsScore + roomScore + skill + bonus + speedScore(name), secretsScore)
    }

    private fun speedScore(floorName: String): Int {
        val limit = timeLimits[floorName] ?: return 100
        if (secondsElapsed <= limit) return 100

        var over = (secondsElapsed - limit) * 100f / limit
        var deduction = 0f
        for ((cap, divisor) in listOf(20f to 2f, 20f to 3.5f, 10f to 4f, 10f to 5f)) {
            if (over <= 0f) break
            deduction += over.coerceAtMost(cap) / divisor
            over -= cap
        }
        if (over > 0f) deduction += over / 6f

        return (100 - deduction).toInt().coerceAtLeast(0)
    }

    // How many more secrets reach `target`. Null if it can't be worked out (or can't be reached by secrets alone).
    private fun secretsNeeded(result: Result, target: Int): Int? {
        if (result.score >= target) return 0

        val total = estimatedTotal() ?: return null
        val required = requiredSecrets[DungeonAPI.floor.value?.name] ?: 1.0

        val points = target - (result.score - result.secretsScore)
        if (points > 40) return null

        val needed = ceil(total * required * points / 40.0).toInt()
        return (needed - secretsFound).coerceAtLeast(0)
    }

    private fun rank(score: Int): String = ranks.first { score >= it.first }.second

    private fun scoreColour(score: Int): String = when {
        score >= 300 -> "§6"
        score >= 270 -> "§a"
        score >= 230 -> "§e"
        else -> "§c"
    }

    private fun lines(example: Boolean = false): List<String> = buildList {
        if (showSecrets.value) {
            if (example) {
                add("§7Secrets: §b24§7/§e~40 §7(§a60%§7)")
            } else {
                val total = estimatedTotal()?.let { "§7/§e~$it" } ?: ""
                val percent = if (secretsPercent > 0.0) " §7(§a${secretsPercent.toString().removeSuffix(".0")}%§7)" else ""
                add("§7Secrets: §b$secretsFound$total$percent")
            }
        }

        if (showScore.value) {
            val result = if (example) null else calculate()
            val score = if (example) 285 else result?.score

            if (score == null) {
                add("§7Score: §8--")
            } else {
                add("§7Score: ${scoreColour(score)}~$score §7(${scoreColour(score)}${rank(score)}§7)")
            }

            if (showNeeded.value) {
                val s = if (example) 0 else result?.let { secretsNeeded(it, 270) }
                val sPlus = if (example) 6 else result?.let { secretsNeeded(it, 300) }
                add("§7Need: §fS §e${s ?: "--"} §7| §6S+ §e${sPlus ?: "--"}")
            }
        }

        if (showCrypts.value) {
            add("§7Crypts: §6${if (example) 5 else crypts}")
        }
    }
}
