package partyroom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
    
    public static boolean isNum(Object o) {
        try {
            Double.parseDouble((String) o);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static int random(int range) {
        return (int) (Math.random() * (range + 1));
    }
    
    public static void throwConsoleError(String error) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "ERROR: " + ChatColor.RESET + error);
    }
    
    public static String LocToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
    
    public static Location StringToLoc(String s) {
        String[] ss = s.split(",");
        return new Location(Bukkit.getWorld(ss[0]), Integer.parseInt(ss[1]), Integer.parseInt(ss[2]), Integer.parseInt(ss[3]));
    }
    
    public static List<Integer> getFilledSlots(Inventory i) {
        List<Integer> list = new ArrayList<Integer>();
        ItemStack[] contents = i.getContents();
        for (int j = 0; j < contents.length; j++) {
            if (contents[j] != null)
                list.add(j);
        }
        return list;
    }

    public static ItemStack ConstructItemStack(Material mat, int amount, int durability, String displayName, String... lore) {
        ItemStack i = new ItemStack(mat, amount, (short) durability);
        ItemMeta meta = i.getItemMeta();
        List<String> loreList = new ArrayList<String>();
        for (String s : lore)
            loreList.add(s.replace("&", "§"));
        meta.setDisplayName(displayName.replace("&", "§"));
        meta.setLore(loreList);
        i.setItemMeta(meta);
        
        return i;
    }
    
    public static void setLore(ItemStack item, int line, String changeTo) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        lore.set(line, changeTo);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

}
