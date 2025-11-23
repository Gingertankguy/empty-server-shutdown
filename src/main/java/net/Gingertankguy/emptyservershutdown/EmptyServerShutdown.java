package net.Gingertankguy.emptyservershutdown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

public class EmptyServerShutdown implements ModInitializer {
	public static final String MOD_ID = "empty-server-shutdown";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static long timeoutTicks = 20 * 900;
	private static long emptySince = -1;

	@Override
	public void onInitialize() {
		emptySince = -1;
		LOGGER.info("Empty Server Stopper Activated!");
		
		ConfigManager.load();
		int seconds = ConfigManager.config.shutdownTimeoutSeconds;

		timeoutTicks = seconds * 20L;
		LOGGER.info("Shutdown timeout set to {} seconds.", seconds);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (server.getPlayerManager().getPlayerList().isEmpty()) {
				emptySince = server.getTicks();
				LOGGER.info("Server is empty, starting {} second shutdown timer.", seconds);
			} else {
				emptySince = -1;
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> tickCheck(server));
	}

	private static void tickCheck(MinecraftServer server) {
     	int playerCount = server.getPlayerManager().getPlayerList().size();

		if (playerCount == 0) {
			if (emptySince < 0) {
				emptySince = server.getTicks();
				LOGGER.info("Server is empty, starting shutdown timer.");
			}

			long elapsed = server.getTicks() - emptySince;
			if (elapsed >= timeoutTicks) {
				LOGGER.info("Server empty for {} seconds; shutting down.", (elapsed / 20));
				if (!(server.isStopped() || server.isStopping())) {
					server.execute(() -> {
						try {
							server.shutdown();
						} catch (Exception e) {
							LOGGER.error("Error shutting down server", e);
						}
					});
				}
			}
		} else {
			if (emptySince >= 0) {
				LOGGER.info("Player joined, shutdown timer reset.");
				emptySince = -1;
			}
		}
    	}
}