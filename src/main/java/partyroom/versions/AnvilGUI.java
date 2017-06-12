package partyroom.versions;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import partyroom.PartyRoom;

/**
* Created by chasechocolate. Abstraction by Explodncheez.
*/
public abstract class AnvilGUI {

    private static Class<? extends AnvilGUI> clazz;

    static {
        switch (PartyRoom.VERSION) {
        case "v1_12_R1":
            clazz = AnvilGUI_v1_12_R1.class;
            break;
        case "v1_11_R1":
            clazz = AnvilGUI_v1_11_R1.class;
            break;
        case "v1_10_R1":
            clazz = AnvilGUI_v1_10_R1.class;
            break;
        case "v1_9_R2":
            clazz = AnvilGUI_v1_9_R2.class;
            break;
        case "v1_9_R1":
            clazz = AnvilGUI_v1_9_R1.class;
            break;
        case "v1_8_R3":
            clazz = AnvilGUI_v1_8_R3.class;
            break;
            default:
                Bukkit.getLogger().info("ERROR: Your server version is not compatible with PartyRoom!");
                break;
        }
    }
    
    public static AnvilGUI get(Player p, AnvilClickEventHandler handler) {
        try {
            return clazz.getDeclaredConstructor(Player.class, AnvilClickEventHandler.class).newInstance(p, handler);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
    
    public abstract void openInv();
 
    public void destroy(){
        player = null;
        handler = null;
        items = null;
 
        HandlerList.unregisterAll(listener);
 
        listener = null;
    }
}