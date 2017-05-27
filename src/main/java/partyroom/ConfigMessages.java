package partyroom;

import org.bukkit.ChatColor;

public class ConfigMessages {
	
	public static void load() {
		for (ConfigMessage msg : ConfigMessage.values()) {
			msg.setString(PartyRoom.getConfiguration().getString("messages." + msg.toString().toLowerCase().replaceAll("_", "-")));
		}
	}
	
	public enum ConfigMessage {
		
		WARN_DEPOSIT,
		ATTEMPT_WITHDRAW_FAIL,
		ATTEMPT_DEPOSIT_FAIL,
		ATTEMPT_DEPOSIT_CANCELLED,
		ATTEMPT_BLACKLIST_FAIL,
		ATTEMPT_BLACKLIST_SUCCESS,
		NOT_ENABLED,
		ALREADY_DROPPING,
		COOLING_DOWN,
		NOT_FILLED_ENOUGH,
		ATTEMPT_PAY_SUCCESS,
		ATTEMPT_PAY_FAIL;
		
		private String string;
		
		public void setString(String s) {
			string = ChatColor.translateAlternateColorCodes('&', s);
		}
		
		public String getString(String var) {
			return var == null ? string : string.replace("%VAR%", var);
		}
	
	}

}
