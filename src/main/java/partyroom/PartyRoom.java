package partyroom;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Map;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import partyroom.listener.PartyListener;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class PartyRoom extends JavaPlugin {
    
    private static WorldGuardPlugin WorldGuard;
    private static Economy economy;
    private static PartyRoom plugin;
    private static FileConfiguration config;
    private static boolean spigot;
    
    public static String VERSION;
    public static boolean debug;
    
    public PartyRoomHandler handler;
    
    public static final String PREFIX = "§6§lPartyRoom > §r";
    
    @Override
    public void onEnable() {
        plugin = this;
        this.saveDefaultConfig();
        config = this.getConfig();
        updateConfig();
        
        ConfigMessages.load();
        handler = new PartyRoomHandler();
        
        checkWorldGuard();
        checkEconomy();
        getCommand("proom").setExecutor(new CommandHandler());
        
        String pack = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = pack.substring(pack.lastIndexOf('.') + 1);
        
        LoaderAndSaver.loadChests(config);
        debug = config.getBoolean("debug");
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
        handler.removeBalloons();
    }
    
    private void updateConfig() {
        if (config.isSet("version")) {
            switch (config.getInt("version")) {
            case 1:
                config.set("messages.attempt-deposit-cancelled", "This item may not be deposited.");
                config.set("version", 2);
                saveConfig();
                reloadConfig();
                break;
            case 2:
                break;
            }
        } else {
            config.set("version", 1);
            Reader reader = new InputStreamReader(getResource("config.yml"));
            config.createSection("messages", (Map<?, ?>) YamlConfiguration.loadConfiguration(reader).getConfigurationSection("messages").getValues(false));
            saveConfig();
            reloadConfig();
        }
    }
    
    public static PartyRoom getPlugin() {
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
        ConfigMessages.load();
        LoaderAndSaver.loadChests(config);
        debug = config.getBoolean("debug");
    }
    
    public static void debug(String message) {
        if (debug)
            Bukkit.getConsoleSender().sendMessage("§7[PartyRoom DEBUG] §r" + message);
    }
    
    private static void checkWorldGuard() {
        Plugin WGplugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
     
        if (WGplugin == null || !(WGplugin instanceof WorldGuardPlugin)) {
            WorldGuard = null;
            return;
        }
     
        WorldGuard = (WorldGuardPlugin) WGplugin;
    }
    
    private static void checkEconomy() {
        Plugin vault = plugin.getServer().getPluginManager().getPlugin("Vault");
        if (vault == null) {
            economy = null;
            Bukkit.getLogger().info("[PARTYROOM] Vault is not installed, so economy settings won't work!");
            return;
        }
        
        RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        economy = economyProvider == null ? null : economyProvider.getProvider();
    }

}
