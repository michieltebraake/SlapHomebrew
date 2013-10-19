package me.naithantu.SlapHomebrew.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import me.naithantu.SlapHomebrew.SlapHomebrew;
import me.naithantu.SlapHomebrew.Controllers.Flag;
import me.naithantu.SlapHomebrew.Storage.YamlStorage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Util {
	public static String getHeader() {
		return ChatColor.GOLD + "[SLAP] " + ChatColor.WHITE;
	}

	public static void dateIntoTimeConfig(String date, String message, YamlStorage timeStorage) {
		FileConfiguration timeConfig = timeStorage.getConfig();
		int i = 1;
		while (timeConfig.contains(date)) {
			date += " (" + i + ")";
			i++;
		}
		timeConfig.set(date, message);
		timeStorage.saveConfig();
	}

	public static boolean hasFlag(SlapHomebrew plugin, Location location, Flag flag) {
		RegionManager regionManager = plugin.getworldGuard().getRegionManager(location.getWorld());
		ApplicableRegionSet regions = regionManager.getApplicableRegions(location);
		for (ProtectedRegion region : regions) {
			for (String string : region.getMembers().getPlayers()) {
				if (string.startsWith("flag:" + flag.toString().toLowerCase()))
					return true;
			}
		}
		return false;
	}

	public static String getFlag(SlapHomebrew plugin, Location location, Flag flag) {
		RegionManager regionManager = plugin.getworldGuard().getRegionManager(location.getWorld());
		ApplicableRegionSet regions = regionManager.getApplicableRegions(location);
		for (ProtectedRegion region : regions) {
			for (String string : region.getMembers().getPlayers()) {
				if (string.startsWith("flag:" + flag.toString().toLowerCase()))
					return string;
			}
		}
		return null;
	}
	
	public static List<Flag> getFlags(SlapHomebrew plugin, Location location) {
		List<Flag> flags = new ArrayList<Flag>();
		RegionManager regionManager = plugin.getworldGuard().getRegionManager(location.getWorld());
		ApplicableRegionSet regions = regionManager.getApplicableRegions(location);
		for (ProtectedRegion region : regions) {
			for (String string : region.getMembers().getPlayers()) {
				if (string.startsWith("flag:")) {
					String flagName = string.replaceFirst("flag:", "");
					try {
						Flag flag = Flag.valueOf(flagName.toLowerCase());
						flags.add(flag);
					} catch (IllegalArgumentException e) {
					}
				}
			}
		}
		return flags;
	}


	public static boolean hasEmptyInventory(Player player) {
		Boolean emptyInv = true;
		PlayerInventory inv = player.getInventory();
		for (ItemStack stack : inv.getContents()) {
			//TODO Is try - catch really required here?
			try {
				if (stack.getType() != (Material.AIR)) {
					emptyInv = false;
				}
			} catch (NullPointerException e) {
			}
		}
		for (ItemStack stack : inv.getArmorContents()) {
			try {
				if (stack.getType() != (Material.AIR)) {
					emptyInv = false;
				}
			} catch (NullPointerException e) {
			}
		}
		return emptyInv;
	}

	public static void broadcastToWorld(String worldName, String message) {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.getWorld().getName().equalsIgnoreCase(worldName)) {
				player.sendMessage(message);
			}
		}
	}

	public static String changeTimeFormat(long time, String format) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		final String timeString = new SimpleDateFormat(format).format(cal.getTime());
		return timeString;
	}
	
	public static PotionEffect getPotionEffect(String name, int time, int power) {
		name = name.toLowerCase();
		time = time * 20;
		PotionEffect effect = null;
		if (name.equals("nightvision")) {
			effect = new PotionEffect(PotionEffectType.NIGHT_VISION, time, power);
		} else if (name.equals("blindness")) {
			effect = new PotionEffect(PotionEffectType.BLINDNESS, time, power);
		} else if (name.equals("confusion")) {
			effect = new PotionEffect(PotionEffectType.CONFUSION, time, power);
		} else if (name.equals("jump")) {
			effect = new PotionEffect(PotionEffectType.JUMP, time, power);
		} else if (name.equals("slowdig")) {
			effect = new PotionEffect(PotionEffectType.SLOW_DIGGING, time, power);
		} else if (name.equals("damageresist")) {
			effect = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, time, power);
		} else if (name.equals("fastdig")) {
			effect = new PotionEffect(PotionEffectType.FAST_DIGGING, time, power);
		} else if (name.equals("fireresist")) {
			effect = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, time, power);
		} else if (name.equals("harm")) {
			effect = new PotionEffect(PotionEffectType.HARM, time, power);
		} else if (name.equals("heal")) {
			effect = new PotionEffect(PotionEffectType.HEAL, time, power);
		} else if (name.equals("hunger")) {
			effect = new PotionEffect(PotionEffectType.HUNGER, time, power);
		} else if (name.equals("strength")) {
			effect = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, time, power);
		} else if (name.equals("invisibility")) {
			effect = new PotionEffect(PotionEffectType.INVISIBILITY, time, power);
		} else if (name.equals("poison")) {
			effect = new PotionEffect(PotionEffectType.POISON, time, power);
		} else if (name.equals("regeneration")) {
			effect = new PotionEffect(PotionEffectType.REGENERATION, time, power);
		} else if (name.equals("slow")) {
			effect = new PotionEffect(PotionEffectType.SLOW, time, power);
		} else if (name.equals("speed")) {
			effect = new PotionEffect(PotionEffectType.SPEED, time, power);
		} else if (name.equals("waterbreathing")) {
			effect = new PotionEffect(PotionEffectType.WATER_BREATHING, time, power);
		} else if (name.equals("weakness")) {
			effect = new PotionEffect(PotionEffectType.WEAKNESS, time, power);
		}
		return effect;
	}
	
    public static String colorize(String s){
    	if(s == null) return null;
    	return ChatColor.translateAlternateColorCodes('&', s);
    }
    
    public static String decolorize(String s){
    	if(s == null) return null;
    	return s.replaceAll("&([0-9a-f])", "");
    }
    
    public static void msg(CommandSender sender, String msg) {
		if (sender instanceof Player) {
			sender.sendMessage(Util.getHeader() + msg);
		} else {
			sender.sendMessage("[SLAP] " + msg);
		}
	}

    public static void badMsg(CommandSender sender, String msg) {
		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.RED + msg);
		} else {
			sender.sendMessage(msg);
		}
	}

    public static void noPermission(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "You do not have access to that command.");
	}

    public static boolean testPermission(CommandSender sender, String perm) {
		String permission = "slaphomebrew." + perm;
		if (!(sender instanceof Player) || sender.hasPermission(permission))
			return true;
		return false;
	}
    
    public static String getTimePlayedString(long l) {
    	String returnString = "";
    	int t = 0; 
    	l = l / 1000;
    	if (l < 60) {
    		t = 1; //Seconds
    	} else if (l < 3600) {
    		t = 2; //Minutes
    	} else if (l < 86400) {
    		t = 3; //Hours
    	} else {
    		t = 4; //Days
    	}
    	switch (t) {
    	case 4:
    		int days = (int)Math.floor(l / 86400.00);
    		l = l - (days * 86400);
    		returnString = days + " days, ";
    	case 3:
    		int hours = (int)Math.floor(l / 3600.00);
    		l = l - (hours * 3600);
    		returnString = returnString + hours + " hours, ";
    	case 2:
    		int minutes = (int)Math.floor(l / 60.00);
    		l = l - (minutes * 60);
    		returnString = returnString + minutes + " minutes and ";
    	case 1:
    		returnString = returnString + l + " seconds";
    		break;
    	default:
    		returnString = "Unkown";
    	}
    	return returnString;
    }
    
    /**
     * Remove all PotionEffects from a player
     * @param p the player
     */
    public static void wipeAllPotionEffects(Player p) {
    	HashSet<PotionEffect> effects = new HashSet<>(p.getActivePotionEffects());
    	for (PotionEffect effect : effects) {
    		p.removePotionEffect(effect.getType());
    	}
    }
    
    public static BukkitTask runASync(SlapHomebrew plugin, Runnable runnable) {
    	return getScheduler(plugin).runTaskAsynchronously(plugin, runnable);
    }
    
    public static BukkitTask runASyncLater(SlapHomebrew plugin, Runnable runnable, int delay) {
    	return getScheduler(plugin).runTaskLaterAsynchronously(plugin, runnable, delay);
    }
    
    public static BukkitTask runASyncTimer(SlapHomebrew plugin, Runnable runnable, int delay, int period) {
    	return getScheduler(plugin).runTaskTimerAsynchronously(plugin, runnable, delay, period);
    }
    
    public static BukkitTask run(SlapHomebrew plugin, Runnable runnable) {
    	return getScheduler(plugin).runTask(plugin, runnable);
    }
    
    public static BukkitTask runLater(SlapHomebrew plugin, Runnable runnable, int delay) {
    	return getScheduler(plugin).runTaskLater(plugin, runnable, delay);
    }
    
    public static BukkitTask runTimer(SlapHomebrew plugin, Runnable runnable, int delay, int period) {
    	return getScheduler(plugin).runTaskTimer(plugin, runnable, delay, period);
    }
    
    public static BukkitScheduler getScheduler(SlapHomebrew plugin) {
    	return plugin.getServer().getScheduler();
    }
    
}
