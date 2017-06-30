package partyroom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import partyroom.PartyChest.ChestParticles;
import partyroom.PartyChest.RegionTarget;
import partyroom.PartyChest.YSpawnTarget;

public class LoaderAndSaver {
    
    public static void loadChests(FileConfiguration config) {
        PartyRoom.getPlugin().handler.stopAll();
        
        if (config.getConfigurationSection("party-chests") == null || config.getConfigurationSection("party-chests").getKeys(false) == null)
            return;

        if (config.isSet("global-blacklist"))
            try {
                for (String s : config.getConfigurationSection("global-blacklist").getKeys(false)) {
                    
                    PartyRoom.getPlugin().handler.getGlobalBlacklist().put(s.toUpperCase(), new HashSet<String>(config.getStringList("global-blacklist." + s)));
                }
            } catch (Exception e) {
                Utilities.throwConsoleError("Invalid global blacklist§f. Ignoring value.");
            }
        
        
        for (String path : config.getConfigurationSection("party-chests").getKeys(false)) {
            ConfigurationSection c = config.getConfigurationSection("party-chests." + path);
            Location loc = null;
            int count, radius, dropdelay, dropcooldown, announceinterval, minslots;
            boolean enabled, stack = false;
            Material mat;
            byte data;
            String name, region = "", message = "&6Drop Party will start in &e%TIME%&6!", message2 = "§6Drop Party has started!";
            String costString;
            RegionTarget target = RegionTarget.RADIUS;
            YSpawnTarget ytarget = YSpawnTarget.DEFAULT;
            
            Map<String, HashSet<String>> blacklist = new HashMap<String, HashSet<String>>();
            
            try {
                loc = Utilities.StringToLoc(path);
            } catch (Exception e) {
                Utilities.throwConsoleError(path + ". is an invalid entry! It will not be loaded.");
                return;
            }
            
            if (loc == null || loc.getBlock() == null || !(loc.getBlock().getState() instanceof Chest)) {
                Utilities.throwConsoleError(path + ". is an invalid entry as the block it points to is no longer a Single-Chest!");
                return;
            }
            
            try {
                name = c.isSet("name") ? c.getString("name") : "" + ((int) (Math.random() * 4800));
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected String in config at: §eparty-chests." + path + ".enabled§f. Using random number instead.");
                name = "" + ((int) (Math.random() * 4800));
            }
            
            try {
                stack = c.getBoolean("stack-unstackables");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected TRUE/FALSE in config at: §eparty-chests." + path + ".stack-unstackables§f. Using FALSE.");
                stack = false;
            }
            
            
            try {
                enabled = c.getBoolean("enabled");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected TRUE/FALSE in config at: §eparty-chests." + path + ".enabled§f. Using FALSE.");
                enabled = false;
            }
            
            try {
                count = c.getInt("balloon-count");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected INTEGER in config at: §eparty-chests." + path + ".balloon-count§f. Using default value.");
                count = 20;
            }
            
            try {
                minslots = c.getInt("min-slots-to-drop");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected INTEGER in config at: §eparty-chests." + path + ".min-slots-to-drop§f. Using default value.");
                minslots = 0;
            }
            
            try {
                dropdelay = c.getInt("drop-party-delay");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected INTEGER in config at: §eparty-chests." + path + ".drop-party-delay§f. Using default value.");
                dropdelay = 0;
            }
            
            try {
                dropcooldown = c.getInt("drop-party-cooldown");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected INTEGER in config at: §eparty-chests." + path + ".drop-party-cooldown§f. Using default value.");
                dropcooldown = 0;
            }
            
            try {
                announceinterval = c.getInt("announce-interval");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected INTEGER in config at: §eparty-chests." + path + ".announce-interval§f. Using default value.");
                announceinterval = 0;
            }
            
            try {
                message = c.getString("announce-message") == null ? "&6Drop Party will start in &e%TIME%&6!" : c.getString("announce-message");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected STRING in config at: §eparty-chests." + path + ".announce-message§f. Using default value.");
            }
            
            try {
                message2 = c.getString("start-message") == null ? "&6Drop Party has started!" : c.getString("start-message");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected STRING in config at: §eparty-chests." + path + ".start-message§f. Using default value.");
            }

            try {
                String[] str = c.getString("balloon-material").split(":");
                mat = Material.valueOf(str[0]);
                data = Byte.parseByte(str[1]);
            } catch (Exception e) {
                Utilities.throwConsoleError("Invalid MATERIAL in config at: §eparty-chests." + path + ".balloon-material§f. Using default value.");
                mat = Material.CAKE_BLOCK;
                data = 0;
            }
            
            try {
                costString = Utilities.isNum(c.get("pull-lever-cost")) ? Double.toString(c.getDouble("pull-lever-cost")) : c.getString("pull-lever-cost");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected NUMBER in config at: §eparty-chests." + path + ".pull-lever-cost§f. Using default value.");
                costString = "0";
            }
            
            try {
                target = RegionTarget.valueOf(c.getString("type").toUpperCase());
            } catch (Exception e) {
                Utilities.throwConsoleError("Accepted values in config at: §eparty-chests." + path + ".type§f are REGION and RADIUS. Using default value.");
                target = RegionTarget.RADIUS;
            }

            try {
                ytarget = YSpawnTarget.valueOf(c.getString("balloon-spawn-height").toUpperCase());
            } catch (Exception e) {
                Utilities.throwConsoleError("Accepted values in config at: §eparty-chests." + path + ".balloon-spawn-height§f are DEFAULT, RANDOM, MAX, and MIN. Using default value.");
                ytarget = YSpawnTarget.DEFAULT;
            }
            
            try {
                radius = c.getInt("radius");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected INTEGER in config at: §eparty-chests." + path + ".radius§f. Using default value.");
                radius = 10;
            }
            
            try {
                region = c.getString("worldguard-region");
            } catch (Exception e) {
                Utilities.throwConsoleError("Expected STRING in config at: §eparty-chests." + path + ".region§f. Using Radius Mode instead.");
                radius = 10;
                target = RegionTarget.RADIUS;
            }

            if (c.isSet("blacklist"))
                try {
                    for (String s : c.getConfigurationSection("blacklist").getKeys(false)) {
                        
                        blacklist.put(s, new HashSet<String>(c.getStringList("blacklist." + s)));
                    }
                } catch (Exception e) {
                    Utilities.throwConsoleError("Invalid config at: §eparty-chests." + path + ".blacklist§f. Ignoring value.");
                }

            ChestParticles particle = null;
            if (c.isSet("particle-effect")) {
                try {
                    particle = ChestParticles.valueOf(c.getString("particle-effect").toUpperCase());
                } catch (Exception e) {
                    //Utilities.throwConsoleError("Invalid config at: §eparty-chests." + path + ".particle-effect§f. Ignoring value.");
                }
            }
            
            new PartyChest(path, name, stack, count, mat, data, costString.equals("0") ? null : new PullCost(costString), target, ytarget, dropdelay, dropcooldown, announceinterval, minslots, message, message2, radius, region, enabled, blacklist, particle);
        }
    }
    
    public static void saveChests(FileConfiguration c) {
        if (PartyRoom.getPlugin().handler.getPartyChests().isEmpty())
            return;
        for (PartyChest p : PartyRoom.getPlugin().handler.getPartyChests()) {
            saveToFile(c, p);
        }
        PartyRoom.getPlugin().saveConfig();
    }
    
    private static void saveToFile(FileConfiguration c, PartyChest p) {
        String path = "party-chests." + p.getChestString() + ".";

        c.set(path + "name", p.getName());
        c.set(path + "enabled", p.isEnabled());
        c.set(path + "stack-unstackables", p.stack());
        c.set(path + "balloon-count", p.getCount());
        c.set(path + "balloon-material", p.getMaterial());
        c.set(path + "pull-lever-cost", p.getCost() == null ? "0" : p.getCost().toString());
        c.set(path + "type", p.getRegionTarget().toString());
        c.set(path + "balloon-spawn-height", p.getYTarget().toString());
        c.set(path + "min-slots-to-drop", p.getMinSlots());
        c.set(path + "drop-party-delay", p.getDropDelay());
        c.set(path + "drop-party-cooldown", p.getDropCooldown());
        c.set(path + "announce-interval", p.getAnnounceInterval());
        c.set(path + "announce-message", p.getAnnounceMessage().replace("§", "&"));
        c.set(path + "start-message", p.getStartMessage().replace("§", "&"));
        c.set(path + "worldguard-region", p.getRegion() == null ? "''" : p.getRegion());
        c.set(path + "radius", p.getRadius());
        c.set(path + "blacklist", a(p.getBlacklist()));
        c.set(path + "particle-effect", p.getChestParticle());
        
        c.set("global-blacklist", a(PartyRoom.getPlugin().handler.getGlobalBlacklist()));
        c.set("debug", PartyRoom.debug);
    }
    
    private static Map<String, ArrayList<String>> a(Map<String, HashSet<String>> input) {
        Map<String, ArrayList<String>> a = new HashMap<String, ArrayList<String>>();
        for (String m : input.keySet()) {
            a.put(m, new ArrayList<String>(input.get(m)));
        }
        return a;
    }

}
