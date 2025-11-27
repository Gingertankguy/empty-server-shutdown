package net.Gingertankguy.emptyservershutdown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

public class EmptyServerShutdown implements ModInitializer {
	public static final String MOD_ID = "empty-server-shutdown";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static long timeoutMS = 1000 * 600;
	private static long emptySince = -1;

	@Override
	public void onInitialize() {
		emptySince = -1;
		ConfigManager.load();
		LOGGER.info("[EmptyServerShutdown] Empty Server Stopper Activated!");
		
		int seconds = ConfigManager.config.shutdownTimeoutSeconds;

		timeoutMS = seconds * 1000L;
		LOGGER.info("[EmptyServerShutdown] Shutdown timeout set to {} seconds.", seconds);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			startBackgroundChecker(server);
			if (server.getPlayerManager().getPlayerList().isEmpty()) {
				emptySince = System.currentTimeMillis();
				LOGGER.info("[EmptyServerShutdown] Server is empty; starting shutdown timer.");
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, server, sender) -> {
			emptySince = -1;
			LOGGER.info("[EmptyServerShutdown] Player joined, shutdown timer reset.");
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (server.getPlayerManager().getPlayerList().isEmpty()) {
				emptySince = System.currentTimeMillis();
				LOGGER.info("[EmptyServerShutdown] Server is empty; starting shutdown timer.");
			}
		});
	}

	private void startBackgroundChecker(MinecraftServer server) {
		Thread watcher = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(5000);

					if (emptySince < 0)
						continue;

					long elapsed = System.currentTimeMillis() - emptySince;
					if (elapsed >= timeoutMS) {
						LOGGER.info("[EmptyServerShutdown] Server empty for {} seconds; shutting down.", (elapsed / 1000));
						server.execute(() -> {
							try {
								server.stop(false);
							} catch (Exception e) {
								EmptyServerShutdown.LOGGER.error("Error during shutdown", e);
							}
						});
						break;
					}
				} catch (Exception e) {
					LOGGER.error("[EmptyServerShutdown] Shutdown checker error", e);
				}
			}
		}, "EmptyShutdownWatcher");

		watcher.setDaemon(true);
		watcher.start();
	}
}