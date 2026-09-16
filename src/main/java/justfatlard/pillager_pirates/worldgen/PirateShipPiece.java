package justfatlard.pillager_pirates.worldgen;

import justfatlard.pillager_pirates.PillagerPirates;
import justfatlard.pillager_pirates.integration.BigBoatsHelm;
import justfatlard.pillager_pirates.ship.Blueprint;
import justfatlard.pillager_pirates.ship.Crew;
import justfatlard.pillager_pirates.ship.ShipType;
import justfatlard.pillager_pirates.ship.Shipwright;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Map;

/**
 * One ship, written into the world a chunk at a time.
 *
 * <p>{@link #postProcess} is called once for every chunk the ship's bounding box touches and may only
 * write inside the chunk it was handed. The blueprint is drawn in full from the stored seed and each
 * call writes the part of it that lands in its chunk; a mob's anchor is one block, one block is in one
 * chunk, so every hand is put aboard exactly once.
 *
 * <p>The blueprint is cached on the piece because the structure start is shared by every chunk that
 * references it. The field is volatile because chunk workers run in parallel: two threads racing here
 * waste a draw, which is harmless.
 */
public class PirateShipPiece extends StructurePiece {

	private static final String TAG_SHIP = "Ship";
	private static final String TAG_SEED = "Seed";
	private static final String TAG_HEADING = "Heading";
	private static final String TAG_X = "OriginX";
	private static final String TAG_Y = "OriginY";
	private static final String TAG_Z = "OriginZ";

	private final ShipType ship;
	private final BlockPos origin;
	private final Rotation heading;
	private final long seed;

	private volatile Blueprint plan;

	public PirateShipPiece(ShipType ship, BlockPos origin, Rotation heading, long seed, Blueprint plan) {
		super(ShipStructureRegistration.PIRATE_SHIP_PIECE, 0, bounds(ship, origin, heading, plan));
		this.ship = ship;
		this.origin = origin;
		this.heading = heading;
		this.seed = seed;
		this.plan = plan;
	}

	public PirateShipPiece(CompoundTag tag) {
		super(ShipStructureRegistration.PIRATE_SHIP_PIECE, tag);
		this.ship = ShipType.byId(tag.getStringOr(TAG_SHIP, ShipType.SLOOP.id));
		this.seed = tag.getLongOr(TAG_SEED, 0L);
		this.heading = Rotation.values()[Math.floorMod(tag.getIntOr(TAG_HEADING, 0), 4)];
		this.origin = new BlockPos(
			tag.getIntOr(TAG_X, boundingBox.getCenter().getX()),
			tag.getIntOr(TAG_Y, boundingBox.minY()),
			tag.getIntOr(TAG_Z, boundingBox.getCenter().getZ()));
	}

	/**
	 * The ship's frame to the world's: centred on the origin along its length, then turned. The
	 * structure's water soundings go through this too, so they sample the water the ship will actually
	 * be written into.
	 */
	static BlockPos toWorld(BlockPos origin, Rotation heading, ShipType ship, BlockPos local) {
		return origin.offset(local.offset(0, 0, -ship.length / 2).rotate(heading));
	}

	private static BoundingBox bounds(ShipType ship, BlockPos origin, Rotation heading, Blueprint plan) {
		BoundingBox local = plan.bounds();
		BlockPos a = toWorld(origin, heading, ship, new BlockPos(local.minX(), local.minY(), local.minZ()));
		BlockPos b = toWorld(origin, heading, ship, new BlockPos(local.maxX(), local.maxY(), local.maxZ()));
		return BoundingBox.fromCorners(a, b);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putString(TAG_SHIP, ship.id);
		tag.putLong(TAG_SEED, seed);
		tag.putInt(TAG_HEADING, heading.ordinal());
		tag.putInt(TAG_X, origin.getX());
		tag.putInt(TAG_Y, origin.getY());
		tag.putInt(TAG_Z, origin.getZ());
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
		try {
			Blueprint blueprint = plan();
			placeBlocks(level, blueprint, chunkBox);
			fillContainers(level, blueprint, chunkBox);
			raiseBanners(level, blueprint, chunkBox);
			placeHelm(level, blueprint, chunkBox);
			for (Blueprint.Crew hand : blueprint.crew()) {
				BlockPos at = toWorld(hand.pos());
				if (chunkBox.isInside(at)) Crew.spawn(level, at, hand.hand(), random);
			}
		} catch (Exception e) {
			// One bad chunk of a ship beats aborting the chunk's whole decoration pass.
			PillagerPirates.LOGGER.error("Pirate ship placement failed at {}", chunkPos, e);
		}
	}

	private Blueprint plan() {
		Blueprint cached = plan;
		if (cached == null) {
			cached = Shipwright.draw(ship, seed);
			plan = cached;
		}
		return cached;
	}

	private BlockPos toWorld(BlockPos local) {
		return toWorld(origin, heading, ship, local);
	}

	private void placeBlocks(WorldGenLevel level, Blueprint blueprint, BoundingBox chunkBox) {
		for (Map.Entry<BlockPos, BlockState> entry : blueprint.blocks().entrySet()) {
			BlockPos at = toWorld(entry.getKey());
			if (!chunkBox.isInside(at)) continue;

			BlockState state = entry.getValue().rotate(heading);
			level.setBlock(at, state, Block.UPDATE_CLIENTS);

			// Rails and bars are drawn unconnected; the chunk works out what joins to what once its
			// neighbours exist, the way vanilla's own structure pieces leave their fences. A ladder is
			// checked the same way, and one with nothing behind it goes rather than floating.
			Block block = state.getBlock();
			if (block instanceof FenceBlock || block instanceof IronBarsBlock || block instanceof LadderBlock) {
				level.getChunk(at).markPosForPostProcessing(at);
			}
		}
	}

	private void fillContainers(WorldGenLevel level, Blueprint blueprint, BoundingBox chunkBox) {
		for (Blueprint.Container container : blueprint.containers()) {
			BlockPos at = toWorld(container.pos());
			if (!chunkBox.isInside(at)) continue;
			if (level.getBlockEntity(at) instanceof RandomizableContainer box) {
				// Seeded off the ship and the position rather than the chunk's random, so a chest rolls
				// the same contents whichever order its chunk happened to generate in.
				box.setLootTable(container.loot(), seed ^ at.asLong());
			}
		}
	}

	private void placeHelm(WorldGenLevel level, Blueprint blueprint, BoundingBox chunkBox) {
		if (blueprint.helm() == null || !FabricLoader.getInstance().isModLoaded("big-boats-justfatlard")) return;
		BlockPos at = toWorld(blueprint.helm());
		if (!chunkBox.isInside(at)) return;
		BigBoatsHelm.place(level, at, heading.rotate(blueprint.helmFacing()), ship.tonnage);
	}

	private void raiseBanners(WorldGenLevel level, Blueprint blueprint, BoundingBox chunkBox) {
		ItemStack ominous = null;
		for (BlockPos pos : blueprint.banners()) {
			BlockPos at = toWorld(pos);
			if (!chunkBox.isInside(at)) continue;
			if (!(level.getBlockEntity(at) instanceof BannerBlockEntity banner)) continue;
			if (ominous == null) {
				ominous = Raid.getOminousBannerInstance(level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN));
			}
			banner.applyComponentsFromItemStack(ominous);
		}
	}
}
