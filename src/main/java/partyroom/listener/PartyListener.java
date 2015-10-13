package partyroom.listener;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import partyroom.OwnedItemStack;
import partyroom.PartyChest;
import partyroom.PartyRoom;
import partyroom.PartyRoomHandler;
import partyroom.Utilities;

public class PartyListener implements Listener {
	
	@EventHandler
	public void blockFall(EntityChangeBlockEvent e) {
		if (e.getEntityType() == EntityType.FALLING_BLOCK && e.getEntity().hasMetadata("partyroom")) {
			e.getBlock().setMetadata("partyroom", new FixedMetadataValue(PartyRoom.getPlugin(), e.getEntity().getMetadata("partyroom").get(0).asString()));
			PartyRoomHandler.addBalloon(e.getBlock());
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerPopBalloon(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (e.getClickedBlock().hasMetadata("partyroom")) {
				
				e.setCancelled(true);
				
				PartyChest Pchest = PartyRoomHandler.getPartyChest(e.getClickedBlock().getMetadata("partyroom").get(0).asString());
				e.getClickedBlock().getWorld().playEffect(e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5), Effect.EXPLOSION_LARGE, 0);
				e.getPlayer().playSound(e.getClickedBlock().getLocation(), Sound.CHICKEN_EGG_POP, 0.8F, 0.5F);
				
				ItemStack loot = Pchest.getRandomLoot();
				if (loot != null)
					new OwnedItemStack(loot, e.getPlayer()).drop(e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5));
				e.getClickedBlock().setType(Material.AIR);
				PartyRoomHandler.removeBalloon(e.getClickedBlock());
				
			} else if (e.getClickedBlock().getType() == Material.LEVER && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				
				final Block lever = e.getClickedBlock();
				PartyChest chest = PartyRoomHandler.findChestForLever(lever);
				
				if (chest != null) {
					Player p = e.getPlayer();
					if (!p.hasPermission("partyroom.pull")) {
						e.setCancelled(true);
						p.sendMessage(PartyRoom.PREFIX + "You do not have permission to start Drop Parties!");
						p.playSound(p.getLocation(), Sound.ZOMBIE_METAL, 0.4F, 1.2F);
						return;
					}
					
					if (chest.attemptPull(p)) {
						
						final byte b = lever.getData();
						new BukkitRunnable() {
							public void run() {
								lever.setData(b);
							}
						}.runTaskLater(PartyRoom.getPlugin(), 40L);
						
					} else {
						e.setCancelled(true);
					}
				}
				
			} else if (e.getClickedBlock().getType() == Material.CHEST && e.getAction() == Action.RIGHT_CLICK_BLOCK && PartyRoomHandler.isPartyChest(e.getClickedBlock())) {
				e.getPlayer().sendMessage(PartyRoom.PREFIX + "§cWARNING: Anything you put in this chest CANNOT be taken out!!");
				e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.NOTE_PLING, 0.8F, 0.9F);
				
				// Bypasses protection plugins
				e.getPlayer().openInventory(((Chest) e.getClickedBlock().getState()).getBlockInventory());
			}
		}
	}
	
	@EventHandler
	public void onChestClicky(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			
			Player p = (Player) e.getWhoClicked();
			
			if (e.getView().getTopInventory().getHolder() instanceof Chest) {
				Chest chest = (Chest) e.getView().getTopInventory().getHolder();
				if (PartyRoomHandler.isPartyChest(chest)) {
					
					//top inventory clicks
					if (e.getRawSlot() < e.getView().getTopInventory().getSize()) {
						if (!p.hasPermission("partyroom.withdraw")) {
							e.setCancelled(true);
							p.sendMessage(PartyRoom.PREFIX + "You can't take items out of Party Chests!");
							p.playSound(p.getLocation(), Sound.ZOMBIE_METAL, 0.4F, 1.2F);
						}
						return;
					} else {
						if (!p.hasPermission("partyroom.deposit")) {
							e.setCancelled(true);
							p.sendMessage(PartyRoom.PREFIX + "You do not have permission to deposit items!");
							p.playSound(p.getLocation(), Sound.ZOMBIE_METAL, 0.4F, 1.2F);
							return;
						}
						if (PartyRoomHandler.getPartyChest(Utilities.LocToString(chest.getLocation())).isPulled()) {
							e.setCancelled(true);
							p.sendMessage(PartyRoom.PREFIX + "There is currently a drop going on!");
							p.playSound(p.getLocation(), Sound.ZOMBIE_METAL, 0.4F, 1.2F);
							return;
						}
						p.playSound(p.getLocation(), Sound.NOTE_PLING, 0.4F, 0.7F);
					}
					
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.getBlock().hasMetadata("partyroom")) {
			e.setCancelled(true);
			e.getBlock().setType(Material.AIR);
		}
		if (e.getBlock().getType().toString().contains("CHEST") && PartyRoomHandler.closeToPartyChest(e.getBlock())) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(PartyRoom.PREFIX + "You can't break Party Chests.");
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onItemPickup(PlayerPickupItemEvent e) {
		if (e.getItem().hasMetadata("owner")) {
			String name = e.getItem().getMetadata("owner").get(0).asString();
			Player p = Bukkit.getPlayer(name);
			if (!e.getPlayer().getName().equals(name) && p != null && p.isOnline()) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onDoubleChestCreate(BlockPlaceEvent e) {
		Block b = e.getBlockPlaced();
		if (b.getType().toString().contains("CHEST") && PartyRoomHandler.closeToPartyChest(b)) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(PartyRoom.PREFIX + "Party Room Chests don't work as Double Chests, sorry.");
		}
	}

}
