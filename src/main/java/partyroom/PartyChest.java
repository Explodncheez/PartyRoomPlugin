package partyroom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import partyroom.ConfigMessages.ConfigMessage;
import partyroom.gui.ChestEditor;
import partyroom.versions.SoundHandler.Sounds;

public class PartyChest {
    
    public enum RegionTarget {
        RADIUS,
        REGION
    }
    
    public enum ChestParticles {
        SPIRAL_FALL,
        RANDOM_FALL,
        SPARKLE;
    }
    
    public enum YSpawnTarget {
        DEFAULT(0, Utilities.ConstructItemStack(Material.ENDER_PEARL, 1, 0, "§c§lDEFAULT", "§7Spawns up to 8 blocks above the floor.")),
        RANDOM(1, Utilities.ConstructItemStack(Material.ENDER_EYE, 1, 0, "§c§lRANDOM", "§7Spawns at a random region height.")),
        MIN(2, Utilities.ConstructItemStack(Material.SLIME_BALL, 1, 0, "§c§lMIN", "§7Spawns 1 block above the floor.")),
        MAX(3, Utilities.ConstructItemStack(Material.MAGMA_CREAM, 1, 0, "§c§lMAX", "§7Spawns at region's highest possible Y coord."));
        
        private YSpawnTarget(int slot, ItemStack item) {
            this.slot = slot;
            this.item = item;
            this.d = item.getItemMeta().getLore().get(0);
        }
        
        private int slot;
        private ItemStack item;
        private String d;
        
        private static Map<Integer, YSpawnTarget> byInts = new HashMap<Integer, YSpawnTarget>();
        static {
            for (YSpawnTarget y : YSpawnTarget.values())
                byInts.put(y.i(), y);
        }
        
        public static YSpawnTarget get(int slot) {
            return byInts.get(slot);
        }
        
        public int i() {
            return slot;
        }
        
        public String getDescription() {
            return d;
        }
        
        public ItemStack getIcon(PartyChest chest) {
            return chest.getYTarget() == this ? ChestEditor.ench(item.clone()) : item.clone();
        }
    }
    
    private Location location;
    
    private String chestLocation, chestName;
    private PullCost pcost;
    private int radius, count;
    private String regionName;
    private RegionTarget rtarget;
    private YSpawnTarget ytarget;
    private Material blockType;
    private byte blockData = 0;
    private BukkitTask delayRunnable, dpRunnable, coolingRunnable;
    
    private int dropDelay, dropCooldown, announceDelay, minSlots;
    private String announceMessage, startMessage;
    
    private Map<PredicateItem, HashSet<String>> blacklist;
    private Set<SimpleLoc> balloonLocs;
    private ChestParticles particle;
    
    private Inventory proxy;
    
    private boolean stack, delayed, pulled, enabled, coolingdown;
    private long time;
    private int hash;
    
    private Map<Integer, Integer> fill;
    private int fillCount;
    private int balloonsLeft;
    private boolean willDropEverything;
    private boolean canDropEverything;
    
    public PartyChest(String chestLoc, String chestName, boolean stack, int ballooncount, Material blockType, byte blockData, PullCost pcost, RegionTarget rtarget, YSpawnTarget ytarget, int dropDelay, int dropCooldown, int announceDelay, int minSlots, String announceMessage, String startMessage, int radius, String region, boolean enabled, Map<PredicateItem, HashSet<String>> blacklist, ChestParticles particles) {
        
        this.chestLocation = chestLoc;
        this.pcost = pcost;
        this.rtarget = rtarget;
        this.ytarget = ytarget;
        this.radius = radius;
        this.count = ballooncount;
        this.pulled = false;
        this.enabled = enabled;
        this.dropCooldown = dropCooldown;
        this.blacklist = blacklist == null ? new HashMap<PredicateItem, HashSet<String>>() : blacklist;
        this.chestName = chestName;
        this.stack = stack;
        
        this.dropDelay = dropDelay;
        this.announceDelay = announceDelay;
        this.minSlots = minSlots;
        this.announceMessage = announceMessage.replace("&", "§");
        this.startMessage = startMessage.replace("&", "§");
        Block block = (this.location = Utilities.StringToLoc(chestLocation)).getBlock();
        
        try {
            Chest chest = (Chest) block.getState();
            Inventory chestInv = chest.getBlockInventory();
            proxy = Bukkit.createInventory(null, chestInv.getSize(), chest.getCustomName() == null ? this.chestName : chest.getCustomName());
            proxy.setContents(chestInv.getContents());
        } catch (Exception e) {
            Bukkit.getLogger().info("[PROOM] ERROR: Inventory binding for Party Chest at " + chestLocation + " failed!");
        }
        
        this.particle = particles;
        
        setMaterial(blockType, blockData);
        
        if (region != null && !region.equals("''")) {
        	ProtectedRegion PRegion = null;
        	
        	if (PartyRoom.getWG() != null) {
            	
            	PRegion = getRegionFor(block.getWorld(), region);
            	
            	if (PRegion == null) {
                    Utilities.throwConsoleError("The specified region §c" + region + " §rdoes not exist!");
            	}
        	}
        	
            if (PRegion instanceof ProtectedCuboidRegion) {
                this.regionName = region;
            } else {
                Utilities.throwConsoleError("PartyRoom Regions must be Cuboid and §c" + region + " §ris not!");
                this.rtarget = RegionTarget.RADIUS;
            }
        }
        
        this.fill = new HashMap<>();
        
        PartyRoom.getPlugin().handler.addPartyChest(this);
    }
    
    private static ProtectedRegion getRegionFor(World world, String regionName) {
    	if (PartyRoom.getWG() == null)
    		return null;

    	RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    	RegionManager regions = container.get(BukkitAdapter.adapt(world));
    	return regions.getRegion(regionName);
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PartyChest))
            return false;
        
        PartyChest pc = (PartyChest) other;
        return pc.chestLocation.equals(this.chestLocation)
                && pc.chestName.equals(this.chestName);
    }
    
    @Override
    public int hashCode() {
        if (this.hash != 0)
            return this.hash;
        
        int hash = this.location.hashCode();
        return this.hash = hash;
    }
    
    public ChestParticles getEnumParticle() {
        return particle;
    }
    
    public String getChestParticle() {
        return particle == null ? "none" : particle.toString();
    }
    
    public Map<PredicateItem, HashSet<String>> getBlacklist() {
        return blacklist;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean e) {
        enabled = e;
    }
    
    public boolean stack() {
        return stack;
    }
    
    public void setStack(boolean b) {
        stack = b;
    }
    
    /**
     * @return delay of drop party after chest is pulled, in seconds
     */
    public int getDropDelay() {
        return dropDelay;
    }
    
    public void setDropDelay(int seconds) {
        dropDelay = seconds;
    }
    
    public void setDropCooldown(int seconds) {
        dropCooldown = seconds;
    }
    
    public int getDropCooldown() {
        return dropCooldown;
    }
    
    public int getAnnounceInterval() {
        return announceDelay;
    }
    
    public void setAnnounceInterval(int seconds) {
        announceDelay = seconds;
    }
    
    public int getMinSlots() {
        return minSlots;
    }
    
    public void setMinSlots(int slots) {
        minSlots = slots;
    }
    
    public String getName() {
        return chestName;
    }
    
    public void setName(String name) {
        PartyRoom.getPlugin().handler.handleNameChange(chestName, name, this);
        chestName = name;
    }
    
    public String getAnnounceMessage(int timeLeft) {
        int minutes = timeLeft / 60, seconds = timeLeft % 60;
        return announceMessage.replace("%NAME%", chestName).replace("%TIME%", (minutes > 0 ? minutes + " minute" + (minutes == 1 ? ", " : "s, ") : "") + seconds + " second" + (seconds == 1 ? "" : "s"));
    }
    
    public String getAnnounceMessage() {
        return announceMessage.replace("%NAME%", chestName);
    }
    
    public String getStartMessage() {
        return startMessage.replace("%NAME%", chestName);
    }
    
    public void setAnnounceMessage(String s) {
        announceMessage = s.replace("&", "§");
    }
    
    public void setStartMessage(String s) {
        startMessage = s.replace("&", "§");
    }
    
    public void setPulled(boolean pull) {
        pulled = pull;
    }

    public PullCost getCost() {
        return pcost;
    }
    
    public void setCost(String s) {
        pcost.reload(s);
    }
    
    public int getRadius() {
        return radius;
    }
    
    public void setRadius(int r) {
        radius = r;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int c) {
        count = c;
    }
    
    public BukkitTask getDropRunnable() {
        return dpRunnable;
    }
    
    public BukkitTask getDelayRunnable() {
        return delayRunnable;
    }
    
    public void setRegionTarget(RegionTarget newtarget) {
        rtarget = newtarget;
    }
    
    public void setYTarget(YSpawnTarget newytarget) {
        ytarget = newytarget;
    }
    
    public String getRegion() {
        return regionName;
    }
    
    public boolean setWGRegion(String s) {
        ProtectedRegion region = getRegionFor(Utilities.StringToLoc(chestLocation).getWorld(), s);
        if (region != null && region instanceof ProtectedCuboidRegion) {
            regionName = s;
            return true;
        }
        return false;
    }
    
    public ProtectedCuboidRegion getWGRegion() {
        return (ProtectedCuboidRegion) getRegionFor(Utilities.StringToLoc(chestLocation).getWorld(), regionName);
    }
    
    public String getMaterial() {
        return blockType.toString() + ":" + blockData;
    }
    
    public void setMaterial(Material m, int data) {
        if (m.isBlock()) {
            this.blockType = m;
            this.blockData = (byte) data;
        } else {
            Utilities.throwConsoleError(m.toString() + " is not a valid block!");
            this.blockType = Material.CAKE;
            this.blockData = 0;
        }
    }
    
    public Material getBlockMaterial() {
        return blockType;
    }
    
    public String getChestString() {
        return chestLocation;
    }
    
    public Chest getChest() {
        return (Chest) Utilities.StringToLoc(chestLocation).getBlock().getState();
    }
    
    public RegionTarget getRegionTarget() {
        return rtarget;
    }
    
    public YSpawnTarget getYTarget() {
        return ytarget;
    }
    
    public boolean isPulled() {
        return pulled;
    }
    
    public boolean isDelayed() {
        return delayed;
    }
    
    public PartyRoomRegion getProomRegion() {
        Chest chest = (Chest) Utilities.StringToLoc(chestLocation).getBlock().getState();
        
        switch (rtarget) {
            case REGION:
                ProtectedCuboidRegion region = getWGRegion();
                
                if (region == null) {
                    Utilities.throwConsoleError("ERROR: The region " + regionName + " no longer exists!");
                    return null;
                }
                
                BlockVector3 vMax = region.getMaximumPoint();
                BlockVector3 vMin = region.getMinimumPoint();
                return new PartyRoomRegion(chest.getWorld(), vMin.getBlockX(), vMin.getBlockY(), vMin.getBlockZ(), vMax.getBlockX(), vMax.getBlockY(), vMax.getBlockZ());
            default:
                Location c = chest.getLocation();
                return new PartyRoomRegion(chest.getWorld(), c.getBlockX() - radius, c.getBlockY(), c.getBlockZ() - radius, c.getBlockX() + radius, c.getBlockY() + radius, c.getBlockZ() + radius);
        }
    }
    
    // TODO get random loot
    public ItemStack getRandomLoot() {
        if (this.fillCount <= 0)
            return null;
        
        Chest chest = (Chest) Utilities.StringToLoc(chestLocation).getBlock().getState();
        List<Integer> filledSlots = Utilities.getFilledSlots(chest.getBlockInventory());
        
        // ratio of how full chest is to how many loons left to drop
        // higher number = drop items more urgently since we runnin out of loons
        float fillRatio = (float) filledSlots.size() / balloonsLeft;
        ItemStack i = Math.random() * 0.8 <= fillRatio ?
                chest.getBlockInventory().getContents()[filledSlots.get(Utilities.random(filledSlots.size() - 1))]
                        : null;
        
        if (i != null && i.getType() != Material.AIR) {
            i = i.clone();
            // if false, we need to drop the entire stack or we run outta loons
            if (i.getAmount() > 1 && filledSlots.size() < balloonsLeft) {
                // bias the RNG towards stack size as loon count gets lower
                int amt = i.getAmount();
                int ramt = (int) ((Math.random() * (1 - fillRatio)) + (amt * fillRatio));
                //amt *= Math.random() * (1 + fillRatio * fillRatio);
                if (ramt < 1) ramt = 1;
                else if (ramt > i.getAmount()) ramt = i.getAmount();
                
                i.setAmount(ramt);
            }
            chest.getBlockInventory().removeItem(i);
            this.updateInventoryProxy();
            --balloonsLeft;
            return i;
        }
        --balloonsLeft;
        return null;
    }
    
    public boolean attemptPull(Player puller) {
        if (!enabled) {
            puller.sendMessage(PartyRoom.PREFIX + ConfigMessage.NOT_ENABLED.getString(null));
            return false;
        }
        
        if (pulled || delayed) {
            puller.sendMessage(PartyRoom.PREFIX + ConfigMessage.ALREADY_DROPPING.getString(null));
            return false;
        }
        
        if (coolingdown) {
            long t = (dropCooldown - (System.currentTimeMillis() - time) / 1000);
            puller.sendMessage(PartyRoom.PREFIX + ConfigMessage.COOLING_DOWN.getString(t + " §fmore second" + (time == 1 ? "" : "s")));
            return false;
        }
        
        if (minSlots > 0) {
            int count = 0;
            for (ItemStack i : ((Chest) Utilities.StringToLoc(chestLocation).getBlock().getState()).getBlockInventory().getContents()) {
                if (i != null && i.getType() != Material.AIR)
                    count++;
            }
            if (count < minSlots) {
                puller.sendMessage(PartyRoom.PREFIX + ConfigMessage.NOT_FILLED_ENOUGH.getString("" + minSlots));
                return false;
            }
        }
        
        if (pcost != null && !pcost.has(puller)) {
            return false;
        }

        PartyRoomRegion PRoom = getProomRegion();
        if (PRoom == null)
            return false;

        pulled = true;
        balloonsLeft = count;
        PartyRoom.debug("Delaying drop party by " + dropDelay + " seconds...");
        delayedDrop(PRoom);
        return true;
    }
    
    private void delayedDrop(final PartyRoomRegion proom) {
        if (dropDelay > 0) {
            delayed = true;
            delayRunnable = new BukkitRunnable() {
                int elapsedTime;
                @Override
                public void run() {
                    if (++elapsedTime > dropDelay) {
                        cancel();
                        delayed = false;
                        dropBalloons(proom, Math.max(2, count));
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage(PartyRoom.PREFIX + getStartMessage());
                            p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.4F, 1.75F);
                        }
                        return;
                    }
                    if (announceDelay == 1 || (announceDelay > 0 && elapsedTime % announceDelay == 1)) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage(PartyRoom.PREFIX + getAnnounceMessage(dropDelay - elapsedTime + 1));
                        }
                    }
                }
            }.runTaskTimer(PartyRoom.getPlugin(), 5L, 20L);
        } else
            dropBalloons(proom, Math.max(2, count));
    }
    
    private void dropBalloons(final PartyRoomRegion p, final int amount) {
        this.balloonLocs = new HashSet<>();
        PartyRoom.getPlugin().handler.removeBalloons(this);
        PartyRoom.debug("Attempting to drop " + amount + " balloons in mode " + rtarget.toString() + "-" + ytarget.toString() + ": " + p.toString());
        final Chest chest = (Chest) Utilities.StringToLoc(chestLocation).getBlock().getState();
        checkChestFill(chest);
        
        if (dropCooldown > 0) {
            coolingdown = true;
            time = System.currentTimeMillis();
            coolingRunnable = new BukkitRunnable() {
                @Override
                public void run() {
                    coolingdown = false;
                }
            }.runTaskLaterAsynchronously(PartyRoom.getPlugin(), dropCooldown * 20);
        }
        dpRunnable = new BukkitRunnable() {
            int cycle = 0;
            @Override
            public void run() {
                if (++cycle > amount) {
                    cancel();
                    pulled = false;
                    return;
                }
                Location rloc = p.randomLocationConstrainY(ytarget);
                int j = 0;
                // try to find a valid location to drop 5 times
                while (j < 5 && (balloonLocs.contains(new SimpleLoc(rloc)) || rloc.getBlock().getType() != Material.AIR)) {
                    rloc = p.randomLocationConstrainY(ytarget);
                    ++j;
                }
                
                if (rloc.getBlock().getType() == Material.AIR && balloonLocs.add(new SimpleLoc(rloc))) {
                    rloc.getWorld().playSound(rloc, Sounds.ENTITY_CHICKEN_EGG.a(), 0.4F, 1.2F);
                    
                    PartyRoom.debug("Dropped balloon at " + Utilities.LocToString(rloc));
                    FallingBlock fe = chest.getWorld().spawnFallingBlock(rloc, blockType, blockData);
                    fe.setMetadata("partyroom", new FixedMetadataValue(PartyRoom.getPlugin(), chestLocation));
                } else {
                    PartyRoom.debug("Failed to drop balloon at " + Utilities.LocToString(rloc));
                    --balloonsLeft;
                }
            }
        }.runTaskTimer(PartyRoom.getPlugin(), 20L, 20L);
    }
    
    public void removeBalloon(Location loc) {
        balloonLocs.remove(new SimpleLoc(loc));
    }
    
    private void checkChestFill(Chest chest) {
        this.fill.clear();
        this.fillCount = 0;
        ItemStack[] arr = chest.getBlockInventory().getStorageContents();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null) {
                this.fill.put(i, arr[i].getAmount());
                ++this.fillCount;
            }
        }
        this.canDropEverything = this.fillCount >= this.count;
    }
    
    public boolean depositItem(InventoryInteractEvent e, ItemStack item) {
        boolean previouslyCancelled = e.isCancelled();
        e.setCancelled(true);
        if (item == null || (e instanceof InventoryClickEvent && ((InventoryClickEvent) e).getClick() == ClickType.DOUBLE_CLICK) || e.getInventory().firstEmpty() == -1)
            return false;
        
        Player p = (Player) e.getWhoClicked();
        
        if (previouslyCancelled && !p.hasPermission("partyroom.bypass")) {
            p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_DEPOSIT_CANCELLED.getString(null));
            p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
            return false;
        }

        if (!p.hasPermission("partyroom.deposit")) {
            p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_DEPOSIT_FAIL.getString(null));
            p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
            return false;
        }
        
        if (PartyRoom.getPlugin().handler.isBlacklisted(item, this)) {
            if (p.hasPermission("partyroom.bypass")) {
                p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_BLACKLIST_SUCCESS.getString(null));
                p.playSound(p.getLocation(), Sounds.ENTITY_ITEM_BREAK.a(), 0.4F, 1.8F);
            } else {
                p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_BLACKLIST_FAIL.getString(getName()));
                p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
                return false;
            }
        }
        if (isPulled()) {
            p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ALREADY_DROPPING.getString(null));
            p.playSound(p.getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.a(), 0.4F, 1.2F);
            return false;
        }
        
        if (stack()) {
            ItemStack overflow = item.clone();
            overflow.setAmount(a(e.getView().getTopInventory(), overflow.clone()));
            
            if (overflow.getAmount() > 0)
                for (ItemStack extra : p.getInventory().addItem(overflow).values())
                    p.getWorld().dropItem(p.getLocation(), extra);
            
        } else {
            for (ItemStack extra : getChest().getBlockInventory().addItem(item).values())
                for (ItemStack ovf : p.getInventory().addItem(extra).values())
                    p.getWorld().dropItem(p.getLocation(), ovf);
        }
        p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.4F, 0.7F);
        this.updateInventoryProxy();
        return true;
    }
    
    // returns overflow of adding an item in
    private int a(Inventory inv, ItemStack item) {
        int a = item.getAmount();
        int slot;
        // we have an empty slot to put it in
        if (item.getAmount() == 64 && (slot = inv.firstEmpty()) > -1) {
            inv.setItem(slot, item);
            return 0;
        }
        
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getContents()[i] == null)
                continue;
            ItemStack compare = inv.getContents()[i];
            if (compare.getAmount() < 64 && item.isSimilar(compare)) {
                int amount = compare.getAmount() + item.getAmount();
                a = amount - 64;
                compare.setAmount(Math.min(64, amount));
                
                if (a > 0) {
                    item.setAmount(a);
                    return a(inv, item);
                }
                break;
            }
        }
        
        if (a > 0 && (slot = inv.firstEmpty()) > -1) {
            inv.setItem(slot, item);
            return 0;
        }
        return a;
    }
    
    public void updateInventoryProxy() {
        this.proxy.setContents(((Chest) location.getBlock().getState()).getBlockInventory().getContents());
    }
    
    public void view(Player p) {
        p.openInventory(this.proxy);
    }
    
    public void forceStart() {
        if (delayed) {
            delayRunnable.cancel();
            delayed = false;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(PartyRoom.PREFIX + getStartMessage());
                p.playSound(p.getLocation(), Sounds.BLOCK_NOTE_PLING.a(), 0.4F, 1.75F);
            }
        }
        if (coolingdown) {
            coolingRunnable.cancel();
            coolingdown = false;
        }
        pulled = true;
        dropBalloons(getProomRegion(), Math.max(2, count));
    }
    
    public void forceStop() {
        if (delayRunnable != null) delayRunnable.cancel();
        if (dpRunnable != null) dpRunnable.cancel();
        if (coolingRunnable != null) coolingRunnable.cancel();
        pulled = false;
        delayed = false;
        coolingdown = false;
    }

}
