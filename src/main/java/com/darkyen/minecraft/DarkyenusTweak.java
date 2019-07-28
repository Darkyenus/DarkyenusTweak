
package com.darkyen.minecraft;

import com.darkyen.minecraft.util.Materials;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Statistic;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.darkyen.minecraft.util.Util.parseTimeMs;
import static com.darkyen.minecraft.util.Util.parseUUID;

/**
 *
 */
public final class DarkyenusTweak extends JavaPlugin {

	private HashMap<UUID, ChatColor> customChatColors = null;
	private File customChatColorsFile = null;

	@Override
	public void onEnable () {
		final FileConfiguration config = getConfig();
		final Server server = getServer();
		final Logger LOG = getLogger();
		saveDefaultConfig();

		if (config.getBoolean("saddle-recipe", false)) {
			try {
				ShapedRecipe saddleCraftRecipe = new ShapedRecipe(new NamespacedKey(this, "saddle"), new ItemStack(Material.SADDLE));
				saddleCraftRecipe.shape("lll", "lil", "i i");
				saddleCraftRecipe.setIngredient('l', Material.LEATHER);
				saddleCraftRecipe.setIngredient('i', Material.IRON_INGOT);
				if (!server.addRecipe(saddleCraftRecipe)) {
					LOG.warning("Saddle recipe could not be added");
				}
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Could not activate saddle crafting", e);
			}
		}

		if (config.getBoolean("record-recipe", false)) {
			try {
				addRecordRecipe(server, Material.MUSIC_DISC_13, Material.YELLOW_DYE);// 13
				addRecordRecipe(server, Material.MUSIC_DISC_CAT, Material.GREEN_DYE);// cat
				addRecordRecipe(server, Material.MUSIC_DISC_BLOCKS, Material.ORANGE_DYE);// blocks
				addRecordRecipe(server, Material.MUSIC_DISC_CHIRP, Material.RED_DYE);// chirp
				addRecordRecipe(server, Material.MUSIC_DISC_FAR, Material.LIME_DYE);// far
				addRecordRecipe(server, Material.MUSIC_DISC_MALL, Material.LIGHT_BLUE_DYE);// mall
				addRecordRecipe(server, Material.MUSIC_DISC_MELLOHI, Material.MAGENTA_DYE);// mellohi
				addRecordRecipe(server, Material.MUSIC_DISC_STAL, Material.BLACK_DYE);// stal
				addRecordRecipe(server, Material.MUSIC_DISC_STRAD, Material.WHITE_DYE);// strad
				addRecordRecipe(server, Material.MUSIC_DISC_WARD, Material.CYAN_DYE);// ward
				addRecordRecipe(server, Material.MUSIC_DISC_11, Material.GRAY_DYE);// 11
				addRecordRecipe(server, Material.MUSIC_DISC_WAIT, Material.BLUE_DYE);// wait
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Could not activate record crafting", e);
			}
		}

		if (config.getBoolean("horse-armor-recipe", false)) {
			try {
				addHorseArmorRecipe(server, Material.IRON_INGOT, Material.IRON_BLOCK, Material.IRON_HORSE_ARMOR);
				addHorseArmorRecipe(server, Material.GOLD_INGOT, Material.GOLD_BLOCK, Material.GOLDEN_HORSE_ARMOR);
				addHorseArmorRecipe(server, Material.DIAMOND, Material.DIAMOND_BLOCK, Material.DIAMOND_HORSE_ARMOR);
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Could not activate horse armor crafting", e);
			}
		}

		if (config.getBoolean("name-tag-recipe", false)) {
			try {
				ShapedRecipe nameTagCraftRecipe = new ShapedRecipe(new NamespacedKey(this, "name_tag"), new ItemStack(Material.NAME_TAG));
				nameTagCraftRecipe.shape("  s", " p ", "p  ");
				nameTagCraftRecipe.setIngredient('s', Material.STRING);
				nameTagCraftRecipe.setIngredient('p', Material.PAPER);
				if (!server.addRecipe(nameTagCraftRecipe)) {
					LOG.log(Level.WARNING, "Could not add name tag recipe");
				}
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Could not activate name tag crafting", e);
			}
		}

		if (config.getBoolean("block-drops", false)) {
			server.getPluginManager().registerEvents(new Listener() {

				@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
				public void onBlockBreak (BlockBreakEvent event) {
					if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
						return;// They are fine in creative!
					}

					final Block block = event.getBlock();
					final Material blockMaterial = block.getType();
					final Material toolType = event.getPlayer().getInventory().getItemInMainHand().getType();

					if (Materials.GLASS.contains(blockMaterial)
							|| Materials.GLASS_PANE.contains(blockMaterial)
							|| (blockMaterial == Material.BOOKSHELF && !Materials.AXES.contains(toolType))) {
						event.setDropItems(false);
						block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(blockMaterial));
					}
				}

			}, this);
		}

		if (config.getBoolean("bonemeal-grows-grass", false)) {
			server.getPluginManager().registerEvents(new Listener() {
				/**  Allow bonemeal to grow grass on dirt. */
				@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
				public void onBonemealUse (PlayerInteractEvent event) {
					if (event.getHand() != EquipmentSlot.HAND) return;

					final ItemStack bonemealItemStack = event.getItem();
					if (bonemealItemStack == null || bonemealItemStack.getType() != Material.BONE_MEAL) {
						return;
					}

					final Block clickedBlock = event.getClickedBlock();
					if (clickedBlock == null || clickedBlock.getType() != Material.DIRT) {
						return;
					}

					clickedBlock.setType(Material.GRASS_BLOCK);
					event.setUseInteractedBlock(Event.Result.DENY);
					event.setUseItemInHand(Event.Result.DENY);

					if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
						int finalAmount = bonemealItemStack.getAmount() - 1;
						if (finalAmount == 0) {
							event.getPlayer().getInventory().setItemInMainHand(null);
						} else {
							bonemealItemStack.setAmount(finalAmount);
							event.getPlayer().getInventory().setItemInMainHand(bonemealItemStack);
						}
					}
				}
			}, this);
		}

		if (config.getBoolean("limit-fire-spread", false)) {
			server.getPluginManager().registerEvents(new Listener() {
				/** Limits spread distance only to nearest blocks */
				@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
				public void onFireSpread (BlockSpreadEvent event) {
					final Block source = event.getSource();
					final Block fire = event.getBlock();
					if (Math.abs(source.getX() - fire.getX()) > 1 || Math.abs(source.getY() - fire.getY()) > 1
							|| Math.abs(source.getZ() - fire.getZ()) > 1) {
						event.setCancelled(true);
					}
				}
			}, this);
		}

		if (config.getBoolean("chat-format", false)) {
			Map<UUID, ChatColor> customChatColors = null;
			if (config.getBoolean("chat-format-custom-colors", false)) {
				customChatColors = this.customChatColors = new HashMap<>();
				customChatColorsFile = new File(getDataFolder(), "custom-chat-colors.yml");
				final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(customChatColorsFile);
				for (String playerUUID : configuration.getKeys(false)) {
					final UUID uuid = parseUUID(playerUUID);
					if (uuid == null) {
						continue;
					}
					final String string = configuration.getString(playerUUID);
					if (string == null) {
						continue;
					}
					final ChatColor chatColor;
					try {
						chatColor = ChatColor.valueOf(string.toUpperCase());
					} catch (IllegalArgumentException ex) {
						continue;
					}
					customChatColors.put(uuid, chatColor);
				}

				final PluginCommand command = getCommand("chatcolor");
				assert command != null;
				command.setExecutor((sender, command12, label, args) -> {
					if (!(sender instanceof OfflinePlayer)) {
						sender.sendMessage("In-game use only");
						return true;
					}
					if (args.length >= 1) {
						StringBuilder requestedSb = new StringBuilder(args[0]);
						for (int i = 1; i < args.length; i++) {
							requestedSb.append("_").append(args[i]);
						}
						final String requested = requestedSb.toString();

						for (ChatColor color : CHAT_COLORS) {
							if (color.name().equalsIgnoreCase(requested)) {
								this.customChatColors.put(((OfflinePlayer) sender).getUniqueId(), color);
								sender.sendMessage("Picked color: "+color+color.name().replace('_', ' ').toLowerCase());
								return true;
							}
						}
					}

					final StringBuilder sb = new StringBuilder();
					sb.append(ChatColor.RED).append("No such color. Try: ");
					for (ChatColor color : CHAT_COLORS) {
						sb.append(color).append(color.name().replace('_', ' ').toLowerCase()).append(", ");
					}
					sb.setLength(sb.length() - 2);
					sender.sendMessage(sb.toString());
					return true;
				});

				command.setTabCompleter((sender, command1, alias, args) -> {
					if (args.length <= 1) {
						return CHAT_COLORS_COMPLETE;
					} else {
						return Collections.emptyList();
					}
				});

			}
			enableChatTweaks(config.getBoolean("chat-format-color-shuffle", false), customChatColors);
		}

		if (config.getBoolean("limit-creeper-explosions", false)) {
			final double underBlastResistance = config.getDouble("limit-creeper-explosions-under-blast-resistance", 3);
			final HashSet<NamespacedKey> protectedBlocks = new HashSet<>();
			for (String s : config.getStringList("limit-creeper-explosions-protect-blocks")) {
				protectedBlocks.add(NamespacedKey.minecraft(s));
			}

			server.getPluginManager().registerEvents(new Listener() {
				/** Limit creeper explosions only to more fragile blocks */
				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
				public void onCreeperExplode (EntityExplodeEvent event) {
					if (event.getEntityType() != EntityType.CREEPER) {
						return;
					}

					event.blockList().removeIf(b ->
							b.getType().getBlastResistance() >= underBlastResistance
									|| protectedBlocks.contains(b.getType().getKey()));
				}

				@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
				public void onCreeperDestroyItem (EntityDamageByEntityEvent event) {
					if (event.getEntityType() != EntityType.DROPPED_ITEM || !(event.getDamager() instanceof Creeper)) {
						return;
					}

					event.setCancelled(true);
				}
			}, this);
		}

		if (config.getBoolean("sleep-tweaks", false)) {
			final int sleepingIgnoredUnderY = config.getInt("sleeping-ignored-under-y", 50);
			final long sleepingIgnoredAfterIdleMs = parseTimeMs(config.getString("sleeping-ignored-after-idle", "15m"), TimeUnit.MINUTES.toMillis(15), getLogger());
			final float sleepingPercentRequired = (float)config.getDouble("sleeping-percent-required", 50.0) / 100f;

			server.getPluginManager().registerEvents(new Listener() {

				class Idle {
					long lastActivity;
				}

				private final WeakHashMap<Player, Idle> lastActivity = new WeakHashMap<>();

				private boolean isIdle(Player player) {
					if (sleepingIgnoredAfterIdleMs >= Long.MAX_VALUE) {
						return false;
					}
					final Idle idle = lastActivity.get(player);
					if (idle == null) {
						// We don't know
						return false;
					}
					return idle.lastActivity + sleepingIgnoredAfterIdleMs < System.currentTimeMillis();
				}

				private boolean sleepingIgnored(GameMode mode) {
					switch (mode) {
						case CREATIVE:
						case SPECTATOR:
							return true;
						default:
							return false;
					}
				}

				@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
				public void idlePeopleDoNotSleep(PlayerMoveEvent event){
					final Player player = event.getPlayer();
					// Update idle timing
					if (sleepingIgnoredAfterIdleMs < Long.MAX_VALUE) {
						Idle idle = lastActivity.get(player);
						if (idle == null) {
							lastActivity.put(player, idle = new Idle());
						}
						idle.lastActivity = System.currentTimeMillis();
					}
				}

				private final Location ignoreWhenNotSleeping_loc = new Location(null, 0, 0, 0);
				private boolean ignoreWhenNotSleeping(Player p) {
					return isIdle(p) || sleepingIgnored(p.getGameMode()) || p.getLocation(ignoreWhenNotSleeping_loc).getY() < sleepingIgnoredUnderY;
				}

				private final ArrayList<Player> doImprovedWakeup_sleeping = new ArrayList<>();
				private void doImprovedWakeup(World world) {
					final ArrayList<Player> sleeping = this.doImprovedWakeup_sleeping;
					try {
						int totalSleepy = 0;
						int ignored = 0;

						final List<Player> players = world.getPlayers();
						for (Player p : players) {
							if (sleepingIgnored(p.getGameMode())) continue;

							totalSleepy++;
							if (p.isSleeping()) {
								sleeping.add(p);
							} else if (ignoreWhenNotSleeping(p)) {
								ignored++;
							}
						}

						if (sleeping.size() + ignored >= totalSleepy) {
							// Sleep will happen anyway
							return;
						}

						final int requiredSleepers = Math.round(totalSleepy * sleepingPercentRequired);
						final int fulfilledSleepers = sleeping.size() + ignored;

						if (fulfilledSleepers < requiredSleepers) {
							final int more = requiredSleepers - fulfilledSleepers;
							final TextComponent component = new TextComponent(ChatColor.DARK_AQUA + "Waiting for " + more + " more " + (more == 1 ? "person" : "people") + " to sleep");
							for (Player p : players) {
								if (sleepingIgnored(p.getGameMode()))
									continue;
								p.spigot().sendMessage(ChatMessageType.ACTION_BAR, component);
							}
							return;
						}

						// Skip!
						{// Message
							final TextComponent message = new TextComponent("Night has been skipped");
							message.setColor(ChatColor.BLUE.asBungee());
							for (Player p : players) {
								p.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
							}
						}
						// Wakeup
						for (Player player : sleeping) {
							// NOTE: This does (true, true, true) wakeup, while vanilla does (false, false, true)
							player.wakeup(true);
							player.setStatistic(Statistic.TIME_SINCE_REST, 0);
						}

						if (Boolean.TRUE.equals(world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE))) {
							world.setTime(0);
						}

						if (Boolean.TRUE.equals(world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE))) {
							world.setStorm(false);
							world.setThundering(false);
						}
					} finally {
						sleeping.clear();
					}
				}

				BukkitTask extraSleepCheck = null;

				@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
				public void notEverybodyHasToSleep(PlayerBedEnterEvent event) {
					final BukkitTask oldSleepCheck = this.extraSleepCheck;
					if (oldSleepCheck != null) {
						oldSleepCheck.cancel();
					}
					this.extraSleepCheck = getServer().getScheduler().runTaskLater(DarkyenusTweak.this,
							() -> doImprovedWakeup(event.getBed().getWorld()), 120 /* MC Sleep behavior will kick in in 100 ticks, we want to be a bit later than that */);
				}
			}, this);
		}

		if (config.getBoolean("beds-are-reliable-respawn-points", false)) {
			server.getPluginManager().registerEvents(new Listener() {

				@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
				public void bedsArePermanentSpawns(PlayerBedLeaveEvent event) {
					event.setSpawnLocation(false); // It would override it
					event.getPlayer().setBedSpawnLocation(event.getBed().getLocation(), true);
				}

			}, this);
		}
	}

	private void addRecordRecipe (Server server, Material record, Material dye) {
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "record_"+record), new ItemStack(record));
		recipe.shape(" c ", "cdc", " c ");
		recipe.setIngredient('c', Material.COAL_BLOCK);
		recipe.setIngredient('d', dye);

		if (!server.addRecipe(recipe)) {
			getLogger().log(Level.WARNING, "Could not add recipe for " + record);
		}
	}


	private void addHorseArmorRecipe (Server server, Material ingot, Material block, Material result) {
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "horse_armor_"+block), new ItemStack(result));
		recipe.shape("i  ", "bwb", "i i");
		recipe.setIngredient('w', new RecipeChoice.MaterialChoice(Tag.WOOL));
		recipe.setIngredient('b', block);
		recipe.setIngredient('i', ingot);

		if (!server.addRecipe(recipe)) {
			getLogger().log(Level.WARNING, "Could not add recipe for "+result);
		}
	}

	private static final List<ChatColor> CHAT_COLORS = Collections.unmodifiableList(Arrays.asList(ChatColor.AQUA, ChatColor.BLUE, ChatColor.DARK_AQUA,
			ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_PURPLE, ChatColor.DARK_RED, ChatColor.GOLD, ChatColor.GREEN,
			ChatColor.RED, ChatColor.YELLOW));
	private static final List<String> CHAT_COLORS_COMPLETE = CHAT_COLORS.stream().map(c -> c.name().toLowerCase()).collect(Collectors.toList());
	private void enableChatTweaks(boolean colorShuffle, @Nullable Map<UUID, ChatColor> customColors) {
		final ArrayList<ChatColor> hashedColors = new ArrayList<>(CHAT_COLORS);

		if (colorShuffle) {
			Collections.shuffle(hashedColors);
		}

		getServer().getPluginManager().registerEvents(new Listener() {
			@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
			// Highest because this changes behavior of chat event
			// and we want it called only after making sure that nobody wants it dead
			public void chatFormatter (AsyncPlayerChatEvent event) {
				final Player player = event.getPlayer();
				final UUID playerUniqueId = player.getUniqueId();

				ChatColor playerColor = null;
				if (customColors != null) {
					playerColor = customColors.get(playerUniqueId);
				}
				if (playerColor == null) {
					playerColor = hashedColors.get(((int)playerUniqueId.getLeastSignificantBits() & 0x7FFF_FFFF) % hashedColors.size());
				}

				event.setFormat(playerColor + "<" + ChatColor.WHITE + ChatColor.BOLD + "%s" + ChatColor.RESET + playerColor + "> "
						+ ChatColor.RESET + "%s");
			}
		}, this);
	}


	@Override
	public void onDisable () {
		if (customChatColors != null && customChatColorsFile != null) {
			final YamlConfiguration config = new YamlConfiguration();
			for (Map.Entry<UUID, ChatColor> entry : customChatColors.entrySet()) {
				config.set(entry.getKey().toString(), entry.getValue().name());
			}
			try {
				config.save(customChatColorsFile);
			} catch (IOException e) {
				getLogger().log(Level.SEVERE, "Failed to save custom chat colors", e);
			}
		}
		customChatColors = null;
		customChatColorsFile = null;
	}
}
