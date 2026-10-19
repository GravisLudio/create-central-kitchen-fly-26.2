## Create: Central Kitchen (Fly Port)

**Unofficial port** of [Create: Central Kitchen](https://github.com/DragonsPlusMinecraft/CreateCentralKitchen) by DragonsPlus to **Minecraft 26.2 on Fabric**, built on [Create Fly](https://modrinth.com/mod/create-fly). Not made or endorsed by the original authors. LGPL-3.0-or-later like the original. See [NOTICE.md](NOTICE.md) for credits and what changed.

Central Kitchen adds no blocks of its own: it makes Create's machines work with the kitchen blocks of other mods.

- Packagers unpack ingredients into Farmer's Delight cooking pots.
- Mechanical arms work with cooking pots, skillets, stoves, cutting boards, feasts and pies.
- Farmer's Delight cutting board recipes also work in Create's mechanical saw and deployer (and show in JEI).
- Blaze burners under cooking blocks get a chef hat and raise their head; tools in a deployer in use mode are drawn tilted.
- Ponder scenes for all of the above.

**Requires:** Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API, Create Fly 6.0.9+. **Optional:** Farmer's Delight Refabricated 26.2, JEI. Install it on the server and on the clients.

Branch `26.2/6.0.0-dev`. Build with `gradlew jar` (JDK 25); tests: `gradlew runGametest` (server, headless) and `gradlew runClientGameTest` (real client, screenshots).

---

### Original README (DragonsPlus)

A mod offering tools and methods to automate food processing in Create.

#### Contribute
Feel free to open a PR to either translate the mod or to add another feature! All help is appreciated!

If you want to help translate, use the language file in `src/generated/` and submit to `src/translations/`.
