package justfatlard.pillager_pirates.integration;

import justfatlard.big_boats.BigBoats;
import justfatlard.big_boats.block.HelmBlock;
import justfatlard.big_boats.block.HelmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;

/**
 * The ship's wheel, when big-boats is installed: a real helm, rated for exactly the ship it stands on.
 *
 * <p>Taking a pillager ship is meant to end with sailing it away, so it comes with the one part of that
 * a player could not otherwise find aboard. The rating is the ship's own tonnage and no more - a sloop
 * carries a Tonnage I helm, a galleon a Tonnage III - so the helm is a trophy sized to the fight, and
 * the christening bottle is still the player's to bring.
 *
 * <p>Only reached behind an {@code isModLoaded} check: naming a big-boats type anywhere else would load
 * it whether or not big-boats is there.
 */
public final class BigBoatsHelm {

	private BigBoatsHelm() {}

	public static void place(WorldGenLevel level, BlockPos pos, Direction facing, int tonnage) {
		level.setBlock(pos, BigBoats.HELM_BLOCK.defaultBlockState().setValue(HelmBlock.FACING, facing),
			Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(pos) instanceof HelmBlockEntity helm) {
			helm.setTonnage(tonnage);
		}
	}
}
