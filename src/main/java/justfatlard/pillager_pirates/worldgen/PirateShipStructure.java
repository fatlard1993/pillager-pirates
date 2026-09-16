package justfatlard.pillager_pirates.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import justfatlard.pillager_pirates.ship.Blueprint;
import justfatlard.pillager_pirates.ship.ShipType;
import justfatlard.pillager_pirates.ship.Shipwright;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A pillager ship riding on open water.
 *
 * <p>The biome list only says where a ship may be looked for. Whether one actually goes there is
 * decided here, by sounding the water it would sit in: every point under the hull has to be sea deep
 * enough to float it with a block to spare, and a ring well clear of it has to be open water too. A
 * chunk that is ocean by biome but half beach, or a river too narrow to turn in, is turned down. The
 * ship is tried at all four headings before the chunk gives up on it, so a long channel still gets
 * a ship lying along it.
 *
 * <p>{@code terrain_adaptation} has to stay {@code none} in the JSON: there is no terrain under a
 * ship to adapt, only sea floor well below the keel.
 */
public class PirateShipStructure extends Structure {

	public static final MapCodec<PirateShipStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			Structure.settingsCodec(instance),
			ExtraCodecs.NON_EMPTY_STRING
				.fieldOf("ship")
				.forGetter(structure -> structure.ship.id)
		).apply(instance, PirateShipStructure::new));

	/** Clear water kept beside the hull at full depth. */
	private static final int BERTH = 2;
	/** How far out the surface has to stay open, so a ship is not moored in a pond. */
	private static final int SEA_ROOM = 12;

	private final ShipType ship;

	public PirateShipStructure(StructureSettings settings, String ship) {
		super(settings);
		this.ship = ShipType.byId(ship);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int waterTop = context.chunkGenerator().getSeaLevel() - 1;
		BlockPos origin = new BlockPos(
			context.chunkPos().getMiddleBlockX(),
			waterTop,
			context.chunkPos().getMiddleBlockZ());

		// Drawn here and carried on the piece: the piece writes the ship one chunk at a time and every
		// one of those chunks has to agree on it.
		long seed = context.random().nextLong();
		Rotation first = Rotation.getRandom(context.random());

		for (int turn = 0; turn < 4; turn++) {
			Rotation heading = Rotation.values()[(first.ordinal() + turn) % 4];
			if (!seaworthy(context, origin, heading)) continue;

			Blueprint plan = Shipwright.draw(ship, seed);
			return Optional.of(new GenerationStub(origin, builder ->
				builder.addPiece(new PirateShipPiece(ship, origin, heading, seed, plan))));
		}
		return Optional.empty();
	}

	private boolean seaworthy(GenerationContext context, BlockPos origin, Rotation heading) {
		int reach = ship.halfBeam + BERTH;
		int stern = -2;
		int bow = ship.length + 1;

		// The centreline first: most failures are land under the middle of the ship, and it is the
		// cheapest place to find out.
		for (int z = stern; z <= bow; z += 3) {
			if (!deepWater(context, origin, heading, 0, z)) return false;
		}
		for (int z = stern; z <= bow; z += 3) {
			if (!deepWater(context, origin, heading, reach, z)) return false;
			if (!deepWater(context, origin, heading, -reach, z)) return false;
		}

		int room = ship.halfBeam + SEA_ROOM;
		int[][] ring = {
			{0, stern - SEA_ROOM}, {0, bow + SEA_ROOM},
			{room, ship.length / 2}, {-room, ship.length / 2},
			{room, stern - SEA_ROOM / 2}, {-room, stern - SEA_ROOM / 2},
			{room, bow + SEA_ROOM / 2}, {-room, bow + SEA_ROOM / 2},
		};
		for (int[] point : ring) {
			if (!openWater(context, origin, heading, point[0], point[1])) return false;
		}
		return true;
	}

	/** Water from the surface down past the keel, with nothing standing on it. */
	private boolean deepWater(GenerationContext context, BlockPos origin, Rotation heading, int x, int z) {
		NoiseColumn column = column(context, origin, heading, x, z);
		int waterTop = origin.getY();
		if (!column.getBlock(waterTop + 1).isAir()) return false;
		for (int y = waterTop - ship.draft - 1; y <= waterTop; y++) {
			if (!column.getBlock(y).getFluidState().is(FluidTags.WATER)) return false;
		}
		return true;
	}

	private boolean openWater(GenerationContext context, BlockPos origin, Rotation heading, int x, int z) {
		NoiseColumn column = column(context, origin, heading, x, z);
		int waterTop = origin.getY();
		return column.getBlock(waterTop + 1).isAir()
			&& column.getBlock(waterTop).getFluidState().is(FluidTags.WATER);
	}

	private NoiseColumn column(GenerationContext context, BlockPos origin, Rotation heading, int x, int z) {
		BlockPos at = PirateShipPiece.toWorld(origin, heading, ship, new BlockPos(x, 0, z));
		return context.chunkGenerator().getBaseColumn(at.getX(), at.getZ(), context.heightAccessor(),
			context.randomState());
	}

	@Override
	public StructureType<?> type() {
		return ShipStructureRegistration.PIRATE_SHIP;
	}
}
