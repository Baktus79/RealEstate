package me.EtienneDx.RealEstate.Transactions;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.Utils;
import net.md_5.bungee.api.ChatColor;
import no.vestlandetmc.rd.handler.Region;
import no.vestlandetmc.rd.handler.RegionManager;
import no.vestlandetmc.rd.handler.Trust;

public class ClaimRent extends BoughtTransaction
{
	LocalDateTime startDate = null;
	int duration;
	public boolean autoRenew = false;
	public boolean buildTrust = true;
	public int periodCount = 0;
	public int maxPeriod;

	public ClaimRent(Map<String, Object> map)
	{
		super(map);
		if(map.get("startDate") != null)
			startDate = LocalDateTime.parse((String) map.get("startDate"), DateTimeFormatter.ISO_DATE_TIME);
		duration = (int)map.get("duration");
		autoRenew = (boolean) map.get("autoRenew");
		periodCount = (int) map.get("periodCount");
		maxPeriod = (int) map.get("maxPeriod");
		try {
			buildTrust = (boolean) map.get("buildTrust");
		}
		catch (final Exception e) {
			buildTrust = true;
		}
	}

	public ClaimRent(Region claim, Player player, double price, Location sign, int duration, int rentPeriods, boolean buildTrust)
	{
		super(claim, player, price, sign);
		this.duration = duration;
		this.maxPeriod = RealEstate.instance.config.cfgEnableRentPeriod ? rentPeriods : 1;
		this.buildTrust = buildTrust;
	}

	@Override
	public Map<String, Object> serialize() {
		final Map<String, Object> map = super.serialize();

		if(startDate != null)
			map.put("startDate", startDate.format(DateTimeFormatter.ISO_DATE_TIME));
		map.put("duration", duration);
		map.put("autoRenew",  autoRenew);
		map.put("periodCount", periodCount);
		map.put("maxPeriod", maxPeriod);
		map.put("buildTrust", buildTrust);

		return map;
	}

	@Override
	public boolean update()
	{
		if(buyer == null)
		{
			if(sign.getBlock().getState() instanceof Sign)
			{
				final Sign s = (Sign) sign.getBlock().getState();
				s.setLine(0, RealEstate.instance.config.cfgSignsHeader);
				s.setLine(1, ChatColor.DARK_GREEN + RealEstate.instance.config.cfgReplaceRent);
				//s.setLine(2, owner != null ? Bukkit.getOfflinePlayer(owner).getName() : "SERVER");
				String price_line = "";
				if(RealEstate.instance.config.cfgUseCurrencySymbol)
				{
					if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
					{
						price_line = RealEstate.instance.config.cfgCurrencySymbol + " " + (int)Math.round(price);
					}
					else
					{
						price_line = RealEstate.instance.config.cfgCurrencySymbol + " " + price;
					}

				}
				else
				{
					if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
					{
						price_line = (int)Math.round(price) + " " + RealEstate.econ.currencyNamePlural();
					}
					else
					{
						price_line = price + " " + RealEstate.econ.currencyNamePlural();
					}
				}
				final String period = (maxPeriod > 1 ? maxPeriod + "x " : "") + Utils.getTime(duration, null, false);
				if(this.buildTrust) {
					s.setLine(2, price_line);
					s.setLine(3, period);
				} else {
					s.setLine(2, RealEstate.instance.config.cfgContainerRentLine);
					s.setLine(3, price_line + " - " + period);
				}
				s.update(true);
			}
			else
			{
				return true;
			}
		}
		else
		{
			// we want to know how much time has gone by since startDate
			int days = Period.between(startDate.toLocalDate(), LocalDate.now()).getDays();
			Duration hours = Duration.between(startDate.toLocalTime(), LocalTime.now());
			if(hours.isNegative() && !hours.isZero())
			{
				hours = hours.plusHours(24);
				days--;
			}
			if(days >= duration)// we exceeded the time limit!
			{
				payRent();
			}
			else if(sign.getBlock().getState() instanceof Sign)
			{
				final Sign s = (Sign) sign.getBlock().getState();
				s.setLine(0, ChatColor.GOLD + RealEstate.instance.config.cfgReplaceOngoingRent); //Changed the header to "[Rented]" so that it won't waste space on the next line and allow the name of the player to show underneath.
				s.setLine(1, Utils.getSignString(Bukkit.getOfflinePlayer(buyer).getName()));//remove "Rented by"
				s.setLine(2, "Time remaining : ");

				final int daysLeft = duration - days - 1;// we need to remove the current day
				final Duration timeRemaining = Duration.ofHours(24).minus(hours);

				s.setLine(3, Utils.getTime(daysLeft, timeRemaining, false));
				s.update(true);
			}
		}
		return false;

	}

	private void unRent(boolean msgBuyer)
	{
		final Region claim = RegionManager.getRegion(sign);

		claim.removeUserTrust(buyer);

		if(msgBuyer && Bukkit.getOfflinePlayer(buyer).isOnline() && RealEstate.instance.config.cfgMessageBuyer)
		{
			Bukkit.getPlayer(buyer).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.AQUA +
					"The rent for the " + (claim.getParent() == null ? "claim" : "subclaim") + " at " + ChatColor.BLUE + "[" +
					sign.getWorld().getName() + ", X: " + sign.getBlockX() + ", Y: " +
					sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]" + ChatColor.AQUA + " is now over, your access has been revoked.");
		}

		final OfflinePlayer player = Bukkit.getOfflinePlayer(buyer);
		final ClaimUnrent unrentEvent = new ClaimUnrent(claim, player);
		Bukkit.getPluginManager().callEvent(unrentEvent);

		claim.removeAllTrust();
		claim.setRestrict(false);

		buyer = null;
		RealEstate.transactionsStore.saveData();
		update();
	}

	private void payRent()
	{
		if(buyer == null) return;

		final OfflinePlayer buyerPlayer = Bukkit.getOfflinePlayer(this.buyer);
		final OfflinePlayer seller = owner == null ? null : Bukkit.getOfflinePlayer(owner);
		final Region claim = RegionManager.getRegion(sign);

		final String claimType = claim.getParent() == null ? "claim" : "subclaim";

		if((autoRenew || periodCount < maxPeriod) && Utils.makePayment(claim, owner, this.buyer, price, false, false))
		{
			periodCount = (periodCount + 1) % maxPeriod;
			startDate = LocalDateTime.now();
			if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
			{
				((Player)buyerPlayer).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.AQUA +
						"Paid rent for the " + claimType + " at " + ChatColor.BLUE + "[" + sign.getWorld().getName() + ", X: " + sign.getBlockX() +
						", Y: " + sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]" +
						ChatColor.AQUA + "for the price of " + ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
			}

			if(seller != null)
			{
				if(seller.isOnline() && RealEstate.instance.config.cfgMessageOwner)
				{
					((Player)seller).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.AQUA + buyerPlayer.getName() +
							" has paid rent for the " + claimType + " at " + ChatColor.BLUE + "[" +
							sign.getWorld().getName() + ", X: " + sign.getBlockX() + ", Y: " +
							sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]" +
							ChatColor.AQUA + "at the price of " + ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
				}
			}

		}
		else if (autoRenew)
		{
			if(buyerPlayer.isOnline() && RealEstate.instance.config.cfgMessageBuyer)
			{
				((Player)buyerPlayer).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED +
						"Couldn't pay the rent for the " + claimType + " at " + ChatColor.BLUE + "[" + sign.getWorld().getName() + ", X: " +
						sign.getBlockX() + ", Y: " +
						sign.getBlockY() + ", Z: " + sign.getBlockZ() + "]" + ChatColor.RED + ", your access has been revoked.");
			}

			unRent(false);
			return;
		}
		else
		{
			unRent(true);
			return;
		}
		update();
		RealEstate.transactionsStore.saveData();
	}

	@Override
	public boolean tryCancelTransaction(Player p, boolean force)
	{
		if(buyer != null)
		{
			if(p.hasPermission("realestate.admin") && force == true)
			{
				this.unRent(true);
				RealEstate.transactionsStore.cancelTransaction(this);
				return true;
			}
			else
			{
				final Region claim = RegionManager.getRegion(sign);
				if(p != null)
					p.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + "This " + (claim.getParent() == null ? "claim" : "subclaim") +
							" is currently rented, you can't cancel the transaction!");
				return false;
			}
		}
		else
		{
			RealEstate.transactionsStore.cancelTransaction(this);
			return true;
		}
	}

	@Override
	public void interact(Player player)
	{
		final Region claim = RegionManager.getRegion(sign);
		if(claim == null)
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + "This claim does not exist!");
			RealEstate.transactionsStore.cancelTransaction(claim);
			return;
		}
		final String claimType = claim.getParent() == null ? "claim" : "subclaim";

		if (owner != null && owner.equals(player.getUniqueId()))
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + "You already own this " + claimType + "!");
			return;
		}
		if(claim.getParent() == null && owner != null && !owner.equals(claim.getOwnerUUID()))
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + Bukkit.getOfflinePlayer(owner).getName() +
					" does not have the right to rent this " + claimType + "!");
			RealEstate.transactionsStore.cancelTransaction(claim);
			return;
		}
		if(!player.hasPermission("realestate." + claimType + ".rent"))
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + "You do not have the permission to rent " +
					claimType + "s!");
			return;
		}
		if(player.getUniqueId().equals(buyer))
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + "You are already renting this " +
					claimType + "!");
			return;
		}
		if(buyer != null)
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + "Someone already rents this " +
					claimType + "!");
			return;
		}

		if(Utils.makePayment(claim, owner, player.getUniqueId(), price, false, true))// if payment succeed
		{
			buyer = player.getUniqueId();
			startDate = LocalDateTime.now();
			autoRenew = false;
			final Trust trustType = buildTrust ? Trust.MANAGER : Trust.CONTAINER;
			claim.removeAllTrust();
			claim.setUserTrust(buyer, trustType);
			claim.setRestrict(true);
			update();
			RealEstate.transactionsStore.saveData();

			RealEstate.instance.addLogEntry(
					"[" + RealEstate.transactionsStore.dateFormat.format(RealEstate.transactionsStore.date) + "] " + player.getName() +
					" has rented a " + claimType + " at " +
					"[" + player.getLocation().getWorld() + ", " +
					"X: " + player.getLocation().getBlockX() + ", " +
					"Y: " + player.getLocation().getBlockY() + ", " +
					"Z: " + player.getLocation().getBlockZ() + "] " +
					"Price: " + price + " " + RealEstate.econ.currencyNamePlural());

			if(owner != null)
			{
				final OfflinePlayer seller = Bukkit.getOfflinePlayer(owner);

				if(RealEstate.instance.config.cfgMessageOwner && seller.isOnline())
				{
					((Player)seller).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.GREEN + player.getName() + ChatColor.AQUA +
							" has just rented your " + claimType + " at " +
							ChatColor.BLUE + "[" + sign.getWorld().getName() + ", X: " + sign.getBlockX() + ", Y: " + sign.getBlockY() + ", Z: "
							+ sign.getBlockZ() + "]" + ChatColor.AQUA +
							" for " + ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
				}

			}

			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.AQUA + "You have successfully rented this " + claimType +
					" for " + ChatColor.GREEN + price + RealEstate.econ.currencyNamePlural());

			destroySign();
		}
	}

	@Override
	public void preview(Player player)
	{
		final Region claim = RegionManager.getRegion(sign);
		String msg = "";
		if(player.hasPermission("realestate.info"))
		{
			final String claimType = claim.getParent() == null ? "claim" : "subclaim";
			msg = ChatColor.BLUE + "-----= " + ChatColor.WHITE + "[" + ChatColor.GOLD + "RealEstate Rent Info" + ChatColor.WHITE + "]" +
					ChatColor.BLUE + " =-----\n";
			if(buyer == null)
			{
				final String ownerName = Bukkit.getOfflinePlayer(claim.getOwnerUUID()).getName();

				msg += ChatColor.AQUA + "This " + claimType + " is for rent for " +
						ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural() + ChatColor.AQUA + " for " +
						(maxPeriod > 1 ? "" + ChatColor.GREEN + maxPeriod + ChatColor.AQUA + " periods of " : "") +
						ChatColor.GREEN + Utils.getTime(duration, null, true) + "\n";

				if(claimType.equalsIgnoreCase("claim"))
				{
					msg += ChatColor.AQUA + "The current owner is: " + ChatColor.GREEN + ownerName;
				}
				else
				{
					msg += ChatColor.AQUA + "The main claim owner is: " + ChatColor.GREEN + ownerName + "\n";
					msg += ChatColor.LIGHT_PURPLE + "Note: " + ChatColor.AQUA + "You will only rent access to this subclaim!";
				}
			}
			else
			{
				int days = Period.between(startDate.toLocalDate(), LocalDate.now()).getDays();
				Duration hours = Duration.between(startDate.toLocalTime(), LocalTime.now());
				if(hours.isNegative() && !hours.isZero())
				{
					hours = hours.plusHours(24);
					days--;
				}
				final int daysLeft = duration - days - 1;// we need to remove the current day
				final Duration timeRemaining = Duration.ofHours(24).minus(hours);
				final String ownerName = Bukkit.getOfflinePlayer(claim.getOwnerUUID()).getName();

				msg += ChatColor.AQUA + "This " + claimType + " is currently rented by " +
						ChatColor.GREEN + Bukkit.getOfflinePlayer(buyer).getName() + ChatColor.AQUA + " for " +
						ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural() + ChatColor.AQUA + " for " +
						(maxPeriod - periodCount > 1 ? "" + ChatColor.GREEN + (maxPeriod - periodCount) + ChatColor.AQUA + " periods of " +
								ChatColor.GREEN + Utils.getTime(duration, null, false) + ChatColor.AQUA + ". The current period will end in " : "another ") +
						ChatColor.GREEN + Utils.getTime(daysLeft, timeRemaining, true) + "\n";
				if((owner != null && owner.equals(player.getUniqueId()) || buyer.equals(player.getUniqueId())) && RealEstate.instance.config.cfgEnableAutoRenew)
				{
					msg += ChatColor.AQUA + "Automatic renew is currently " + ChatColor.LIGHT_PURPLE + (autoRenew ? "enabled" : "disabled") + "\n";
				}
				if(claimType.equalsIgnoreCase("claim"))
				{
					msg += ChatColor.AQUA + "The current owner is: " + ChatColor.GREEN + ownerName;
				}
				else
				{
					msg += ChatColor.AQUA + "The main claim owner is: " + ChatColor.GREEN + ownerName;
				}
			}
		}
		else
		{
			msg = RealEstate.instance.config.chatPrefix + ChatColor.RED + "You don't have the permission to view real estate informations!";
		}
		player.sendMessage(msg);
	}

	@Override
	public void msgInfo(CommandSender cs)
	{
		final Region claim = RegionManager.getRegion(claimId);
		final World world = claim.getWorld();
		cs.sendMessage(ChatColor.DARK_GREEN + "" + claim.getArea() +
				ChatColor.AQUA + " blocks to " + ChatColor.DARK_GREEN + "Lease " + ChatColor.AQUA + "at " + ChatColor.DARK_GREEN +
				"[" + world.getName() + ", " +
				"X: " + claim.getLesserBoundary().getX() + ", " +
				"Y: " + claim.getLesserBoundary().getY() + ", " +
				"Z: " + claim.getLesserBoundary().getZ() + "] " + ChatColor.AQUA + "for " +
				ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural() + ChatColor.AQUA + " per period of " + ChatColor.GREEN +
				Utils.getTime(duration, Duration.ZERO, false));
	}

}
