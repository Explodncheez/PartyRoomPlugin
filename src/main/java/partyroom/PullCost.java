package partyroom;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import partyroom.ConfigMessages.ConfigMessage;

public class PullCost {
	
	private static final DecimalFormat df = new DecimalFormat("0.00");
	
	public PullCost() {
		this.items = new HashSet<ItemStack>(0);
		this.raw = "0";
		this.cash = 0;
	}
	
	public PullCost(String items) {
		this.items = new HashSet<ItemStack>();
		this.raw = items;
		reload(items);
	}
	
	private String raw;
	private double cash;
	private Set<ItemStack> items;

	public void reload(String items) {
		this.items.clear();
		this.raw = items;
		this.cash = 0;
		
		for (String s : items.split(" ")) {
			if (Utilities.isNum(s))
				cash += Double.parseDouble(s);
			else {
				try {
					String[] parts = s.split(":");
					Material m = Material.valueOf(parts[0].toUpperCase());
					
					short data = Short.parseShort(parts[1]);
					int amount = Integer.parseInt(parts[2]);
					ItemStack i = new ItemStack(m, amount, data);
					
					this.items.add(i);
					
				} catch (Exception e) {
					
					Bukkit.getLogger().info("**** Error reading value: " + s + " in Pull Cost.");
				}
			}
		}
	}
	
	public boolean has(Player p) {
		if (PartyRoom.getEcon() != null && cash > 0) {
			if (!PartyRoom.getEcon().has(p, cash)) {
				p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_PAY_FAIL.getString(toReadableString()));
				return false;
			}
		}
		
		if (!items.isEmpty())
			for (ItemStack i : items) {
				if (!p.getInventory().containsAtLeast(i, i.getAmount())) {
					p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_PAY_FAIL.getString(toReadableString()));
					return false;
				}
			}

		if (PartyRoom.getEcon() != null && cash > 0) {
			PartyRoom.getEcon().withdrawPlayer(p, cash);
		}
		
		if (!items.isEmpty()) {
			for (ItemStack i : items) {
				p.getInventory().removeItem(i);
				p.updateInventory();
			}
		}
		
		String payment = toReadableString();
		if (!payment.isEmpty())
		    p.sendMessage(PartyRoom.PREFIX + ConfigMessage.ATTEMPT_PAY_SUCCESS.getString(payment));
		return true;
	}
	
	public String toReadableString() {
		String s = "";
		if (cash > 0)
			s += df.format(cash) + ", ";
		for (ItemStack i : items) {
			s += i.getAmount() + " x " + i.getType().toString().toLowerCase().replace("_", " ") + ", ";
		}
		
		return s.isEmpty() ? "" : s.substring(0, s.length() - 2);
	}
	
	@Override
	public String toString() {
		return raw;
	}
	
}
