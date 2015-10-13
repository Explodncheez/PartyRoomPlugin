package partyroom;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;

public class Utilities {
	
	public static Set<Player> getNearbyPlayers(Location center, double radius) {
		Set<Player> set = new HashSet<Player>();
		
		// using a temporary entity as reference
		Entity referenceEntity = center.getWorld().spawn(center, Snowball.class);
		referenceEntity.remove();
		
		for (Entity e : referenceEntity.getNearbyEntities(radius, radius, radius)) {
			if (e instanceof Player)
				set.add((Player) e);
		}
		
		return set;
	}
	
	public static int random(int range) {
		return (int) (Math.random() * (range + 1));
	}
	
	public static void throwConsoleError(String error) {
		Bukkit.getLogger().info(ChatColor.RED + "" + ChatColor.BOLD + "ERROR: " + ChatColor.RESET + error);
	}
	
	public static String LocToString(Location loc) {
		return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	
	public static Location StringToLoc(String s) {
		String[] ss = s.split(",");
		return new Location(PartyRoom.getPlugin().getServer().getWorld(ss[0]), Integer.parseInt(ss[1]), Integer.parseInt(ss[2]), Integer.parseInt(ss[3]));
	}

}
