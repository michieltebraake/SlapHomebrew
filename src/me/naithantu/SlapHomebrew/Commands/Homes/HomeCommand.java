package me.naithantu.SlapHomebrew.Commands.Homes;

import java.util.List;

import me.naithantu.SlapHomebrew.Commands.AbstractCommand;
import me.naithantu.SlapHomebrew.Commands.Exception.CommandException;
import me.naithantu.SlapHomebrew.Controllers.Homes;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand extends AbstractCommand {
	
	private Homes homes;
	
	public HomeCommand(CommandSender sender, String[] args) {
		super(sender, args);
	}

	public boolean handle() throws CommandException {
		Player p = getPlayer(); //Player
		testPermission("home"); //Perm
		String playername = p.getName();

		homes = plugin.getHomes(); //Get Homes
		
		if (args.length > 0) { //If homename given
			if (args[0].equalsIgnoreCase("list")) { //If list
				new HomesCommand(p, args).handle();
			} else { //Teleport to home with name
				p.teleport(homes.getHome(playername, args[0]));
			}
		} else { //If no homename given
			List<String> homeList = homes.getHomes(p.getName()); //Get homes
			int homelistSize = homeList.size(); //Check number of homes set
			if (homelistSize == 0) { //No homes set, move to /homes
				new HomesCommand(p, args).handle();
				return true;
			}
			int homesAllowed = homes.getTotalNumberOfHomes(playername); //Get number of allowed homes
			if (homesAllowed == 1 && homelistSize == 1) { //If only 1 home set, and 1 home allowed
				p.teleport(homes.getHome(playername, homeList.get(0))); //Teleport to only home
			} else { //Multiple homes allowed
				new HomesCommand(p, args).handle();
			}
		}
		return true;
	}	
	
}
