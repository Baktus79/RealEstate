package me.EtienneDx.RealEstate;

import java.time.Duration;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import net.milkbowl.vault.economy.EconomyResponse;
import no.vestlandetmc.rd.handler.Region;
import no.vestlandetmc.rd.handler.RegionManager;
import no.vestlandetmc.rd.handler.Town;

public class Utils
{
	public static boolean makePayment(Region claim, UUID receiver, UUID giver, double amount, boolean msgSeller, boolean msgBuyer)
	{
		// seller might be null if it is the server
		final OfflinePlayer s = receiver != null ? Bukkit.getOfflinePlayer(receiver) : null, b = Bukkit.getOfflinePlayer(giver);
		if(!RealEstate.econ.has(b, amount))
		{
			if(b.isOnline() && msgBuyer)
			{
				((Player)b).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED +
						"You don't have enough money to make this transaction!");
			}
			if(s != null && s.isOnline() && msgSeller)
			{
				((Player)s).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED +
						b.getName() + " doesn't have enough money to make this transaction!");
			}
			return false;
		}
		final EconomyResponse resp = RealEstate.econ.withdrawPlayer(b, amount);
		if(!resp.transactionSuccess())
		{
			if(b.isOnline() && msgBuyer)
			{
				((Player)b).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED +
						"Could not withdraw the money!");
			}
			if(s != null && s.isOnline() && msgSeller)
			{
				((Player)s).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED +
						"Could not withdraw the money!");
			}
			return false;
		}
		if(s != null)
		{

			/*resp = RealEstate.econ.depositPlayer(s, amount);
			if(!resp.transactionSuccess()) {
				if(b.isOnline() && msgBuyer) {
					((Player)b).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED +
							"Could not deposit to " + s.getName() + ", refunding Player!");
				}

				if(s != null && s.isOnline() && msgSeller) {
					((Player)s).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED +
							"Could not deposit to you, refunding" + b.getName() + "!");
				}

				RealEstate.econ.depositPlayer(b, amount);
				return false;
			}*/

			if(claim.hasParent()) {
				final Town town = RegionManager.getTown(claim.getParentID());
				if(town != null) { town.depositBalance(amount); }
			}

			if(s.isOnline()) {
				s.getPlayer().sendMessage(""
						+ ChatColor.GOLD + amount + " kroner"
						+ ChatColor.YELLOW + " ble innbetalt til din bykonto fra "
						+ ChatColor.GOLD + b.getName() + ChatColor.YELLOW + ". Inntekt fra utleieplott.");
			}

		}

		return true;
	}

	public static String getTime(int days, Duration hours, boolean details)
	{
		String time = "";
		if(days >= 7)
		{
			time += days / 7 + " uker" + (days >= 14 ? "s" : "");
		}
		if(days % 7 > 0)
		{
			time += (time.isEmpty() ? "" : " ") + days % 7 + " dag" + (days % 7 > 1 ? "s" : "");
		}
		if((details || days < 7) && hours != null && hours.toHours() > 0)
		{
			time += (time.isEmpty() ? "" : " ") + hours.toHours() + " timer" + (hours.toHours() > 1 ? "s" : "");
		}
		if((details || days == 0) && hours != null && (time.isEmpty() || hours.toMinutes() % 60 > 0))
		{
			time += (time.isEmpty() ? "" : " ") + hours.toMinutes() % 60 + " min" + (hours.toMinutes() % 60 > 1 ? "s" : "");
		}

		return time;
	}

	public static void transferClaim(Region claim, UUID buyer, UUID seller)
	{

		// start to change owner
		if (seller != null)
		{
			for(final Region child : RegionManager.getSubRegions(claim.getRegionID()))
			{
				if (child.getOwnerUUID().equals(claim.getOwnerUUID()))
				{
					child.removeUserTrust(seller);
				}
			}

			claim.removeUserTrust(seller);

		}

		claim.transferRegion(buyer);

	}

	public static String getSignString(String str)
	{
		if(str.length() > 16)
			str = str.substring(0, 16);
		return str;
	}
}
