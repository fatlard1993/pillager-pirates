package justfatlard.pillager_pirates;

import justfatlard.pillager_pirates.worldgen.ShipStructureRegistration;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PillagerPirates implements ModInitializer {

	public static final String MOD_ID = "pillager-pirates-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ShipStructureRegistration.register();
		LOGGER.info("Pillager Pirates loaded");
	}
}
