package partyroom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import partyroom.PartyChest.RegionTarget;

public class LoaderAndSaver {
	
	public static void loadChests(FileConfiguration config) {
		if (config.getConfigurationSection("party-chests") == null || config.getConfigurationSection("party-chests").getKeys(false) == null)
			return;
		for (String path : config.getConfigurationSection("party-chests").getKeys(false)) {
			ConfigurationSection c = config.getConfigurationSection("party-chests." + path);
			Location loc = null;
			int count, radius;
			Material mat;
			byte data;
			String region = "";
			double cost;
			RegionTarget target = RegionTarget.RADIUS;
			
			try {
				loc = Utilities.StringToLoc(path);
			} catch (Exception e) {
				Utilities.throwConsoleError(path + " is an invalid entry! It will not be loaded.");
				return;
			}
			
			if (loc == null || !(loc.getBlock().getState() instanceof Chest)) {
				Utilities.throwConsoleError(path + " is an invalid entry as the block it points to is no longer a Single-Chest!");
				return;
			}
			
			try {
				count = c.getInt("balloon-count");
			} catch (Exception e) {
				Utilities.throwConsoleError("Expected INTEGER in config at: §eparty-chests." + path + "balloon-count§f. Using default value.");
				count = 20;
			}

			try {
				String[] str = c.getString("balloon-material").split(":");
				mat = Material.valueOf(str[0]);
				data = Byte.parseByte(str[1]);
			} catch (Exception e) {
				Utilities.throwConsoleError("Invalid MATERIAL in config at: §eparty-chests." + path + "balloon-material§f. Using default value.");
				mat = Material.CAKE_BLOCK;
				data = 0;
			}
			
			try {
				cost = c.getDouble("pull-lever-cost");
			} catch (Exception e) {
				Utilities.throwConsoleError("Expected NUMBER in config at: §eparty-chests." + path + "pull-lever-cost§f. Using default value.");
				cost = 0;
			}
			
			try {
				target = RegionTarget.valueOf(c.getString("type").toUpperCase());
			} catch (Exception e) {
				Utilities.throwConsoleError("Accepted values in config at: §eparty-chests." + path + "type§f are REGION and RADIUS. Using default value.");
				target = RegionTarget.RADIUS;
			}
			
			try {
				radius = c.getInt("radius");
			} catch (Exception e) {
				Utilities.throwConsoleError("Expected INTEGER in config at: §eparty-chests." + path + "radius§f. Using default value.");
				radius = 10;
			}
			
			if (PartyRoom.getWG() != null && target == RegionTarget.REGION) {
				try {
					region = c.getString("worldguard-region");
				} catch (Exception e) {
					Utilities.throwConsoleError("Expected STRING in config at: §eparty-chests." + path + "region§f. Using Radius Mode instead.");
					radius = 10;
					target = RegionTarget.RADIUS;
				}
			}
			
			new PartyChest(path, count, mat, data, cost, target, radius, region);
		}
	}
	
	public static void saveChests(FileConfiguration c) {
		if (PartyRoomHandler.getPartyChests().isEmpty())
			return;
		for (PartyChest p : PartyRoomHandler.getPartyChests()) {
			saveToFile(c, p);
		}
		PartyRoom.getPlugin().saveConfig();
	}
	
	private static void saveToFile(FileConfiguration c, PartyChest p) {
		String path = "party-chests." + Utilities.LocToString(p.getChest().getLocation()) + ".";
		
		c.set(path + "balloon-count", p.getCount());
		c.set(path + "balloon-material", p.getMaterial());
		c.set(path + "pull-lever-cost", p.getCost());
		c.set(path + "type", p.getRegionTarget());
		c.set(path + "worldguard-region", p.getRegion());
		c.set(path + "radius", p.getRadius());
	}

}
