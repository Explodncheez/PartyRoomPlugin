package partyroom.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import partyroom.PartyChest;
import partyroom.PartyChest.RegionTarget;
import partyroom.PartyRoom;
import partyroom.Utilities;
import partyroom.anvil.AnvilGUI;

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
		RADIUS
	}
	
	public ChestEditor(PartyChest chest) {
		this.chest = chest;
		thingy = new WeakHashMap<Player, AnvilEditor>();
		Editors.put(chest.getChestString(), this);
		editorInterface = Bukkit.createInventory(null, 9, "§5Party Chest Editor");
		update();
	}
	
	private PartyChest chest;
	private Inventory editorInterface;
	private Map<Player, AnvilEditor> thingy;
	
	private void update() {
		editorInterface.setItem(2, Utilities.ConstructItemStack(Material.CAKE, 1, 0, "§a§lBalloon Block Type", "§e<< §f" + chest.getBlockMaterial().toString().toLowerCase() + " §e>>", "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(3, Utilities.ConstructItemStack(Material.MELON_SEEDS, 1, 0, "§b§lBalloon Count", "§e<< §f" + chest.getCount() + " §e>>", "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(4, Utilities.ConstructItemStack(Material.LEVER, 1, 0, "§6§lLever-Pull Cost", "§e<< §f$" + chest.getCost() + " §e>>", "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(5, Utilities.ConstructItemStack(Material.FLINT, 1, 0, "§c§lRegion Type", "§e<< §f" + chest.getRegionTarget().toString().toLowerCase() + " §e>>", chest.getRegionTarget() == RegionTarget.RADIUS ? "§eRadius: " + chest.getRadius() : "§eRegion: " + chest.getRegion(), "", "§f§l[ Click to Change ]"));
		editorInterface.setItem(6, Utilities.ConstructItemStack(chest.isEnabled() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, 1, 0, "§d§lEnabled?", "§e<< §f" + (chest.isEnabled() ? "§aEnabled!" : "§cDisabled.") + " §e>>", "", "§f§l[ Click to Toggle ]"));
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
				openAnvil(p, Utilities.ConstructItemStack(Material.PAPER, 1, 0, "Enter block type"));
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.MATERIAL);
				break;
			case MELON_SEEDS: // count
				openAnvil(p, Utilities.ConstructItemStack(Material.PAPER, 1, 0, "Enter balloon count"));
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.COUNT);
				break;
			case LEVER: // cost
				openAnvil(p, Utilities.ConstructItemStack(Material.PAPER, 1, 0, "Enter lever-pull cost"));
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.COST);
				break;
			case FLINT:
				Inventory inv = Bukkit.createInventory(null, 9, "§5Region Type");
				inv.setItem(0, Utilities.ConstructItemStack(Material.COMPASS, 1, 0, "§a§lRadius", "§e<< §f" + chest.getRadius() + " blocks §e>>", "", "§f§l[ Click to Change ]"));
				inv.setItem(1, Utilities.ConstructItemStack(Material.STAINED_GLASS_PANE, 1, 4, "§a§lWorldGuard Region", "§e<< §f" + (chest.getRegion() == null || chest.getRegion() == "" ? "§oNone Set" : chest.getRegion())  + " §e>>", "", "§f§l[ Click to Change ]"));
				inv.setItem(8, Utilities.ConstructItemStack(Material.BED, 1, 0, "§c§lBack"));
				p.openInventory(inv);
				addPlayerEditor(p, this);
				break;
			case EMERALD_BLOCK:
			case REDSTONE_BLOCK:
				chest.setEnabled(!chest.isEnabled());
				editorInterface.setItem(6, Utilities.ConstructItemStack(chest.isEnabled() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, 1, 0, "§d§lEnabled?", "§e<< §f" + (chest.isEnabled() ? "§aEnabled!" : "§cDisabled.") + " §e>>", "", "§f§l[ Click to Toggle ]"));
				break;
				default:
					break;
			}
			break;
		case "§5Region Type":
			e.setCancelled(true);
			switch (e.getCurrentItem().getType()) {
			case COMPASS:  // radius
				openAnvil(p, Utilities.ConstructItemStack(Material.PAPER, 1, 0, "Enter radius"));
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.RADIUS);
				break;
			case STAINED_GLASS_PANE: // region
				openAnvil(p, Utilities.ConstructItemStack(Material.PAPER, 1, 0, "Enter region name"));
				addPlayerEditor(p, this);
				thingy.put(p, AnvilEditor.REGION);
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
	
	private void openAnvil(final Player p, ItemStack output) {
		AnvilGUI gui = new AnvilGUI(p, new AnvilGUI.AnvilClickEventHandler() {
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
								p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid block type specified! §f(BlockType:Data)");
								p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
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
								p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid lever-pull cost specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
							}
						}
						break;
					case COUNT:
						if (e.getName() != null) {
							try {
								int amt = Integer.parseInt(e.getName());
								
								if (amt < 1) {
									p.sendMessage(PartyRoom.PREFIX + "§cWhy would you want less than one balloon per party? That's just sad.");
									p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
									return;
								}
								if (amt > 100) {
									p.sendMessage(PartyRoom.PREFIX + "§cWARNING: §fYou've specified a large number of balloons. This may cause issues!");
								}
								
								chest.setCount(amt);
								p.sendMessage(PartyRoom.PREFIX + "§e" + amt + "§f balloons will now drop with each party!");
								p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid balloon count specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
							}
						}
						break;
					case REGION:
						if (e.getName() != null) {
							try {
								if (chest.setRegion(e.getName()) == null) {
									p.sendMessage(PartyRoom.PREFIX + "§cThe specified region: §e" + e.getName() + " §cdoes not exist in your current world.");
									p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
									return;
								}
								p.sendMessage(PartyRoom.PREFIX + "§eParty Room Region §fset to: §e" + e.getName() + "§f!");
								p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cThe specified region: §e" + e.getName() + " §cdoes not exist in your current world.");
								p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
							}
						}
						break;
					case RADIUS:
						if (e.getName() != null) {
							try {
								chest.setRadius(Integer.parseInt(e.getName()));
								p.sendMessage(PartyRoom.PREFIX + "§eParty Room Radius §fset to: §e" + e.getName() + " blocks§f!");
								p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 0.8F, 1.8F);
							} catch (Exception ex) {
								p.sendMessage(PartyRoom.PREFIX + "§cInvalid balloon drop radius specified! §fPlease use whole numbers.");
								p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
							}
						}
						break;
					}
				 
				} else {
					e.setWillClose(false);
					e.setWillDestroy(false);
				}
			}
		});
			 
		gui.setSlot(AnvilGUI.AnvilSlot.INPUT_LEFT, output);
		gui.open();
	}

}
