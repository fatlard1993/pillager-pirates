package justfatlard.pillager_pirates.ship;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * One wood's worth of shipbuilding stock.
 *
 * <p>A ship is built from two of these the way a vanilla shipwreck is: one for the hull and one for
 * the decks and rails. The pairings lean on dark oak, the wood pillagers build their outposts from.
 */
public record Timber(Block planks, Block stairs, Block slab, Block log, Block fence, Block gate, Block trapdoor,
		Block door) {

	public static final Timber OAK = new Timber(Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.OAK_SLAB,
		Blocks.OAK_LOG, Blocks.OAK_FENCE, Blocks.OAK_FENCE_GATE, Blocks.OAK_TRAPDOOR, Blocks.OAK_DOOR);
	public static final Timber SPRUCE = new Timber(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB,
		Blocks.SPRUCE_LOG, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_FENCE_GATE, Blocks.SPRUCE_TRAPDOOR, Blocks.SPRUCE_DOOR);
	public static final Timber DARK_OAK = new Timber(Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_STAIRS,
		Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_FENCE_GATE,
		Blocks.DARK_OAK_TRAPDOOR, Blocks.DARK_OAK_DOOR);
	public static final Timber BIRCH = new Timber(Blocks.BIRCH_PLANKS, Blocks.BIRCH_STAIRS, Blocks.BIRCH_SLAB,
		Blocks.BIRCH_LOG, Blocks.BIRCH_FENCE, Blocks.BIRCH_FENCE_GATE, Blocks.BIRCH_TRAPDOOR, Blocks.BIRCH_DOOR);
	public static final Timber JUNGLE = new Timber(Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB,
		Blocks.JUNGLE_LOG, Blocks.JUNGLE_FENCE, Blocks.JUNGLE_FENCE_GATE, Blocks.JUNGLE_TRAPDOOR, Blocks.JUNGLE_DOOR);

	/** A hull wood and a deck wood. */
	public record Pair(Timber hull, Timber deck) {}

	private static final Pair[] PAIRS = {
		new Pair(DARK_OAK, SPRUCE),
		new Pair(DARK_OAK, SPRUCE),
		new Pair(DARK_OAK, BIRCH),
		new Pair(DARK_OAK, BIRCH),
		new Pair(SPRUCE, DARK_OAK),
		new Pair(SPRUCE, DARK_OAK),
		new Pair(OAK, SPRUCE),
		new Pair(SPRUCE, JUNGLE),
	};

	public static Pair pick(RandomSource random) {
		return PAIRS[random.nextInt(PAIRS.length)];
	}
}
