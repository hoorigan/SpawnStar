package com.winterhaven_mc.spawnstar;

import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A self-cancelling, repeating task that generates ender signal particles
 * at a player's location as long as they are in the warmup hashmap
 * 
 * @author savage
 *
 */
class ParticleTask extends BukkitRunnable {

	SpawnStarMain plugin;
	Player player;

	/**
	 * Class constructor method
	 */
	ParticleTask(final Player player) {
		
		this.plugin = SpawnStarMain.instance;
		this.player = player;
		
	}
	
		
	@Override
	public void run() {

		// if player is in the warmup hashmap, display the particle effect at their location
		if (plugin.warmupManager.isWarmingUp(player)) {
			player.getWorld().playEffect(player.getLocation().add(0.0d, 1.0d, 0.0d), Effect.ENDER_SIGNAL, 0, 10);
		}
		// otherwise cancel this repeating task if the player is not in the warmup hashmap
		else {
			this.cancel();
		}
	}

}
