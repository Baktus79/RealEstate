package me.EtienneDx.RealEstate.Transactions;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import me.EtienneDx.RealEstate.RealEstate;
import no.vestlandetmc.rd.handler.Region;

public abstract class BoughtTransaction extends ClaimTransaction
{
	public UUID buyer = null;
	public ExitOffer exitOffer = null;
	public boolean destroyedSign = false;

	public BoughtTransaction(Map<String, Object> map)
	{
		super(map);
		if(map.get("buyer") != null)
			buyer = UUID.fromString((String)map.get("buyer"));
		if(map.get("exitOffer") != null)
			exitOffer = (ExitOffer) map.get("exitOffer");
		if(map.get("destroyedSign") != null)// may be the case on upgrading from 0.0.1-SNAPSHOT
			destroyedSign = (boolean) map.get("destroyedSign");
	}

	public BoughtTransaction(Region claim, Player player, double price, Location sign)
	{
		super(claim, player, price, sign);
	}

	@Override
	public Map<String, Object> serialize()
	{
		final Map<String, Object> map = super.serialize();
		if(buyer != null)
			map.put("buyer", buyer.toString());
		if(exitOffer != null)
			map.put("exitOffer", exitOffer);
		map.put("destroyedSign",  destroyedSign);

		return map;
	}

	public void destroySign()
	{
		if(this instanceof ClaimRent && RealEstate.instance.config.cfgDestroyRentSigns ||
				this instanceof ClaimLease && RealEstate.instance.config.cfgDestroyLeaseSigns)
		{
			if(!destroyedSign && getHolder().getState() instanceof Sign)
				getHolder().breakNaturally();
			destroyedSign = true;
		}
	}

	public UUID getBuyer()
	{
		return buyer;
	}

	@Override
	public void setOwner(UUID newOwner)
	{
		this.owner = newOwner;
	}
}
