package partyroom;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import partyroom.PartyChest.RegionTarget;
import partyroom.gui.ChestEditor;

public class CommandHandler implements CommandExecutor {
	
	String[] help = {
		"§f§m================================",
		"§6§l<<==>> §e§lParty Room v2.0 §6§l<<==>>",
		"",
		"§eInspired by §aRunescape's Falador Party Room§e!",
		"§fDeposit items into §bParty Chests§f and pull a §bLever",
		"§fwithin 1 block of the chest to start a §bBalloon Drop§f!",
		"§fWhen popped, balloons may contain items from the",
		"§fChest! Any items from balloons can only be picked",
		"§fup by the person who popped them for 60 seconds.",
		"",
		"§c§lWarning: §rAny items put into the Chest §c§lCANNOT",
		"§fbe withdrawn again! Be careful what you click!",
		"§f§m================================"
	};
	
	String[] commands = {
		"§f§m================================",
		"§2§lCommands:",
		"§a/proom create: §fUse when looking at any",
		"§f§lsingle chest §rto create a Party Chest.",
		"§a/proom remove: §fUse when looking at any",
		"Party Chest to remove it.",
		"§a/proom help: §fShow help text",
		"§a/proom commands: §fShows this screen",
		"§a/proom reload: §fReload data from config",
		"§a/proom save: §fSave data to config"
	};

	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (cmd.equalsIgnoreCase("proom")) {
			if (args.length > 0) {
				String s = args[0];
				if (s.equalsIgnoreCase("create") && sender instanceof Player) {
					Player p = (Player) sender;
					if (!p.hasPermission("partyroom.create")) {
						p.sendMessage(PartyRoom.PREFIX + "You do not have permission to create or remove Party Chests!");
						p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
						return true;
					}
					Block block = p.getTargetBlock((Set<Material>) null, 8);
					if (block == null) {
						p.sendMessage(PartyRoom.PREFIX + "Use this command while looking at a Chest to make a Party Chest.");
						p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
						return true;
					}
					if (block.getState() instanceof Chest) {
						Chest chest = (Chest) block.getState();
						if (!PartyRoomHandler.isPartyChest(chest)) {
							new PartyChest(Utilities.LocToString(chest.getLocation()), 20, Material.CAKE_BLOCK, (byte) 0, 0, RegionTarget.RADIUS, 10, "", false);
							LoaderAndSaver.saveChests(PartyRoom.getConfiguration());
							p.sendMessage(PartyRoom.PREFIX + "§eTurned that Chest into a Party Chest!");
							p.sendMessage(PartyRoom.PREFIX + "Edit it in §eplugin config §for via §eSneak+RightClick§f.");
							p.sendMessage(PartyRoom.PREFIX + "Don't forget to place a Lever near the Chest!");
							p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.4F, 1.4F);
						} else {
							p.sendMessage(PartyRoom.PREFIX + "The chest you are looking at is already a Party Chest!");
							p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
						}
					} else {
						p.sendMessage(PartyRoom.PREFIX + "Look at any Single-Chest and use §e/proom create§f to create a Party Chest!");
						p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
					}
					return true;
				}
				if (s.equalsIgnoreCase("remove") && sender instanceof Player) {
					Player p = (Player) sender;
					if (!p.hasPermission("partyroom.create")) {
						p.sendMessage(PartyRoom.PREFIX + "You do not have permission to create or remove Party Chests!");
						p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
						return true;
					}
					Block block = p.getTargetBlock((Set<Material>) null, 8);
					if (block == null) {
						p.sendMessage(PartyRoom.PREFIX + "Use this command while looking at a Party Chest to remove it.");
						p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
						return true;
					}
					if (block.getState() instanceof Chest) {
						Chest chest = (Chest) block.getState();
						if (PartyRoomHandler.isPartyChest(chest)) {
							PartyRoomHandler.removePartyChest(PartyRoomHandler.getPartyChest(Utilities.LocToString(chest.getLocation())));
							p.sendMessage(PartyRoom.PREFIX + "Removed Party Chest!");
							p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.4F, 0.8F);
							PartyRoom.getConfiguration().set("party-chests." + Utilities.LocToString(chest.getLocation()), null);
							PartyRoom.getPlugin().saveConfig();
						} else {
							p.sendMessage(PartyRoom.PREFIX + "The chest you are looking at is not a Party Chest!");
							p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
						}
					} else {
						p.sendMessage(PartyRoom.PREFIX + "The block you are looking at is not a Party Chest!");
						p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
					}
					return true;
				}
				if (s.equalsIgnoreCase("help")) {
					if (!sender.hasPermission("partyroom.help")) {
						sender.sendMessage(PartyRoom.PREFIX + "You do not have permission to view Help Text!");
						if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
						return true;
					}
					for (String helptext : help) {
						sender.sendMessage(helptext);
					}
					return true;
				}
				if (s.equalsIgnoreCase("commands")) {
					if (!sender.hasPermission("partyroom.commands")) {
						sender.sendMessage(PartyRoom.PREFIX + "You do not have permission to view Commands!");
						if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.2F);
						return true;
					}
					for (String helptext : commands) {
						sender.sendMessage(helptext);
					}
					return true;
				}
				if (s.equalsIgnoreCase("reload") && sender.isOp()) {
					PartyRoom.reloadConfiguration();
					PartyRoomHandler.clear();
					LoaderAndSaver.loadChests(PartyRoom.getConfiguration());
					sender.sendMessage(PartyRoom.PREFIX + "Reloaded Party Chests from Configuration!");
					ChestEditor.removeAllEditors();
					return true;
				}
				if (s.equalsIgnoreCase("save") && sender.isOp()) {
					LoaderAndSaver.saveChests(PartyRoom.getConfiguration());
					sender.sendMessage(PartyRoom.PREFIX + "Saved Party Chests to Configuration!");
					return true;
				}
			}
		}
		return false;
	}

}
