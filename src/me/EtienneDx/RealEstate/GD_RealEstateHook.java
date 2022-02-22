package me.EtienneDx.RealEstate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.TrustResult;
import com.griefdefender.api.claim.TrustResultTypes;
import com.griefdefender.api.event.ChangeClaimEvent;
import com.griefdefender.api.event.Event;
import com.griefdefender.api.event.ProcessTrustUserEvent;
import com.griefdefender.api.event.RemoveClaimEvent;
import com.griefdefender.lib.kyori.adventure.text.Component;
import com.griefdefender.lib.kyori.event.EventBus;

import me.EtienneDx.RealEstate.Transactions.BoughtTransaction;
import me.EtienneDx.RealEstate.Transactions.Transaction;

public class GD_RealEstateHook
{

	public GD_RealEstateHook() {
		new ProcessTrustUserEventListener();
		new ChangeClaimEventListener();
		new RemoveClaimEventListener();
	}

	private class ProcessTrustUserEventListener {

		public ProcessTrustUserEventListener() {
			final EventBus<Event> eventBus = GriefDefender.getEventManager().getBus();

			eventBus.subscribe(ProcessTrustUserEvent.class, (ProcessTrustUserEvent event) -> {
				final User user = event.getUser();
				if (user == null) {
					return;
				}
				final Player player = Bukkit.getPlayer(user.getUniqueId());
				if (player == null) {
					return;
				}
				final Transaction b = RealEstate.transactionsStore.getTransaction(event.getClaim());
				if(b != null && player.getUniqueId().equals(b.getOwner()) && b instanceof BoughtTransaction)
				{
					if(((BoughtTransaction)b).getBuyer() != null) {
						event.setMessage(Component.text("This claim is currently involved in a transaction, you can't access it!"));
						final TrustResult trustResult = TrustResult.builder().user(event.getUser()).claims(event.getClaims()).trust(event.getTrustType()).type(TrustResultTypes.NOT_TRUSTED).build();
						event.setNewTrustResult(trustResult);
					}
				}
			});
		}
	}

	private class ChangeClaimEventListener {

		public ChangeClaimEventListener() {
			final EventBus<Event> eventBus = GriefDefender.getEventManager().getBus();

			eventBus.subscribe(ChangeClaimEvent.class, (ChangeClaimEvent event) -> {
				final User user = event.getCause().first(User.class).orElse(null);
				if (user == null) {
					return;
				}
				final Player player = Bukkit.getPlayer(user.getUniqueId());
				if (player == null) {
					return;
				}
				final Transaction b = RealEstate.transactionsStore.getTransaction(event.getClaim());
				if(b != null && player.getUniqueId().equals(b.getOwner()) && b instanceof BoughtTransaction)
				{
					if(((BoughtTransaction)b).getBuyer() != null) {
						event.setMessage(Component.text("This claim is currently involved in a transaction, you can't resize it!"));
						event.cancelled(true);
					}
				}
			});
		}
	}

	private class RemoveClaimEventListener {

		public RemoveClaimEventListener() {
			final EventBus<Event> eventBus = GriefDefender.getEventManager().getBus();

			eventBus.subscribe(RemoveClaimEvent.class, (RemoveClaimEvent event) -> {
				final User user = event.getCause().first(User.class).orElse(null);
				if (user == null) {
					return;
				}
				final Player player = Bukkit.getPlayer(user.getUniqueId());
				if (player == null) {
					return;
				}
				final Transaction b = RealEstate.transactionsStore.getTransaction(event.getClaim());
				if(b != null && player.getUniqueId().equals(b.getOwner()) && b instanceof BoughtTransaction)
				{
					if(((BoughtTransaction)b).getBuyer() != null) {
						event.setMessage(Component.text("This claim is currently involved in a transaction, you can't remove it!"));
						event.cancelled(true);
					}
				}
			});
		}
	}
}
