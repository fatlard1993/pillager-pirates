package justfatlard.pillager_pirates.ship;

import justfatlard.pillager_pirates.PillagerPirates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Puts a ship's company aboard, once, during the one worldgen pass its chunk ever gets.
 *
 * <p>Every hand is marked persistent. A ship on open water is a long way from any player, which is
 * exactly when the despawn rules would clear its decks before anybody sailed out to find it. Nobody
 * comes aboard afterwards: the structures' monster spawn override is an empty list, so a cleared ship
 * stays cleared and its dark hold does not fill with zombies either. A list with pillagers in it would
 * keep a ship manned the way an outpost is, at the cost of pillagers appearing on the tops of the
 * sails and dropping into the sea.
 *
 * <p>Every illager hand also carries the {@link #TAG} entity tag, saved with it, so a crewman can be
 * told from any other pillager whenever it loads: that is how the crew gets its clothes back.
 */
public final class Crew {

	private Crew() {}

	/** The entity tag every hand aboard is given at spawn, the brig's allays excepted. */
	public static final String TAG = PillagerPirates.MOD_ID + ".crew";

	private static final TagKey<Structure> SHIPS = TagKey.create(Registries.STRUCTURE,
		Identifier.fromNamespaceAndPath(PillagerPirates.MOD_ID, "pirate_ship"));

	/**
	 * Whether this mob is one of a ship's company.
	 *
	 * <p>Crews put aboard before the tag existed are still recognised, by standing inside a ship
	 * with persistence on, which a passing patrol does not have; they are tagged the first time, so
	 * the structure is only asked once. Only asked while the chunk under the mob is loaded, so
	 * this never loads one itself: a crewman that loads ahead of its deck is caught next time.
	 */
	public static boolean isCrew(Mob mob, ServerLevel level) {
		if (mob.entityTags().contains(TAG)) return true;
		if (!mob.isPersistenceRequired()) return false;
		BlockPos pos = mob.blockPosition();
		if (!level.hasChunkAt(pos)) return false;
		if (!level.structureManager().getStructureWithPieceAt(pos, SHIPS).isValid()) return false;
		mob.addTag(TAG);
		return true;
	}

	public static void spawn(WorldGenLevel level, BlockPos pos, Blueprint.Hand hand, RandomSource random) {
		try {
			switch (hand) {
				case PILLAGER -> place(level, EntityTypes.PILLAGER, pos, random, false);
				case VINDICATOR -> place(level, EntityTypes.VINDICATOR, pos, random, false);
				case EVOKER -> place(level, EntityTypes.EVOKER, pos, random, false);
				case CAPTAIN -> place(level, EntityTypes.PILLAGER, pos, random, true);
				case ALLAY -> place(level, EntityTypes.ALLAY, pos, random, false);
			}
		} catch (Exception e) {
			// A missing crewman is a quieter ship, not a broken chunk.
			PillagerPirates.LOGGER.error("Could not place {} at {}", hand, pos, e);
		}
	}

	/**
	 * One mob, finished before it is handed to the world: during worldgen the chunk takes its copy of an
	 * entity the moment it is added, so anything set afterwards is set on nothing.
	 *
	 * <p>A captain is a patrol leader, and has to be told so before {@code finalizeSpawn}: that is
	 * where a patrol leader is handed the ominous banner it wears, and the banner is what makes it a
	 * captain as far as the game is concerned - the ominous bottle it drops comes from that.
	 */
	private static void place(WorldGenLevel level, EntityType<? extends Mob> type, BlockPos pos, RandomSource random,
			boolean captain) {
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) return;

		mob.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
		if (captain && mob instanceof PatrollingMonster leader) leader.setPatrolLeader(true);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		mob.setPersistenceRequired();
		if (type != EntityTypes.ALLAY) mob.addTag(TAG);
		level.addFreshEntityWithPassengers(mob);
	}
}
