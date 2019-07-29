package pl.locatebiome;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

@Mod("locatebiome")
public class LocateBiome {
	static int timeout = 120_000;

	public LocateBiome() {
		MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
	}

	private void serverStarting(FMLServerStartingEvent event) {
		LocateBiomeCommand.register(event.getCommandDispatcher());
	}
}
