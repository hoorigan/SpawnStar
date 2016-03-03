package com.winterhaven_mc.spawnstar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


/**
 * Implements command executor for <code>CreativeNoNo</code> commands.
 * 
 * @author      Tim Savage
 * @version		1.0
 *  
 */
public class CommandManager implements CommandExecutor, TabCompleter {

	private final SpawnStarMain plugin; // reference to main class
	private ArrayList<String> enabledWorlds;

	/**
	 * constructor method for <code>CommandManager</code> class
	 * 
	 * @param plugin reference to main class
	 */
	CommandManager(SpawnStarMain plugin) {
		this.plugin = plugin;
		plugin.getCommand("spawnstar").setExecutor(this);
		updateEnabledWorlds();
	}

	/**
	 * Tab completer for SpawnStar
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		List<String> returnList = new ArrayList<String>();
		
		// return list of valid matching subcommands
		if (args.length == 1) {
			
			for (SubCommand subcmd : SubCommand.values()) {
				if (sender.hasPermission("spawnstar." + subcmd.toString()) 
						&& subcmd.toString().startsWith(args[0].toLowerCase())) {
					returnList.add(subcmd.toString());
				}
			}
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
			for (Player player : plugin.getServer().getOnlinePlayers()) {
				returnList.add(player.getName());
			}
		}
		return returnList;
	}


	/** command executor method for SpawnStar
	 * 
	 */
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		int maxArgs = 3;
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"command-args-count-over");
			if (sender.hasPermission("spawnstar.status")) {
				sender.sendMessage(ChatColor.AQUA + "/spawnstar status");
			}
			if (sender.hasPermission("spawnstar.reload")) {
				sender.sendMessage(ChatColor.AQUA + "/spawnstar reload");
			}
			if (sender.hasPermission("spawnstar.destroy")) {
				sender.sendMessage(ChatColor.AQUA + "/spawnstar destroy");
			}
			if (sender.hasPermission("spawnstar.give")) {
				sender.sendMessage(ChatColor.AQUA + "/spawnstar give <player> [amount]");
			}
			return true;
		}
		
		/*
		 * status command
		 */
		if (args.length < 1 || args[0].equalsIgnoreCase("status")) {
			
			// if command sender does not have permission to view status, output error message and return true
			if (!sender.hasPermission("spawnstar.status")) {
				plugin.messageManager.sendPlayerMessage(sender, "permission-denied-status");
				return true;
			}
			
			// output config settings
			String versionString = this.plugin.getDescription().getVersion();
			sender.sendMessage(ChatColor.DARK_AQUA + "[SpawnStar] " + ChatColor.AQUA + "Version: " + ChatColor.RESET + versionString);
			if (plugin.debug) {
				sender.sendMessage(ChatColor.DARK_RED + "DEBUG: true");
			}
			sender.sendMessage(ChatColor.GREEN + "Language: " + ChatColor.RESET + plugin.getConfig().getString("language"));
			sender.sendMessage(ChatColor.GREEN + "Item: " + ChatColor.RESET + SpawnStarUtilities.getStandard().getType().toString());
			sender.sendMessage(ChatColor.GREEN + "Data: " + ChatColor.RESET + SpawnStarUtilities.getStandard().getData().toString());
			sender.sendMessage(ChatColor.GREEN + "Minimum spawn distance: " + ChatColor.RESET + plugin.getConfig().getInt("minimum-distance"));
			sender.sendMessage(ChatColor.GREEN + "Warmup: " + ChatColor.RESET + plugin.getConfig().getInt("teleport-warmup") + " seconds");
			sender.sendMessage(ChatColor.GREEN + "Cooldown: " + ChatColor.RESET + plugin.getConfig().getInt("teleport-cooldown") + " seconds");
			sender.sendMessage(ChatColor.GREEN + "Shift-click required: " + ChatColor.RESET + plugin.getConfig().getBoolean("shift-click"));
			sender.sendMessage(ChatColor.GREEN + "Cancel on damage/movement/interaction: " + ChatColor.RESET + "[ "
					+ plugin.getConfig().getBoolean("cancel-on-damage") + "/"
					+ plugin.getConfig().getBoolean("cancel-on-movement") + "/"
					+ plugin.getConfig().getBoolean("cancel-on-interaction") + " ]");
			sender.sendMessage(ChatColor.GREEN + "Remove from inventory: " + ChatColor.RESET + plugin.getConfig().getString("remove-from-inventory"));
			sender.sendMessage(ChatColor.GREEN + "Allow in recipes: " + ChatColor.RESET + plugin.getConfig().getBoolean("allow-in-recipes"));
			sender.sendMessage(ChatColor.GREEN + "From nether: " + ChatColor.RESET + plugin.getConfig().getBoolean("from-nether"));
			sender.sendMessage(ChatColor.GREEN + "From end: " + ChatColor.RESET + plugin.getConfig().getBoolean("from-end"));
			sender.sendMessage(ChatColor.GREEN + "Lightning: " + ChatColor.RESET + plugin.getConfig().getBoolean("lightning"));
			sender.sendMessage(ChatColor.GREEN + "Enabled Words: " + ChatColor.RESET + getEnabledWorlds().toString());
			return true;
		}
		String subcmd = args[0];

		/*
		 *  reload command
		 */
		if (cmd.getName().equalsIgnoreCase("spawnstar") &&
				subcmd.equalsIgnoreCase("reload")) {
			
			// if sender does not have permission to reload config, send error message and return true
			if (!sender.hasPermission("spawnstar.reload")) {
				plugin.messageManager.sendPlayerMessage(sender, "permission-denied-reload");
				return true;
			}

			// reload main configuration
			plugin.reloadConfig();

			// update enabledWorlds field
			updateEnabledWorlds();
			
			// reload messages
			plugin.messageManager.reload();
			
			// set debug flag
			plugin.debug = plugin.getConfig().getBoolean("debug");
			
			// refresh reference item in case changes were made
			SpawnStarUtilities.setStandard(SpawnStarUtilities.createItem(1));
			
			// send reloaded message to command sender
			plugin.messageManager.sendPlayerMessage(sender, "command-success-reload");
			return true;
		}

		/*
		 *  give command
		 */
		if (cmd.getName().equalsIgnoreCase("spawnstar") && subcmd.equalsIgnoreCase("give")) {
			
			// if command sender does not have permission to give SpawnStars, output error message and return true
			if (!sender.hasPermission("spawnstar.give")) {
				plugin.messageManager.sendPlayerMessage(sender, "permission-denied-give");
				return true;
			}

			// if too few arguments, send error usage message
			if (args.length < 2) {
				plugin.messageManager.sendPlayerMessage(sender, "command-args-count-under");
				sender.sendMessage(ChatColor.AQUA + "/spawnstar give <player> [quantity]");
				return true;
			}
			
			// if too many arguments, send error and usage message
			if (args.length > 3) {
				plugin.messageManager.sendPlayerMessage(sender, "command-args-count-over");
				sender.sendMessage(ChatColor.AQUA + "/spawnstar give <player> [quantity]");
				return true;				
			}
			
			Player player = null;
			String playerName = "";
			int quantity = 1;

			if (args.length > 1) {
				playerName = args[1];
			}
			if (args.length > 2) {
				try {
					quantity = Integer.parseInt(args[2]);
				} catch (NumberFormatException e) {
					plugin.messageManager.sendPlayerMessage(sender, "command-fail-quantity-invalid");
					return true;
				}
			}
			
			// validate quantity (min = 1, max = configured maximum, or runtime Integer.MAX_VALUE)
			quantity = Math.max(1, quantity);
			int maxQuantity = plugin.getConfig().getInt("max-give-amount");
			if (maxQuantity < 0) {
				maxQuantity = Integer.MAX_VALUE;
			}
			quantity = Math.min(maxQuantity, quantity);

			// check exact match first
			player = plugin.getServer().getPlayer(playerName);
			
			// if no match, try substring match
			if (player == null) {
				List<Player> playerList = plugin.getServer().matchPlayer(playerName);
				
				// if only one matching player, use it, otherwise send error message (no match or more than 1 match)
				if (playerList.size() == 1) {
					player = playerList.get(0);
				}
			}
			
			// if match found, try to give item and return
			if (player != null) {
				// add specified quantity of spawnstar(s) to player inventory
				HashMap<Integer,ItemStack> noFit = player.getInventory().addItem(SpawnStarUtilities.createItem(quantity));
				
				// count items that didn't fit in inventory
				int noFitCount = 0;
				for (int index : noFit.keySet()) {
					noFitCount += noFit.get(index).getAmount();
				}
				
				// if remaining items equals quantity given, send player-inventory-full message and return
				if (noFitCount == quantity) {
					plugin.messageManager.sendPlayerMessage(sender, "command-fail-give-inventory-full", quantity);
					return true;
				}
				
				// subtract noFitCount from quantity
				quantity = quantity - noFitCount;
				
				// send success message to player
				plugin.messageManager.sendPlayerMessage(sender, "command-success-give", quantity);
				return true;
			}
			
			// check if name matches known offline player
			HashSet<OfflinePlayer> matchedPlayers = new HashSet<OfflinePlayer>();
			for (OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
				if (playerName.equalsIgnoreCase(offlinePlayer.getName())) {
					matchedPlayers.add(offlinePlayer);
				}
			}
			if (matchedPlayers.isEmpty()) {
				plugin.messageManager.sendPlayerMessage(sender, "command-fail-player-not-found");
				return true;
			}
			else {
				plugin.messageManager.sendPlayerMessage(sender, "command-fail-player-not-online");
				return true;
			}
		}
		
		/*
		 * destroy command
		 */
		if (cmd.getName().equalsIgnoreCase("spawnstar") && subcmd.equalsIgnoreCase("destroy")) {
			
			if (!(sender instanceof Player)) {
				plugin.messageManager.sendPlayerMessage(sender, "command-fail-destroy-console");
				return true;
			}
			
			if (!sender.hasPermission("spawnstar.destroy")) {
				plugin.messageManager.sendPlayerMessage(sender, "permission-denied-destroy");
				return true;
			}
			
			Player player = (Player) sender;
			ItemStack playerItem = player.getItemInHand();
			
			// check that player is holding a spawnstar stack
			if (!SpawnStarUtilities.getStandard().isSimilar(playerItem)) {
				plugin.messageManager.sendPlayerMessage(sender, "command-fail-destroy-no-match");
				return true;
			}
			int quantity = playerItem.getAmount();
			playerItem.setAmount(0);
			player.setItemInHand(playerItem);
			plugin.messageManager.sendPlayerMessage(sender, "command-success-destroy",quantity);			
			return true;
		}
		
		return false;
	}


	/**
	 * update enabledWorlds ArrayList field from config file settings
	 */
	void updateEnabledWorlds() {
		
		// copy list of enabled worlds from config into enabledWorlds ArrayList field
		this.enabledWorlds = new ArrayList<String>(plugin.getConfig().getStringList("enabled-worlds"));
		
		// if enabledWorlds ArrayList is empty, add all worlds to ArrayList
		if (this.enabledWorlds.isEmpty()) {
			for (World world : plugin.getServer().getWorlds()) {
				enabledWorlds.add(world.getName());
			}
		}
		
		// remove each disabled world from enabled worlds field
		for (String disabledWorld : plugin.getConfig().getStringList("disabled-worlds")) {
			this.enabledWorlds.remove(disabledWorld);
		}
	}
	
	
	/**
	 * get list of enabled worlds
	 * @return ArrayList of String enabledWorlds
	 */
	ArrayList<String> getEnabledWorlds() {
		return this.enabledWorlds;
	}

}
