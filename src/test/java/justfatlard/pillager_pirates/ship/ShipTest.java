package justfatlard.pillager_pirates.ship;

import justfatlard.big_boats.ship.ShipConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * What a ship has to be for a player to take it: small enough for the helm it is built to, one
 * piece, and dry inside. Every ship is drawn from a seed, so each holds across a spread of seeds
 * rather than for the one that happened to be looked at.
 */
class ShipTest {

	private static final int SEEDS = 300;

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	/**
	 * Counted the way a big-boats helm counts, plus the helm itself, against big-boats' own capacity
	 * for the ship's rating - so a change on either side that stops a ship being sailable fails here.
	 * With a twentieth to spare: whoever takes a ship will want to add something to it.
	 */
	@ParameterizedTest
	@EnumSource(ShipType.class)
	void fitsTheHelmItIsBuiltFor(ShipType type) {
		int capacity = ShipConfig.capacityForTonnage(type.tonnage);
		int allowed = capacity - capacity / 20;
		int largest = 0;
		for (long seed = 0; seed < SEEDS; seed++) {
			largest = Math.max(largest, solid(Shipwright.draw(type, seed)).size() + 1);
		}
		System.out.printf("%s: largest %d of %d (Tonnage %d)%n", type.id, largest, capacity, type.tonnage);
		assertTrue(largest <= allowed,
			type.id + " reaches " + largest + " blocks with its helm; a Tonnage " + type.tonnage
				+ " helm holds " + capacity + ", and " + allowed + " leaves room to build on");
	}

	/** Everything a helm's flood fill would reach from the keel is everything there is. */
	@ParameterizedTest
	@EnumSource(ShipType.class)
	void isOnePiece(ShipType type) {
		for (long seed = 0; seed < SEEDS; seed++) {
			Blueprint plan = Shipwright.draw(type, seed);
			Set<BlockPos> solid = solid(plan);
			Set<BlockPos> reached = new HashSet<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			BlockPos keel = new BlockPos(0, type.keelY(), type.length / 2);
			reached.add(keel);
			queue.add(keel);
			while (!queue.isEmpty()) {
				BlockPos pos = queue.poll();
				for (Direction direction : Direction.values()) {
					BlockPos next = pos.relative(direction);
					if (solid.contains(next) && reached.add(next)) queue.add(next);
				}
			}
			for (BlockPos pos : solid) {
				if (!reached.contains(pos)) {
					fail(type.id + " seed " + seed + " leaves " + plan.get(pos.getX(), pos.getY(), pos.getZ())
						+ " at " + pos.toShortString() + " behind");
				}
			}

			BlockPos helm = plan.helm();
			assertTrue(solid.contains(helm.below()), type.id + " seed " + seed + ": the helm stands on nothing");
			BlockPos seat = helm.relative(plan.helmFacing());
			for (BlockPos clear : new BlockPos[] {helm, helm.above(), seat, seat.above()}) {
				assertFalse(solid.contains(clear),
					type.id + " seed " + seed + ": " + clear.toShortString() + " is in the pilot's way");
			}
		}
	}

	/**
	 * No way for the sea into the hold. The sea is every block at or below the waterline the ship
	 * does not fill, and it spreads sideways and down through air and through anything that could be
	 * waterlogged: the sea waterlogs such a block the first time the water beside it updates.
	 */
	@ParameterizedTest
	@EnumSource(ShipType.class)
	void holdStaysDry(ShipType type) {
		for (long seed = 0; seed < SEEDS; seed++) {
			Blueprint plan = Shipwright.draw(type, seed);
			Map<BlockPos, BlockState> blocks = plan.blocks();
			BoundingBox box = plan.bounds().inflatedBy(1);

			Set<BlockPos> sea = new HashSet<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			for (int x = box.minX(); x <= box.maxX(); x++) {
				for (int z = box.minZ(); z <= box.maxZ(); z++) {
					for (int y = box.minY(); y <= 0; y++) {
						BlockPos pos = new BlockPos(x, y, z);
						if (!blocks.containsKey(pos) && sea.add(pos)) queue.add(pos);
					}
				}
			}
			while (!queue.isEmpty()) {
				BlockPos pos = queue.poll();
				for (Direction direction : new Direction[] {
						Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
					BlockPos next = pos.relative(direction);
					if (!box.isInside(next) || sea.contains(next)) continue;
					BlockState state = blocks.get(next);
					if (state == null) continue;
					boolean passable = state.isAir() || state.hasProperty(BlockStateProperties.WATERLOGGED);
					if (!passable) continue;
					if (state.isAir()) {
						fail(type.id + " seed " + seed + ": the sea reaches the hold at " + next.toShortString());
					}
					sea.add(next);
					queue.add(next);
				}
			}
		}
	}

	private static Set<BlockPos> solid(Blueprint plan) {
		Set<BlockPos> solid = new HashSet<>();
		plan.blocks().forEach((pos, state) -> {
			if (!state.isAir()) solid.add(pos);
		});
		return solid;
	}
}
