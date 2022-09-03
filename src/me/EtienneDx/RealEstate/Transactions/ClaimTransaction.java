package me.EtienneDx.RealEstate.Transactions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import no.vestlandetmc.rd.handler.Region;

public abstract class ClaimTransaction implements ConfigurationSerializable, Transaction
{
	public UUID claimId;
	public UUID owner = null;
	public double price;
	public Location sign = null;

	public ClaimTransaction(Region claim, Player player, double price, Location sign)
	{
		this.claimId = claim.getRegionID();
		this.owner = player != null ? player.getUniqueId() : null;
		this.price = price;
		this.sign = sign;
	}

	public ClaimTransaction(Map<String, Object> map)
	{
		this.claimId = UUID.fromString(String.valueOf(map.get("claimId")));
		if(map.get("owner") != null)
			this.owner = UUID.fromString((String) map.get("owner"));
		this.price = (double) map.get("price");
		if(map.get("signLocation") != null)
			this.sign = (Location) map.get("signLocation");
	}

	public ClaimTransaction()
	{

	}

	@Override
	public Map<String, Object> serialize()
	{
		final Map<String, Object> map = new HashMap<>();

		map.put("claimId", this.claimId.toString());
		if(owner != null)
			map.put("owner", owner.toString());
		map.put("price", this.price);
		if(sign != null)
			map.put("signLocation", sign);

		return map;
	}

	@Override
	public Block getHolder()
	{
		return sign.getBlock().getState() instanceof Sign ? sign.getBlock() : null;
	}

	@Override
	public UUID getOwner()
	{
		return owner;
	}

	@Override
	public boolean tryCancelTransaction(Player p)
	{
		return this.tryCancelTransaction(p, false);
	}
}
