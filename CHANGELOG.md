## Create: Central Kitchen (Fly Port) 2.6.2-p2 for Minecraft 26.2 (unreleased)

### Added / ported

* Rustic Delight integration: mechanical arms take pancakes from every flavored stack and add matching pancakes to it (up to twelve), with a Ponder scene. Only active when Rustic Delight is installed.

### Fixed

* **Crash at startup when Iris is installed** ("mixins.iris.json:texture.MixinIdentifier ... target net.minecraft.resources.Identifier was loaded too early"). The mixin config plugin of the integrations loaded a class that pulled in `Identifier` before Mixin had prepared every config; it now only uses Fabric Loader and plain strings. Reported on CurseForge for 2.6.2-p1; tested with Iris 1.11.4 and Sodium 0.9.2.

## Create: Central Kitchen (Fly Port) 2.6.2-p1 for Minecraft 26.2

First Fabric release. Port of Create: Central Kitchen 2.6.2b (NeoForge 1.21.1) to Minecraft 26.2 on Create Fly.

### Added / ported

* Packagers unpack ingredients into Farmer's Delight cooking pots (single and multi-serving packages).
* Mechanical arms work with cooking pots, skillets, stoves, cutting boards, feasts and pies.
* Farmer's Delight Cutting Board recipes work in the mechanical saw and the deployer, and show as two JEI categories.
* Blaze burners under Farmer's Delight cooking blocks get a chef hat and raise their head; tools in a deployer in use mode are drawn tilted.
* Ponder scenes for the cooking pot, skillet, stove, cutting board and feasts and pies.
* Items whose leftover is a container (bottle, bowl, bucket) stand upright on belts.

### Notes

* Does not need Create: Dragons Plus any more.
* The integrations for mods that do not exist for Minecraft 26.x yet are not included.

## Create: Central Kitchen 2.6.2b

### Fixed

* Fix mechanical saw ignoring filter for converted Cutting Board recipes (@trpipher)
