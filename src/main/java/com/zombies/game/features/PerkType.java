package com.zombies.game.features;

import com.zombies.COMZombies;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public enum PerkType
{
	JUGGERNOG, SPEED_COLA, QUICK_REVIVE, DOUBLE_TAP, STAMIN_UP, PHD_FLOPPER, DEADSHOT_DAIQ, MULE_KICK, @Deprecated
	TOMBSTONE_SODA, ELECTRIC_C, @Deprecated
	AMMO_0_MATIC, @Deprecated
	WHOS_WHO;

	public PerkType getPerkType(String name)
	{
		for (PerkType pt : values())
		{
			if ((ChatColor.GOLD + pt.toString()).equalsIgnoreCase(name) || (pt.toString().equalsIgnoreCase(name))) { return pt; }
		}
		return null;
	}

	public void initialEffect(COMZombies plugin, final Player player, PerkType type, int slot)
	{
		final World world = player.getLocation().getWorld();
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
		{

			@Override
			public void run()
			{
				world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1L, 1L);
			}

		}, 5L);
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
		{

			@Override
			public void run()
			{
				world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1L, 1L);
			}

		}, 10L);
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
		{

			@Override
			public void run()
			{
				world.playEffect(player.getLocation(), Effect.POTION_BREAK, 1);
			}

		}, 20L);
		ItemStack stack = new ItemStack(Material.AIR, 1);
		String Perktype = "";
		switch (type)
		{
			case JUGGERNOG:
				stack = new ItemStack(Material.CHAINMAIL_CHESTPLATE, 1);
				Perktype = "Juggernog";
				break;
			case SPEED_COLA:
				stack = new ItemStack(Material.FEATHER, 1);
				Perktype = "Speed Cola";
				break;
			case QUICK_REVIVE:
				stack = new ItemStack(Material.GLISTERING_MELON_SLICE, 1);
				Perktype = "Quick Revive";
				break;
			case DOUBLE_TAP:
				stack = new ItemStack(Material.REPEATER, 1);
				Perktype = "Double Tap";
				break;
			case STAMIN_UP:
				stack = new ItemStack(Material.SUGAR, 1);
				Perktype = "Stamina Up";
				break;
			case PHD_FLOPPER:
				stack = new ItemStack(Material.FIRE_CHARGE);
				Perktype = "PHD Flopper";
				break;
			case DEADSHOT_DAIQ:
				stack = new ItemStack(Material.GUNPOWDER);
				Perktype = "Deadshot Daiquiri";
				break;
			case MULE_KICK:
				stack = new ItemStack(Material.STRING);
				Perktype = "Mule Kick";
				break;
			case ELECTRIC_C:
				stack = new ItemStack(Material.NETHER_STAR);
				Perktype = "Electric Cherry";
				break;
			case AMMO_0_MATIC:
				break;
			case TOMBSTONE_SODA:
				// stack = new ItemStack(Material.COBBLE_WALL);
				break;
			case WHOS_WHO:
				// stack = new ItemStack(Material.GoldenApple);
				break;
			default:
				break;

		}
		player.getInventory().setItem(slot, setItemMeta(stack, Perktype));
		player.updateInventory();
	}

	public static void noPower(COMZombies plugin, Player player)
	{
		World world = player.getLocation().getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_GHAST_AMBIENT, 1L, 1L);
	}

	private ItemStack setItemMeta(ItemStack item, String type)
	{
		ItemMeta data = item.getItemMeta();
		data.setDisplayName(type);
		item.setItemMeta(data);
		return item;
	}

	public ItemStack getPerkItem(PerkType type)
	{
		ItemStack stack = new ItemStack(Material.AIR, 1);
		switch (type)
		{
			case JUGGERNOG:
				stack = new ItemStack(Material.CHAINMAIL_CHESTPLATE, 1);
				break;
			case SPEED_COLA:
				stack = new ItemStack(Material.FEATHER, 1);
				break;
			case QUICK_REVIVE:
				stack = new ItemStack(Material.GLISTERING_MELON_SLICE, 1);
				break;
			case DOUBLE_TAP:
				stack = new ItemStack(Material.REPEATER, 1);
				break;
			case STAMIN_UP:
				stack = new ItemStack(Material.SUGAR, 1);
				break;
			case PHD_FLOPPER:
				stack = new ItemStack(Material.FIRE_CHARGE);
				break;
			case DEADSHOT_DAIQ:
				stack = new ItemStack(Material.GUNPOWDER);
				break;
			case MULE_KICK:
				stack = new ItemStack(Material.STRING);
				break;
			case ELECTRIC_C:
				stack = new ItemStack(Material.NETHER_STAR);
				break;
			case AMMO_0_MATIC:
				break;
			case TOMBSTONE_SODA:
				break;
			case WHOS_WHO:
				break;
			default:
				break;
		}
		return stack;
	}
}