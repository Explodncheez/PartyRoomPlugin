package partyroom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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

import partyroom.ConfigMessages.ConfigMessage;
import partyroom.gui.ChestEditor;
import partyroom.versions.SoundHandler.Sounds;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

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
		RANDOM(1, Utilities.ConstructItemStack(Material.EYE_OF_ENDER, 1, 0, "§c§lRANDOM", "§7Spawns at a random region height.")),
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
	
	private Map<String, HashSet<String>> blacklist;
	private ChestParticles particle;
	
	private boolean stack, delayed, pulled, enabled, coolingdown;
	private long time;
	
	public PartyChest(String chest, String chestName, boolean stack, int ballooncount, Material blockType, byte blockData, PullCost pcost, RegionTarget rtarget, YSpawnTarget ytarget, int dropDelay, int dropCooldown, int announceDelay, int minSlots, String announceMessage, String startMessage, int radius, String region, boolean enabled, Map<String, HashSet<String>> blacklist, ChestParticles particles) {
		
		this.chestLocation = chest;
		this.pcost = pcost;
		this.rtarget = rtarget;
		this.ytarget = ytarget;
		this.radius = radius;
		this.count = ballooncount;
		this.pulled = false;
		this.enabled = enabled;
		this.dropCooldown = dropCooldown;
		this.blacklist = blacklist == null ? new HashMap<String, HashSet<String>>() : blacklist;
		this.chestName = chestName;
		this.stack = stack;
		
		this.dropDelay = dropDelay;
		this.announceDelay = announceDelay;
		this.minSlots = minSlots;
		this.announceMessage = announceMessage.replace("&", "§");
		this.startMessage = startMessage.replace("&", "§");
		Block block = Utilities.StringToLoc(chestLocation).getBlock();
		
		this.particle = particles;
		
		setMaterial(blockType, blockData);
		
		if (region != null && !region.equals("''")) {
			ProtectedRegion PRegion = PartyRoom.getWG() == null ? null : PartyRoom.getWG().getRegionManager(block.getWorld()).getRegion(region);
			if (PRegion instanceof ProtectedCuboidRegion) {
				this.regionName = region;
			} else {
				Utilities.throwConsoleError("PartyRoom Regions must be Cuboid and §c" + region + " §ris not!");
				this.rtarget = RegionTarget.RADIUS;
			}
		}
		
		PartyRoom.getPlugin().handler.addPartyChest(this);
	}
	
	public ChestParticles getEnumParticle() {
		return particle;
	}
	
	public String getChestParticle() {
		return particle == null ? "none" : particle.toString();
	}
	
	public Map<String, HashSet<String>> getBlacklist() {
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
		ProtectedRegion region = PartyRoom.getWG().getRegionManager(Utilities.StringToLoc(chestLocation).getWorld()).getRegion(s);
		if (region != null && region instanceof ProtectedCuboidRegion) {
			regionName = s;
			return true;
		}
		return false;
	}
	
	public ProtectedCuboidRegion getWGRegion() {
		return (ProtectedCuboidRegion) PartyRoom.getWG().getRegionManager(Utilities.StringToLoc(chestLocation).getWorld()).getRegion(regionName);
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
			this.blockType = Material.CAKE_BLOCK;
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
				
				BlockVector vMax = region.getMaximumPoint();
				BlockVector vMin = region.getMinimumPoint();
				return new PartyRoomRegion(chest.getWorld(), vMin.getBlockX(), vMin.getBlockY(), vMin.getBlockZ(), vMax.getBlockX(), vMax.getBlockY(), vMax.getBlockZ());
			default:
				Location c = chest.getLocation();
				return new PartyRoomRegion(chest.getWorld(), c.getBlockX() - radius, c.getBlockY(), c.getBlockZ() - radius, c.getBlockX() + radius, c.getBlockY() + radius, c.getBlockZ() + radius);
		}
	}
	
	public ItemStack getRandomLoot() {
		Chest chest = (Chest) Utilities.StringToLoc(chestLocation).getBlock().getState();
		ItemStack i = chest.getBlockInventory().getContents()[Utilities.random(26)];
		if (i != null && i.getType() != Material.AIR) {
			i = i.clone();
			if (i.getAmount() > 1)
				i.setAmount(Utilities.random(i.getAmount() - 1) + 1);
			chest.getBlockInventory().removeItem(i);
			return i;
		}
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
					if (announceDelay == 1 || elapsedTime % announceDelay == 1) {
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
		PartyRoom.debug("Attempting to drop " + amount + " balloons in mode " + rtarget.toString() + "-" + ytarget.toString() + ": " + p.toString());
		final Chest chest = (Chest) Utilities.StringToLoc(chestLocation).getBlock().getState();
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
				rloc.getWorld().playSound(rloc, Sounds.ENTITY_CHICKEN_EGG.a(), 0.4F, 1.2F);
				
				if (rloc.getBlock().getType() == Material.AIR) {
					boolean b = false;;
					for (int i = 1; i <= rloc.getBlockY() - p.yMin(); i++) {
						if (rloc.getBlock().getRelative(0, -i, 0).getType().equals(blockType)) {
							b = true;
							break;
						}
					}
					if (!b) {
						PartyRoom.debug("Dropped balloon at " + Utilities.LocToString(rloc));
						FallingBlock fe = chest.getWorld().spawnFallingBlock(rloc, blockType, blockData);
						fe.setMetadata("partyroom", new FixedMetadataValue(PartyRoom.getPlugin(), chestLocation));
					}
				}
			}
		}.runTaskTimer(PartyRoom.getPlugin(), 20L, 20L);
	}
	
	public boolean depositItem(InventoryInteractEvent e, ItemStack item) {
	    boolean previouslyCancelled = e.isCancelled();
		e.setCancelled(true);
		if (item == null || (e instanceof InventoryClickEvent && ((InventoryClickEvent) e).getClick() == ClickType.DOUBLE_CLICK))
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
		return true;
	}
	
	private int a(Inventory inv, ItemStack item) {
		int a = item.getAmount();
		int slot;
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
