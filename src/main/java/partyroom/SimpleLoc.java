package partyroom;

import org.bukkit.Location;

public class SimpleLoc {
    
    public SimpleLoc(Location loc) {
        this.x = loc.getBlockX();
        this.z = loc.getBlockZ();
    }
    
    private int x, z;
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = hash * 31 + x;
        hash = hash * 31 + z;
        return hash;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof SimpleLoc) {
            SimpleLoc loc = (SimpleLoc) o;
            return loc.x == this.x && loc.z == this.z;
        }
        return false;
    }

}
