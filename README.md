# Pillager Pirates

Pillagers have taken to the sea.

Out on open water there are ships now: whole, afloat, and crewed. They are
built the way the vanilla shipwrecks are, two woods, flared hulls, log masts
and yard arms, trapdoor hatches, a raised stern with a cabin in it, but these
never sank. Their decks are manned, their holds are dry, and their chests are
full of what the crew took from someone else.

## The four ships

| | Size | Rig | Aboard | Crew |
|---|---|---|---|---|
| **Sloop** | 14 × 7 | One mast, a gaff mainsail and a jib | A stern cabin with the plunder in it, stores in the hold | 3 pillagers, sometimes a captain |
| **Longship** | 19 × 7 | One striped square sail | An open deck, a curled prow, banners hung along the sides for shields, oars run out into the water, an armory chest | 3 vindicators and 2 pillagers, half the time a captain |
| **Brigantine** | 24 × 9 | Two square-rigged masts, a crow's nest, a jib | A captain's cabin with the map chest, a cartography table and a bunk; supplies and the treasure chest below decks | 4 pillagers, a vindicator, a captain on the quarterdeck, a lookout in the nest |
| **Galleon** | 32 × 11 | Three masts: two square-rigged, a gaff sail on the mizzen, a crow's nest | A forecastle and a two-storey stern castle; amidships a gun deck with shuttered ports, a brig holding two allays and an armory; a hold under it that rises to the main deck fore and aft | 7 pillagers, 2 vindicators, a captain, and sometimes an evoker in the cabin |

Every ship is drawn from its own seed, so no two are quite alike: the hull and
deck woods are paired from a set that leans on dark oak, the wood pillagers
build their outposts from; the sails are white or light gray with a dark band;
about one in three has sails torn to rags. Each flies the ominous banner at its
stern and from every masthead.

## Where to find them

Out at sea. A ship only goes where it would actually float: the water has to be
deep enough under the whole hull to clear the keel, and open for a dozen blocks
around it, so you will not find one wedged against a beach or moored in a pond.
A spot too tight for a galleon gets something smaller instead. Frozen oceans are
left alone; any other ocean will do, and a river if it is ever wide enough.

They are rarer than shipwrecks, roughly one to every 450 blocks of open ocean,
and never right beside an ocean monument.

They show up on the horizon: sails, masts and flags stand well clear of the
water, and the lanterns on their rails light them after dark. To go straight
to one:

```
/locate structure #pillager-pirates-justfatlard:pirate_ship
```

or name a particular kind, e.g. `pillager-pirates-justfatlard:galleon`.

## Going aboard

Every ship has a boarding ladder up each side amidships, with a gate in the
rail at the top. Swim or row up to it. The crew cannot open gates, and the rails
are too high for them to jump, so they are there when you arrive and they stay
aboard afterwards.

Inside:

- A **hatch** beside the mainmast opens onto a ladder down into the hold. The
  longship has no hold: it sits too low in the water, and everything it carries
  is on deck.
- The raised decks at either end are reached by a **ladder** on the wall facing
  midships, next to the cabin door.
- The brigantine and galleon have a **crow's nest**, with a ladder up the back
  of the mast and a pillager with a crossbow already in it.

The hull is watertight until you make it otherwise. Break a plank below the
waterline and the sea comes in.

## Sailing one home

With [big-boats](https://github.com/fatlard1993/big-boats) installed, every ship
has a helm at its wheel, rated for exactly that ship: Tonnage I on a sloop or a
longship, Tonnage II on a brigantine, Tonnage III on a galleon. Clear the crew,
throw a christening bottle at her, and she is yours to sail.

Every ship is built to be taken that way. Each fits the helm it carries with a
twentieth of the helm's capacity to spare, so there is room to add something of
your own before you set off, and every block of it is joined to the rest, so
nothing is left hanging in the air where she used to be. Break the helm and it
keeps its rating, like any enchanted helm.

Without big-boats the wheel is simply empty.

## The crew

Everyone aboard was there when the ship was built, and nobody despawns.

A **captain** wears the ominous banner and counts as a raid captain in every way
the game cares about: killing one drops an ominous bottle, same as a patrol
captain on land. Galleons sometimes carry an **evoker**, with everything that
comes with one, a totem of undying included.

No more come aboard afterwards. A cleared ship stays cleared, and nothing else
spawns in its hold either, so a ship you have taken is yours to keep.

## What they are carrying

| Chest | Found | Holds |
|---|---|---|
| **Captain's** | The captain's cabin | A buried treasure map, always; charts, compass, clock, spyglass, sometimes a goat horn or an ominous bottle |
| **Treasure** | Stowed aft | Emeralds, iron, gold, the odd diamond; now and then an enchanted book, nautilus armor or a heart of the sea |
| **Armory** | The longship's deck, the galleon's gun deck | A crossbow, maybe enchanted, or an iron axe; arrows, tipped arrows, fireworks, iron armor; sometimes a Piercing, Quick Charge or Multishot book |
| **Supply** | Barrels and chests in the hold | Food, gunpowder, arrows, paper, string, the odd emerald or block of TNT |

All but the supply chests can turn up a sentry armor trim.

Setting foot on a ship earns **Shiver Me Timbers**.

## Changing it

Everything about where ships go and what is on them is datapack data under
`data/pillager-pirates-justfatlard/`, and can be overridden by a datapack of
your own:

- `worldgen/structure_set/pirate_ships.json`: how often ships appear, and the
  mix of kinds.
- `tags/worldgen/biome/has_structure/pirate_ship.json`: which biomes are
  searched. The water check still has the final say.
- `loot_table/chests/`: the four chests.
- `worldgen/structure/<ship>.json`: the `spawn_overrides` there is what keeps
  ships empty once cleared. Put `minecraft:pillager` in its `spawns` list and a
  ship keeps itself manned the way an outpost does, at the cost of the odd
  pillager appearing on top of a sail and dropping into the sea.

## Installing

Server-side only. The ships are built entirely from vanilla blocks and mobs, so
players connect with an unmodified client and see everything. Pandorical is not
needed, on either side. Singleplayer works too. It targets the Minecraft, Fabric Loader and Fabric API versions
declared in this mod's `gradle.properties`.

## License

MIT, see [LICENSE](LICENSE).
