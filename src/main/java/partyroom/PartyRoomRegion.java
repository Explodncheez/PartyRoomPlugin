package partyroom;

import org.bukkit.Location;
import org.bukkit.World;

import partyroom.PartyChest.YSpawnTarget;

public class PartyRoomRegion {
	
	public PartyRoomRegion(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
		this.world = world;
		xMin = Math.min(x1, x2);
		xV = Math.max(x2, x1) - xMin;
		yMin = Math.min(y1, y2);
		yV = Math.max(y1, y2) - yMin;
		zMin = Math.min(z1, z2);
		zV = Math.max(z1, z2) - zMin;
	}
	
	private World world;
	private int xMin, xV, zMin, zV, yMin, yV;
	
	public World getWorld() {
		return world;
	}
	
	public int yMin() {
		return yMin;
	}

	public Location randomLocation() {
		return new Location(world, xMin + Utilities.random(xV) + 0.5, yMin + Utilities.random(yV), zMin + Utilities.random(zV) + 0.5);
	}
	
	public Location randomLocationConstrainY(YSpawnTarget target) {
		double ycoord;
		switch (target) {
		case MAX:
			ycoord = yMin + Math.max(0, yV - 1);
			break;
		case MIN:
			ycoord = yMin + 1;
			break;
		case DEFAULT:
			ycoord =  yMin + Math.min(8, Utilities.random(yV));
			break;
		case RANDOM:
		default:
			ycoord = 1 + (yMin - 1 + Utilities.random(yV));
			break;
		}
		return new Location(world, xMin + Utilities.random(xV) + 0.5, ycoord, zMin + Utilities.random(zV) + 0.5);
	}
	
	@Override
	public String toString() {
		return world.getName() + "," + xMin + "-" + (xMin + xV) + "," + yMin + "-" + (yMin + yV) + "," + zMin + "-" + (zMin + zV);
	}
}
