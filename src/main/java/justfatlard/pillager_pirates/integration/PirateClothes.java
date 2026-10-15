package justfatlard.pillager_pirates.integration;

import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pillager_pirates.PillagerPirates;
import justfatlard.pillager_pirates.ship.Crew;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.Pillager;

import java.util.UUID;

/**
 * Pirate clothes for the pillagers aboard, when Pandorical is installed: a texture drawn over the
 * pillager's own, so it is still a vanilla pillager underneath and a client without Pandorical
 * sees exactly that.
 *
 * <p>The captain wears the tricorne and the coat; every other hand is dressed as one of the
 * deckhands, picked from its UUID so the same pillager wears the same thing every time it loads.
 * The sheets are drawn by {@code generate_crew_textures.py}.
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

	public static void register() {
		// The client has to be handed the sheets, or there is nothing for it to draw.
		PandoricalApi.content().registerModAssets(PillagerPirates.MOD_ID);

		// Pandorical keeps an overlay in memory and drops it when the entity unloads, so every load
		// says it again.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Pillager pillager && Crew.isCrew(pillager, level)) dress(pillager);
		});
	}

	private static void dress(Pillager pillager) {
		PandoricalApi.entityOverlays().set(pillager, pillager.isPatrolLeader() ? CAPTAIN : deckhand(pillager.getUUID()));
	}

	private static Identifier deckhand(UUID uuid) {
		long bits = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
		return DECKHANDS[(int) Math.floorMod(bits ^ (bits >>> 32), (long) DECKHANDS.length)];
	}

	private static Identifier texture(String name) {
		return Identifier.fromNamespaceAndPath(PillagerPirates.MOD_ID, "textures/entity/crew/" + name + ".png");
	}
}
