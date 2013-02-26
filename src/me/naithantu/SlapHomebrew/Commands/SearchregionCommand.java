package me.naithantu.SlapHomebrew.Commands;

import me.naithantu.SlapHomebrew.SlapHomebrew;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SearchregionCommand extends AbstractCommand {
	public SearchregionCommand(CommandSender sender, String[] args, SlapHomebrew plugin) {
		super(sender, args, plugin);
	}

	public boolean handle() {
		if (!testPermission(sender, "searchregion")) {
			this.noPermission(sender);
			return true;
		}

		String arg = null;
		if (args.length == 1) {
			arg = args[0].toLowerCase();
			sender.sendMessage(ChatColor.DARK_AQUA + "Region changes for region " + arg + ":");
			if (SlapHomebrew.worldGuard.containsKey(arg)) {
				String[] worldGuardString = SlapHomebrew.worldGuard.get(arg).split("<==>");
				for (int i = 0; i < worldGuardString.length; i++) {
					sender.sendMessage(ChatColor.GOLD + worldGuardString[i]);
				}
			} else {
				this.badMsg(sender, "No results found.");
			}
		} else {
			return false;
		}

		return true;
	}
}
