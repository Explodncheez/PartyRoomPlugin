package partyroom;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import partyroom.gui.ChestEditor;

public class PartyRoomHandler {

    private Map<PredicateItem, HashSet<String>> globalBlacklist = new HashMap<>();
    
    private Map<String, PartyChest> partyChests = new HashMap<>();
    private Map<String, PartyChest> names = new HashMap<>();
    private Map<PartyChest, Set<String>> balloons = new HashMap<>();
    
    public void stopAll() {
        for (PartyChest chest : partyChests.values()) {
            if (chest.isPulled()) {
                chest.forceStop();
            }
        }
    }
    
    public PartyChest getByName(String name) {
        return names.get(name.toLowerCase());
    }
    
    public void handleNameChange(String oldname, String newname, PartyChest pc) {
        names.remove(oldname);
        names.put(newname, pc);
    }
    
    public int n() {
        return partyChests.size();
    }
    
    public Map<PredicateItem, HashSet<String>> getGlobalBlacklist() {
        return globalBlacklist;
    }
    
    public boolean isBlacklisted(ItemStack item, PartyChest chest) {
        if (item == null)
            return false;

        PredicateItem itemdata = new PredicateItem(item);
        Map<PredicateItem, HashSet<String>> mapToCheck = globalBlacklist.containsKey(itemdata) || globalBlacklist.containsKey(item.getType().toString()) ? globalBlacklist : chest.getBlacklist();
        HashSet<String> blah = mapToCheck.containsKey(itemdata) ? mapToCheck.get(itemdata) : mapToCheck.get(item.getType().toString());
        return blah != null && (blah.isEmpty() || item.getItemMeta().hasDisplayName() && blah.contains(item.getItemMeta().getDisplayName().replace("§", "&")));
    }
    
    public void addPartyChest(PartyChest chest) {
        partyChests.put(chest.getChestString(), chest);
        names.put(chest.getName().toLowerCase(), chest);
    }
    
    public void removePartyChest(PartyChest chest) {
        partyChests.remove(chest.getChestString());
        names.remove(chest.getName().toLowerCase());
        ChestEditor.removeEditor(chest.getChestString());
    }
    
    public PartyChest getPartyChest(String s) {
        return partyChests.get(s);
    }
    
    public Collection<PartyChest> getPartyChests() {
        return partyChests.values();
    }
    
    public boolean isPartyChest(Chest c) {
        return partyChests.containsKey(Utilities.LocToString(c.getLocation()));
    }

    public boolean isPartyChest(Block b) {
        return b.getState() instanceof Chest && partyChests.containsKey(Utilities.LocToString(b.getLocation()));
    }
    
    public void addBalloon(PartyChest owner, Block b) {
        Set<String> set = balloons.get(owner);
        if (set == null)
            balloons.put(owner, set = new HashSet<String>());
        set.add(Utilities.LocToString(b.getLocation()));
    }
    
    public void removeBalloon(PartyChest owner, Block b) {
        b.removeMetadata("partyroom", PartyRoom.getPlugin());
        Set<String> set = balloons.get(owner);
        if (set != null)
            set.remove(Utilities.LocToString(b.getLocation()));
    }
    
    public void removeBalloons(PartyChest owner) {
        Set<String> set = balloons.get(owner);
        if (set != null)
            for (String s : set) {
                Block b = Utilities.StringToLoc(s).getBlock();
                b.removeMetadata("partyroom", PartyRoom.getPlugin());
                b.setType(Material.AIR);
            }
    }
    
    public void removeBalloons() {
        for (Set<String> set : balloons.values())
            for (String s : set) {
                Block b = Utilities.StringToLoc(s).getBlock();
                b.removeMetadata("partyroom", PartyRoom.getPlugin());
                b.setType(Material.AIR);
            }
    }
    
    public PartyChest findChestForLever(Block lever) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block b = lever.getRelative(x, y, z);
                    if (isPartyChest(b)) {
                        return getPartyChest(Utilities.LocToString(b.getLocation()));
                    }
                }
            }
        }
        return null;
    }
    
    public boolean closeToPartyChest(Block placed) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block b = placed.getRelative(x, y, z);
                    if (isPartyChest(b))
                        return true;
                }
            }
        }
        return false;
    }
    
    public void openBlacklist(Player p, PartyChest pchest) {
        
    }
    
    public void clear() {
        for (PartyChest chest : partyChests.values()) {
            chest.forceStop();
        }
        partyChests.clear();
    }

}
