package partyroom;

import org.bukkit.Location;
import org.bukkit.World;

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

	public Location randomLocation() {
		return new Location(world, xMin + Utilities.random(xV), yMin + Utilities.random(yV), zMin + Utilities.random(zV));
	}
	
	public Location randomLocationConstrainY(int c) {
		return new Location(world, xMin + Utilities.random(xV), Math.min(c, yMin + Utilities.random(yV)), zMin + Utilities.random(zV));
	}
}
