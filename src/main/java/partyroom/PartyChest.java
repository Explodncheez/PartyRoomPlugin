package partyroom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class PartyChest {
	
	public enum RegionTarget {
		RADIUS,
		REGION
	}
	
	private String chestLocation;
	private double cash;
	private int radius, count;
	private ProtectedCuboidRegion region;
	private RegionTarget target;
	private Material blockType;
	private byte blockData = 0;
	
	private Boolean pulled;
	
	public PartyChest(String chest, int ballooncount, Material blockType, byte blockData, double cashCostToPull, RegionTarget target, int radius, String region) {
		
		this.chestLocation = chest;
		this.cash = cashCostToPull;
		this.target = target;
		this.radius = radius;
		this.count = ballooncount;
		this.pulled = false;
		Block block = Utilities.StringToLoc(chestLocation).getBlock();
		
		if (blockType.isBlock()) {
			this.blockType = blockType;
			this.blockData = blockData;
		} else {
			Utilities.throwConsoleError(blockType.toString() + " is not a valid block!");
			this.blockType = Material.CAKE;
		}
		
		if (!region.isEmpty()) {
			ProtectedRegion PRegion = PartyRoom.getWG() == null ? null : PartyRoom.getWG().getRegionManager(block.getWorld()).getRegion(region);
			if (PRegion instanceof ProtectedCuboidRegion) {
				this.region = (ProtectedCuboidRegion) PRegion;
			} else {
				Utilities.throwConsoleError("PartyRoom Regions must be Cuboid and §c" + region + " §ris not!");
				this.target = RegionTarget.RADIUS;
			}
		}
		
		PartyRoomHandler.addPartyChest(this);
	}

	public double getCost() {
		return cash;
	}
	
	public int getRadius() {
		return radius;
	}
	
	public int getCount() {
		return count;
	}
	
	public String getRegion() {
		return region == null ? "" : region.getId();
	}
	
	public String getMaterial() {
		return blockType.toString() + ":" + blockData;
	}
	
	public String getChestString() {
		return chestLocation;
	}
	
	public Chest getChest() {
		return (Chest) Utilities.StringToLoc(chestLocation).getBlock().getState();
	}
	
	public String getRegionTarget() {
		return target.toString();
	}
	
	public boolean isPulled() {
		return pulled;
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
		if (pulled) {
			puller.sendMessage(PartyRoom.PREFIX + "There's already a Drop Party going on!");
			return false;
		}
		
		if (PartyRoom.getEcon() != null && cash > 0) {
			if (PartyRoom.getEcon().has(puller, cash)) {
				PartyRoom.getEcon().withdrawPlayer(puller, cash);
				puller.sendMessage(PartyRoom.PREFIX + "You pay §e$" + cash + " §rand start the Drop Party!");
			} else {
				puller.sendMessage(PartyRoom.PREFIX + "§cNot enough money! §fThis costs §e" + cash + "§f!");
				return false;
			}
		}

		pulled = true;
		PartyRoomRegion PRoom;
		Chest chest = (Chest) Utilities.StringToLoc(chestLocation).getBlock().getState();
		
		switch (target) {
			case REGION:
				BlockVector vMax = region.getMaximumPoint();
				BlockVector vMin = region.getMinimumPoint();
				PRoom = new PartyRoomRegion(chest.getWorld(), vMin.getBlockX(), vMin.getBlockY(), vMin.getBlockZ(), vMax.getBlockX(), vMax.getBlockY(), vMax.getBlockZ());
				break;
			default:
				Location c = chest.getLocation();
				PRoom = new PartyRoomRegion(chest.getWorld(), c.getBlockX() - radius, c.getBlockY(), c.getBlockZ() - radius, c.getBlockX() + radius, c.getBlockY() + radius, c.getBlockZ() + radius);
				break;
		}
		
		dropBalloons(PRoom, count);
		return true;
	}
	
	private void dropBalloons(final PartyRoomRegion p, final int amount) {
		final Chest chest = (Chest) Utilities.StringToLoc(chestLocation).getBlock().getState();
		new BukkitRunnable() {
			int cycle = 0;
			@SuppressWarnings("deprecation")
			public void run() {
				if (cycle > amount) {
					this.cancel();
					pulled = false;
					return;
				}
				Location rloc = p.randomLocationConstrainY(8).add(0, 2, 0);
				rloc.getWorld().playSound(rloc, Sound.CHICKEN_EGG_POP, 0.4F, 1.2F);
				
				if (rloc.getBlock().getType() == Material.AIR) {
					FallingBlock fe = chest.getWorld().spawnFallingBlock(rloc, blockType, blockData);
					fe.setMetadata("partyroom", new FixedMetadataValue(PartyRoom.getPlugin(), chestLocation));
				}
				
				cycle ++;
			}
		}.runTaskTimer(PartyRoom.getPlugin(), 20L, 20L);
	}

}
