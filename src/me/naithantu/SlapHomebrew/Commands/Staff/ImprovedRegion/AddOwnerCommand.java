package me.naithantu.SlapHomebrew.Commands.Staff.ImprovedRegion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import me.naithantu.SlapHomebrew.Commands.Exception.CommandException;
import me.naithantu.SlapHomebrew.Commands.Exception.UsageException;
import me.naithantu.SlapHomebrew.Controllers.PlayerLogging.RegionLogger;
import me.naithantu.SlapHomebrew.Controllers.PlayerLogging.RegionLogger.ChangeType;
import me.naithantu.SlapHomebrew.Controllers.PlayerLogging.RegionLogger.ChangerIsA;
import me.naithantu.SlapHomebrew.Util.Util;

import nl.stoux.SlapPlayers.Model.Profile;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class AddOwnerCommand extends AbstractImprovedRegionCommand {

	public AddOwnerCommand(Player p, String[] args) {
		super(p, args);
	}

	@Override
	protected void action() throws CommandException {
		if (args.length < 3) throw new UsageException("irg addowner <Region ID> <Owner 1> [Owner 2].."); //Usage
		
		validateRegionID(args[1]); //Check if a valid region
		ProtectedRegion region = getRegion(args[1]); //Get the region
		
		ArrayList<Profile> offPlayers = new ArrayList<>();
		for (int x = 2; x < args.length; x++) { //Get players
			offPlayers.add(getOfflinePlayer(args[x]));
		}

        //Check if any players added
		if (offPlayers.size() == 0) throw new CommandException("No players found!");

        HashSet<String> playernames = new HashSet<>();
        //Add the owners
		DefaultDomain owners = region.getOwners();
		for (Profile player : offPlayers) {
            UUID playerUUID = UUID.fromString(player.getUUIDString());
			if (!owners.contains(playerUUID)) {
                owners.addPlayer(playerUUID);
                playernames.add(player.getCurrentName());
			}
		}
		
		//Save & Msg
		saveChanges();
		hMsg("Added " + ChatColor.RED + Util.buildString(playernames, ChatColor.YELLOW + ", " + ChatColor.RED) + ChatColor.YELLOW + " as owners to region " + ChatColor.RED + region.getId());
		
		//Log
		RegionLogger.logRegionChange(region, p, ChangerIsA.staff, ChangeType.addowner, Util.buildString(playernames, " "));
	}

}
