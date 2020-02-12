package com.zombies.spawning;

import com.zombies.COMZombies;
import com.zombies.config.CustomConfig;
import com.zombies.game.Game;
import com.zombies.game.Game.ArenaStatus;
import com.zombies.game.features.Barrier;
import com.zombies.game.features.Door;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.*;

import java.util.*;
import java.util.logging.Level;

public class SpawnManager {
	private COMZombies plugin;
	private Game game;
	private HashMap<Entity, Double> health = new HashMap<Entity, Double>();
	private ArrayList<SpawnPoint> points = new ArrayList<SpawnPoint>();
	private ArrayList<Entity> mobs = new ArrayList<Entity>();
	private ArrayList<Entity> mobsToRemove = new ArrayList<Entity>();
	private boolean canSpawn = false;
	private int zombieSpawnInterval;
	private int zombiesSpawned = 0;
	private int zombiesToSpawn = 0;
	private Random random;

	public SpawnManager(COMZombies plugin, Game game) {
		this.plugin = plugin;
		this.game = game;
		zombieSpawnInterval = plugin.getConfig().getInt("config.gameSettings.zombieSpawnDelay");
		random = new Random();
	}

	public void loadAllSpawnsToGame() {
		CustomConfig config = plugin.configManager.getConfig("ArenaConfig");
		points.clear();
		try {
			for (String key : config.getConfigurationSection(game.getName() + ".ZombieSpawns").getKeys(false)) {
				double x = config.getDouble(game.getName() + ".ZombieSpawns." + key + ".x");
				double y = config.getDouble(game.getName() + ".ZombieSpawns." + key + ".y");
				double z = config.getDouble(game.getName() + ".ZombieSpawns." + key + ".z");
				Location loc = new Location(game.getWorld(), x, y, z);
				SpawnPoint point = new SpawnPoint(loc, game, loc.getBlock().getType(), key);
				points.add(point);
			}
		} catch (Exception e) {
			COMZombies.getInstance().log.log(Level.WARNING,
					" Failed to load one or more spawn points in the arena " + game.getName());
			COMZombies.getInstance().log.log(Level.WARNING,
					"Please report this to the authors and include your arena config");
		}
	}

	public SpawnPoint getSpawnPoint(String name) {
		for (SpawnPoint p : points) {
			if (name.equalsIgnoreCase(p.getName())) {
				return p;
			}
		}
		return null;
	}

	public SpawnPoint getSpawnPoint(Location loc) {
		for (SpawnPoint point : points) {
			if (point.getLocation().equals(loc)) {
				return point;
			}
		}
		return null;
	}

	public void removePoint(Player player, SpawnPoint point) {
		CustomConfig config = plugin.configManager.getConfig("ArenaConfig");
		if (points.contains(point)) {
			Location loc = point.getLocation();
			config.set(game.getName() + ".ZombieSpawns." + point.getName(), null);
			config.saveConfig();
			loadAllSpawnsToGame();
			player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Spawn point removed!");
			Block block = loc.getBlock();
			block.setType(Material.AIR);
			points.remove(point);
		}
	}

	public int getCurrentSpawn() {
		return points.size();
	}

	public ArrayList<SpawnPoint> getPoints() {
		return points;
	}

	public void killMob(Entity entity) {
		if (entity instanceof Player)
			return;

		while (!entity.isDead()) {
			entity.remove();
		}

		this.removeEntity(entity);
	}

	public void nuke() {
		killAll(false);
	}

	public void killAll(boolean nextWave) {
		ArrayList<Entity> mobs = this.mobs;
		for (int i = 0; i < mobs.size(); i++) {
			killMob(mobs.get(i));
		}
		if (nextWave)
			game.nextWave();
		mobs.clear();
	}

	public ArrayList<Entity> getEntities() {
		return mobs;
	}

	public void removeEntity(Entity entity) {
		if (mobs.contains(entity)) {
			mobsToRemove.add(entity);
		}
	}

	public void updateEntityList() {
		for (Entity ent : mobsToRemove) {
			health.remove(ent);
			mobs.remove(ent);
		}

		if ((mobs.size() == 0) && (zombiesSpawned >= zombiesToSpawn)) {
			game.nextWave();
		}
	}

	public HashMap<Entity, Double> totalHealth() {
		return health;
	}

	public void addPoint(SpawnPoint point) {
		if (game.mode == ArenaStatus.DISABLED)
			points.add(point);
	}

	// Finds the closest locations to point loc, it results numToGet amount of
	// spawn points

	public int getTotalSpawns() {
		return points.size();
	}

	public Game getGame() {
		return game;
	}

	private ArrayList<SpawnPoint> getNearestPoints(Location loc, int numToGet) {
		ArrayList<SpawnPoint> points = game.spawnManager.getPoints();
		if (numToGet <= 0 || points.size() == 0) {
			return null;
		}
		int numPoints = Math.min(numToGet, points.size());
		ArrayList<SpawnPoint> results = new ArrayList<SpawnPoint>(numPoints);
		for (int i = 0; i < numPoints; i++) {
			results.add(points.get(i));
		}
		ArrayList<Double> distances = new ArrayList<Double>();
		for (int i = 0; i < numToGet; i++) {
			distances.add(Double.POSITIVE_INFINITY);
		}
		for (int pointIndex = 0; pointIndex < points.size(); pointIndex++) {
			Location spawnLoc = points.get(pointIndex).getLocation();
			double dx = spawnLoc.getBlockX() - loc.getBlockX();
			double dy = spawnLoc.getBlockY() - loc.getBlockY();
			double dz = spawnLoc.getBlockZ() - loc.getBlockZ();
			double dist2 = (dx * dx) + (dy * dy) + (dz * dz);
			for (int resultIndex = 0; resultIndex < results.size(); resultIndex++) {
				if (dist2 >= distances.get(resultIndex)) {
					continue;
				}
				distances.add(resultIndex, dist2);
				results.add(resultIndex, points.get(pointIndex));
				results.remove(numPoints);
				distances.remove(numPoints);
				break;
			}
		}
		return results;
	}

	private void smartSpawn(final int wave, final List<Player> players) {

		if (!this.canSpawn || wave != game.waveNumber)
			return;
		if (game.mode != ArenaStatus.INGAME)
			return;
		if (this.zombiesSpawned >= this.zombiesToSpawn)
			return;

		if (mobs.size() >= plugin.config.maxZombies) {
			Runnable delayedSpawnFunc = new Runnable() {
				public void run() {
					synchronized (this) {
						smartSpawn(wave, players);
					}
				}
			};

			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, delayedSpawnFunc, zombieSpawnInterval * 20L);
			return;
		}

		int playersSize = players.size();

		int selectPlayer = random.nextInt(playersSize);
		SpawnPoint selectPoint = null;
		Player player = players.get(selectPlayer);
		ArrayList<SpawnPoint> points = getNearestPoints(player.getLocation(), zombiesToSpawn);
		int totalRetries = 0;
		int curr = 0;
		while (selectPoint == null) {
			if (curr == points.size()) {
				player = players.get(random.nextInt(playersSize));
				points = getNearestPoints(player.getLocation(), zombiesToSpawn / playersSize);
				curr = 0;
				continue;
			}
			selectPoint = points.get(random.nextInt(points.size()));
			if (!(canSpawn(selectPoint)))
				selectPoint = null;
			curr++;
			if (totalRetries > 1000)
				oopsWeHadAnError();
			totalRetries++;
		}
		scheduleSpawn(zombieSpawnInterval, selectPoint, wave, players);
	}

	private void scheduleSpawn(int time, SpawnPoint loc, final int wave, final List<Player> players) {
		if (!this.canSpawn || wave < game.waveNumber)
			return;

		double strength = (int) (((wave * 100) + 50) / 50);
		Location location = new Location(loc.getLocation().getWorld(), loc.getLocation().getBlockX(),
				loc.getLocation().getBlockY(), loc.getLocation().getBlockZ());
		location.add(0.5, 0, 0.5);
		Zombie zomb = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
		zomb.setBaby(false);
		setFollowDistance(zomb);
		setTotalHealth(zomb, strength);
		zomb.setHealth(strength <= 20D ? strength : 20D);
		if (game.spawnManager.getZombiesToSpawn() == 1) {
				setSpeed(zomb, (float) (Math.random()));
		}

		mobs.add(zomb);
		zombiesSpawned++;

		Barrier b = game.barrierManager.getBarrier(loc);
		if (b != null)
			b.initBarrier(zomb);

		Runnable delayedSpawnFunc = new Runnable() {
			public void run() {
				synchronized (this) {
					smartSpawn(wave, players);
				}
			}
		};
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, delayedSpawnFunc, time * 20L);
	}

	public void setFollowDistance(Entity zomb) {
		UUID id = UUID.randomUUID();
		LivingEntity entity = (LivingEntity) zomb;
		AttributeInstance attributes = entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);

		AttributeModifier modifier = new AttributeModifier(id, "COMZombies follow distance multiplier", 512.0F, AttributeModifier.Operation.ADD_NUMBER);

		attributes.addModifier(modifier);
	}

	public void setSpeed(Entity zomb, float speed) {
		UUID id = UUID.randomUUID();
		LivingEntity entity = (LivingEntity) zomb;
		AttributeInstance attributes = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);

		AttributeModifier modifier = new AttributeModifier(id, "COMZombies speed multiplier", speed, AttributeModifier.Operation.ADD_NUMBER);

		attributes.addModifier(modifier);
	}

	public void update() {
		if (game.mode != ArenaStatus.INGAME)
			return;
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				synchronized (this) {
					for (Entity zomb : mobs) {
						if (zomb.isDead()) {
							removeEntity(zomb);
						} else {
							Player closest = getNearestPlayer(zomb);
							Zombie z = (Zombie) zomb;
							z.setTarget(closest);
						}
					}

					updateEntityList();
					update();
				}
			}

			private Player getNearestPlayer(Entity e) {
				Player closest = null;
				for (Player player : SpawnManager.this.game.players) {
					if (closest == null || player.getLocation().distance(e.getLocation()) < closest.getLocation()
							.distance(e.getLocation())) {
						closest = player;
					}
				}
				return closest;
			}

		}, 100L);
	}

	public void setSpawnInterval(int interval) {
		this.zombieSpawnInterval = interval;
	}

	private boolean canSpawn(SpawnPoint point) {
		if (point == null)
			return false;
		boolean isContained = false;
		boolean maySpawn = false;
		for (Door door : game.doorManager.getDoors()) {
			for (SpawnPoint p : door.getSpawnsInRoomDoorLeadsTo()) {
				if (p.getLocation().equals(point.getLocation())) {
					if (door.isOpened()) {
						maySpawn = true;
					}
					isContained = true;
				}
			}
		}
		if (!isContained)
			return true;
		return maySpawn;
	}

	public void setTotalHealth(Entity entity, double totalHealth) {
		if (health.containsKey(entity)) {
			health.remove(entity);
		}
		health.put(entity, totalHealth);
	}

	private void oopsWeHadAnError() {
		for (Player pl : game.players) {
			pl.sendMessage(ChatColor.RED  + "Well..  I guess we had an error trying to pick a spawn point out of the many we had! We'll have to end your game because of our lack of skillez.");
		}
		game.endGame();
	}

	public void nextWave() {
		canSpawn = false;
		zombiesSpawned = 0;
		setSpawnInterval((int) (zombieSpawnInterval / 1.05));
		if (zombieSpawnInterval < 1) {
			zombieSpawnInterval = 1;
		}
	}

	public void startWave(int wave, final List<Player> players) {
		canSpawn = true;

		if (players.size() == 0) {
			this.game.endGame();
			Bukkit.broadcastMessage(
					COMZombies.prefix + "SmartSpawn was sent a players list with no players in it! Game was ended");
			return;
		}
		zombiesToSpawn = (int) ((wave * 0.15) * 30) + (2 * players.size());
		this.smartSpawn(wave, players);
	}

	public int getZombiesToSpawn() {
		return this.zombiesToSpawn;
	}

	public int getZombiesSpawned() {
		return this.zombiesSpawned;
	}

	public int getZombiesAlive() {
		return this.mobs.size();
	}

	public int getSpawnInterval() {
		return this.zombieSpawnInterval;
	}

	public boolean isEntitySpawned(Entity ent) {
		return this.mobs.contains(ent);
	}

	public void reset() {
		this.mobs.clear();
		this.canSpawn = false;
		this.zombiesSpawned = 0;
		this.zombiesToSpawn = 0;
		this.zombieSpawnInterval = plugin.getConfig().getInt("config.gameSettings.waveSpawnInterval");
	}

	/**
	 * gets the current config that the game is on
	 * 
	 * @return the spawn point that the game is on
	 */
	public int getCurrentSpawnPoint() {
		int spawnNum = 0;
		try {
			for (@SuppressWarnings("unused")
			String key : plugin.configManager.getConfig("ArenaConfig")
					.getConfigurationSection(game.getName() + ".ZombieSpawns").getKeys(false)) {
				spawnNum++;
			}
		} catch (NullPointerException e) {
		}
		return spawnNum + 1;
	}

	/**
	 * Adds a spawnPoint to the Arena config file.
	 * 
	 * @param spawn
	 */
	public void addSpawnToConfig(SpawnPoint spawn) {
		World world = null;
		CustomConfig conf = plugin.configManager.getConfig("ArenaConfig");
		try {
			world = spawn.getLocation().getWorld();
		} catch (Exception e) {
			Bukkit.broadcastMessage(
					ChatColor.RED + "" + ChatColor.BOLD + "[Zombies] Could not retrieve the world " + world.getName());
			return;
		}
		double x = spawn.getLocation().getBlockX();
		double y = spawn.getLocation().getBlockY();
		double z = spawn.getLocation().getBlockZ();
		int spawnNum = getCurrentSpawnPoint();
		conf.set(game.getName() + ".ZombieSpawns.spawn" + spawnNum, null);
		conf.set(game.getName() + ".ZombieSpawns.spawn" + spawnNum + ".x", x);
		conf.set(game.getName() + ".ZombieSpawns.spawn" + spawnNum + ".y", y);
		conf.set(game.getName() + ".ZombieSpawns.spawn" + spawnNum + ".z", z);

		conf.saveConfig();
	}
}
