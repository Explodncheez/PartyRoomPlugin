package partyroom.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import partyroom.PartyChest;
import partyroom.PartyChest.RegionTarget;
import partyroom.PartyChest.YSpawnTarget;
import partyroom.PartyRoom;
import partyroom.Utilities;
import partyroom.versions.AnvilGUI;
import partyroom.versions.SoundHandler.Sounds;

public class ChestEditor {
	
	private static Map<String, ChestEditor> Editors = new HashMap<String, ChestEditor>();
	private static Map<Player, ChestEditor> PlayerEditors = new WeakHashMap<Player, ChestEditor>();
	
	public static void addPlayerEditor(Player p, ChestEditor ce) {
		PlayerEditors.put(p, ce);
	}
	
	public static void removeEditor(String stringloc) {
		Editors.remove(stringloc);
	}
	
	public static ChestEditor get(PartyChest chest) {
		String stringloc = chest.getChestString();
		return Editors.get(stringloc) != null ? Editors.get(stringloc) : new ChestEditor(chest);
	}
	
	public static void removeAllEditors() {
		for (Player p : PlayerEditors.keySet()) {
			p.closeInventory();
		}
		for (ChestEditor c : PlayerEditors.values()) {
			c.thingy.clear();
		}
		PlayerEditors.clear();
		Editors.clear();
	}
	
	public static void removeEditor(Player p) {
		if (PlayerEditors.containsKey(p))
			PlayerEditors.remove(p).thingy.remove(p);
	}
	
	public static ChestEditor get(Player p) {
		return PlayerEditors.get(p);
	}
	
	public enum AnvilEditor {
		MATERIAL,
		COUNT,
		COST,
		REGION,
		RADIUS,
		DROP_DELAY,
		DROP_COOLDOWN,
		DROP_CAPACITY,
		ANNOUNCE_DELAY,
		START_MESSAGE,
		DELAY_MESSAGE
	}
	
	public ChestEditor(PartyChest chest) {
		this.chest = chest;
		thingy = new WeakHashMap<Player, AnvilEditor>();
		Editors.put(chest.getChestString(), this);
		editorInterface = Bukkit.createInventory(null, 18, "§5Party Chest Editor");
		update();
	}
	
	private PartyChest chest;
	private Inventory editorInterface;
	private Map<Player, AnvilEditor> thingy;
	
	private void update() {
		editorInterface.setItem(1, Utilities.ConstructItemStack(Material.CAKE, 1, 0, "§a§lBalloon Block Type", "§e<< §f" + chest.getBlockMaterial().toString().toLowerCase() + " §e>>", "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(2, Utilities.ConstructItemStack(Material.MELON_SEEDS, 1, 0, "§b§lBalloon Count", "§e<< §f" + chest.getCount() + " §e>>", "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(3, Utilities.ConstructItemStack(Material.FEATHER, 1, 0, "§c§lBalloon Height", "§e<< §f" + chest.getYTarget().toString() + " Region Y-coord §e>>", chest.getYTarget().getDescription(), "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(4, Utilities.ConstructItemStack(Material.LEVER, 1, 0, "§6§lLever-Pull Cost", "§e<< §f$" + chest.getCost() + " §e>>", "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(5, Utilities.ConstructItemStack(Material.FLINT, 1, 0, "§c§lRegion Type", "§e<< §f" + chest.getRegionTarget().toString().toLowerCase() + " §e>>", chest.getRegionTarget() == RegionTarget.RADIUS ? "§eRadius: " + chest.getRadius() : "§eRegion: " + chest.getRegion(), "", "§f§l[ Click to Change ]"));
		
		editorInterface.setItem(10, Utilities.ConstructItemStack(Material.WATCH, 1, 0, "§c§lDrop Party Delay", "§e<< §f" + chest.getDropDelay() + " second(s) §e>>", "§7Drop Party starts after this delay", "§7when the lever is pulled.", "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(11, Utilities.ConstructItemStack(Material.CHEST, 1, 0, "§6§lMinimum Drop Capacity", "§e<< §f" + chest.getMinSlots() + " items §e>>", "§7Drop Party cannot be started unless", "§7the chest has this many slots filled.", "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(12, Utilities.ConstructItemStack(Material.SIGN, 1, 0, "§a§lAnnouncement Delay", "§e<< §f" + (chest.getAnnounceInterval() > 0 ? chest.getAnnounceInterval() : "0 §c(disabled)") + " §e>>", "§7Repeatively announces the time remaining", "§7until Drop Party after this delay.", "§7Does nothing if §cDrop Party Delay§7 is 0.", "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(13, Utilities.ConstructItemStack(Material.PAPER, 1, 0, "§b§lDelay Interval Message", "§7Use §f%TIME% §7to insert time left.", "§7Use §f& §7for Color Codes.", "§cNote: §fthe GUI editor has a character cap.", "§fFor best results, edit this directly in the config.", "", "§f§l[ Click to Edit ]"));
		editorInterface.setItem(14, Utilities.ConstructItemStack(Material.EMPTY_MAP, 1, 0, "§b§lDrop Start Message", "§7Use §f& §7for Color Codes.", "§cNote: §fthe GUI editor has a character cap.", "§fFor best results, edit this directly in the config.", "", "§f§l[ Click to Edit ]"));
		editorInterface.setItem(16, Utilities.ConstructItemStack(Material.ICE, 1, 0, "§b§lDrop Party Cooldown", "§e<< §f" + chest.getDropCooldown() + " second(s) §e>>", "§7Starts counting down when lever is pulled.", "", "", "§f§l[ Click to Change ]"));
		//editorInterface.setItem(16, Utilities.ConstructItemStack(Material.BOOK_AND_QUILL, 1, 0, "§8§lDeposit Blacklist", "§7These items cannot be deposited.", "", "", "§f§l[ Click to Edit ]"));
		
		editorInterface.setItem(7, Utilities.ConstructItemStack(chest.isEnabled() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, 1, 0, "§d§lEnabled?", "§e<< §f" + (chest.isEnabled() ? "§aEnabled!" : "§cDisabled.") + " §e>>", "", "§f§l[ Click to Toggle ]"));
	}
	
	public void openInventory(Player p) {
		update();
		p.openInventory(editorInterface);
		addPlayerEditor(p, this);
	}
	
	public void handle(InventoryClickEvent e) {
		Inventory top = e.getView().getTopInventory();
		Player p = (Player) e.getWhoClicked();
		
		if (e.getCurrentItem() == null || e.getInventory() != e.getView().getTopInventory())
			return;
		
		switch (top.getName()) {
		case "§5Party Chest Editor":
			e.setCancelled(true);
			switch (e.getCurrentItem().getType()) {
			case CAKE: // material
				openAnvil(p, "Enter block type");
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.MATERIAL);
				break;
			case MELON_SEEDS: // count
				openAnvil(p, "Enter balloon count");
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.COUNT);
				break;
			case FEATHER: // height
				openHeightInterface(p);
				break;
			case LEVER: // cost
				openAnvil(p, "Enter lever-pull cost");
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.COST);
				break;
			case FLINT:
				openRegionInterface(p);
				break;
			case WATCH: // dp delay
				openAnvil(p, "Enter Drop Delay");
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.DROP_DELAY);
				break;
			case CHEST: // min drop capacity
				openAnvil(p, "Enter Minimum Slots");
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.DROP_CAPACITY);
				break;
			case SIGN: // announce delay
				openAnvil(p, "Enter Announce Interval");
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.ANNOUNCE_DELAY);
				break;
			case PAPER: //delay message
				openAnvil(p, "Enter Announcement Message");
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.DELAY_MESSAGE);
				break;
			case EMPTY_MAP: // dp start message
				openAnvil(p, "Enter Party-Start Message");
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.START_MESSAGE);
				break;
			case ICE: // cooldown
				openAnvil(p, "Enter Cooldown");
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.DROP_COOLDOWN);
				break;
			/*case BOOK_AND_QUILL: // blacklist
				openBlacklistInterface(p);
				break;*/
			case EMERALD_BLOCK:
			case REDSTONE_BLOCK:
				chest.setEnabled(!chest.isEnabled());
				editorInterface.setItem(7, Utilities.ConstructItemStack(chest.isEnabled() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, 1, 0, "§d§lEnabled?", "§e<< §f" + (chest.isEnabled() ? "§aEnabled!" : "§cDisabled.") + " §e>>", "", "§f§l[ Click to Toggle ]"));
				break;
				default:
					break;
			}
			break;
		case "§5Region Type":
			e.setCancelled(true);
			boolean right = e.getClick() == ClickType.RIGHT;
			switch (e.getCurrentItem().getType()) {
			case COMPASS:  // radius
				if (right) {
					openAnvil(p, "Enter radius");
					addPlayerEditor(p, this);
					thingy.put(p, AnvilEditor.RADIUS);
				} else {
					chest.setRegionTarget(RegionTarget.RADIUS);
					ench(e.getCurrentItem());
					top.getItem(1).removeEnchantment(Enchantment.ARROW_DAMAGE);
					p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.6F, 1.4F);
					p.sendMessage(PartyRoom.PREFIX + "§fBalloons will now drop in a §e" + chest.getRadius() + "-block Radius§f!");
				}
				break;
			case STAINED_GLASS_PANE: // region
				if (right) {
					openAnvil(p, "Enter region name");
					addPlayerEditor(p, this);
					thingy.put(p, AnvilEditor.REGION);
				} else {
					if (chest.getRegion() == null) {
						p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.4F);
						p.sendMessage(PartyRoom.PREFIX + "§fPlease specify a Region first by Right Clicking the icon.");
						return;
					}
					chest.setRegionTarget(RegionTarget.REGION);
					ench(e.getCurrentItem());
					top.getItem(0).removeEnchantment(Enchantment.ARROW_DAMAGE);
					p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.6F, 1.4F);
					p.sendMessage(PartyRoom.PREFIX + "§fBalloons will now drop in the §e" + chest.getRegion() + " Region§f!");
				}
				break;
			case BED:
				openInventory(p);
				break;
				default:
					break;
			}
			break;
		case "§5Drop Height":
			e.setCancelled(true);
			
			switch (e.getCurrentItem().getType()) {
			case MAGMA_CREAM:
			case ENDER_PEARL:
			case SLIME_BALL:
			case EYE_OF_ENDER:
				top.getItem(chest.getYTarget().i()).removeEnchantment(Enchantment.ARROW_DAMAGE);
				
				YSpawnTarget selected = YSpawnTarget.get(e.getRawSlot());
				chest.setYTarget(selected);
				ench(e.getCurrentItem());
				p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.6F, 1.4F);
				p.sendMessage(PartyRoom.PREFIX + "§fBalloons will now drop at §e" + selected.toString() + " Height Levels§f!");
				break;
			case BED:
				openInventory(p);
				break;
				default:
					break;
			}
			break;
		}
	}
	
	private void openRegionInterface(Player p) {
		Inventory inv = Bukkit.createInventory(null, 9, "§5Region Type");
		ItemStack compass = Utilities.ConstructItemStack(Material.COMPASS, 1, 0, "§a§lRadius", "§e<< §f" + chest.getRadius() + " blocks §e>>", "", "§f§l[ Left Click to Select Mode ]", "§f§l[ Right Click to Specify Radius ]"),
				pane =  Utilities.ConstructItemStack(Material.STAINED_GLASS_PANE, 1, 4, "§a§lWorldGuard Region", "§e<< §f" + (chest.getRegion() == null || chest.getRegion() == "" ? "§oNone Set" : chest.getRegion())  + " §e>>", "", "§f§l[ Left Click to Select Mode ]", "§f§l[ Right Click to Specify Region ]");

		ench(chest.getRegionTarget() == RegionTarget.REGION ? pane : compass);
		
		inv.setItem(0, compass);
		inv.setItem(1, pane);
		inv.setItem(8, Utilities.ConstructItemStack(Material.BED, 1, 0, "§c§lBack"));
		
		p.openInventory(inv);
		addPlayerEditor(p, this);
	}
	
	private void openHeightInterface(Player p) {
		Inventory inv = Bukkit.createInventory(null, 9, "§5Drop Height");

		for (YSpawnTarget y : YSpawnTarget.values()) {
			inv.setItem(y.i(), y.getIcon(chest));
		}
		
		inv.setItem(8, Utilities.ConstructItemStack(Material.BED, 1, 0, "§c§lBack"));
		
		p.openInventory(inv);
		addPlayerEditor(p, this);
	}
	
	private void openAnvil(final Player p, String name) {
		AnvilGUI gui = AnvilGUI.get(p, new AnvilGUI.AnvilClickEventHandler() {
			@Override
			public void onAnvilClick(AnvilGUI.AnvilClickEvent e){
				if(e.getSlot() == AnvilGUI.AnvilSlot.OUTPUT){
					e.setWillClose(true);
					e.setWillDestroy(true);
					
					switch (thingy.get(p)) {
					case MATERIAL:
						if (e.getName() != null) {
							try {
								String[] s = e.getName().split(":");
								chest.setMaterial(Material.valueOf(s[0].toUpperCase().replace(" ", "_")), s.length > 1 ? Integer.parseInt(s[1]) : 0);
								p.sendMessage(PartyRoom.PREFIX + "§eParty Balloon Block §fset to: §e" + s[0] + "§f!");
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid block type specified! §f(BlockType:Data)");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case COST:
						if (e.getName() != null) {
							try {
								double dbt = Double.parseDouble(e.getName());
								
								if (dbt < 0) {
									p.sendMessage(PartyRoom.PREFIX + "§cWARNING: §fcost per pull was less than 0, so it is 0 now!");
									dbt = 0;
								}

								chest.setCost(dbt);
								
								p.sendMessage(PartyRoom.PREFIX + "§fStarting a Drop Party now costs: §e$" + dbt + "§f!");
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid lever-pull cost specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case COUNT:
						if (e.getName() != null) {
							try {
								int amt = Integer.parseInt(e.getName());
								
								if (amt < 1) {
									p.sendMessage(PartyRoom.PREFIX + "§cWhy would you want less than one balloon per party? That's just sad.");
									p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
									return;
								}
								if (amt > 100) {
									p.sendMessage(PartyRoom.PREFIX + "§cWARNING: §fYou've specified a large number of balloons. This may cause issues!");
								}
								
								chest.setCount(amt);
								p.sendMessage(PartyRoom.PREFIX + "§e" + amt + "§f balloons will now drop with each party!");
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid balloon count specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case REGION:
						if (e.getName() != null) {
							try {
								if (!chest.setWGRegion(e.getName())) {
									p.sendMessage(PartyRoom.PREFIX + "§cThe specified region: §e" + e.getName() + " §cdoes not exist in your current world.");
									p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
									return;
								}
								p.sendMessage(PartyRoom.PREFIX + "§eParty Room Region §fset to: §e" + e.getName() + "§f!");
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cThe specified region: §e" + e.getName() + " §cdoes not exist in your current world.");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case RADIUS:
						if (e.getName() != null) {
							try {
								chest.setRadius(Integer.parseInt(e.getName()));
								p.sendMessage(PartyRoom.PREFIX + "§eParty Room Radius §fset to: §e" + e.getName() + " blocks§f!");
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid balloon drop radius specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case ANNOUNCE_DELAY:
						if (e.getName() != null) {
							try {
								chest.setAnnounceInterval(Integer.parseInt(e.getName()));
								p.sendMessage(PartyRoom.PREFIX + "§eParty Room Announcement Interval §fset to: §e" + e.getName() + " seconds§f!");
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid Announcement Interval specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case DROP_COOLDOWN:
						if (e.getName() != null) {
							try {
								chest.setDropCooldown(Integer.parseInt(e.getName()));
								p.sendMessage(PartyRoom.PREFIX + "§eParty Room Drop Party Cooldown §fset to: §e" + e.getName() + " seconds§f!");
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid Drop Party Cooldown specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case DELAY_MESSAGE:
						if (e.getName() != null) {
							try {
								chest.setAnnounceMessage(e.getName());
								p.sendMessage(PartyRoom.PREFIX + "§eParty Room announcement message set to:");
								p.sendMessage(PartyRoom.PREFIX + chest.getAnnounceMessage());
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid string specified!");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case DROP_CAPACITY:
						if (e.getName() != null) {
							try {
								chest.setMinSlots(Integer.parseInt(e.getName()));
								p.sendMessage(PartyRoom.PREFIX + "§eMinimum Slots required to start Drop Party §fset to: §e" + e.getName() + "§f!");
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid value specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case DROP_DELAY:
						if (e.getName() != null) {
							try {
								chest.setDropDelay(Integer.parseInt(e.getName()));
								p.sendMessage(PartyRoom.PREFIX + "§eParty Room Drop Delay §fset to: §e" + e.getName() + " seconds§f!");
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid Drop Delay specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					case START_MESSAGE:
						if (e.getName() != null) {
							try {
								chest.setStartMessage(e.getName());
								p.sendMessage(PartyRoom.PREFIX + "§eParty Room start message set to:");
								p.sendMessage(PartyRoom.PREFIX + chest.getStartMessage());
								p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid string specified!");
								p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
							}
						}
						break;
					default:
						break;
					}
				 
				} else {
					e.setWillClose(false);
					e.setWillDestroy(false);
				}
			}
		});
			 
		gui.setSlot(AnvilGUI.AnvilSlot.INPUT_LEFT, Utilities.ConstructItemStack(Material.PAPER, 1, 0, name));
		gui.openInv();
	}
	
	public static ItemStack ench(ItemStack item) {
		item.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
		return item;
	}

}
