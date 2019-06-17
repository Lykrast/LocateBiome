package pl.locatebiome;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;

@SuppressWarnings("unused")
public class LocateBiome implements ModInitializer {
	static int timeout = 120_000;

	@Override
	public void onInitialize() {
		CommandRegistry.INSTANCE.register(false, LocateBiomeCommand::register);
	}
}
