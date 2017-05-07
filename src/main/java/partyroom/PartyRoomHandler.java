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

	private Map<String, HashSet<String>> globalBlacklist = new HashMap<String, HashSet<String>>();
	
	private Map<String, PartyChest> PartyChests = new HashMap<String, PartyChest>();
	private Map<String, PartyChest> Names = new HashMap<String, PartyChest>();
	private Set<String> Balloons = new HashSet<String>();
	
	public PartyChest getByName(String name) {
		return Names.get(name.toLowerCase());
	}
	
	public void handleNameChange(String oldname, String newname, PartyChest pc) {
		Names.remove(oldname);
		Names.put(newname, pc);
	}
	
	public int n() {
		return PartyChests.size();
	}
	
	public Map<String, HashSet<String>> getGlobalBlacklist() {
		return globalBlacklist;
	}
	
	public boolean isBlacklisted(ItemStack item, PartyChest chest) {
		if (item == null)
			return false;
		
		String itemdata = item.getType().toString() + "," + item.getDurability();
		Map<String, HashSet<String>> mapToCheck = globalBlacklist.containsKey(itemdata) || globalBlacklist.containsKey(item.getType().toString()) ? globalBlacklist : chest.getBlacklist();
		HashSet<String> blah = mapToCheck.containsKey(itemdata) ? mapToCheck.get(itemdata) : mapToCheck.get(item.getType().toString());
		return blah != null && (blah.isEmpty() || item.getItemMeta().hasDisplayName() && blah.contains(item.getItemMeta().getDisplayName().replace("§", "&")));
	}
	
	public void addPartyChest(PartyChest chest) {
		PartyChests.put(chest.getChestString(), chest);
		Names.put(chest.getName().toLowerCase(), chest);
	}
	
	public void removePartyChest(PartyChest chest) {
		PartyChests.remove(chest.getChestString());
		Names.remove(chest.getName().toLowerCase());
		ChestEditor.removeEditor(chest.getChestString());
	}
	
	public PartyChest getPartyChest(String s) {
		return PartyChests.get(s);
	}
	
	public Collection<PartyChest> getPartyChests() {
		return PartyChests.values();
	}
	
	public boolean isPartyChest(Chest c) {
		return PartyChests.containsKey(Utilities.LocToString(c.getLocation()));
	}

	public boolean isPartyChest(Block b) {
		return b.getState() instanceof Chest && PartyChests.containsKey(Utilities.LocToString(b.getLocation()));
	}
	
	public void addBalloon(Block b) {
		Balloons.add(Utilities.LocToString(b.getLocation()));
	}
	
	public void removeBalloon(Block b) {
		b.removeMetadata("partyroom", PartyRoom.getPlugin());
		Balloons.remove(Utilities.LocToString(b.getLocation()));
	}
	
	public void removeBalloons() {
		for (String s : Balloons) {
			Utilities.StringToLoc(s).getBlock().setType(Material.AIR);
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
		for (PartyChest chest : PartyChests.values()) {
			chest.forceStop();
		}
		PartyChests.clear();
	}

}
