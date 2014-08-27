package me.naithantu.SlapHomebrew.Commands.Jail;

import java.util.Arrays;
import java.util.List;

import me.naithantu.SlapHomebrew.PlayerExtension.UUIDControl;
import me.naithantu.SlapHomebrew.SlapHomebrew;
import me.naithantu.SlapHomebrew.Commands.AbstractCommand;
import me.naithantu.SlapHomebrew.Commands.Exception.CommandException;
import me.naithantu.SlapHomebrew.Commands.Exception.ErrorMsg;
import me.naithantu.SlapHomebrew.Commands.Exception.UsageException;
import me.naithantu.SlapHomebrew.Controllers.Jails;
import me.naithantu.SlapHomebrew.Util.Util;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JailCommand extends AbstractCommand {	
	
	public JailCommand(CommandSender sender, String[] args) {
		super(sender, args);
	}

	@Override
	public boolean handle() throws CommandException {
		testPermission("jail"); //Test permission
		if (args.length == 0) return false; //Check usage
		
		UUIDControl.UUIDProfile offPlayer;
		
		//Get jails controller
		Jails jails = plugin.getJails();
		
		switch (args[0].toLowerCase()) {
		case "list": //Send list of jails
			List<String> jailList = jails.getJailList();
			if (jailList.size() == 1) {
				hMsg("There is 1 jail: " + jailList.get(0));
			} else if (jailList.size() > 1) {
				hMsg("There are " + jailList.size() + " jails: " + Arrays.toString(jailList.toArray()) + ".");
			} else {
				throw new CommandException("There are no jails yet.");
			}
			break;
			
		case "create": //Create a new jail -> /Jail create [name] <chat allowed> <msg commands allowed>
			Player player = getPlayer();
			testPermission("jail.create");
			if (args.length <= 1) throw new UsageException("jail create [name] <chat allowed: true/false> <msg commands allowed: true/false>"); //Check usage
			
			boolean chatAllowed = false, msgAllowed = false; //Standard settings
			switch (args.length) { //Parse chat settings
			case 4: msgAllowed = Boolean.parseBoolean(args[3]);
			case 3:	chatAllowed = Boolean.parseBoolean(args[2]);
			}
			
			if (jails.jailExists(args[1].toLowerCase())) throw new CommandException("This jail already exists."); //Check if jail already exists
			
			jails.createJail(args[1].toLowerCase(), player.getLocation(), chatAllowed, msgAllowed); //Create the jail
			hMsg("Created a jail with name: " + args[1] + " | Chat: " + chatAllowed + " | Msg: " + msgAllowed);
			break;
			
		case "remove": //Remove a jail -> /jail remove [name]
			testPermission("jail.remove");
			if (args.length <= 1) throw new UsageException("jail remove [name]"); //Check usage
			
			if (jails.jailExists(args[1].toLowerCase())) { //Check if jail exists
				jails.deleteJail(args[1].toLowerCase()); //Remove jail
				hMsg("Jail removed.");
			} else {
				throw new CommandException(ErrorMsg.invalidJail);
			}
			break;
			
		case "info": //Get info about a jail sentence. Players who are able to jail are also able to get info -> /jail info [player]
			if (args.length != 2) throw new UsageException("jail info [player"); //Check usage
			offPlayer = getOfflinePlayer(args[1]); //Get player
			if (jails.isInJail(offPlayer.getCurrentName())) { //TODO Move to UUID
				jails.getJailInfo(sender, offPlayer.getCurrentName());
			} else {
				throw new CommandException(ErrorMsg.notInJail);
			}
			break;
			
		default: //Default = Jailing people -> /Jail [player] [jail] [time] [h/m/s] [reason]
			if (args.length <= 4) return false;
			
			offPlayer = getOfflinePlayer(args[0]); //Get the player
			String playername = offPlayer.getCurrentName();
			String jail = args[1].toLowerCase();
			
			if (jails.isInJail(playername)) throw new CommandException("Player is already jailed."); //Check if jailed
			if (Util.checkPermission(offPlayer.getUUID(), "jail.exempt")) throw new CommandException("This player cannot be jailed."); //Check if can be jailed
			if (!jails.jailExists(jail)) throw new CommandException(ErrorMsg.invalidJail); //Check if jail exists
			
			int time = parseInt(args[2]); //Parse the time
			long timeInJail;
			
			switch (args[3].toLowerCase()) { //Parse the format
			case "h": case "hour": case "hours":
				timeInJail = (long) (time * 1000 * 60 * 60); break;
			case "m": case "minute": case "min": case "minutes":
				timeInJail = (long) (time * 1000 * 60); break;
			case "s": case "seconds": case "sec": case "second":
				timeInJail = (long) (time * 1000); break;
			default:
				throw new CommandException("This is not a valid time type. Use: hours/minutes/seconds (h/m/s)");
			}
			if (timeInJail > 10800000) throw new CommandException("You cannot jail someone for so long."); //Max jail time = 3 hours
			
			String reason = Util.buildString(args, " ", 4); //Parse reason
			
			Player targetPlayer = plugin.getServer().getPlayer(playername);
			if (targetPlayer == null) { //Throw in offline jail
				jails.putOfflinePlayerInJail(playername, reason, jail, timeInJail);
				hMsg(playername + " will be jailed when he/she logs in.");
			} else { //Throw in online jail
				jails.putOnlinePlayerInJail(targetPlayer, reason, jail, timeInJail);
			}
		}
		return true;
	}
	
	/**
	 * TabComplete on this command
	 * @param sender The sender of the command
	 * @param args given arguments
	 * @return List of options
	 */
	public static List<String> tabComplete(CommandSender sender, String[] args) {
		if (!Util.testPermission(sender, "jail")) return null;
		
		if (args.length == 1) {
			//List all players (exclude sender if a player)
			String[] exclude = ((sender instanceof Player) ? new String[]{sender.getName()} : new String[]{});
			List<String> options = listAllPlayers(exclude);
			
			//Add options
			if (Util.testPermission(sender, "jail.remove")) options.add(0, "remove");
			if (Util.testPermission(sender, "jail.create")) options.add(0, "create");
			options.add(0, "list");
			options.add(0, "info");
			
			//Filter results
			filterResults(options, args[0]);
			return options;
		} else {
			switch (args[0].toLowerCase()) {
			case "create": case "info": case "list": //No futher usage for create or info
				return null;
				
			case "remove": //Return jails for remove
				if (args.length > 2) {
					return null;
				} else {
					List<String> list = SlapHomebrew.getInstance().getJails().getJailList(); //Get all jails
					filterResults(list, args[1]); //Filter the results
					return list;
				}
				
			default:
				switch (args.length) {
				case 2: //Playername given -> Jail name
					List<String> jails = SlapHomebrew.getInstance().getJails().getJailList();
					filterResults(jails, args[1]);
					return jails;
					
				case 3:  //Jail name given -> Add time
					if (args[2].isEmpty()) {
						return createNewList("3", "5", "8", "10", "15");
					} else {
						return null;
					}
					
				case 4: //Time given -> Add time formats
					List<String> formats = createNewList("sec", "seconds", "min", "minutes", "hour", "hours");
					filterResults(formats, args[3]);
					return formats;	
				}
			}
		}
		return null;
	}
	
}
