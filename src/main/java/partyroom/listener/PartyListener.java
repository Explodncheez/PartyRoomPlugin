package partyroom.listener;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import partyroom.OwnedItemStack;
import partyroom.PartyChest;
import partyroom.PartyRoom;
import partyroom.Utilities;
import partyroom.ConfigMessages.ConfigMessage;
import partyroom.gui.ChestEditor;
import partyroom.versions.SoundHandler.Sounds;

public class PartyListener implements Listener {
    
    @EventHandler(priority = EventPriority.LOW)
    public void blockFall(EntityChangeBlockEvent e) {
        if (e.getEntityType() == EntityType.FALLING_BLOCK && e.getEntity().hasMetadata("partyroom")) {
            e.setCancelled(true);
            Block block = e.getEntity().getLocation().getBlock();
            if (block.getType() == Material.AIR) {
                FallingBlock fb = (FallingBlock) e.getEntity();
                block.setType(fb.getMaterial());
                String id = e.getEntity().getMetadata("partyroom").get(0).asString();
                
                PartyChest pchest = PartyRoom.getPlugin().handler.getPartyChest(id);
                block.setMetadata("partyroom", new FixedMetadataValue(PartyRoom.getPlugin(), id));
                PartyRoom.getPlugin().handler.addBalloon(pchest, block);
            }
        }
    }
    
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerPopBalloon(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (e.getClickedBlock().hasMetadata("partyroom")) {
                
                e.setCancelled(true);
                
                PartyChest Pchest = PartyRoom.getPlugin().handler.getPartyChest(e.getClickedBlock().getMetadata("partyroom").get(0).asString());
                e.getClickedBlock().getWorld().playEffect(e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5), PartyRoom.isSpigot() ? Effect.WITHER_BREAK_BLOCK : Effect.SMOKE, 0);
                e.getPlayer().playSound(e.getClickedBlock().getLocation(), Sounds.ENTITY_CHICKEN_EGG.a(), 0.8F, 0.5F);
                
                ItemStack loot = Pchest.getRandomLoot();
                if (loot != null)
                    new OwnedItemStack(loot, e.getPlayer()).drop(e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5));
                e.getClickedBlock().setType(Material.AIR);
                PartyRoom.getPlugin().handler.removeBalloon(Pchest, e.getClickedBlock());
                Pchest.removeBalloon(e.getClickedBlock().getLocation());
                
            } else if (e.getClickedBlock().getType() == Material.LEVER && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                
                final Block lever = e.getClickedBlock();
                PartyChest chest = PartyRoom.getPlugin().handler.findChestForLever(lever);
                
                if (chest != null) {
                    Player p = e.getPlayer();
                    if (!p.hasPermission("partyroom.pull")) {
                        e.setCancelled(true);
                        p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_DEPOSIT_FAIL.getString(null));
                        p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
                        return;
                    }
                    
                    if (chest.attemptPull(p)) {
                        
                        final byte b = lever.getData();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                lever.setType(Material.LEVER);
                            }
                        }.runTaskLater(PartyRoom.getPlugin(), 40L);
                        
                    } else {
                        e.setCancelled(true);
                    }
                }
                
            } else if (e.getClickedBlock().getType() == Material.CHEST && e.getAction() == Action.RIGHT_CLICK_BLOCK && PartyRoom.getPlugin().handler.isPartyChest(e.getClickedBlock())) {
                if (e.getPlayer().hasPermission("partyroom.create") && e.getPlayer().isSneaking()) {
                    PartyChest Pchest = PartyRoom.getPlugin().handler.getPartyChest(Utilities.LocToString(e.getClickedBlock().getLocation()));
                    ChestEditor ce = ChestEditor.get(Pchest);
                    ce.openInventory(e.getPlayer());
                    e.setCancelled(true);
                    return;
                }
                e.getPlayer().sendMessage(PartyRoom.PREFIX + ConfigMessage.WARN_DEPOSIT.getString(null));
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.8F, 0.9F);
                
                // Bypasses protection plugins
                e.setCancelled(true);
                e.getPlayer().openInventory(((Chest) e.getClickedBlock().getState()).getBlockInventory());
            }
        }
    }
    
    @EventHandler
    public void onChestDraggy(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();

            if (ChestEditor.isViewing(p)) {
                e.setCancelled(true);
                return;
            }

            if (e.getView().getTopInventory().getHolder() instanceof Chest) {
                Chest chest = (Chest) e.getView().getTopInventory().getHolder();
                if (PartyRoom.getPlugin().handler.isPartyChest(chest)) 
                    e.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestClicky(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            
            Player p = (Player) e.getWhoClicked();
            
            if (e.getSlot() == -1)
                return;
            
            ChestEditor ce = ChestEditor.get(p);
            if (ce != null) {
                ce.handle(e);
                return;
            }
            
            if (ChestEditor.isViewing(p)) {
                e.setCancelled(true);
                return;
            }
            
            if (e.getView().getTopInventory().getHolder() instanceof Chest) {
                Chest chest = (Chest) e.getView().getTopInventory().getHolder();
                final PartyChest pc = PartyRoom.getPlugin().handler.getPartyChest(Utilities.LocToString(chest.getLocation()));
                
                if (pc != null) {

                    //top inventory clicks
                    if (e.getRawSlot() < e.getView().getTopInventory().getSize()) {
                        if (e.getCursor().getType() != Material.AIR) {
                            if (pc.depositItem(e, e.getCursor()))
                                e.setCursor(null);
                            return;
                        }
                        
                        if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
                            if (!p.hasPermission("partyroom.withdraw")) {
                                e.setCancelled(true);
                                p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_WITHDRAW_FAIL.getString(null));
                                p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
                            } else {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        pc.updateInventoryProxy();
                                    }
                                }.runTaskLater(PartyRoom.getPlugin(), 1L);
                            }
                        }
                    } else {
                        // depositing items
                        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR && e.getView().getTopInventory().contains(e.getCursor().getType())) {
                            if (!p.hasPermission("partyroom.withdraw")) {
                                e.setCancelled(true);
                                p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_WITHDRAW_FAIL.getString(null));
                                p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
                            } else {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        pc.updateInventoryProxy();
                                    }
                                }.runTaskLater(PartyRoom.getPlugin(), 1L);
                            }
                        }
                        if (e.isShiftClick()) {
                            if (pc.depositItem(e, e.getCurrentItem()))
                                e.setCurrentItem(null);
                        }
                        
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
        if (e.getBlock().getType().toString().contains("CHEST") && PartyRoom.getPlugin().handler.closeToPartyChest(e.getBlock())) {
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
        if (b.getType().toString().contains("CHEST") && PartyRoom.getPlugin().handler.closeToPartyChest(b)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(PartyRoom.PREFIX + "Party Room Chests don't work as Double Chests, sorry.");
        }
    }
    
    @EventHandler
    public void inventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        ChestEditor.removeEditor(p);
        ChestEditor.removeViewer(p);
    }

}
