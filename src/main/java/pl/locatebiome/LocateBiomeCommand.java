package pl.locatebiome;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.chat.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Objects;

class LocateBiomeCommand {
	static void register(CommandDispatcher<ServerCommandSource> commandDispatcher_1) {
		LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("locatebiome").requires((serverCommandSource_1) ->
				serverCommandSource_1.hasPermissionLevel(2));
		Registry.BIOME.stream().forEach(biome -> builder.then(CommandManager.literal(Objects.requireNonNull(Registry.BIOME.getId(biome)).toString())
				.executes(context -> execute(context.getSource(), biome))));
		commandDispatcher_1.register(builder);
	}

	private static int execute(ServerCommandSource source, Biome biome) {
		new Thread(() -> {
			BlockPos executorPos = new BlockPos(source.getPosition());
			BlockPos biomePos = null;
			TranslatableComponent biomeName = new TranslatableComponent(biome.getTranslationKey());
			try {
				biomePos = spiralOutwardsLookingForBiome(source, source.getWorld(), biome, executorPos.getX(), executorPos.getZ());
			} catch (CommandSyntaxException e) {
				e.printStackTrace();
			}

			if (biomePos == null) {
				if (source.getEntity() instanceof PlayerEntity) {
					try {
						source.getPlayer().sendChatMessage(new TranslatableComponent("commands.locatebiome.fail", biomeName, LocateBiome.timeout / 1000),
								ChatMessageType.GAME_INFO);
					} catch (CommandSyntaxException e) {
						e.printStackTrace();
					}
				}
				return;
			}
			BlockPos finalBiomePos = biomePos;
			source.getMinecraftServer().execute(() -> {
				int distance = MathHelper.floor(getDistance(executorPos.getX(), executorPos.getZ(), finalBiomePos.getX(), finalBiomePos.getZ()));
				Component coordinates = Components.bracketed(new TranslatableComponent("chat.coordinates", finalBiomePos.getX(), "~",
						finalBiomePos.getZ())).setStyle(new Style().setColor(ChatFormat.GREEN)
								.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + finalBiomePos.getX() + " ~ " + finalBiomePos.getZ()))
								.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("chat.coordinates.tooltip"))));
				source.sendFeedback(new TranslatableComponent("commands.locate.success", biomeName, coordinates, distance), true);
			});
		}).start();
		return 0;
	}

	private static BlockPos spiralOutwardsLookingForBiome(ServerCommandSource source, World world, Biome biomeToFind, double startX, double startZ) throws CommandSyntaxException {
		double a = 16 / Math.sqrt(Math.PI);
		double b = 2 * Math.sqrt(Math.PI);
		double x, z;
		double dist = 0;
		long start = System.currentTimeMillis();
		BlockPos.PooledMutable pos = BlockPos.PooledMutable.get();
		int previous = 0;
		int i = 0;
		for (int n = 0; dist < Integer.MAX_VALUE; ++n) {
			if ((System.currentTimeMillis() - start) > LocateBiome.timeout)
				return null;
			double rootN = Math.sqrt(n);
			dist = a * rootN;
			x = startX + (dist * Math.sin(b * rootN));
			z = startZ + (dist * Math.cos(b * rootN));
			pos.set(x, 0, z);
			if (previous == 3)
				previous = 0;
			String dots = (previous == 0 ? "." : previous == 1 ? ".." : "...");
			if (source.getEntity() instanceof PlayerEntity)
				source.getPlayer().sendChatMessage(new TranslatableComponent("commands.locatebiome.scanning", dots), ChatMessageType.GAME_INFO);
			if (i == 9216) {
				previous++;
				i = 0;
			}
			i++;
			if (world.getBiome(pos).equals(biomeToFind)) {
				pos.close();
				if (source.getEntity() instanceof PlayerEntity)
					source.getPlayer().sendChatMessage(new TranslatableComponent("commands.locatebiome.found", new TranslatableComponent(biomeToFind.getTranslationKey()), (System.currentTimeMillis() - start) / 1000), ChatMessageType.GAME_INFO);
				return new BlockPos((int) x, 0, (int) z);
			}
		}
		return null;
	}

	private static double getDistance(int posX, int posZ, int biomeX, int biomeZ) {
		return MathHelper.sqrt(Math.pow(biomeX - posX, 2) + Math.pow(biomeZ - posZ, 2));
	}
}
