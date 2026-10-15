package justfatlard.pillager_pirates;

import justfatlard.pillager_pirates.worldgen.ShipStructureRegistration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PillagerPirates implements ModInitializer {

	public static final String MOD_ID = "pillager-pirates-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ShipStructureRegistration.register();
		// Optional: the crew's pirate clothes need Pandorical, on the server and on the client. Without
		// it they are plain pillagers, and nothing else changes.
		if (FabricLoader.getInstance().isModLoaded("pandorical")) {
			justfatlard.pillager_pirates.integration.PirateClothes.register();
		}
		LOGGER.info("Pillager Pirates loaded");
	}
}
