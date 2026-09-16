package justfatlard.pillager_pirates.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A whole ship, drawn in its own coordinates before any of it touches the world.
 *
 * <p>A ship spans several chunks and its piece is asked to write each of them separately, so the ship
 * is drawn in full from its seed and every chunk writes the part of it that lands inside. The frame is
 * the one {@link ShipType} describes: bow toward +z, keel on x = 0, waterline at y = 0. Turning it to
 * face the way it was placed happens at write time.
 *
 * <p>Air is a real entry here, not an absence. Below the waterline it is what empties the hold of the
 * sea the ship was set down in.
 */
public final class Blueprint {

	public enum Hand { PILLAGER, VINDICATOR, CAPTAIN, EVOKER, ALLAY }

	public record Container(BlockPos pos, ResourceKey<LootTable> loot) {}

	public record Crew(BlockPos pos, Hand hand) {}

	/** The blocks themselves rather than the wool tag: tags are not bound until a world loads. */
	private static final Set<Block> CANVAS = Set.copyOf(Blocks.WOOL.asList());

	private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
	private final List<Container> containers = new ArrayList<>();
	private final List<BlockPos> banners = new ArrayList<>();
	private final List<Crew> crew = new ArrayList<>();
	private BlockPos helm;
	private Direction helmFacing = Direction.NORTH;

	public void set(int x, int y, int z, BlockState state) {
		blocks.put(new BlockPos(x, y, z), state);
	}

	public void set(BlockPos pos, BlockState state) {
		blocks.put(pos.immutable(), state);
	}

	public BlockState get(int x, int y, int z) {
		return blocks.getOrDefault(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
	}

	public boolean isOpen(int x, int y, int z) {
		return get(x, y, z).isAir();
	}

	/** A chest or barrel, filled from {@code loot} when its chunk is written. */
	public void container(BlockPos pos, BlockState state, ResourceKey<LootTable> loot) {
		set(pos, state);
		containers.add(new Container(pos.immutable(), loot));
	}

	/** A banner block that gets the illager pattern put on it once it exists. */
	public void banner(BlockPos pos, BlockState state) {
		set(pos, state);
		banners.add(pos.immutable());
	}

	public void crew(BlockPos pos, Hand hand) {
		crew.add(new Crew(pos.immutable(), hand));
	}

	/**
	 * Where a big-boats helm goes, facing the way the pilot sits. Not a block here: the ship is drawn
	 * the same whether or not big-boats is installed, and only the piece knows which it is.
	 */
	public void helm(BlockPos pos, Direction facing) {
		helm = pos.immutable();
		helmFacing = facing;
	}

	public BlockPos helm() {
		return helm;
	}

	public Direction helmFacing() {
		return helmFacing;
	}

	/**
	 * Drops canvas that has come loose from the rest of the ship.
	 *
	 * <p>Torn sails are torn at random, and now and then the holes cut a patch of canvas off from
	 * everything holding it up. A patch like that is not attached to anything a helm can find, so a
	 * crew that sails the ship away would leave it hanging in the air where the ship used to be.
	 * Only wool goes: anything else that came loose would be a drawing mistake, and {@code ShipTest}
	 * is where that should show up rather than here.
	 */
	void pruneLooseCanvas(BlockPos anchor) {
		Set<BlockPos> reached = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		reached.add(anchor);
		queue.add(anchor);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (reached.contains(next) || get(next.getX(), next.getY(), next.getZ()).isAir()) continue;
				reached.add(next);
				queue.add(next);
			}
		}
		blocks.entrySet().removeIf(entry -> !reached.contains(entry.getKey())
			&& CANVAS.contains(entry.getValue().getBlock()));
	}

	public Map<BlockPos, BlockState> blocks() {
		return blocks;
	}

	public List<Container> containers() {
		return containers;
	}

	public List<BlockPos> banners() {
		return banners;
	}

	public List<Crew> crew() {
		return crew;
	}

	public BoundingBox bounds() {
		return BoundingBox.encapsulatingPositions(blocks.keySet()).orElseThrow();
	}
}
