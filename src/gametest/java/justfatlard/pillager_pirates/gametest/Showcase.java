package justfatlard.pillager_pirates.gametest;

import justfatlard.pillager_pirates.ship.Blueprint;
import justfatlard.pillager_pirates.ship.ShipType;
import justfatlard.pillager_pirates.ship.Shipwright;
import justfatlard.pillager_pirates.worldgen.PirateShipPiece;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The pictures for the readme and the mod page: a brigantine afloat and crewed, and a longship
 * beside it, both built the way worldgen builds them.
 *
 * <p>The ships go in through {@link PirateShipPiece}, the same piece the structure places, so what
 * is photographed is what a player sails up to. Run it under xvfb-run; the frames land in
 * build/run/clientGameTest/screenshots.
 */
public final class Showcase implements FabricClientGameTest {

	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();

			context.getInput().pressKey(options -> options.keyToggleGui);
			context.runOnClient(client -> client.options.renderDistance().set(16));
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("gamerule doWeatherCycle false");
			server.runCommand("weather clear");
			server.runCommand("time set 1000");
			server.runCommand("gamemode spectator @a");

			BlockPos origin = server.computeOnServer(s -> connection.getServerPlayer().blockPosition());
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();

			// Open water for them to sit in, with the surface where the ground was. In strips: a
			// fill is capped at 32768 blocks and a sea the size of the first attempt is silently
			// refused, which leaves the ships sitting on grass.
			for (int strip = -60; strip <= 60; strip += 20) {
				server.runCommand("fill %d %d %d %d %d %d minecraft:water"
					.formatted(x - 60, y - 3, z + strip, x + 60, y - 1, z + strip + 19));
				server.runCommand("fill %d %d %d %d %d %d minecraft:air"
					.formatted(x - 60, y, z + strip, x + 60, y + 12, z + strip + 19));
			}

			String sea = server.computeOnServer(s -> "water at the surface: "
				+ s.overworld().getBlockState(new BlockPos(x + 10, y - 1, z + 10)));
			if (!sea.contains("water")) throw new AssertionError("PROBE " + sea);

			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				build(level, ShipType.BRIGANTINE, new BlockPos(x, y - 1, z - 6), Rotation.NONE, 20260916L);
				build(level, ShipType.LONGSHIP, new BlockPos(x + 22, y - 1, z + 10), Rotation.CLOCKWISE_90, 4242L);
			});
			context.waitTicks(120);

			look(server, x - 22.0, y + 6.0, z + 16.0, x, y + 3.0, z - 4.0);
			context.waitTicks(80);
			shoot(context, "brigantine");

			look(server, x + 34.0, y + 10.0, z + 30.0, x + 8.0, y + 4.0, z);
			context.waitTicks(80);
			shoot(context, "fleet");
		}
	}

	/** One ship, through the piece the structure itself places. */
	private static void build(ServerLevel level, ShipType type, BlockPos at, Rotation heading, long seed) {
		Blueprint plan = Shipwright.draw(type, seed);
		PirateShipPiece piece = new PirateShipPiece(type, at, heading, seed, plan);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
			RandomSource.create(seed), BoundingBox.infinite(), new ChunkPos(at.getX() >> 4, at.getZ() >> 4), at);
	}

	/**
	 * Stand the camera at one place and point it at another. The camera's y is the feet, so it
	 * looks from 1.62 above where it stands.
	 */
	private void look(TestServerContext server, double x, double y, double z,
			double atX, double atY, double atZ) {
		double dx = atX - x;
		double dy = atY - (y + 1.62);
		double dz = atZ - z;
		double yaw = -Math.toDegrees(Math.atan2(dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand("tp @a %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
	}

	private void shoot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name)
			.withSize(WIDTH, HEIGHT)
			.disableCounterPrefix());
	}
}
