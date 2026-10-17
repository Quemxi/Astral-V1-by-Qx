@file:Suppress("UNUSED", "UNCHECKED_CAST")

package foo.starred.athen.modules.impl.render

import foo.starred.athen.annotations.Load
import foo.starred.athen.annotations.OnlyIn
import foo.starred.athen.api.dungeon.DungeonAPI
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.PacketEvent
import foo.starred.athen.modules.Module
import foo.starred.snowbird.api.level
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * Astral: tidies SkyBlock's damage indicators (display only).
 * It only changes how damage numbers are shown on your own screen.
 */
@Load
@OnlyIn(skyblock = true)
object DamageSplash : Module(
    "Damage splash",
    "Reformats SkyBlock's damage indicators, or hides them.",
    ConfigCategory.RENDER
) {
    private val hideDamage = config.switch("Hide damage indicators").unique("hideDamage")
    private val uppercase by config.switch("Uppercase numbers")
    private val hideInClear by config.switch("Hide in dungeon clear")
    private val hideInBoss by config.switch("Hide in dungeon boss")

    private val damageRegex = Regex("[✧✯]?(\\d{1,3}(?:,\\d{3})*[⚔+✧❤♞☄✷ﬗ✯]*)")
    private val colors = listOf("§6", "§c", "§e", "§f")

    init {
        on<PacketEvent.Process.Pre, ClientboundSetEntityDataPacket> {
            val entity = level?.getEntity(id) as? ArmorStand ?: return@on

            for (entry in packedItems) {
                val value = entry.value() as? Optional<Component> ?: continue
                val content = value.getOrNull() ?: continue
                val raw = content.string
                val damage = damageRegex.matchEntire(raw)?.groupValues?.get(1) ?: continue

                val inDungeon = DungeonAPI.floor.value != null
                val inBoss = DungeonAPI.inBoss.value
                val hide = hideDamage.value ||
                    (inDungeon && !inBoss && hideInClear) ||
                    (inBoss && hideInBoss)

                if (hide) {
                    entity.remove(Entity.RemovalReason.DISCARDED)
                    it.cancel()
                    return@on
                }

                val shown = if (uppercase) damage.uppercase() else damage
                val crit = raw.contains('✧') || raw.contains('✯')
                val text = if (crit) "§f✧${rainbow(shown)}§f✧" else "§3$shown"

                entity.entityData.assignValues(packedItems)
                entity.customName = Component.literal(text)
                it.cancel()
            }
        }
    }

    private fun rainbow(input: String): String {
        val out = StringBuilder()
        var last: String? = null
        for (ch in input) {
            val pick = colors.filter { it != last }.random()
            out.append(pick).append(ch).append("§r")
            last = pick
        }
        return out.toString()
    }
}
