package me.EtienneDx.RealEstate;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import me.EtienneDx.RealEstate.Transactions.BoughtTransaction;
import me.EtienneDx.RealEstate.Transactions.Transaction;
import no.vestlandetmc.rd.api.RegionDeletedEvent;
import no.vestlandetmc.rd.api.RegionExpandEvent;
import no.vestlandetmc.rd.api.RegionPermissionCheckEvent;
import no.vestlandetmc.rd.api.RegionTrustRemove;
import no.vestlandetmc.rd.handler.Region;
import no.vestlandetmc.rd.handler.RegionManager;
import no.vestlandetmc.rd.handler.User;

public class ClaimPermissionListener implements Listener {

	void registerEvents()
	{
		final PluginManager pm = RealEstate.instance.getServer().getPluginManager();

		pm.registerEvents(this, RealEstate.instance);
	}

	@EventHandler
	public void onClaimPermission(RegionPermissionCheckEvent event) {
		final Transaction transaction = RealEstate.transactionsStore.getTransaction(event.getRegion());
		final User user = RegionManager.getUser(event.getPlayer().getUniqueId());
		if(user.ignoringClaims()) { return; }
		// we only have to remove the owner's access

		if(transaction != null && user.getUniqueId().equals(transaction.getOwner()) &&
				transaction instanceof BoughtTransaction && ((BoughtTransaction)transaction).getBuyer() != null) {
			event.getPlayer().sendMessage(ChatColor.RED + "This claim is currently involved in a transaction, you can't access it!");
			event.setCancelled(true);
		}
	}

	// more of a safety measure, normally it shouldn't be needed
	@EventHandler
	public void onRegionDeleted(RegionDeletedEvent event) {
		Transaction tr = RealEstate.transactionsStore.getTransaction(event.getRegion());
		if(tr != null) tr.tryCancelTransaction(null, true);
		for (final Region child : RegionManager.getSubRegions(event.getRegion().getRegionID())) {
			tr = RealEstate.transactionsStore.getTransaction(child);
			if(tr != null) tr.tryCancelTransaction(null, true);
		}
	}

	@EventHandler
	public void onExpand(RegionExpandEvent event) {
		final Transaction transaction = RealEstate.transactionsStore.getTransaction(event.getRegion());
		if(event.getPlayer().hasPermission("regiondefender.command.expandclaim.others")) { return; }

		if(transaction != null && transaction instanceof BoughtTransaction && ((BoughtTransaction)transaction).getBuyer() != null
				&& event.getPlayer().getUniqueId().equals(((BoughtTransaction) transaction).getBuyer())) {
			event.getPlayer().sendMessage(ChatColor.RED + "Dette claimet kan ikke utvides av deg, ta kontakt med eier.");
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onTrustRemove(RegionTrustRemove event) {
		final Transaction transaction = RealEstate.transactionsStore.getTransaction(event.getRegion());
		if(event.getPlayer().hasPermission("regiondefender.command.untrust.others")) { return; }

		if(transaction != null && transaction instanceof BoughtTransaction && ((BoughtTransaction)transaction).getBuyer() != null
				&& event.getPlayer().getUniqueId().equals(((BoughtTransaction) transaction).getBuyer())) {
			final UUID buyer = ((BoughtTransaction) transaction).getBuyer();

			if(event.isAllRemoved()) {
				event.getPlayer().sendMessage(ChatColor.RED + "Du kan ikke fjerne alle med trust, da mister du selv tilgang på plotten.");
				event.setCancelled(true);
			}

			else if(event.getRemovedPlayer().getUniqueId().equals(event.getPlayer().getUniqueId())) {
				event.getPlayer().sendMessage(ChatColor.RED + "Du kan ikke fjerne din egen trust, da mister du tilgangen på plotten.");
				event.setCancelled(true);
			}

			else if(event.getRemovedPlayer().getUniqueId().equals(buyer)) {
				event.getPlayer().sendMessage(ChatColor.RED + "Du kan ikke fjerne leietageren sin trust.");
				event.setCancelled(true);
			}
		}
	}

}
