package me.naithantu.SlapHomebrew.Controllers.PlayerLogging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import me.naithantu.SlapHomebrew.Commands.AbstractCommand;
import me.naithantu.SlapHomebrew.Commands.Exception.CommandException;
import me.naithantu.SlapHomebrew.Util.SQLPool;
import me.naithantu.SlapHomebrew.Util.Util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionLogger extends AbstractLogger {

	private static RegionLogger instance;
	
	private HashSet<Batchable> changes;
	
	private SimpleDateFormat format;
	
	private String sqlQuery = 
			"INSERT INTO `mcecon`.`logger_regions` (`world`, `region`, `changed_time`, `changed_by`, `changer_is_a`, `type`, `parameter`) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?);";
	
	public RegionLogger() {
		super();
		if (!enabled) return;
		changes = new HashSet<>();
		format = new SimpleDateFormat("dd MMM. HH:mm");
		instance = this;
	}
	
	@Override
	public void createTables() throws SQLException {
		executeUpdate(
			"CREATE TABLE IF NOT EXISTS `logger_regions` ( " +
			"`world` varchar(255) NOT NULL, " +
			"`region` varchar(255) NOT NULL, " +
			"`changed_time` bigint(20) NOT NULL, " +
			"`changed_by` varchar(20) NOT NULL, " +
			"`changer_is_a` enum('staff','owner','member','') NOT NULL, " +
			"`type` enum('addmember','removemember','addowner','removeowner','create','remove','flag','priority','redefine') NOT NULL, " +
			"`parameter` varchar(255) DEFAULT NULL, " +
			"KEY `region` (`region`), " +
			"KEY `changed_time` (`changed_time`), " +
			"KEY `world` (`world`) " +
			") ENGINE=InnoDB DEFAULT CHARSET=latin1;"
		);
	}
		
	/**
	 * Add a change to the MySQL DB
	 * @param world The world the region is in
	 * @param region The name of the region
	 * @param changedBy The name of the player who changed the region
	 * @param changer The type of the player who changed the region (Staff, Owner, Member)
	 * @param changeType The type of the change
	 * @param parameters The paramaters given to the change (players, flags, etc)
	 */
	private void addChange(String world, String region, String changedBy, ChangerIsA changer, ChangeType changeType, String parameters) {
		RegionChange change = new RegionChange(world, region, System.currentTimeMillis(), changedBy, changer, changeType, parameters);
		changes.add(change);
		if (changes.size() >= 5 && plugin.isEnabled()) {
			batch();
		}
	}

	/**
	 * Add a change to the MySQL DB
	 * @param region The changed region
	 * @param changedBy The player who changed the region
	 * @param changer The type of the player who changed the region (Staff, Owner, Member)
	 * @param changeType The type of change
	 * @param parameters Extra parameters. Can be null.
	 */
	public static void logRegionChange(ProtectedRegion region, Player changedBy, ChangerIsA changer, ChangeType changeType, String parameters) {
		if (instance != null) instance.addChange(changedBy.getWorld().getName(), region.getId(), changedBy.getName(), changer, changeType, parameters);
	}
	
	
	public static void getRegionChanges(final CommandSender sender, final String region, final String world) throws CommandException {
		if (instance == null) {
			AbstractCommand.removeDoingCommand(sender);
			throw new CommandException("The RegionLogger is currently disabled.");
		}
		Util.runASync(instance.plugin, new Runnable() {
			
			@Override
			public void run() {
				ArrayList<RegionChange> foundChanges = new ArrayList<>();
				for (Batchable b : instance.changes) { //Loop thru unbatched RegionChanges
					RegionChange change = (RegionChange) b; //Cast
					if (change.region.equalsIgnoreCase(region) && change.world.equalsIgnoreCase(world)) { //Check if match
						foundChanges.add(change);
					}
				}
				Connection con = SQLPool.getConnection();
				try {
					PreparedStatement prep = con.prepareStatement( //Prep Statement for getting changes
						"SELECT `changed_time`, `changed_by`, `changer_is_a`, `type`, `parameter` FROM `logger_regions` " +
						"WHERE `world` = ? AND `region` = ?;"
					);
					prep.setString(1, world);
					prep.setString(2, region);
					ResultSet rs = prep.executeQuery(); //Get changes
					while (rs.next()) { //Go thru results
						foundChanges.add(instance.new RegionChange( //Add changes
								world, region, 
								rs.getLong(1), 
								rs.getString(2), 
								ChangerIsA.valueOf(rs.getString(3)), 
								ChangeType.valueOf(rs.getString(4)), 
								rs.getString(5))
						);
					}
				} catch (SQLException | IllegalArgumentException e) { //Meh. 
					Util.badMsg(sender, "Something went wrong. Beep bep.");
					e.printStackTrace();
				}
				
				if (foundChanges.size() > 0) { //Entries
					Collections.sort(foundChanges); //Sort list
					Util.msg(sender, "Changes for region " + ChatColor.GREEN + region + ChatColor.WHITE + " (world: " + ChatColor.GREEN + world + ChatColor.WHITE + ").");
					for (RegionChange change : foundChanges) {
						sender.sendMessage(ChatColor.GOLD + " \u2517\u25B6 " + change);
					}
				} else { //No changes found
					Util.badMsg(sender, "No changes found for region " + ChatColor.YELLOW + region + ChatColor.RED + " in world " + ChatColor.YELLOW + world + ChatColor.RED + ".");
				}
				AbstractCommand.removeDoingCommand(sender);
			}
		});
	}
	
	@Override
	public void batch() {
		batch(sqlQuery, changes);
	}
			
	@Override
	public void shutdown() {
		batch();
		instance = null;
	}
	
	private class RegionChange implements Batchable, Comparable<RegionChange> {
		
		String world;
		String region;
		long changedTime;
		String changedBy;
		ChangerIsA aType;
		ChangeType type;
		String parameters;
		
		public RegionChange(String world, String region, long changedTime, String changedBy, ChangerIsA aType, ChangeType type, String parameters) {
			this.world = world;
			this.region = region;
			this.changedTime = changedTime;
			this.changedBy = changedBy;
			this.aType = aType;
			this.type = type;
			this.parameters = parameters;
		}
		
		@Override
		public void addBatch(PreparedStatement preparedStatement) throws SQLException {
			preparedStatement.setString(1, world);
			preparedStatement.setString(2, region);
			preparedStatement.setLong(3, changedTime);
			preparedStatement.setString(4, changedBy);
			preparedStatement.setString(5, aType.toString());
			preparedStatement.setString(6, type.toString());
			preparedStatement.setString(7, parameters);
		}
		
		@Override
		public int compareTo(RegionChange o) {
			return (int) (changedTime - o.changedTime);
		}
		
		@Override
		public String toString() {
			String s = format.format(changedTime) + " | " + ChatColor.WHITE;
			s += changedBy + " (" + aType.toString().substring(0, 1).toUpperCase() + ") ";
			switch (type) {
			case addmember:
				s += "added members: " + parameters;
				break;
			case addowner:
				s += "added owners: " + parameters;
				break;
			case create:
				s += "created the region.";
				break;
			case flag:
				s += "added/removed a flag: " + parameters;
				break;
			case priority:
				s += "changed the priority to: " + parameters;
				break;
			case redefine:
				s += "redefined the region.";
				break;
			case remove:
				s += "removed the region.";
				break;
			case removemember:
				s += "removed members: " + parameters;
				break;
			case removeowner:
				s += "removed owners: " + parameters;
				break;
			}
			return s;
		}
				
	}
	
	/**
	 * A type of region change
	 */
	public enum ChangeType {
		addmember, removemember, addowner, removeowner, create, remove, flag, priority, redefine
	}
	
	/**
	 * The type of a player who changes a region
	 */
	public enum ChangerIsA {
		staff, owner, member
	}

}
