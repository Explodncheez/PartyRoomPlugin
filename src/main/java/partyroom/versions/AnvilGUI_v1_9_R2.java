package partyroom.versions;

import net.minecraft.server.v1_9_R2.BlockPosition;
import net.minecraft.server.v1_9_R2.ChatMessage;
import net.minecraft.server.v1_9_R2.ContainerAnvil;
import net.minecraft.server.v1_9_R2.EntityHuman;
import net.minecraft.server.v1_9_R2.EntityPlayer;
import net.minecraft.server.v1_9_R2.PacketPlayOutOpenWindow;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import partyroom.PartyRoom;
import partyroom.gui.ChestEditor;

public class AnvilGUI_v1_9_R2 extends AnvilGUI {
    private class AnvilContainer extends ContainerAnvil {
        public AnvilContainer(EntityHuman entity) {
            super(entity.inventory, entity.world, new BlockPosition(0, 0, 0), entity);
        }
 
        @Override
        public boolean a(EntityHuman entityhuman){
            return true;
        }
    }
    
    public AnvilGUI_v1_9_R2(Player player, final AnvilClickEventHandler handler){
    	super();
    	
        this.player = player;
        this.handler = handler;
 
        this.listener = new Listener(){
            @EventHandler
            public void onInventoryClick(InventoryClickEvent event){
                if(event.getWhoClicked() instanceof Player){
                    Player clicker = (Player) event.getWhoClicked();
 
                    if(event.getInventory().equals(inv)){
                        event.setCancelled(true);
 
                        ItemStack item = event.getCurrentItem();
                        int slot = event.getRawSlot();
                        String name = "";
 
                        if(item != null){
                            if(item.hasItemMeta()){
                                ItemMeta meta = item.getItemMeta();
 
                                if(meta.hasDisplayName()){
                                    name = meta.getDisplayName();
                                }
                            }
                        }
 
                        AnvilClickEvent clickEvent = new AnvilClickEvent(AnvilSlot.bySlot(slot), name);
 
                        handler.onAnvilClick(clickEvent);
 
                        if(clickEvent.getWillClose()){
                        	ChestEditor ce = ChestEditor.get(clicker);
                            event.getWhoClicked().closeInventory();
							ce.openInventory(clicker);
							ChestEditor.addPlayerEditor(clicker, ce);
                        }
 
                        if(clickEvent.getWillDestroy()){
                            destroy();
                        }
                    }
                }
            }
 
            @EventHandler
            public void onInventoryClose(InventoryCloseEvent event){
                if(event.getPlayer() instanceof Player){
                    Player player = (Player) event.getPlayer();
                    Inventory inv = event.getInventory();
 
                    if(inv.equals(AnvilGUI_v1_9_R2.this.inv)){
                        inv.clear();
                        destroy();
                    }
                }
            }
 
            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event){
                if(event.getPlayer().equals(getPlayer())){
                    destroy();
                }
            }
        };
 
        Bukkit.getPluginManager().registerEvents(listener, PartyRoom.getPlugin()); //Replace with instance of main class
    }
 
    public void openInv(){
        EntityPlayer p = ((CraftPlayer) player).getHandle();
 
        AnvilContainer container = new AnvilContainer(p);
 
        //Set the items to the items from the inventory given
        inv = container.getBukkitView().getTopInventory();
 
        for(AnvilSlot slot : items.keySet()){
            inv.setItem(slot.getSlot(), items.get(slot));
        }
 
        //Counter stuff that the game uses to keep track of inventories
        int c = p.nextContainerCounter();
 
        //Send the packet
        p.playerConnection.sendPacket(new PacketPlayOutOpenWindow(c,"minecraft:anvil",new ChatMessage("Repairing",new Object[]{}),0));
 
        //Set their active container to the container
        p.activeContainer = container;
 
        //Set their active container window id to that counter stuff
        p.activeContainer.windowId = c;
 
        //Add the slot listener
        p.activeContainer.addSlotListener(p);
    }
}