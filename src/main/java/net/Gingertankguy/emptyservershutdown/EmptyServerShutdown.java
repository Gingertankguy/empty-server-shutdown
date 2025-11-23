package net.Gingertankguy.emptyservershutdown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

		timeoutTicks = ConfigManager.config.shutdownTimeoutSeconds * 20L;
		LOGGER.info("Shutdown timeout set to {} seconds.", ConfigManager.config.shutdownTimeoutSeconds);

		ServerPlayConnectionEvents.JOIN.register((handler, server, sender) -> {
			emptySince = -1;
          	LOGGER.info("Player joined — shutdown timer reset.");
        	});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
          	if (server.getPlayerManager().getPlayerList().isEmpty()) {
                	emptySince = server.getTicks();
                	LOGGER.info("Server is empty — starting shutdown timer.");
          	}
        	});

		ServerTickEvents.END_SERVER_TICK.register(server -> tickCheck(server));
	}

	private static void tickCheck(MinecraftServer server) {
     	if (emptySince < 0) return;

     	long elapsed = server.getTicks() - emptySince;
     	if (elapsed >= timeoutTicks) {
          	LOGGER.info("Server empty for {} ticks — shutting down.", elapsed);
     		server.shutdown();
     	}
    	}
}