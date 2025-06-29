package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

/**
 * DropSystem - Handles enemy death drops and world spawning
 */
public class DropSystem {

    /**
     * Process enemy death and potentially create ammo drop
     */
    public static void processEnemyDeath(Entity enemy, EntityManager entityManager,
                                         AssetManager assetManager, AudioManager audioManager) {
        if (enemy == null || entityManager == null) return;

        // Random chance for each ammo type (as requested - all random)
        for (AmmoType ammoType : AmmoType.values()) {
            float dropChance = ammoType.getEnemyDropChance(); // Random chance per ammo type

            if (FastMath.nextRandomFloat() < dropChance) {
                // Create ammo drop at enemy position
                Vector3f dropPosition = enemy.getPosition().add(
                        (FastMath.nextRandomFloat() - 0.5f) * 2f, // Random X offset
                        0.2f, // Slightly above ground
                        (FastMath.nextRandomFloat() - 0.5f) * 2f  // Random Z offset
                );

                AmmoPickup ammoDrop = new AmmoPickup(dropPosition, ammoType, assetManager, audioManager);
                entityManager.addEntity(ammoDrop);

                System.out.println("Enemy dropped: " + ammoType.displayName);
                break; // Only drop one type per enemy
            }
        }
    }

    /**
     * Create random weapon spawn
     */
    public static WeaponPickup createRandomWeaponSpawn(Vector3f position, AssetManager assetManager, AudioManager audioManager) {
        // Pick random weapon type
        WeaponType[] weapons = WeaponType.values();
        int randomIndex = (int)(FastMath.nextRandomFloat() * weapons.length);
        WeaponType randomWeapon = weapons[randomIndex];

        return new WeaponPickup(position, randomWeapon, assetManager, audioManager);
    }

    /**
     * Create random ammo spawn
     */
    public static AmmoPickup createRandomAmmoSpawn(Vector3f position, AssetManager assetManager, AudioManager audioManager) {
        // Pick random ammo type based on spawn chances
        float roll = FastMath.nextRandomFloat();
        float accumulator = 0f;

        for (AmmoType ammoType : AmmoType.values()) {
            accumulator += ammoType.worldSpawnChance;
            if (roll <= accumulator) {
                return new AmmoPickup(position, ammoType, assetManager, audioManager);
            }
        }

        // Fallback to first ammo type
        return new AmmoPickup(position, AmmoType.PISTOL_9MM, assetManager, audioManager);
    }
}