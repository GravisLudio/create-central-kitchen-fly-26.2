# Create: Central Kitchen (Fly Port) — notice

This is an **unofficial** port of **Create: Central Kitchen** to **Minecraft 26.2 on Fabric**, for **Create Fly**.
It is not made, endorsed or supported by the original authors.

- **Original mod:** Create: Central Kitchen by **DragonsPlus** (MarbleGateKeeper and contributors), version 2.6.2b for NeoForge 1.21.1.
  - Source: <https://github.com/DragonsPlusMinecraft/CreateCentralKitchen>
  - Modrinth: <https://modrinth.com/mod/create-central-kitchen>
  - CurseForge: <https://www.curseforge.com/minecraft/mc-mods/create-central-kitchen>
- **License:** GNU LGPL-3.0-or-later, the same as the original (see `LICENSE.txt`). The original copyright headers are kept.
- **Code taken from Create: Dragons Plus** (DragonsPlus, LGPL-3.0-or-later): `common/recipe/RecipeConverter`, adapted to Fabric's lifecycle events so this mod no longer needs Dragons Plus.
- **Port:** GravisLudio, <https://github.com/GravisLudio/create-central-kitchen-fly-26.2>. Written with the help of an AI assistant (Claude, by Anthropic) and tested with automated game tests on a server and in a real client, and by hand.

## What changed from the original

- NeoForge APIs replaced with Fabric 26.2 and Create Fly ones (`com.zurrtum.create`): registries, config, packager unpacking on `Container`s, mechanical arm points, belt rule, mixins.
- Farmer's Delight integration ported to Farmer's Delight Refabricated 26.2: cooking pot, skillet, stove, cutting board, feasts and pies, Cutting Board recipes as sawing and deploying recipes.
- Client side ported: chef hat and raised head on blaze burners under Farmer's Delight cooking blocks, tilted tools in a deployer in use mode, two JEI categories, Ponder scenes (their structure files migrated to the 26.2 data format).
- No longer needs Create: Dragons Plus.
- **Not ported (yet):** the integrations for mods that do not exist for Minecraft 26.x (Brewin' and Chewin', Extra Delight, Miner's Delight, Cultural Delights, Festive Delight, Hearth and Harvest, End's Delight, My Nether's Delight, Dungeon's Delight, Rustic Delight), Dragons Plus's grouped Ponder cards, and the Multiblock Keg and Multiblock Oven that the original lists as future plans.
