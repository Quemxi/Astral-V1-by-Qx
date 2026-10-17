package foo.starred.athen.modules.impl.dungeon.terminals.simulator

import foo.starred.athen.annotations.Load
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.config.ConfigManager
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.events.LocationEvent
import foo.starred.athen.events.TickEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.modules.impl.dungeon.terminals.simulator.base.SimulatorMenu
import foo.starred.athen.modules.impl.dungeon.terminals.simulator.impl.*
import foo.starred.athen.utils.command
import foo.starred.kbus.extensions.override
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.data.Observable

@Load
object TerminalSimulator : Module(
    "Terminal simulator",
    "Simulator terminal, terminal simulators?",
    ConfigCategory.DUNGEONS
) {
    private val ipInput by config.input("Simulator server IP", "hypixelp3sim.zapto.org").description("Optional")
    private val pingInput = config.input("Ping", "0", "0").unique("ping")

    var ping = 0
    val s = Observable(false)
    val s0 = Observable(false)

    init {
        run {
            ping = pingInput.value.toIntOrNull() ?: return@run
        }

        pingInput.state.observe {
            ping = it.toIntOrNull() ?: return@observe
        }

        observable.observe {
            SimulatorMenu.a()
            if (!it) return@observe

            "Run \"/astral simulate terminals ping <ping>\" to change ping!".mod()
            ConfigManager.update(_key, false)
        }

        command {
            "simulate" / "terminals" {
                SimulatorMenu.a()
            }

            "simulate" / "terminals" / "ping" / int("int") {
                ConfigManager.update("$_key.ping", int("int").toString())
                "Ping set to ${ping}ms".mod()
            }

            "simulate" / "terminals" / "rubix" {
                RubixSimulator().a()
            }

            "simulate" / "terminals" / "color" {
                ColorSimulator().a()
            }

            "simulate" / "terminals" / "melody" {
                MelodySimulator().a()
            }

            "simulate" / "terminals" / "name" {
                NameSimulator().a()
            }

            "simulate" / "terminals" / "panes" {
                PanesSimulator().a()
            }

            "simulate" / "terminals" / "numbers" {
                NumbersSimulator().a()
            }
        }

        on<TickEvent.Client.End> {
            TickEvent.Server.post()
        }.override(s0)

        on<LocationEvent.Server.Connect> {
            s0.value = client.currentServer?.ip == ipInput
        }

        on<LocationEvent.Server.Disconnect> {
            s0.value = false
        }
    }
}
