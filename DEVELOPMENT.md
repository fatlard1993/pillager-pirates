# Pillager Pirates - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Building

```bash
./gradlew build
```

The jar lands in `build/libs/`. Version targets live in `gradle.properties`.

The big-boats integration compiles against `../big-boats/build/libs/`, looking
for the jar named by the `mod_version` in big-boats' own `gradle.properties`, so
build big-boats first. The tests read its capacity table too. It is never bundled; at runtime the helm is only placed when
big-boats is installed.

The crew's pirate clothes compile against `../pandorical/build/libs/`, the same
way, by the `mod_version` in Pandorical's `gradle.properties`, so build
Pandorical first too. It is never bundled and only a suggestion in
`fabric.mod.json`: `integration/PirateClothes` is the only class that names a
Pandorical type, and it is only reached when Pandorical is loaded.

## Tests

```bash
./gradlew test
```

`ShipTest` draws every ship type across a few hundred seeds and holds three
things a player taking a ship depends on:

- **It fits its helm.** Counted the way a big-boats helm counts, against
  big-boats' own capacity for the ship's Tonnage, with a twentieth to spare.
- **It is one piece.** A helm's flood fill goes face to face; anything joined
  only at an edge is left floating when the ship sails.
- **The hold stays dry.** The sea spreads sideways and down through air and
  through anything waterloggable, since the sea waterlogs such a block the first
  time the water beside it updates.

## How a ship gets built

- `ship/Shipwright` draws a whole ship into a `Blueprint` from its type and a
  seed, in the ship's own frame: bow toward +z, keel on x = 0, waterline at
  y = 0. It never looks at the world, which is what lets every chunk the ship
  crosses redraw it and agree.
- `ship/Hull` is the hull as a solid: `inside(x, y, z)`. The shell is every
  inside block with outside next to it, so the hold is watertight by
  construction.
- `worldgen/PirateShipStructure` sounds the water before committing: deep water
  under the whole hull at full draft, open water in a ring well clear of it, at
  any of the four headings.
- `worldgen/PirateShipPiece` turns the blueprint to its heading and writes the
  part that lands in each chunk, then fills containers, puts the ominous pattern
  on banners, and spawns the crew whose anchor block is in that chunk.

## Testing a change

`/place structure pillager-pirates-justfatlard:<ship> <x> <y> <z>` over deep
ocean places one on demand (it runs the same water check, so it refuses over
land). The dev server in `run/` works with RCON for scripted checks.

## The crew's clothes

Every illager hand is given the `pillager-pirates-justfatlard.crew` entity tag
when it is spawned, which is saved with it. With Pandorical loaded,
`integration/PirateClothes` gives each tagged pillager and vindicator an entity
overlay every time it loads (Pandorical keeps overlays in memory only): the
captain's sheet for the patrol leader, otherwise one of three deckhands' picked
from the UUID; a vindicator is the bosun or the quartermaster, the same way.
Evokers are tagged too but left in their robes. Crews spawned before the tag
existed are recognised by `Crew.isCrew`, by standing persistent inside a ship's
piece, and tagged then.

The six sheets in `assets/.../textures/entity/crew/` are drawn by
`generate_crew_textures.py`, pixel by pixel on the vanilla illager layout. It
needs no Pillow and writes the same bytes every run. `--preview DIR` also writes
each sheet over the vanilla illager it is for, as a front and back view and as
the whole sheet, to look at.

A vindicator shows a different set of arm boxes depending on its pose: the
crossed arms (upper arms at 44,22, the forearm bar at 40,38) while idle, the two
separate arms (40,46) once it is after somebody. Its sheets paint both alike, so
the clothes do not change when it raises its axe, and leave vanilla's hands
showing in each. Its own coat sits on the jacket box over the body, so its
shirts and waistcoats go on the jacket too: the body only shows at the neck.
A pillager never crosses its arms, and its sheets leave those boxes empty.

```bash
python3 generate_crew_textures.py --preview /tmp/crew
```
