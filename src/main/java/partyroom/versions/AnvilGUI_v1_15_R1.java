package partyroom.versions;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.BiFunction;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.ChatMessage;
import net.minecraft.server.v1_15_R1.ContainerAccess;
import net.minecraft.server.v1_15_R1.ContainerAnvil;
import net.minecraft.server.v1_15_R1.Containers;
import net.minecraft.server.v1_15_R1.EntityHuman;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_15_R1.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_15_R1.World;
import partyroom.PartyRoom;

public class AnvilGUI_v1_15_R1 extends AnvilGUI {
    private class AnvilContainer extends ContainerAnvil {
        public AnvilContainer(final EntityHuman entity, int windowId) {
        	// windowId, playerinventory
            super(windowId, entity.inventory, new ContainerAccess() {
        		@Override
        		public <T> Optional<T> a(BiFunction<World, BlockPosition, T> bifunction) {
        			return Optional.empty();
        		}
        		
        		@Override
        		public World getWorld() {
        			return entity.getWorld();
        		}
        		
        		@Override
        		public BlockPosition getPosition() {
        			return new BlockPosition(entity);
        		}
            });
        }
 
        @Override
        public boolean canUse(EntityHuman entityhuman){
            return true;
        }
    }
    
    public AnvilGUI_v1_15_R1(Player player, final AnvilClickEventHandler handler) {
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
                            event.getWhoClicked().closeInventory();
                        }
 
                        if(clickEvent.getWillDestroy()){
                            if (clickEvent.slot == AnvilSlot.OUTPUT && clicker.getLevel() > 0)
                                clicker.setLevel(clicker.getLevel());
                            destroy();
                        }
                    }
                }
            }
 
            @EventHandler
            public void onInventoryClose(InventoryCloseEvent event){
                if(event.getPlayer() instanceof Player){
                    Inventory inv = event.getInventory();
 
                    if(inv.equals(AnvilGUI_v1_15_R1.this.inv)){
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
 
        Bukkit.getPluginManager().registerEvents(listener, PartyRoom.getPlugin());
    }
 
    public void openInv() {
        EntityPlayer p = ((CraftPlayer) player).getHandle();
        
        //Counter stuff that the game uses to keep track of inventories
        int c = p.nextContainerCounter();
 
        //Set their active container window id to that counter stuff
        AnvilContainer container = new AnvilContainer(p, c);
        container.setTitle(ChatSerializer.a("\"text\":\"Repairing\""));
 
        //Set the items to the items from the inventory given
        inv = container.getBukkitView().getTopInventory();
 
        for(AnvilSlot slot : items.keySet()){
            inv.setItem(slot.getSlot(), items.get(slot));
        }
 
        //Send the packet
        p.playerConnection.sendPacket(new PacketPlayOutOpenWindow(c, Containers.ANVIL, new ChatMessage("Repairing",new Object[]{})));
 
        //Set their active container to the container
        p.activeContainer = container;

        //Add the slot listener
        p.activeContainer.addSlotListener(p);
    }
 
    public enum AnvilSlot {
        INPUT_LEFT(0),
        INPUT_RIGHT(1),
        OUTPUT(2);
 
        private int slot;
 
        private AnvilSlot(int slot){
            this.slot = slot;
        }
 
        public int getSlot(){
            return slot;
        }
 
        public static AnvilSlot bySlot(int slot){
            for(AnvilSlot anvilSlot : values()){
                if(anvilSlot.getSlot() == slot){
                    return anvilSlot;
                }
            }
 
            return null;
        }
    }
 
    public class AnvilClickEvent {
        private AnvilSlot slot;
 
        private String name;
 
        private boolean close = true;
        private boolean destroy = true;
 
        public AnvilClickEvent(AnvilSlot slot, String name){
            this.slot = slot;
            this.name = name;
        }
 
        public AnvilSlot getSlot(){
            return slot;
        }
 
        public String getName(){
            return name;
        }
 
        public boolean getWillClose(){
            return close;
        }
 
        public void setWillClose(boolean close){
            this.close = close;
        }
 
        public boolean getWillDestroy(){
            return destroy;
        }
 
        public void setWillDestroy(boolean destroy){
            this.destroy = destroy;
        }
    }
 
    public interface AnvilClickEventHandler {
        public void onAnvilClick(AnvilClickEvent event);
    }
 
    protected Player player;
 
    protected AnvilClickEventHandler handler;
 
    protected HashMap<AnvilSlot, ItemStack> items = new HashMap<AnvilSlot, ItemStack>();
 
    protected Inventory inv;
 
    protected Listener listener;
 
    public Player getPlayer(){
        return player;
    }
 
    public void setSlot(AnvilSlot slot, ItemStack item){
        items.put(slot, item);
    }
 
    public void destroy(){
        player = null;
        handler = null;
        items = null;
 
        HandlerList.unregisterAll(listener);
 
        listener = null;
    }
}