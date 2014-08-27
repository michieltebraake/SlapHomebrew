package me.naithantu.SlapHomebrew.Commands.Staff.ImprovedRegion;

import java.util.ArrayList;

import me.naithantu.SlapHomebrew.Commands.Exception.CommandException;
import me.naithantu.SlapHomebrew.Commands.Exception.UsageException;
import me.naithantu.SlapHomebrew.Controllers.PlayerLogging.RegionLogger;
import me.naithantu.SlapHomebrew.Controllers.PlayerLogging.RegionLogger.ChangeType;
import me.naithantu.SlapHomebrew.Controllers.PlayerLogging.RegionLogger.ChangerIsA;
import me.naithantu.SlapHomebrew.Util.Util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RemoveMemberCommand extends AbstractImprovedRegionCommand {

	private boolean all;
	
	public RemoveMemberCommand(Player p, String[] args, boolean all) {
		super(p, args);
		this.all = all;
	}


	@Override
	protected void action() throws CommandException {
		ProtectedRegion region;
		
		int firstMember = 1;
		boolean skipLastTwo = false;
		
		if (all) {
			if (args.length < 3) throw new UsageException("irg removemember <Region ID> <Member 1> [Member 2]..");
			validateRegionID(args[1]); //Check if a valid region
			region = getRegion(args[1]); //Get the region
			
			firstMember = 2;
		} else {
			if (args.length < 2) throw new UsageException("irg removemember <member>.. " + ChatColor.GRAY + "[-region <regionname>]");
			if (args[args.length - 2].toLowerCase().replaceAll("\\(?-?", "").equals("region")) { //Check if regionname given
				region = getRegion(args[args.length - 1]);
				skipLastTwo = true;
			} else {
				region = getHighestOwnedPriorityRegion(false);
			}
		}
		
		ArrayList<String> offPlayers = new ArrayList<>();
		for (int x = firstMember; x < (skipLastTwo ? args.length - 2 : args.length); x++) { //Get players
			offPlayers.add(getOfflinePlayer(args[x]).getCurrentName());
		}
		
		if (offPlayers.size() == 0) throw new CommandException("No players found!");
		
		DefaultDomain memberDomain = region.getMembers();
		for (String player : offPlayers) { //Get players
			if (memberDomain.contains(player)) {
				memberDomain.removePlayer(player);
			}
		}
		
		//Save & Msg
		saveChanges();
		hMsg("Removed " + ChatColor.RED + Util.buildString(offPlayers, ChatColor.YELLOW + ", " + ChatColor.RED) + ChatColor.YELLOW + " as members from region " + ChatColor.RED + region.getId());
		
		//Log
		RegionLogger.logRegionChange(region, p, (all ? ChangerIsA.staff : ChangerIsA.owner), ChangeType.removemember, Util.buildString(offPlayers, " "));
	}

}
