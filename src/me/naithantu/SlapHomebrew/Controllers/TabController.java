package me.naithantu.SlapHomebrew.Controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import me.naithantu.SlapHomebrew.Storage.YamlStorage;
import me.naithantu.SlapHomebrew.Util.Util;
import nl.stoux.slapbridged.bukkit.SlapBridged;
import nl.stoux.slapbridged.objects.OtherPlayer;
import nl.stoux.slapbridged.objects.OtherServer;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.mcsg.double0negative.tabapi.TabAPI;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class TabController extends AbstractController {

	//Fake max number of players
	private int maxPlayers;
	
	//Storage & Config
	private YamlStorage yaml;
	private FileConfiguration config;
	
	//TabAPI is available & setup
	private boolean tabApiSetup;
	
	//Tab sections in highest to lowest order
	private ArrayList<TabSection> tabSections;
	
	//Tab Sections map for easy access (Adding players etc).
	//K:[TabSection name] => V:[TabSection]
	private HashMap<String, TabSection> tabSectionsMap;
	
	//Players who are put in a different TabSection than they would standard go in.
	//K:[Playername (Lowercase)] => V:[TabSection name] 
	private HashMap<String, String> playerExceptions;
	
	/**
	 * Create a TabController
	 */
	public TabController() {
		//Initialize Storage
		yaml = new YamlStorage(plugin, "tabsettings");
		config = yaml.getConfig();
		
		//Get max players from config
		maxPlayers = config.getInt("maxplayers");
		
		//Check if TabAPI up & Running
		tabApiSetup = false;
		TabAPI tabApi = (TabAPI) plugin.getServer().getPluginManager().getPlugin("TabAPI");
		if (tabApi != null) {
			tabApiSetup = tabApi.isEnabled();
		}
		
		//If TabAPI not setup, return.
		if (!tabApiSetup) return;
		
		//Enable
		onEnable(false);
	}
	
	/**
	 * Load the TabSections from the Config
	 * Also loads Player exceptions
	 */
	private void loadTabSections() {
		//Get TabSection
		tabSections = new ArrayList<>();
		tabSectionsMap = new HashMap<>();
		
		//	=> Parse TabSection from config
		List<String> tabSectionNames = config.getStringList("TabSections");
		ConfigurationSection sectionConfig = config.getConfigurationSection("TabSection");
		if (sectionConfig == null || tabSectionNames.isEmpty()) { //If no section found. Add default group
			if (tabSectionNames.isEmpty()) { //Add a default name to the list of TabSections
				tabSectionNames = new ArrayList<String>();
				tabSectionNames.add("Default");
				config.set("TabSections", tabSectionNames);
			}
			
			if (sectionConfig == null) { //Add a default group
				config.set("TabSection.Default.header", "-- Default --");
				config.set("TabSection.Default.colorcode", "b");
			}
			
			//Save config
			yaml.saveConfig();
		} else { //Get all sections from Config
			for (String section : tabSectionNames) {
				String header = sectionConfig.getString(section + ".header"); //Get header
				String colorcodeString = sectionConfig.getString(section + ".colorcode"); //Get colorcode as string
				char colorcode = colorcodeString.charAt(0); //First char, as it should only be one char long.
				
				//Create TabSection and add to maps
				TabSection tabSection = new TabSection(section, header, colorcode);
				tabSections.add(tabSection);
				tabSectionsMap.put(tabSection.name.toLowerCase(), tabSection);
			}
		}
		
		//	=> Load player exceptions
		playerExceptions = new HashMap<>();
		ConfigurationSection exceptionSection = config.getConfigurationSection("PlayerExceptions");
		if (exceptionSection != null) { //Players given
			for (String player : exceptionSection.getKeys(false)) { //Loop thru players
				playerExceptions.put(player.toLowerCase(), exceptionSection.getString(player).toLowerCase()); //Put in map, lowercase both
			}
		}
	}
	
	/*
	 ************************** 
	 * Player/Group Functions *
	 **************************
	 */
	
	/**
	 * Get all available TabSections
	 * @return List with all the names of the TabSections
	 */
	public List<String> getTabSections() {
		List<String> sectionNames = new ArrayList<>();
		for (TabSection section : tabSections) {
			sectionNames.add(section.name);
		}
		return sectionNames;
	}
	
	/**
	 * Check if a TabSection exists
	 * @param tabsection The TabSection
	 * @return exists
	 */
	public boolean isTabSection(String tabsection) {
		return tabSectionsMap.containsKey(tabsection.toLowerCase());
	}
	
	/**
	 * Set the TabSection for a player.
	 * This method assumes the TabSection is an existing one.
	 * @param player The player
	 * @param tabSection The TabSection
	 */
	public void setTabSectionForPlayer(String player, String tabSection) {
		//To lowercase
		String playerLC = player.toLowerCase();
		String tabSectionLC = tabSection.toLowerCase();
		
		//Put in map
		playerExceptions.put(playerLC, tabSectionLC);
		
		//Save in config
		config.set("PlayerExceptions." + playerLC, tabSectionLC);
		yaml.saveConfig();
		
		//Check if player is online
		Player onlinePlayer = plugin.getServer().getPlayer(player);
		if (onlinePlayer != null) { //Player is online
			playerSwitchGroup(onlinePlayer); //Switch groups
		}
	}
	
	/**
	 * Get the TabSection a certain player is in
	 * @param player The player
	 * @return The name of the TabSection (appended with "Doesn't exist" if the TabSection doesn't exist) or null if the player isn't in a TabSection.
	 */
	public String getTabSectionForPlayer(String player) {
		//To lowercase
		String playerLC = player.toLowerCase();
		if (playerExceptions.containsKey(playerLC)) { //See if the player has an exception
			String tabSection = playerExceptions.get(playerLC); //Get the tabsection
			//Check if the TabSection exists
			if (tabSectionsMap.containsKey(tabSection)) {
				return tabSectionsMap.get(tabSection).name;
			} else {
				return tabSection + " (TabSection doesn't exist)";
			}
			
		} else { //Else return null
			return null;
		}
	}
	
	/**
	 * Remove a player from a TabSection
	 * @param player The playername
	 * @return is removed (not removed when not in a tab section)
	 */
	public boolean removeTabSectionForPlayer(String player) {
		//To lowercase
		String playerLC = player.toLowerCase();
		if (playerExceptions.containsKey(playerLC)) { //Check if indeed in a tabsection
			//Remove from map & yaml
			playerExceptions.remove(playerLC);
			config.set("PlayerExceptions." + playerLC, null);
			yaml.saveConfig();
			
			//Check if player is online
			Player onlinePlayer = plugin.getServer().getPlayer(player);
			if (onlinePlayer != null) { //Player is online
				playerSwitchGroup(onlinePlayer); //Switch groups
			}
			return true;
		} else {
			return false;
		}
	}
		
	/**
	 * Add a player to the TabSections
	 * @param playername The name of the player
	 */
	private void addToGroup(String playername) {
		String playernameLC = playername.toLowerCase(); //To lowercase
		if (playerExceptions.containsKey(playernameLC)) { //See if the player has a group exception
			String targetGroup = playerExceptions.get(playernameLC); //Get the target group
			if (tabSectionsMap.containsKey(targetGroup)) { //See if group exists
				tabSectionsMap.get(targetGroup).players.add(playername); //Add to players
			}
		} else { //Not an exception. Default behaviour based on rank
			PermissionUser user = PermissionsEx.getUser(playername);
			if (user != null) { //User exists
				PermissionGroup[] groups = user.getGroups(); //Get groups
				if (groups != null && groups.length > 0) { //Has a group
					String targetGroup = groups[0].getName().toLowerCase(); //Get name => To LC
					if (tabSectionsMap.containsKey(targetGroup)) { //TabSection exists
						tabSectionsMap.get(targetGroup).players.add(playername);
					}
				}
			}
			
		}
	}
	
	/**
	 * Remove a player from a group
	 * @param playername The name of the player
	 */
	private void removeFromGroup(String playername) {
		//Loop thru the TabSections to find the player
		for (TabSection section : tabSections) {
			if (section.players.remove(playername)) { //If the player has been removed (means they were in this group)
				return; //Stop looping
			}
		}
	}
	
	/**
	 * A player switches TabSections
	 * @param player The player
	 */
	public void playerSwitchGroup(Player player) {
		if (!tabApiSetup) return; //TabAPI not setup
		
		//Remove & Re-Add to groups
		String playername = player.getName();
		removeFromGroup(playername);
		addToGroup(playername);
		
		//Refresh Tab
		refreshTab();
	}
	
	/*
	 *****************
	 * Tab functions *
	 *****************
	 */
	
	private class TabSection {
		
		private String name; //Name of the TabSection
		private String header; //Header at the top of the TabSection
		private ChatColor prependColor; //The color add in-front of all the entries
		private TreeSet<String> players; //All players in this section
		
		/**
		 * Create a TabSection
		 * @param name The name of the TabSection
		 * @param header The header at the top of the TabSection
		 * @param colorcode The ChatColor code. (Example: 'b')
		 */
		public TabSection(String name, String header, char colorcode) {
			this.name = name;
			this.header = header;
			this.prependColor = ChatColor.getByChar(colorcode);
			players = new TreeSet<>();
		}
	}
	
	/**
	 * Create the Tab & update it for all players
	 */
	private void createTab() {
		//Establish tabsize
		int tabSize = 0;
		
		//	=> Loop thru TabSections on this server
		for (TabSection section : tabSections) {
			if (!section.players.isEmpty()) {
				tabSize += section.players.size() + 1;
			}
		}
		
		//	=> If SlapBridged, also add other servers
		List<OtherServer> otherServers = null;
		int otherServersSize = 0;
		if (plugin.hasSlapBridged()) { //See if API available
			if ((otherServersSize = SlapBridged.getAPI().getOtherServers().size()) > 0) {
				tabSize++;
			}
			
			otherServers = SlapBridged.getAPI().getOtherServers(); //Get other servers
			for (OtherServer server : otherServers) { //Loop thru servers 
				if (server.getNrOfPlayersOnline() > 0) { //If any players online
					tabSize += server.getNrOfPlayersOnline() + 1;
				} else {
					tabSize += 2; //Name + "No players"
				}
			}
		}
		
		if (tabSize == 0) return; //Nothing to show | Should be impossible
		
		//Get total number of players online
		int playersOnline = Util.getOnlinePlayers().length;
		if (plugin.hasSlapBridged()) { //If API available
			playersOnline += SlapBridged.getAPI().getTotalPlayersOnline();
		}
		
		//Create tab array
		int x = 0;
		String[] tab = new String[tabSize];
		
		//	=> Add players from this server
		for (TabSection section : tabSections) {
			if (section.players.isEmpty()) continue; //Skip this section if no players in it. 
			
			tab[x++] = section.prependColor + section.header; //Set header 
			for (String player : section.players) { //Add players
				tab[x++] = section.prependColor + player;
			}
		}
		
		//	=> Add other servers
		if (otherServers != null) {
			if (otherServersSize > 0) { //Add a space between the servers
				tab[x++] = ChatColor.WHITE + "     " + ChatColor.WHITE;
			}
			
			//Loop thru Servers
			for (OtherServer otherServer : otherServers) {
				tab[x++] = Util.colorize(otherServer.getTabName()); //Set Server name
				String color = Util.colorize(otherServer.getTabName().substring(0, 2)); //Get color
				if (otherServer.getNrOfPlayersOnline() > 0) { //If players online
					for (OtherPlayer p : otherServer.getPlayers().values()) {
						tab[x++] = color + p.getPlayername();
					}
				} else {
					tab[x++] = color + "No players";
				}
			}
		}
		
		//Update tab for all players
		for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
			updateTab(onlinePlayer, tab, playersOnline, (maxPlayers > playersOnline ? maxPlayers : maxPlayers * 2));
		}
		TabAPI.updateAll();
	}
	
	/**
	 * Update the Tab for a player
	 * @param p The player
	 * @param tab The tab with strings
	 * @param playersOnline The number of players online
	 * @param maxPlayers The maximum number of players online
	 */
	private void updateTab(Player p, String[] tab, int playersOnline, int maxPlayers) {
		TabAPI.clearTab(p); //Clear previous tab settings
		TabAPI.setPriority(plugin, p, 0); //Set priority to default
		
		//Set opening text
		TabAPI.setTabString(plugin, p, 0, 0, ChatColor.GOLD + "Welcome to");
		TabAPI.setTabString(plugin, p, 0, 1, ChatColor.GOLD + "SLAPGaming");
		TabAPI.setTabString(plugin, p, 0, 2, ChatColor.GOLD + "Online " + playersOnline + "/" + maxPlayers);
		
		//Loop thru tab strings
		int row = 2; int colom = 0;
		for (String player : tab) {
			TabAPI.setTabString(plugin, p, row, colom, player);
			row++;
			if (row == 20) {
				row = 2;
				colom++;
				if (colom == 3) { //There are no 3 coloms!
					return;
				}
			}
		}
	}
	
	/**
	 * Refresh the Tab with a 2 tick delay
	 */
	private void refreshTab() {
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				createTab();
			}
		}, 2);
	}

	
	
	/*
	 ********** 
	 * Events *
	 **********
	 */
	
	/**
	 * Player joins the server
	 * @param p The player
	 */
	public void playerJoin(Player p) {
		if (!tabApiSetup) return; //TabAPI not setup
		
		//Add to group & refresh
		addToGroup(p.getName());
		refreshTab();
	}
	
	/**
	 * Player quits the server
	 * @param p The player
	 */
	public void playerQuit(Player p) {
		if (!tabApiSetup) return; //TabAPI not setup
		
		//Remove from group & refresh
		removeFromGroup(p.getName());
		refreshTab();
	}
	
	/**
	 * Other server Tab Activity
	 */
	public void otherServerActivity() {
		if (!tabApiSetup) return; //Return if not setup
		refreshTab(); //Refresh tab
	}
	
	/**
	 * Enable the TabSections
	 * Loads tab sections, followed by looping thru players
	 * @param reloadConfig Reload the config before enabling
	 */
	public void onEnable(boolean reloadConfig) {
		if (reloadConfig) {
			yaml.reloadConfig();
			config = yaml.getConfig();
		}
		
		loadTabSections();
		
		//Add online players
		Player[] players = Util.getOnlinePlayers();
		if (players.length > 0) {
			for (Player player : players) { //Loop thru players
				addToGroup(player.getName()); //Add to group
			}
		}
	}
	
	@Override
	public void shutdown() {
		
	}
	
	/*
	 *********************
	 * Max Players Stuff *
	 *********************
	 */
	
	/**
	 * Set the max number of players in TAB & /List
	 * @param players The number of players
	 */
	public void setMaxPlayers(int players) {
		maxPlayers = players;
		config.set("maxplayers", players);
		yaml.saveConfig();
	}
	
	/**
	 * Get the max amount of players
	 * @return the number of players
	 */
	public int getMaxPlayers() {
		return maxPlayers;
	}
	
	

}
