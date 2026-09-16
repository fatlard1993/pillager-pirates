package justfatlard.pillager_pirates.ship;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * The solid of a ship from keel to main deck, as a question that can be asked of any block: is this
 * inside the hull?
 *
 * <p>Everything about the hull's shape falls out of that one test. The shell is every inside block
 * that has outside next to it, and because water only moves sideways and down, a shell drawn that way
 * is watertight by construction as long as the shell below the waterline is full blocks: the hold can
 * be emptied of the sea without anything leaking back in. {@code ShipTest} holds both of those, and
 * that the hull is one piece.
 */
final class Hull {

	private final ShipType type;
	private final int bowLength;

	Hull(ShipType type) {
		this.type = type;
		this.bowLength = Math.round(type.length * 0.32F);
	}

	int bowZ() {
		return type.length - 1;
	}

	/** The stem slopes aft toward the keel at a block per layer, so the bow meets the water raked. */
	int bowZ(int y) {
		return bowZ() - Math.max(0, -y);
	}

	int sternZ(int y) {
		return Math.max(0, (type.draft / 2) - (y - type.keelY()));
	}

	/** Half the beam at deck level, at a given station along the hull. */
	int beam(int z) {
		int fromBow = bowZ() - z;
		int w = type.halfBeam;
		if (fromBow < bowLength) {
			w = taper(fromBow);
		}
		// A longship is double-ended: its stern closes to a point the same way its bow does.
		if (type == ShipType.LONGSHIP && z < bowLength) {
			return Math.min(w, taper(z));
		}
		// A transom rather than a point: the stern closes in by a block, a galleon's by two.
		int sternTaper = type.halfBeam >= 5 ? 2 - z : 1 - z;
		return Math.max(0, w - Math.max(0, sternTaper));
	}

	private int taper(int fromEnd) {
		return (int) Math.round(type.halfBeam * Math.pow(fromEnd / (double) bowLength, type.bowFullness));
	}

	/** Half the width of the hull at a layer and station, or -1 where there is no hull there at all. */
	int halfWidth(int y, int z) {
		if (y < type.keelY() || y > type.deck) return -1;
		if (z < sternZ(y) || z > bowZ(y)) return -1;
		if (y == type.keelY()) return 0;
		int w = beam(z) - type.inset(y);
		return w < 0 ? -1 : w;
	}

	boolean inside(int x, int y, int z) {
		int w = halfWidth(y, z);
		return w >= 0 && Math.abs(x) <= w;
	}

	/** Part of the deck's outline: where the bulwarks stand and the rails run. */
	boolean edge(int x, int z) {
		int y = type.deck;
		return inside(x, y, z)
			&& (!inside(x + 1, y, z) || !inside(x - 1, y, z) || !inside(x, y, z + 1) || !inside(x, y, z - 1));
	}

	/**
	 * Outside beside it or below it - or, on a frame station, outside diagonally below it.
	 *
	 * <p>Anywhere the hull is too narrow to stand in, it is all shell: toward the stem, a sliver of
	 * hold a block or two wide would cut the stem timber off from everything else.
	 *
	 * <p>The frames are what hold the hull together. Where a layer steps in, it meets the layer below
	 * only along an edge: that keeps water out, but it is not a join, and a helm's flood fill goes
	 * face to face - it would sail off and leave every strake under the turn of the bilge floating
	 * where the ship had been. Filling the step on every third station ties each strake to the next
	 * like a rib, and stands in the hold looking like one. Filling it everywhere would do the same for
	 * three times the blocks, and blocks are what a helm's rating is counted in.
	 */
	private boolean shell(int x, int y, int z) {
		if (halfWidth(y, z) < 2) return true;
		if (!inside(x + 1, y, z) || !inside(x - 1, y, z)
				|| !inside(x, y, z + 1) || !inside(x, y, z - 1)
				|| !inside(x, y - 1, z)) {
			return true;
		}
		return z % 3 == 1
			&& (!inside(x + 1, y - 1, z) || !inside(x - 1, y - 1, z)
				|| !inside(x, y - 1, z + 1) || !inside(x, y - 1, z - 1));
	}

	/**
	 * Layers of the interior that are floor rather than air.
	 *
	 * <p>The layer just above the keel is only a few blocks wide, so it is boarded over to give the hold
	 * a flat floor. A hull too shallow to stand up in below its deck is solid all the way down: an
	 * empty slot nobody can get into is a pocket of nothing. A galleon's depth is split by a gun deck at
	 * the waterline, amidships only: fore and aft of it the hold rises to the main deck, which keeps
	 * the stern hold tall enough to stow plunder in and a galleon inside what a Tonnage III helm holds.
	 */
	private boolean floor(int y, int z) {
		int headroom = type.deck - (type.keelY() + 2);
		if (headroom < 2) return true;
		if (y == type.keelY() + 1) return true;
		return y == 0 && gunDeck(z);
	}

	boolean gunDeck(int z) {
		return type == ShipType.GALLEON && z >= 10 && z <= bowZ() - 7;
	}

	void draw(Blueprint plan, Timber hull, Timber deck) {
		int hw = type.halfBeam;
		for (int y = type.keelY(); y <= type.deck; y++) {
			for (int z = 0; z <= bowZ(); z++) {
				for (int x = -hw; x <= hw; x++) {
					if (!inside(x, y, z)) continue;
					plan.set(x, y, z, block(x, y, z, hull, deck));
				}
			}
		}
	}

	private BlockState block(int x, int y, int z, Timber hull, Timber deck) {
		if (y == type.keelY()) {
			return hull.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		}
		if (y == type.deck) {
			return edge(x, z) ? wale(x, z, hull) : deck.planks().defaultBlockState();
		}
		if (!shell(x, y, z)) {
			return floor(y, z) ? deck.planks().defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		// Nothing that can be waterlogged goes in the water. The sea waterlogs a stair the first time
		// the water beside it updates, the update runs stair to stair round the whole hull, and a
		// waterlogged stair leaks through its open side faces into the hold.
		if (!inside(x, y - 1, z) && y > 0) {
			return flare(x, y, z, hull);
		}
		return hull.planks().defaultBlockState();
	}

	/**
	 * The deck's outline is a log laid along the hull, so a dark band runs round the ship at deck level
	 * and the flat plank side reads as a hull rather than a wall. Across the transom it lies athwartships.
	 */
	private BlockState wale(int x, int z, Timber hull) {
		boolean transom = inside(x + 1, type.deck, z) && inside(x - 1, type.deck, z);
		return hull.log().defaultBlockState()
			.setValue(RotatedPillarBlock.AXIS, transom ? Direction.Axis.X : Direction.Axis.Z);
	}

	/**
	 * A shell block with nothing under it: the turn of the bilge, the rake of the stem. An upside-down
	 * stair there turns its solid half inboard and cuts the corner off underneath, which is what makes
	 * the hull read as curved rather than stepped - the same trick the vanilla shipwrecks use.
	 */
	private BlockState flare(int x, int y, int z, Timber hull) {
		Direction inboard;
		if (x != 0) {
			inboard = x > 0 ? Direction.WEST : Direction.EAST;
		} else {
			inboard = z > type.length / 2 ? Direction.NORTH : Direction.SOUTH;
		}
		return hull.stairs().defaultBlockState()
			.setValue(StairBlock.FACING, inboard)
			.setValue(StairBlock.HALF, Half.TOP);
	}
}
