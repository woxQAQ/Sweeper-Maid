package com.woxQAQ.sweeper_maid;

import com.google.common.collect.Lists;
import com.woxQAQ.sweeper_maid.command.SMCommands;
import com.woxQAQ.sweeper_maid.config.SMCommonConfig;
import com.woxQAQ.sweeper_maid.log.CountDown;
import com.woxQAQ.sweeper_maid.save.SMSavedData;
import com.woxQAQ.sweeper_maid.utils.EntityUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Mod(SweeperMaid.MODID)
public class SweeperMaid {
	public static final String MODID = "sweeper_maid";
	public static final String MODNAME = "Sweeper Maid";
	public static final String VERSION = ModList.get().getModFileById(MODID).versionString();

	public SweeperMaid() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SMCommonConfig.getConfig());
		MinecraftForge.EVENT_BUS.register(this);
	}

	private int sweepTickRemain = 0;
	private boolean toSweep = false;
	private boolean firstTick = true;

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(SMCommands.register());
	}

	private void onStart() {
		this.toSweep = true;
		this.sweepTickRemain = SMCommonConfig.getItemSweepTickRemain();
	}

	private void broadcastMessage(MinecraftServer server, String message) {

		server.getPlayerList().getPlayers().forEach(player -> {
			try {
				player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
						createCommandSourceStack(player, player.level(), player.blockPosition()),
						Component
								.literal(message)
								.withStyle(ChatFormatting.GRAY),
						player, 0)));
			} catch (CommandSyntaxException ignored) {
			}
		});
	}

	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event) {
		if (SMCommonConfig.ITEM_SWEEP_INTERVAL.get() == 0) {
			return;
		}
		switch (event.phase) {
			case START -> {
				this.sweepTickRemain -= 1;
				if (this.sweepTickRemain <= 0) {
					this.onStart();
					return;
				}
				CountDown countDown = new CountDown(this.sweepTickRemain);
				if (countDown.getMessage().isEmpty()) {
					return;
				}
				broadcastMessage(event.getServer(), countDown.getMessage());
			}
			case END -> {
				if (this.firstTick) {
					this.firstTick = false;
					this.toSweep = false;
					return;
				}
				if (this.toSweep) {
					this.toSweep = false;
					SimpleContainer dustbin = SMSavedData.getDustbin();
					SimpleContainer oldBin = new SimpleContainer(dustbin.getContainerSize());
					for (int i = 0; i < dustbin.getContainerSize(); ++i) {
						oldBin.setItem(i, dustbin.getItem(i));
						dustbin.setItem(i, ItemStack.EMPTY);
					}
					AtomicInteger droppedItems = new AtomicInteger();
					AtomicInteger extraEntities = new AtomicInteger();
					event.getServer().getAllLevels().forEach(serverLevel -> {
						Iterable<Entity> entities = serverLevel.getAllEntities();
						List<Entity> killedEntities = Lists.newArrayList();
						for (Entity entity : entities) {
							if (entity instanceof ItemEntity itemEntity && !EntityUtils.inBlacklist(itemEntity)) {
								dustbin.addItem(itemEntity.getItem());
								droppedItems.addAndGet(1);
								killedEntities.add(itemEntity);
							} else if (entity != null) {
								ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
								if (typeKey != null) {
									String type = typeKey.toString();
									if (SMCommonConfig.EXTRA_ENTITY_TYPES.get().contains(type)) {
										extraEntities.addAndGet(1);
										killedEntities.add(entity);
									}
								}
							}
						}
						killedEntities.forEach(Entity::kill);
					});
					for (int i = 0; i < oldBin.getContainerSize(); ++i) {
						dustbin.addItem(oldBin.getItem(i));
					}
					broadcastMessage(event.getServer(), SMCommonConfig.getMessageAfterSweep(droppedItems.get(),
							extraEntities.get()));
					event.getServer().getPlayerList().broadcastSystemMessage(
							Component.literal(SMCommonConfig.CHAT_MESSAGE_AFTER_SWEEP.get())
									.append(Component.literal("/sweepermaid dustbin")
											.withStyle(style -> style.withColor(ChatFormatting.GREEN)
													.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
															"/sweepermaid dustbin")))),
							false);
					dustbin.setChanged();
				}
			}
		}
	}

	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) {
		ServerLevel world = event.getServer().getLevel(Level.OVERWORLD);
		assert world != null;
		if (!world.isClientSide) {
			SMSavedData worldData = world.getDataStorage().computeIfAbsent(SMSavedData::new, SMSavedData::new,
					SMSavedData.SAVED_DATA_NAME);
			SMSavedData.setInstance(worldData);
		}
	}

	private static CommandSourceStack createCommandSourceStack(Player player, Level level, BlockPos blockPos) {
		return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(blockPos), Vec2.ZERO, (ServerLevel) level, 2,
				player.getName().getString(), player.getDisplayName(), level.getServer(), player);
	}
}
