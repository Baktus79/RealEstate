package me.EtienneDx.RealEstate.Transactions;

import java.util.Map;
import java.util.UUID;

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

public class ClaimSell extends ClaimTransaction
{
	public ClaimSell(Region claim, Player player, double price, Location sign)
	{
		super(claim, player, price, sign);
	}

	public ClaimSell(Map<String, Object> map)
	{
		super(map);
	}

	@Override
	public boolean update()
	{
		if(sign.getBlock().getState() instanceof Sign)
		{
			final Sign s = (Sign) sign.getBlock().getState();
			s.setLine(0, RealEstate.instance.config.cfgSignsHeader);
			s.setLine(1, ChatColor.DARK_GREEN + RealEstate.instance.config.cfgReplaceSell);
			s.setLine(2, owner != null ? Utils.getSignString(Bukkit.getOfflinePlayer(owner).getName()) : "SERVER");
			if(RealEstate.instance.config.cfgUseCurrencySymbol)
			{
				if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
				{
					s.setLine(3, RealEstate.instance.config.cfgCurrencySymbol + " " + (int)Math.round(price));
				}
				else
				{
					s.setLine(3, RealEstate.instance.config.cfgCurrencySymbol + " " + price);
				}
			}
			else
			{
				if(RealEstate.instance.config.cfgUseDecimalCurrency == false)
				{
					s.setLine(3, (int)Math.round(price) + " " + RealEstate.econ.currencyNamePlural());
				}
				else
				{
					s.setLine(3, price + " " + RealEstate.econ.currencyNamePlural());
				}
			}
			s.update(true);
		}
		else
		{
			RealEstate.transactionsStore.cancelTransaction(this);
		}
		return false;
	}

	@Override
	public boolean tryCancelTransaction(Player p, boolean force)
	{
		// nothing special here, this transaction can only be waiting for a buyer
		RealEstate.transactionsStore.cancelTransaction(this);
		return true;
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

		if (player.getUniqueId().equals(owner))
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + "You already own this " + claimType + "!");
			return;
		}
		if(claim.getParent() == null && owner != null && !owner.equals(claim.getOwnerUUID()))
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + Bukkit.getPlayer(owner).getDisplayName() +
					" does not have the right to sell this " + claimType + "!");
			RealEstate.transactionsStore.cancelTransaction(claim);
			return;
		}
		if(!player.hasPermission("realestate." + claimType + ".buy"))
		{
			player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + "You do not have the permission to purchase " +
					claimType + "s!");
			return;
		}
		// the player has the right to buy, let's make the payment

		if(Utils.makePayment(claim, owner, player.getUniqueId(), price, false, true))// if payment succeed
		{
			Utils.transferClaim(claim, player.getUniqueId(), owner);
			// normally, this is always the case, so it's not necessary, but until I proven my point, here
			if(claim.getParent() != null || claim.getOwnerUUID().equals(player.getUniqueId()))
			{
				player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.AQUA + "You have successfully purchased this " + claimType +
						" for " + ChatColor.GREEN + price + RealEstate.econ.currencyNamePlural());
				RealEstate.instance.addLogEntry(
						"[" + RealEstate.transactionsStore.dateFormat.format(RealEstate.transactionsStore.date) + "] " + player.getName() +
						" has purchased a " + claimType + " at " +
						"[" + player.getLocation().getWorld() + ", " +
						"X: " + player.getLocation().getBlockX() + ", " +
						"Y: " + player.getLocation().getBlockY() + ", " +
						"Z: " + player.getLocation().getBlockZ() + "] " +
						"Price: " + price + " " + RealEstate.econ.currencyNamePlural());

				if(RealEstate.instance.config.cfgMessageOwner && owner != null)
				{
					final OfflinePlayer oldOwner = Bukkit.getOfflinePlayer(owner);
					if(oldOwner.isOnline())
					{
						((Player) oldOwner).sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.AQUA + player.getDisplayName() +
								" has purchased your " + claimType + " at " + ChatColor.BLUE +
								"[" + player.getLocation().getWorld().getName() + ", " +
								"X: " + player.getLocation().getBlockX() + ", " +
								"Y: " + player.getLocation().getBlockY() + ", " +
								"Z: " + player.getLocation().getBlockZ() + "] " + ChatColor.AQUA + "for " + ChatColor.GREEN +
								price + " " + RealEstate.econ.currencyNamePlural());
					}
				}
			}
			else
			{
				player.sendMessage(RealEstate.instance.config.chatPrefix + ChatColor.RED + "Cannot purchase claim!");
				return;
			}
			RealEstate.transactionsStore.cancelTransaction(claim);
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
			final String ownerName = Bukkit.getOfflinePlayer(claim.getOwnerUUID()).getName();
			msg = ChatColor.BLUE + "-----= " + ChatColor.WHITE + "[" + ChatColor.GOLD + "RealEstate Sale Info" + ChatColor.WHITE + "]" +
					ChatColor.BLUE + " =-----\n";
			msg += ChatColor.AQUA + "This " + claimType + " is for sale for " +
					ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural() + "\n";
			if(claimType.equalsIgnoreCase("claim"))
			{
				msg += ChatColor.AQUA + "The current owner is: " + ChatColor.GREEN + ownerName;
			}
			else
			{
				msg += ChatColor.AQUA + "The main claim owner is: " + ChatColor.GREEN + ownerName + "\n";
				msg += ChatColor.LIGHT_PURPLE + "Note: " + ChatColor.AQUA + "You will only buy access to this subclaim!";
			}
		}
		else
		{
			msg = RealEstate.instance.config.chatPrefix + ChatColor.RED + "You don't have the permission to view real estate informations!";
		}
		player.sendMessage(msg);
	}

	@Override
	public void setOwner(UUID newOwner)
	{
		this.owner = newOwner;
	}

	@Override
	public void msgInfo(CommandSender cs)
	{
		final Region claim = RegionManager.getRegion(claimId);
		final World world = claim.getWorld();
		cs.sendMessage(ChatColor.DARK_GREEN + "" + claim.getArea() +
				ChatColor.AQUA + " blocks to " + ChatColor.DARK_GREEN + "Sell " + ChatColor.AQUA + "at " + ChatColor.DARK_GREEN +
				"[" + world.getName() + ", " +
				"X: " + claim.getLesserBoundary().getX() + ", " +
				"Y: " + claim.getLesserBoundary().getY() + ", " +
				"Z: " + claim.getLesserBoundary().getZ() + "] " + ChatColor.AQUA + "for " +
				ChatColor.GREEN + price + " " + RealEstate.econ.currencyNamePlural());
	}
}
