package pl.locatebiome;

import java.util.Objects;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

class LocateBiomeCommand {
	static void register(CommandDispatcher<CommandSource> commandDispatcher_1) {
		LiteralArgumentBuilder<CommandSource> builder = Commands.literal("locatebiome").requires((CommandSource_1) ->
				CommandSource_1.hasPermissionLevel(2));
		ForgeRegistries.BIOMES.getValues().stream().forEach(biome -> builder.then(Commands.literal(Objects.requireNonNull(biome.getRegistryName()).toString())
				.executes(context -> execute(context.getSource(), biome))));
		commandDispatcher_1.register(builder);
	}

	private static int execute(CommandSource source, Biome biome) {
		new Thread(() -> {
			BlockPos executorPos = new BlockPos(source.getPos());
			BlockPos biomePos = null;
			TranslationTextComponent biomeName = new TranslationTextComponent(biome.getTranslationKey());
			try {
				biomePos = spiralOutwardsLookingForBiome(source, source.getWorld(), biome, executorPos.getX(), executorPos.getZ());
			} catch (CommandSyntaxException e) {
				e.printStackTrace();
			}

			if (biomePos == null) {
				source.sendFeedback(new TranslationTextComponent(source.getServer() instanceof DedicatedServer ? "optimizeWorld.stage.failed" : "commands.locatebiome.fail",
						biomeName, LocateBiome.timeout / 1000).applyTextStyles(TextFormatting.RED), true);
				return;
			}
			BlockPos finalBiomePos = biomePos;
			source.getServer().execute(() -> {
				int distance = MathHelper.floor(getDistance(executorPos.getX(), executorPos.getZ(), finalBiomePos.getX(), finalBiomePos.getZ()));
				ITextComponent coordinates = TextComponentUtils.wrapInSquareBrackets(new TranslationTextComponent("chat.coordinates", finalBiomePos.getX(), "~",
						finalBiomePos.getZ())).setStyle(new Style().setColor(TextFormatting.GREEN)
								.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + finalBiomePos.getX() + " ~ " + finalBiomePos.getZ()))
								.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("chat.coordinates.tooltip"))));
				source.sendFeedback(new TranslationTextComponent("commands.locate.success", biomeName, coordinates, distance), true);
			});
		}).start();
		return 0;
	}

	private static BlockPos spiralOutwardsLookingForBiome(CommandSource source, World world, Biome biomeToFind, double startX, double startZ) throws CommandSyntaxException {
		double a = 16 / Math.sqrt(Math.PI);
		double b = 2 * Math.sqrt(Math.PI);
		double x, z;
		double dist = 0;
		long start = System.currentTimeMillis();
		BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
		int previous = 0;
		int i = 0;
		for (int n = 0; dist < Integer.MAX_VALUE; ++n) {
			if ((System.currentTimeMillis() - start) > LocateBiome.timeout)
				return null;
			double rootN = Math.sqrt(n);
			dist = a * rootN;
			x = startX + (dist * Math.sin(b * rootN));
			z = startZ + (dist * Math.cos(b * rootN));
			pos.setPos(x, 0, z);
			if (previous == 3)
				previous = 0;
			String dots = (previous == 0 ? "." : previous == 1 ? ".." : "...");
			if (source.getEntity() instanceof PlayerEntity && !(source.getServer() instanceof DedicatedServer))
				source.asPlayer().sendMessage(new TranslationTextComponent("commands.locatebiome.scanning", dots), ChatType.GAME_INFO);
			if (i == 9216) {
				previous++;
				i = 0;
			}
			i++;
			if (world.getBiome(pos).equals(biomeToFind)) {
				pos.close();
				if (source.getEntity() instanceof PlayerEntity && !(source.getServer() instanceof DedicatedServer))
					source.asPlayer().sendMessage(new TranslationTextComponent("commands.locatebiome.found", new TranslationTextComponent(biomeToFind.getTranslationKey()), (System.currentTimeMillis() - start) / 1000), ChatType.GAME_INFO);
				return new BlockPos((int) x, 0, (int) z);
			}
		}
		return null;
	}

	private static double getDistance(int posX, int posZ, int biomeX, int biomeZ) {
		return MathHelper.sqrt(Math.pow(biomeX - posX, 2) + Math.pow(biomeZ - posZ, 2));
	}
}
