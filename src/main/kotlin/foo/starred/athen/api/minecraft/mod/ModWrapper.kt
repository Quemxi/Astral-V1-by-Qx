@file:Suppress("ConstPropertyName")

package foo.starred.athen.api.minecraft.mod

import foo.starred.athen.annotations.Priority
import foo.starred.athen.events.InternalEvent
import foo.starred.athen.events.core.on

@Priority
object ModWrapper {
    const val discord: String = "https://discord.gg/DB5S3DjQVa"
    const val version: String = /*$ mod_version*/"0.1.0"
    const val name: String = /*$ mod_name*/"Astral"
    const val id: String = /*$ mod_id*/"astral"

    var loaded: Boolean = false
        private set

    init {
        on<InternalEvent.Mod.Loading.End> {
            loaded = true
        }
    }
}
