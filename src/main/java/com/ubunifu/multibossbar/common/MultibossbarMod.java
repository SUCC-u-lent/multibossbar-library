package com.ubunifu.multibossbar.common;

import com.ubunifu.multibossbar.common.command.MultibossBarCommand;
import com.ubunifu.multibossbar.common.networking.ModNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultibossbarMod implements ModInitializer {
	public static final String MOD_ID = "multibossbar";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize(ModContainer mod) {
		ModNetworking.registerS2CPackets();
		registerCommands();
	}

	private static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			MultibossBarCommand.register(dispatcher,registryAccess);
		});
	}
}
