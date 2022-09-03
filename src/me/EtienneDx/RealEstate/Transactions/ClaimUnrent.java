package me.EtienneDx.RealEstate.Transactions;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import no.vestlandetmc.rd.handler.Region;

public class ClaimUnrent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();
	private final Region shopClaim;
	private final OfflinePlayer player;

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	public ClaimUnrent(Region shopPlot, OfflinePlayer player) {
		this.shopClaim = shopPlot;
		this.player = player;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public OfflinePlayer getRenter() {
		return this.player;
	}

	public UUID getPlotUUID() {
		return shopClaim.getRegionID();
	}

	public UUID getTownUUID() {
		return shopClaim.isInTown() ? shopClaim.getParentID() : null;
	}

}
