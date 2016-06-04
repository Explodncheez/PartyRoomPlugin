package partyroom;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;

import partyroom.gui.ChestEditor;

public class PartyRoomHandler {
	
	private static Map<String, PartyChest> PartyChests = new HashMap<String, PartyChest>();
	private static Set<String> Balloons = new HashSet<String>();
	
	public static void addPartyChest(PartyChest chest) {
		PartyChests.put(chest.getChestString(), chest);
	}
	
	public static void removePartyChest(PartyChest chest) {
		PartyChests.remove(chest.getChestString());
		ChestEditor.removeEditor(chest.getChestString());
	}
	
	public static PartyChest getPartyChest(String s) {
		return PartyChests.get(s);
	}
	
	public static Collection<PartyChest> getPartyChests() {
		return PartyChests.values();
	}
	
	public static boolean isPartyChest(Chest c) {
		return PartyChests.containsKey(Utilities.LocToString(c.getLocation()));
	}

	public static boolean isPartyChest(Block b) {
		return b.getState() instanceof Chest && PartyChests.containsKey(Utilities.LocToString(b.getLocation()));
	}
	
	public static void addBalloon(Block b) {
		Balloons.add(Utilities.LocToString(b.getLocation()));
	}
	
	public static void removeBalloon(Block b) {
		Balloons.remove(Utilities.LocToString(b.getLocation()));
	}
	
	public static void removeBalloons() {
		for (String s : Balloons) {
			Utilities.StringToLoc(s).getBlock().setType(Material.AIR);
		}
	}
	
	public static PartyChest findChestForLever(Block lever) {
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					Block b = lever.getRelative(x, y, z);
					if (isPartyChest(b)) {
						return PartyRoomHandler.getPartyChest(Utilities.LocToString(b.getLocation()));
					}
				}
			}
		}
		return null;
	}
	
	public static boolean closeToPartyChest(Block placed) {
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
	
	public static void clear() {
		PartyChests.clear();
	}

}
