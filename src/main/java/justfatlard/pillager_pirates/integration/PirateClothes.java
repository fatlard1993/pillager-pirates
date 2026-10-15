package justfatlard.pillager_pirates.integration;

import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pillager_pirates.PillagerPirates;
import justfatlard.pillager_pirates.ship.Crew;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;

import java.util.UUID;

/**
 * Pirate clothes for the pillagers and vindicators aboard, when Pandorical is installed: a texture
 * drawn over the illager's own, so it is still a vanilla illager underneath and a client without
 * Pandorical sees exactly that. The evokers keep their robes.
 *
 * <p>The captain wears the tricorne and the coat; every other pillager is dressed as one of the
 * deckhands, and every vindicator as the bosun or the quartermaster, picked from its UUID so the
 * same hand wears the same thing every time it loads. The sheets are drawn by
 * {@code generate_crew_textures.py}; a vindicator's dress its crossed arms and its raised ones
 * alike.
 *
 * <p>Only reached behind an {@code isModLoaded} check: naming a Pandorical type anywhere else would
 * load it whether or not Pandorical is there.
 */
public final class PirateClothes {

	private PirateClothes() {}

	private static final Identifier CAPTAIN = texture("captain");
	private static final Identifier[] DECKHANDS = {
		texture("deckhand_stripes"),
		texture("deckhand_patch"),
		texture("deckhand_sash"),
	};
	private static final Identifier[] MATES = {
		texture("bosun"),
		texture("quartermaster"),
	};

	public static void register() {
		// The client has to be handed the sheets, or there is nothing for it to draw.
		PandoricalApi.content().registerModAssets(PillagerPirates.MOD_ID);

		// Pandorical keeps an overlay in memory and drops it when the entity unloads, so every load
		// says it again.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Pillager pillager && Crew.isCrew(pillager, level)) {
				dress(pillager, pillager.isPatrolLeader() ? CAPTAIN : pick(DECKHANDS, pillager.getUUID()));
			} else if (entity instanceof Vindicator vindicator && Crew.isCrew(vindicator, level)) {
				dress(vindicator, pick(MATES, vindicator.getUUID()));
			}
		});
	}

	private static void dress(Mob mob, Identifier clothes) {
		PandoricalApi.entityOverlays().set(mob, clothes);
	}

	private static Identifier pick(Identifier[] outfits, UUID uuid) {
		long bits = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
		return outfits[(int) Math.floorMod(bits ^ (bits >>> 32), (long) outfits.length)];
	}

	private static Identifier texture(String name) {
		return Identifier.fromNamespaceAndPath(PillagerPirates.MOD_ID, "textures/entity/crew/" + name + ".png");
	}
}
