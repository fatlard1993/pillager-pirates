package justfatlard.pillager_pirates.ship;

import justfatlard.pillager_pirates.PillagerPirates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Draws one ship, whole, from its type and a seed.
 *
 * <p>The seed is the only input, so the same seed always draws the same ship: that is what lets every
 * chunk the ship crosses redraw it independently and agree on where each block goes. Nothing in here
 * may look at the world.
 */
public final class Shipwright {

	public static final ResourceKey<LootTable> SUPPLY = loot("chests/pirate_supply");
	public static final ResourceKey<LootTable> ARMORY = loot("chests/pirate_armory");
	public static final ResourceKey<LootTable> TREASURE = loot("chests/pirate_treasure");
	public static final ResourceKey<LootTable> CAPTAIN = loot("chests/pirate_captain");

	private static ResourceKey<LootTable> loot(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(PillagerPirates.MOD_ID, path));
	}

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private final ShipType type;
	private final RandomSource random;
	private final Hull hull;
	private final Timber wood;
	private final Timber trim;
	private final DyeColor canvas;
	private final DyeColor accent;
	private final boolean tattered;
	private final Blueprint plan = new Blueprint();
	private final int deckY;

	private Shipwright(ShipType type, long seed) {
		this.type = type;
		this.random = RandomSource.create(seed);
		this.hull = new Hull(type);
		Timber.Pair pair = Timber.pick(random);
		this.wood = pair.hull();
		this.trim = pair.deck();
		this.canvas = random.nextInt(4) == 0 ? DyeColor.LIGHT_GRAY : DyeColor.WHITE;
		this.accent = random.nextBoolean() ? DyeColor.GRAY : DyeColor.BLACK;
		this.tattered = random.nextInt(3) == 0;
		this.deckY = type.deck;
	}

	public static Blueprint draw(ShipType type, long seed) {
		return new Shipwright(type, seed).build();
	}

	private Blueprint build() {
		hull.draw(plan, wood, trim);
		bulwarks(type != ShipType.SLOOP);
		switch (type) {
			case SLOOP -> sloop();
			case LONGSHIP -> longship();
			case BRIGANTINE -> brigantine();
			case GALLEON -> galleon();
		}
		plan.pruneLooseCanvas(new BlockPos(0, type.keelY(), type.length / 2));
		return plan;
	}

	// ---------------------------------------------------------------- the four ships

	private void sloop() {
		int roof = castle(0, 2, deckY, 2, true);
		cabinLantern(0, roof - 1, 1);
		plan.container(new BlockPos(1, deckY + 1, 1), chest(Direction.SOUTH), TREASURE);
		helm(deckY + 1, 4);

		int mast = 8;
		int top = deckY + 13;
		mast(mast, top);
		masthead(mast, top);
		gaffSail(mast, roof + 3, 1, top - 3);
		BlockPos tip = bowsprit(new int[][] {{1, 1}, {2, 2}, {2, 3}});
		jib(mast, top - 5, tip.getZ(), tip.getY());

		hatch(mast);
		gangway(6);
		holdStores(2, 0, 2, hull.bowZ() - 3);
		sternFlag(roof + 1);

		List<BlockPos> deck = spots(deckY + 1, 4, hull.bowZ());
		station(deck, Blueprint.Hand.PILLAGER, 3);
		if (random.nextInt(3) == 0) station(spots(roof + 1, 0, 3), Blueprint.Hand.CAPTAIN, 1);
	}

	private void longship() {
		int bow = hull.bowZ();
		prow(bow);
		sternPost();

		int mast = 9;
		int top = deckY + 11;
		mast(mast, top);
		masthead(mast, top);
		squareSail(mast, deckY + 9, 4, deckY + 4, true);

		int gangway = 8;
		shieldsAndOars(gangway);
		helm(deckY + 1, 3);
		plan.container(new BlockPos(0, deckY + 1, 5), chest(Direction.SOUTH), ARMORY);
		plan.container(new BlockPos(1, deckY + 1, bow - 5), barrel(), SUPPLY);
		plan.container(new BlockPos(-1, deckY + 1, bow - 5), barrel(), SUPPLY);
		gangway(gangway);
		deckLantern(hull.halfWidth(deckY, 5), 5);
		deckLantern(-hull.halfWidth(deckY, 5), 5);

		List<BlockPos> deck = spots(deckY + 1, 0, bow);
		station(deck, Blueprint.Hand.VINDICATOR, 3);
		station(deck, Blueprint.Hand.PILLAGER, 2);
		if (random.nextBoolean()) station(deck, Blueprint.Hand.CAPTAIN, 1);
	}

	private void brigantine() {
		int roof = castle(0, 5, deckY, 3, true);
		captainsCabin(1, 4, roof, true);
		helm(roof + 1, 3);

		int main = 10;
		int mainTop = deckY + 18;
		mast(main, mainTop);
		squareSail(main, deckY + 9, 4, deckY + 4, false);
		int nest = crowsNest(main, deckY + 10);
		squareSail(main, deckY + 15, 3, deckY + 12, false);
		masthead(main, mainTop);

		int fore = 17;
		int foreTop = deckY + 14;
		mast(fore, foreTop);
		squareSail(fore, deckY + 8, 3, deckY + 4, false);
		squareSail(fore, deckY + 13, 3, deckY + 10, false);
		masthead(fore, foreTop);
		BlockPos tip = bowsprit(new int[][] {{2, 1}, {3, 2}, {3, 3}, {4, 4}});
		jib(fore, foreTop - 4, tip.getZ(), tip.getY());

		hatch(main);
		gangway(7);
		holdStores(3, 1, 7, hull.bowZ() - 3);
		plan.container(new BlockPos(0, type.keelY() + 2, 2), chest(Direction.SOUTH), TREASURE);
		sternFlag(roof + 1);
		deckLantern(hull.halfWidth(deckY, 14), 14);
		deckLantern(-hull.halfWidth(deckY, 14), 14);

		List<BlockPos> deck = spots(deckY + 1, 6, hull.bowZ());
		station(deck, Blueprint.Hand.PILLAGER, 4);
		station(deck, Blueprint.Hand.VINDICATOR, 1);
		station(spots(roof + 1, 0, 5), Blueprint.Hand.CAPTAIN, 1);
		station(spots(nest + 1, main - 2, main + 2), Blueprint.Hand.PILLAGER, 1);
	}

	private void galleon() {
		int quarterdeck = castle(0, 7, deckY, 3, true);
		captainsCabin(1, 6, quarterdeck, false);
		int poop = castle(0, 3, quarterdeck, 2, true);
		captainsQuarters(1, 2, quarterdeck + 1, poop);
		helm(quarterdeck + 1, 6);

		int bow = hull.bowZ();
		int forecastle = castle(bow - 4, bow, deckY, 3, false);
		cabinLantern(0, forecastle - 1, bow - 3);
		plan.container(new BlockPos(0, deckY + 1, bow - 2), barrel(), SUPPLY);

		int mizzen = 10;
		int mizzenTop = deckY + 19;
		mast(mizzen, mizzenTop);
		gaffSail(mizzen, poop + 2, 4, mizzenTop - 5);
		masthead(mizzen, mizzenTop);

		int main = 16;
		int mainTop = deckY + 23;
		mast(main, mainTop);
		squareSail(main, deckY + 10, 6, deckY + 4, false);
		int nest = crowsNest(main, deckY + 11);
		squareSail(main, deckY + 18, 4, deckY + 13, false);
		masthead(main, mainTop);

		int fore = 22;
		int foreTop = deckY + 20;
		mast(fore, foreTop);
		squareSail(fore, deckY + 9, 5, deckY + 5, false);
		squareSail(fore, deckY + 16, 3, deckY + 11, false);
		masthead(fore, foreTop);
		BlockPos tip = bowsprit(new int[][] {{5, 1}, {6, 2}, {6, 3}, {7, 4}, {7, 5}});
		jib(fore, foreTop - 5, tip.getZ(), tip.getY());

		hatch(main);
		gangway(13);
		int brig = main + 3;
		brig(brig);
		gunPorts(brig, brig + 3);
		plan.container(new BlockPos(-hull.halfWidth(1, 21) + 1, 1, 21), chest(Direction.EAST), ARMORY);
		holdStores(4, 1, 10, main - 1);
		plan.container(new BlockPos(0, type.keelY() + 2, 3), chest(Direction.SOUTH), TREASURE);
		sternFlag(poop + 1);

		List<BlockPos> deck = spots(deckY + 1, 9, bow - 5);
		station(deck, Blueprint.Hand.PILLAGER, 5);
		station(deck, Blueprint.Hand.VINDICATOR, 2);
		station(spots(forecastle + 1, bow - 4, bow), Blueprint.Hand.PILLAGER, 1);
		station(spots(quarterdeck + 1, 4, 7), Blueprint.Hand.CAPTAIN, 1);
		station(spots(nest + 1, main - 2, main + 2), Blueprint.Hand.PILLAGER, 1);
		if (random.nextInt(10) < 3) station(spots(deckY + 1, 1, 6), Blueprint.Hand.EVOKER, 1);
	}

	// ---------------------------------------------------------------- hull fittings

	/**
	 * A planked bulwark with a rail along its top, or on a boat as small as a sloop just the rail. Either
	 * way the rail is what keeps the crew aboard: it is too tall for a mob to jump.
	 */
	private void bulwarks(boolean planked) {
		int hw = type.halfBeam;
		for (int z = 0; z <= hull.bowZ(); z++) {
			for (int x = -hw; x <= hw; x++) {
				if (!hull.edge(x, z)) continue;
				if (planked) {
					plan.set(x, deckY + 1, z, planks());
					plan.set(x, deckY + 2, z, fence());
				} else {
					plan.set(x, deckY + 1, z, fence());
				}
			}
		}
	}

	/**
	 * A raised deckhouse at one end of the ship: walls on the hull's outline, a door and a ladder on the
	 * side facing midships, and a railed deck on top. Returns the y of that deck.
	 *
	 * <p>The ladder rather than a stair is deliberate. Illagers path over stairs and not up ladders, so
	 * the crew stays on whichever deck it was put on, and a stair beside a bulwark gives anything
	 * climbing it a step high enough to walk over the rail into the sea.
	 */
	private int castle(int zFrom, int zTo, int floorY, int height, boolean aft) {
		int hw = type.halfBeam;
		int wallZ = aft ? zTo : zFrom;
		Direction inboard = aft ? Direction.SOUTH : Direction.NORTH;
		int roofY = floorY + height + 1;

		for (int z = zFrom; z <= zTo; z++) {
			for (int x = -hw; x <= hw; x++) {
				if (!hull.inside(x, deckY, z)) continue;
				boolean wall = hull.edge(x, z) || z == wallZ;
				for (int y = floorY + 1; y <= floorY + height; y++) {
					plan.set(x, y, z, wall ? planks() : AIR);
				}
				plan.set(x, roofY, z, deckPlanks());
				if (wall) plan.set(x, roofY + 1, z, fence());
			}
		}

		// Windows: fence grates along the sides and across the end of the ship, never on the wall the
		// door and the ladder share.
		int windowY = floorY + 2;
		int outer = aft ? zFrom : zTo;
		for (int z = zFrom + 1; z < zTo; z += 2) {
			int w = hull.halfWidth(deckY, z);
			plan.set(w, windowY, z, fence());
			plan.set(-w, windowY, z, fence());
		}
		int endWidth = hull.halfWidth(deckY, outer);
		for (int x = -endWidth + 1; x < endWidth; x += 2) {
			if (x != 0) plan.set(x, windowY, outer, fence());
		}

		plan.set(0, floorY + 1, wallZ, door(inboard, DoubleBlockHalf.LOWER));
		plan.set(0, floorY + 2, wallZ, door(inboard, DoubleBlockHalf.UPPER));

		int ladderX = Math.max(2, hull.halfWidth(deckY, wallZ) - 1);
		int ladderZ = wallZ + inboard.getStepZ();
		for (int y = floorY + 1; y <= roofY; y++) {
			plan.set(ladderX, y, ladderZ, ladder(inboard, false));
		}
		plan.set(ladderX, roofY + 1, wallZ, AIR);

		// Lanterns on the outboard corners of the roof rail.
		int cornerZ = aft ? zFrom + 1 : zTo - 1;
		int cornerW = hull.halfWidth(deckY, cornerZ);
		plan.set(cornerW, roofY + 2, cornerZ, lantern(false));
		plan.set(-cornerW, roofY + 2, cornerZ, lantern(false));
		return roofY;
	}

	private void captainsCabin(int zFrom, int zTo, int roofY, boolean withBed) {
		int y = deckY + 1;
		plan.container(new BlockPos(1, y, zFrom), chest(Direction.SOUTH), CAPTAIN);
		plan.set(-1, y, zFrom, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
		cabinLantern(0, roofY - 1, zFrom + 1);
		if (withBed) {
			int side = hull.halfWidth(deckY, zTo - 2) - 1;
			bed(-side, y, zTo - 1, Direction.NORTH);
		}
		plan.container(new BlockPos(hull.halfWidth(deckY, zTo) - 1, y, zTo - 1), barrel(), SUPPLY);
	}

	private void captainsQuarters(int zFrom, int zTo, int y, int roofY) {
		int side = hull.halfWidth(deckY, zFrom) - 1;
		bed(-side, y, zTo, Direction.NORTH);
		plan.set(side, y, zFrom, Blocks.LECTERN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
		cabinLantern(0, roofY - 1, zFrom);
	}

	private void cabinLantern(int x, int y, int z) {
		plan.set(x, y, z, lantern(true));
	}

	private void deckLantern(int x, int z) {
		plan.set(x, deckY + 3, z, lantern(false));
	}

	/** A hatch in the deck just forward of the mast, and a ladder down the mast into the hold. */
	private void hatch(int mastZ) {
		int z = mastZ + 1;
		for (int y = type.keelY() + 2; y < deckY; y++) {
			plan.set(0, y, z, ladder(Direction.SOUTH, false));
		}
		plan.set(0, deckY, z, trim.trapdoor().defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
			.setValue(TrapDoorBlock.HALF, Half.TOP));
	}

	/**
	 * A way aboard from the water on both sides: a ladder up the hull and a gate in the bulwark at its
	 * head. The gate is the only gap in the rail at deck level, and illagers cannot open gates.
	 */
	private void gangway(int z) {
		int w = hull.halfWidth(deckY, z);
		for (int side = -1; side <= 1; side += 2) {
			int x = side * w;
			Direction out = side > 0 ? Direction.EAST : Direction.WEST;
			for (int y = 0; y <= deckY; y++) {
				// The ladder needs a full face behind it, and the turn of the bilge is a stair.
				if (y < deckY) plan.set(x, y, z, planks());
				plan.set(x + side, y, z, ladder(out, y <= 0));
			}
			plan.set(x, deckY + 1, z, trim.gate().defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, out));
			plan.set(x, deckY + 2, z, AIR);
		}
	}

	/** Stores stood against the sides of the hold, between two stations. */
	private void holdStores(int barrels, int chests, int zFrom, int zTo) {
		int y = type == ShipType.GALLEON ? 1 : type.keelY() + 2;
		List<BlockPos> walls = new ArrayList<>();
		for (int z = zFrom; z <= zTo; z++) {
			int w = hull.halfWidth(y, z) - 1;
			if (w < 1) continue;
			for (int x : new int[] {-w, w}) {
				if (plan.isOpen(x, y, z) && !plan.isOpen(x, y - 1, z)) walls.add(new BlockPos(x, y, z));
			}
		}
		for (int i = 0; i < chests && !walls.isEmpty(); i++) {
			BlockPos pos = walls.remove(random.nextInt(walls.size()));
			plan.container(pos, chest(pos.getX() > 0 ? Direction.WEST : Direction.EAST), SUPPLY);
		}
		for (int i = 0; i < barrels && !walls.isEmpty(); i++) {
			plan.container(walls.remove(random.nextInt(walls.size())), barrel(), SUPPLY);
		}
		if (type != ShipType.GALLEON) return;

		// A galleon has a hold under its lower deck as well, and that is where the cargo goes.
		int holdY = type.keelY() + 2;
		for (int z = 4; z < 26; z += 5) {
			int w = hull.halfWidth(holdY, z) - 1;
			if (w < 1) continue;
			BlockPos pos = new BlockPos(random.nextBoolean() ? w : -w, holdY, z);
			if (plan.isOpen(pos.getX(), holdY, z)) plan.container(pos, barrel(), SUPPLY);
		}
	}

	/** A cell of iron bars against the lower deck's side, and the allays the crew keeps in it. */
	private void brig(int z0) {
		int y = 1;
		int w = hull.halfWidth(y, z0 + 1) - 1;
		for (int z = z0; z <= z0 + 3; z++) {
			for (int x = w - 2; x <= w; x++) {
				boolean bars = x == w - 2 || z == z0 || z == z0 + 3;
				plan.set(x, y, z, bars ? Blocks.IRON_BARS.defaultBlockState() : AIR);
				plan.set(x, y + 1, z, bars ? Blocks.IRON_BARS.defaultBlockState() : AIR);
			}
		}
		plan.crew(new BlockPos(w, y, z0 + 1), Blueprint.Hand.ALLAY);
		plan.crew(new BlockPos(w - 1, y, z0 + 2), Blueprint.Hand.ALLAY);
		cabinLantern(w - 3, deckY - 1, z0 + 1);
	}

	/**
	 * Shutters along the lower deck, where a warship would carry its guns. None on the brig's side of
	 * it: an open port is a gap an allay fits through.
	 */
	private void gunPorts(int brigFrom, int brigTo) {
		for (int z = 12; z <= 24; z += 3) {
			int w = hull.halfWidth(2, z);
			for (int side = -1; side <= 1; side += 2) {
				if (side > 0 && z >= brigFrom && z <= brigTo) continue;
				Direction out = side > 0 ? Direction.EAST : Direction.WEST;
				plan.set(side * w, 2, z, wood.trapdoor().defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, out.getOpposite())
					.setValue(TrapDoorBlock.OPEN, true));
			}
		}
	}

	/** A longship's stem: up out of the bow, forward, then curled back over itself. */
	private void prow(int bow) {
		path(new int[][] {{1, 0}, {2, 0}, {3, 1}, {4, 1}, {5, 1}, {6, 0}, {6, -1}}, bow, i -> planks());
	}

	private void sternPost() {
		int stern = hull.sternZ(deckY);
		path(new int[][] {{1, 0}, {2, 0}, {3, -1}, {4, -1}}, stern, i -> planks());
		plan.set(0, deckY + 5, stern - 1, fence());
		plan.banner(new BlockPos(0, deckY + 6, stern - 1), standingBanner());
	}

	/**
	 * Banners hung on the outside of the bulwark like a rank of shields, with oars run out between
	 * them. The oar blades dip into the sea, so the block that sits in the water is waterlogged.
	 */
	private void shieldsAndOars(int gangwayZ) {
		DyeColor[] shields = {DyeColor.GRAY, DyeColor.BLACK, DyeColor.LIGHT_GRAY};
		for (int z = 4; z <= hull.bowZ() - 5; z++) {
			if (Math.abs(z - gangwayZ) <= 1) continue;
			int w = hull.halfWidth(deckY, z);
			for (int side = -1; side <= 1; side += 2) {
				Direction out = side > 0 ? Direction.EAST : Direction.WEST;
				if (z % 2 == 0) {
					BlockPos pos = new BlockPos(side * (w + 1), deckY + 1, z);
					if ((z / 2) % 2 == 0) {
						plan.banner(pos, wallBanner(DyeColor.WHITE, out));
					} else {
						plan.set(pos, wallBanner(shields[(z / 2) % shields.length], out));
					}
				} else if (z % 4 == 1) {
					plan.set(side * (w + 1), deckY, z, oar(deckY <= 0));
					plan.set(side * (w + 2), deckY, z, oar(deckY <= 0));
					plan.set(side * (w + 2), deckY - 1, z, oar(deckY - 1 <= 0));
				}
			}
		}
	}

	// ---------------------------------------------------------------- rigging

	private void mast(int z, int top) {
		for (int y = type.keelY() + 1; y <= top; y++) {
			plan.set(0, y, z, wood.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
		}
	}

	private void masthead(int z, int top) {
		plan.banner(new BlockPos(0, top + 1, z), standingBanner());
	}

	/**
	 * A square sail hung from a yard across the ship, bellied forward the way wind from astern fills
	 * it: the middle of the canvas stands a block ahead of its edges.
	 */
	private void squareSail(int mastZ, int yardY, int span, int bottomY, boolean striped) {
		for (int x = -span; x <= span; x++) {
			if (x == 0) continue;
			plan.set(x, yardY, mastZ, wood.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
		int band = (yardY + bottomY) / 2;
		for (int y = bottomY; y < yardY; y++) {
			for (int x = -span + 1; x <= span - 1; x++) {
				boolean belly = Math.abs(x) <= span - 3 && y > bottomY && y < yardY - 1;
				int z = belly ? mastZ + 1 : mastZ;
				if (x == 0 && !belly) continue;
				if (torn(y == bottomY)) continue;
				DyeColor color = striped
					? (Math.floorMod(x, 2) == 0 ? canvas : accent)
					: (y == band ? accent : canvas);
				plan.set(x, y, z, Blocks.WOOL.pick(color).defaultBlockState());
			}
		}
	}

	/**
	 * A fore-and-aft sail set behind the mast: a boom along the foot and canvas rising toward the
	 * peak of the gaff at its after end.
	 */
	private void gaffSail(int mastZ, int boomY, int aftZ, int peakY) {
		for (int z = aftZ; z < mastZ; z++) {
			plan.set(0, boomY, z, wood.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		}
		int throatY = peakY - 2;
		for (int z = aftZ + 1; z < mastZ; z++) {
			double aft = (mastZ - z) / (double) (mastZ - aftZ);
			int head = throatY + (int) Math.round(aft * 2);
			for (int y = boomY + 1; y <= head; y++) {
				if (torn(y == boomY + 1)) continue;
				plan.set(0, y, z, Blocks.WOOL.pick(y == head ? accent : canvas).defaultBlockState());
			}
		}
	}

	/** A triangle of canvas from the foremast down to the end of the bowsprit. */
	private void jib(int mastZ, int headY, int tipZ, int tipY) {
		int fromZ = mastZ + 1;
		int footY = deckY + 5;
		for (int z = fromZ; z < tipZ; z++) {
			double along = (z - mastZ) / (double) (tipZ - mastZ);
			int stay = (int) Math.round(headY + (tipY - headY) * along);
			double foot = (z - fromZ) / (double) (tipZ - fromZ);
			int bottom = (int) Math.round(footY + (tipY + 1 - footY) * foot);
			for (int y = bottom; y < stay; y++) {
				plan.set(0, y, z, Blocks.WOOL.pick(canvas).defaultBlockState());
			}
		}
	}

	/** Steps of {dy, dz} out from the bow; returns where the bowsprit ends. */
	private BlockPos bowsprit(int[][] steps) {
		int last = steps.length - 1;
		return path(steps, hull.bowZ(), i -> i < last - 1
			? wood.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z)
			: fence());
	}

	/**
	 * Blocks along the centreline at steps of {dy, dz} from a station, joined face to face: where a
	 * step moves both up and along, the corner between is filled from the step before. Two blocks
	 * that only touch at an edge are two things to a helm's flood fill, and the second gets left
	 * behind when the ship sails. Returns the last block.
	 */
	private BlockPos path(int[][] steps, int fromZ, IntFunction<BlockState> state) {
		BlockPos end = null;
		for (int i = 0; i < steps.length; i++) {
			int y = deckY + steps[i][0];
			int z = fromZ + steps[i][1];
			if (end != null && end.getY() != y && end.getZ() != z) {
				plan.set(0, end.getY(), z, state.apply(i));
			}
			plan.set(0, y, z, state.apply(i));
			end = new BlockPos(0, y, z);
		}
		return end;
	}

	/** Marks the wheel: the pilot sits aft of it, so it faces aft, and the seat is kept clear. */
	private void helm(int y, int z) {
		plan.helm(new BlockPos(0, y, z), Direction.NORTH);
	}

	/** A railed platform round the mast with a ladder up the mast's after face. Returns the floor y. */
	private int crowsNest(int mastZ, int y) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (dx == 0 && dz == 0) continue;
				plan.set(dx, y, mastZ + dz, trim.slab().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
				if (Math.abs(dx) == 2 || Math.abs(dz) == 2) plan.set(dx, y + 1, mastZ + dz, fence());
			}
		}
		for (int yy = deckY + 1; yy <= y; yy++) {
			plan.set(0, yy, mastZ - 1, ladder(Direction.NORTH, false));
		}
		return y;
	}

	private void sternFlag(int railY) {
		int stern = hull.sternZ(deckY);
		plan.set(0, railY, stern, fence());
		plan.set(0, railY + 1, stern, fence());
		plan.banner(new BlockPos(0, railY + 2, stern), standingBanner());
	}

	// ---------------------------------------------------------------- crew

	/** Clear, floored, two-high spots on one level of the ship within a run of stations. */
	private List<BlockPos> spots(int y, int zFrom, int zTo) {
		List<BlockPos> found = new ArrayList<>();
		int hw = type.halfBeam + 2;
		for (int z = zFrom; z <= zTo; z++) {
			for (int x = -hw; x <= hw; x++) {
				BlockState floor = plan.get(x, y - 1, z);
				if (floor.isAir() || !floor.isSolid()) continue;
				if (!plan.isOpen(x, y, z) || !plan.isOpen(x, y + 1, z)) continue;
				BlockPos spot = new BlockPos(x, y, z);
				BlockPos wheel = plan.helm();
				if (wheel != null && (spot.equals(wheel) || spot.equals(wheel.relative(plan.helmFacing())))) continue;
				found.add(spot);
			}
		}
		return found;
	}

	/** Puts {@code count} hands on random spots, keeping them a block apart from each other. */
	private void station(List<BlockPos> spots, Blueprint.Hand hand, int count) {
		for (int i = 0; i < count && !spots.isEmpty(); i++) {
			BlockPos pos = spots.get(random.nextInt(spots.size()));
			plan.crew(pos, hand);
			spots.removeIf(other -> other.distManhattan(pos) <= 2);
		}
	}

	// ---------------------------------------------------------------- block states

	private BlockState planks() {
		return wood.planks().defaultBlockState();
	}

	private BlockState deckPlanks() {
		return trim.planks().defaultBlockState();
	}

	private BlockState fence() {
		return trim.fence().defaultBlockState();
	}

	private BlockState oar(boolean wet) {
		return trim.fence().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet);
	}

	private BlockState ladder(Direction facing, boolean wet) {
		return Blocks.LADDER.defaultBlockState()
			.setValue(LadderBlock.FACING, facing)
			.setValue(LadderBlock.WATERLOGGED, wet);
	}

	private BlockState door(Direction facing, DoubleBlockHalf half) {
		return trim.door().defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
			.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, half)
			.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT);
	}

	private BlockState lantern(boolean hanging) {
		return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hanging);
	}

	private static BlockState chest(Direction facing) {
		return Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
	}

	private static BlockState barrel() {
		return Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
	}

	private void bed(int x, int y, int z, Direction headward) {
		Block bed = Blocks.BED.pick(DyeColor.GRAY);
		BlockState state = bed.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, headward);
		plan.set(x, y, z, state.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
		plan.set(x + headward.getStepX(), y, z + headward.getStepZ(), state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
	}

	private BlockState standingBanner() {
		// Rotation 4 turns the face toward the ship's side, so the pattern shows from abeam.
		return Blocks.BANNER.white().defaultBlockState().setValue(BannerBlock.ROTATION, 4);
	}

	private static BlockState wallBanner(DyeColor color, Direction facing) {
		return Blocks.WALL_BANNER.pick(color).defaultBlockState().setValue(WallBannerBlock.FACING, facing);
	}

	private boolean torn(boolean hem) {
		if (!tattered) return false;
		return random.nextInt(hem ? 3 : 12) == 0;
	}
}
