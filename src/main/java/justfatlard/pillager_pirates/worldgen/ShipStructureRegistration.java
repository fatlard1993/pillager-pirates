package justfatlard.pillager_pirates.worldgen;

import justfatlard.pillager_pirates.PillagerPirates;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Puts the structure type and the piece type into the built-in registries.
 *
 * <p>The ships themselves are datapack objects: the four kinds, where they may sail and how often all
 * live in {@code data/.../worldgen/}. What has to be registered in code is only the pair of codecs
 * those files are read through.
 */
public final class ShipStructureRegistration {

	private ShipStructureRegistration() {}

	public static StructureType<PirateShipStructure> PIRATE_SHIP;
	public static StructurePieceType PIRATE_SHIP_PIECE;

	public static void register() {
		PIRATE_SHIP_PIECE = Registry.register(
			BuiltInRegistries.STRUCTURE_PIECE,
			Identifier.fromNamespaceAndPath(PillagerPirates.MOD_ID, "pirate_ship"),
			(context, tag) -> new PirateShipPiece(tag));

		PIRATE_SHIP = Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE,
			Identifier.fromNamespaceAndPath(PillagerPirates.MOD_ID, "pirate_ship"),
			() -> PirateShipStructure.CODEC);

		// Marked optional so a client without this mod is not dropped during Fabric's config-phase
		// registry sync: everything the ships are built from is vanilla, so the client needs nothing.
		RegistryAttributeHolder.get(BuiltInRegistries.STRUCTURE_TYPE).addAttribute(RegistryAttribute.OPTIONAL);
		RegistryAttributeHolder.get(BuiltInRegistries.STRUCTURE_PIECE).addAttribute(RegistryAttribute.OPTIONAL);
	}
}
