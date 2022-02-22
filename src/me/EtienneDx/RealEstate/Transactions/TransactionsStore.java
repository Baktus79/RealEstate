package me.EtienneDx.RealEstate.Transactions;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;

import me.EtienneDx.RealEstate.RealEstate;
import net.md_5.bungee.api.ChatColor;

public class TransactionsStore
{
	public final String dataFilePath = RealEstate.pluginDirPath + "transactions.data";
	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	Date date = new Date();

	public HashMap<String, ClaimSell> claimSell;
	public HashMap<String, ClaimRent> claimRent;
	public HashMap<String, ClaimLease> claimLease;

	public TransactionsStore()
	{
		loadData();
		new BukkitRunnable()
		{

			@Override
			public void run()
			{
				final Iterator<ClaimRent> ite = claimRent.values().iterator();
				while(ite.hasNext())
				{
					if(ite.next().update())
						ite.remove();
				}

				final Iterator<ClaimLease> it = claimLease.values().iterator();
				while(it.hasNext())
				{
					if(it.next().update())
						it.remove();
				}
				saveData();
			}
		}.runTaskTimer(RealEstate.instance, 1200L, 1200L);// run every 60 seconds
	}

	public void loadData()
	{
		claimSell = new HashMap<>();
		claimRent = new HashMap<>();
		claimLease = new HashMap<>();

		final File file = new File(this.dataFilePath);

		if(file.exists())
		{
			final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
			try {
				RealEstate.instance.addLogEntry(new String(Files.readAllBytes(FileSystems.getDefault().getPath(this.dataFilePath))));
			} catch (final IOException e) {
				e.printStackTrace();
			}
			final ConfigurationSection sell = config.getConfigurationSection("Sell");
			final ConfigurationSection rent = config.getConfigurationSection("Rent");
			final ConfigurationSection lease = config.getConfigurationSection("Lease");
			if(sell != null)
			{
				RealEstate.instance.addLogEntry(sell.toString());
				RealEstate.instance.addLogEntry(sell.getKeys(false).size() + "");
				for(final String key : sell.getKeys(false))
				{
					final ClaimSell cs = (ClaimSell)sell.get(key);
					claimSell.put(key, cs);
				}
			}
			if(rent != null)
			{
				for(final String key : rent.getKeys(false))
				{
					final ClaimRent cr = (ClaimRent)rent.get(key);
					claimRent.put(key, cr);
				}
			}
			if(lease != null)
			{
				for(final String key : lease.getKeys(false))
				{
					final ClaimLease cl = (ClaimLease)lease.get(key);
					claimLease.put(key, cl);
				}
			}
		}
	}

	public void saveData()
	{
		final YamlConfiguration config = new YamlConfiguration();
		for (final ClaimSell cs : claimSell.values())
			config.set("Sell." + cs.claimId, cs);
		for (final ClaimRent cr : claimRent.values())
			config.set("Rent." + cr.claimId, cr);
		for (final ClaimLease cl : claimLease.values())
			config.set("Lease." + cl.claimId, cl);
		try
		{
			config.save(new File(this.dataFilePath));
		}
		catch (final IOException e)
		{
			RealEstate.instance.log.info("Unable to write to the data file at \"" + this.dataFilePath + "\"");
		}
	}

	public boolean anyTransaction(Claim claim)
	{
		return claim != null &&
				(claimSell.containsKey(claim.getUniqueId().toString()) ||
						claimRent.containsKey(claim.getUniqueId().toString()) ||
						claimLease.containsKey(claim.getUniqueId().toString()));
	}

	public Transaction getTransaction(Claim claim)
	{
		if(claimSell.containsKey(claim.getUniqueId().toString()))
			return claimSell.get(claim.getUniqueId().toString());
		if(claimRent.containsKey(claim.getUniqueId().toString()))
			return claimRent.get(claim.getUniqueId().toString());
		if(claimLease.containsKey(claim.getUniqueId().toString()))
			return claimLease.get(claim.getUniqueId().toString());
		return null;
	}

	public void cancelTransaction(Claim claim)
	{
		if(anyTransaction(claim))
		{
			final Transaction tr = getTransaction(claim);
			cancelTransaction(tr);
		}
		saveData();
	}

	public void cancelTransaction(Transaction tr)
	{
		if(tr.getHolder() != null)
			tr.getHolder().breakNaturally();
		if(tr instanceof ClaimSell)
		{
			claimSell.remove(String.valueOf(((ClaimSell) tr).claimId));
		}
		if(tr instanceof ClaimRent)
		{
			claimRent.remove(String.valueOf(((ClaimRent) tr).claimId));
		}
		if(tr instanceof ClaimLease)
		{
			claimLease.remove(String.valueOf(((ClaimLease) tr).claimId));
		}
		saveData();
	}

	public boolean canCancelTransaction(Transaction tr)
	{
		return tr instanceof ClaimSell || tr instanceof ClaimRent && ((ClaimRent)tr).buyer == null ||
				tr instanceof ClaimLease && ((ClaimLease)tr).buyer == null;
	}

	public void sell(Claim claim, Player player, double price, Location sign)
	{
		final ClaimSell cs = new ClaimSell(claim, claim.isAdminClaim() ? null : player, price, sign);
		claimSell.put(claim.getUniqueId().toString(), cs);
		cs.update();
		saveData();

		final World world = Bukkit.getWorld(claim.getWorldUniqueId());
		RealEstate.instance.addLogEntry("[" + this.dateFormat.format(this.date) + "] " + (player == null ? "The Server" : player.getName()) +
				" has made " + (claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for sale at " +
				"[" + world.getName() + ", " +
				"X: " + claim.getGreaterBoundaryCorner().getX() + ", " +
				"Y: " + claim.getGreaterBoundaryCorner().getY() + ", " +
				"Z: " + claim.getGreaterBoundaryCorner().getZ() + "] " +
				"Price: " + price + " " + RealEstate.econ.currencyNamePlural());

		if(player != null)
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.AQUA + "You have successfully created " +
					(claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " sale for " +
					ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
		}
		if(RealEstate.instance.config.cfgBroadcastSell)
		{
			for(final Player p : Bukkit.getServer().getOnlinePlayers())
			{
				if(p != player)
				{
					p.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.DARK_GREEN + (player == null ? "The Server" : player.getDisplayName()) +
							ChatColor.AQUA + " has put " +
							(claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for sale for " +
							ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
				}
			}
		}
	}

	public void rent(Claim claim, Player player, double price, Location sign, int duration, int rentPeriods, boolean buildTrust)
	{
		final ClaimRent cr = new ClaimRent(claim, claim.isAdminClaim() ? null : player, price, sign, duration, rentPeriods, buildTrust);
		claimRent.put(claim.getUniqueId().toString(), cr);
		cr.update();
		saveData();

		final World world = Bukkit.getWorld(claim.getWorldUniqueId());
		RealEstate.instance.addLogEntry("[" + this.dateFormat.format(this.date) + "] " + (player == null ? "The Server" : player.getName()) +
				" has made " + (claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for" + (buildTrust ? "" : " container") + " rent at " +
				"[" + world.getName() + ", " +
				"X: " + claim.getLesserBoundaryCorner().getX() + ", " +
				"Y: " + claim.getLesserBoundaryCorner().getY() + ", " +
				"Z: " + claim.getLesserBoundaryCorner().getZ() + "] " +
				"Price: " + price + " " + RealEstate.econ.currencyNamePlural());

		if(player != null)
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.AQUA + "You have successfully put " +
					(claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for" + (buildTrust ? "" : " container") + " rent for " +
					ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
		}
		if(RealEstate.instance.config.cfgBroadcastSell)
		{
			for(final Player p : Bukkit.getServer().getOnlinePlayers())
			{
				if(p != player)
				{
					p.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.DARK_GREEN + (player == null ? "The Server" : player.getDisplayName()) +
							ChatColor.AQUA + " has put " +
							(claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for" + (buildTrust ? "" : " container") + " rent for " +
							ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
				}
			}
		}
	}

	public void lease(Claim claim, Player player, double price, Location sign, int frequency, int paymentsCount)
	{
		final ClaimLease cl = new ClaimLease(claim, claim.isAdminClaim() ? null : player, price, sign, frequency, paymentsCount);
		claimLease.put(claim.getUniqueId().toString(), cl);
		cl.update();
		saveData();

		final World world = Bukkit.getWorld(claim.getWorldUniqueId());
		RealEstate.instance.addLogEntry("[" + this.dateFormat.format(this.date) + "] " + (player == null ? "The Server" : player.getName()) +
				" has made " + (claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for lease at " +
				"[" + world.getName() + ", " +
				"X: " + claim.getLesserBoundaryCorner().getX() + ", " +
				"Y: " + claim.getLesserBoundaryCorner().getY() + ", " +
				"Z: " + claim.getLesserBoundaryCorner().getZ() + "] " +
				"Payments Count : " + paymentsCount + " " +
				"Price: " + price + " " + RealEstate.econ.currencyNamePlural());

		if(player != null)
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.AQUA + "You have successfully put " +
					(claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for lease for " +
					ChatColor.GREEN + paymentsCount + ChatColor.AQUA + " payments of " +
					ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
		}
		if(RealEstate.instance.config.cfgBroadcastSell)
		{
			for(final Player p : Bukkit.getServer().getOnlinePlayers())
			{
				if(p != player)
				{
					p.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.DARK_GREEN + (player == null ? "The Server" : player.getDisplayName()) +
							ChatColor.AQUA + " has put " +
							(claim.isAdminClaim() ? "an admin" : "a") + " " + (claim.getParent() == null ? "claim" : "subclaim") + " for lease for " +
							ChatColor.GREEN + paymentsCount + ChatColor.AQUA + " payments of " +
							ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
				}
			}
		}
	}

	public Transaction getTransaction(Player player)
	{
		if(player == null) return null;
		final Claim c = GriefDefender.getCore().getClaimAt(player.getLocation());
		return getTransaction(c);
	}
}
