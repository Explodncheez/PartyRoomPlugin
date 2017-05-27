package partyroom;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class OwnedItemStack {
    
    public OwnedItemStack(ItemStack item, Player owner) {
        this.item = item;
        this.owner = owner;
    }
    
    private ItemStack item;
    private Player owner;
    
    public void drop(Location loc) {
        PartyRoom.debug(owner.getName() + " got OwnedItemStack of type " + item.getType() + ":" + item.getDurability() + " at " + Utilities.LocToString(loc));
        
        final Item itemEntity = loc.getWorld().dropItem(loc, item);
        itemEntity.setMetadata("owner", new FixedMetadataValue(PartyRoom.getPlugin(), owner.getName()));
        new BukkitRunnable() {
            int cycle = 0;
            public void run() {
                if (itemEntity.isDead() || itemEntity == null) {
                    itemEntity.removeMetadata("owner", PartyRoom.getPlugin());
                    this.cancel();
                    return;
                }
                if (cycle > 60) {
                    itemEntity.removeMetadata("owner", PartyRoom.getPlugin());
                    this.cancel();
                    return;
                }
                cycle ++;
            }
        }.runTaskTimer(PartyRoom.getPlugin(), 20L, 20L);
    }

}
