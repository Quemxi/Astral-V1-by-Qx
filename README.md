# **Astral**

A **Hypixel SkyBlock Quality-of-Life** mod for Minecraft 26.1.x, with a built-in **Performance HUD**
(FPS, ping, TPS, coordinates, facing, clock, memory), colour-blind-friendly colours, a damage splash tidy-up, a movable dungeon map, and a movable dungeon secrets/score panel.

* Open the config menu with `/astral config`
* Move HUD elements with `/astral hud`
* Requires Fabric Loader, Fabric API and Fabric Language Kotlin

See `LICENSE` for licence terms.

## Dungeon map and score (display only)

* **Dungeon map**: draws the map item the game already gives you in your hotbar.
* **Dungeon score**: shows secrets found, an estimated total, an estimated score/rank, and how many more
  secrets reach S and S+. Everything comes from the tab list, the sidebar and chat that the game already
  shows you.
* Neither feature scans the world or rooms, makes network requests, or sends chat/commands for you.
* The total secret count and the score are **estimates**. Hypixel's end-of-run screen is the real result.
* Always check Hypixel's current rules before using any mod: https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications
