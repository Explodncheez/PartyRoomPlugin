package partyroom;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PredicateItem {
    
    public static class InvalidPredicateException extends RuntimeException {
        private static final long serialVersionUID = -2134587775776183050L;
    }
    
    public PredicateItem(ItemStack i) {
        this(i.getType(), i.getDurability());
    }
    
    public PredicateItem(String s) throws InvalidPredicateException {
        try {
            String[] split = s.split(",");
            this.mat = Material.valueOf(split[0].toUpperCase());
            this.data = Integer.parseInt(split[1]);
        } catch (Exception e) {
            throw new InvalidPredicateException();
        }
    }
    
    public PredicateItem(Material mat, int data) {
        this(mat);
        this.data = data;
    }
    
    public PredicateItem(Material mat) {
        this.mat = mat;
    }
    
    private Material mat;
    private int data;
    
    public Material getMaterial() {
        return mat;
    }
    
    public short getData() {
        return (short) data;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof PredicateItem) {
            PredicateItem p = (PredicateItem) o;
            return p.mat == this.mat && (p.data == this.data || this.data < 0 || p.data < 0);
        }
        return false;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public int hashCode() {
        int hash = mat.getId() * 19 + 7;
        hash += data * 7 + 3;
        return hash;
    }
}
