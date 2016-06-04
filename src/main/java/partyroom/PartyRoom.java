package partyroom;

import java.lang.reflect.Method;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import partyroom.listener.PartyListener;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class PartyRoom extends JavaPlugin {
	
	private static WorldGuardPlugin WorldGuard;
	private static Economy economy;
	private static Plugin plugin;
	private static FileConfiguration config;
	private static boolean spigot;
	
	public static final String PREFIX = ChatColor.GOLD + "" + ChatColor.BOLD + "PartyRoom > " + ChatColor.RESET;
	
	@Override
	public void onEnable() {
		plugin = this;
		this.saveDefaultConfig();
		config = this.getConfig();
		checkWorldGuard();
		checkEconomy();
		getCommand("proom").setExecutor(new CommandHandler());
		
		LoaderAndSaver.loadChests(config);
		getServer().getPluginManager().registerEvents(new PartyListener(), this);
		
		try {
			Method m = ItemMeta.class.getDeclaredMethod("spigot");
			spigot = m != null;
		} catch (Exception e) {
			spigot = false;
		}
	}
	
	@Override
	public void onDisable() {
		LoaderAndSaver.saveChests(config);
		PartyRoomHandler.removeBalloons();
	}
	
	public static Plugin getPlugin() {
		return plugin;
	}
	
	public static FileConfiguration getConfiguration() {
		return config == null ? config = plugin.getConfig() : config;
	}
	
	public static boolean isSpigot() {
		return spigot;
	}
	
	public static WorldGuardPlugin getWG() {
		return WorldGuard;
	}
	
	public static Economy getEcon() {
		return economy;
	}
	
	public static void reloadConfiguration() {
		plugin.reloadConfig();
		config = plugin.getConfig();
	}
	
	private static void checkWorldGuard() {
	    Plugin WGplugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
	 
	    if (config.getBoolean("use-worldguard") && (WGplugin == null || !(WGplugin instanceof WorldGuardPlugin))) {
	        WorldGuard = null;
	        Utilities.throwConsoleError("WorldGuard plugin not found! Using Radius Mode instead!");
	        return;
	    }
	 
	    WorldGuard = (WorldGuardPlugin) WGplugin;
	}
	
	private static void checkEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        economy = economyProvider == null ? null : economyProvider.getProvider();
    }

}
