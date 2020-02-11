package com.zombies.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.zombies.COMZombies;
import com.zombies.game.Game;

public class ReloadCommand implements SubCommand
{

	private COMZombies plugin;

	public ReloadCommand(ZombiesCommand cmd)
	{
		plugin = cmd.plugin;
	}

	public boolean onCommand(Player player, String[] args)
	{
		if (player.hasPermission("zombies.reload") || player.hasPermission("zombies.admin"))
		{
			try
			{
				Bukkit.getServer().getPluginManager().disablePlugin(plugin);
				Bukkit.getServer().getPluginManager().enablePlugin(plugin);
				plugin.configManager.getConfig("ArenaConfig").reloadConfig();
				plugin.reloadConfig();
				for (Game gl : plugin.manager.games)
				{
					gl.endGame();
					gl.setDisabled();
				}
				plugin.manager.games.clear();
				plugin.manager.loadAllGames();
				plugin.clearAllSetup();
				for (Game gl : plugin.manager.games)
				{
					gl.enable();
				}
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "" + ChatColor.BOLD + "Zombies has been reloaded!");
			} catch (org.bukkit.command.CommandException e)
			{
			}
			return true;
		}
		else
		{
			CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "You do not have permission to reload zombies!");
			return true;
		}
	}
}
