package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

/**
 * FIXED DropSystem - Only spawns ammo for implemented weapons
 */
public class DropSystem {

    // ADDED: Define which ammo types are actually usable in the game
    private static final AmmoType[] IMPLEMENTED_AMMO_TYPES = {
            AmmoType.PISTOL_9MM,  // Used by Revolver
            AmmoType.SMG_9MM      // Used by Sten Gun
            // Note: RIFLE_556 and SHOTGUN_12G are excluded since no weapons use them
    };

    /**
     * Process enemy death with score tracking and health pack drops
     * FIXED: Only drops ammo for implemented weapons
     */
    public static void processEnemyDeath(Entity enemy, EntityManager entityManager,
                                         AssetManager assetManager, AudioManager audioManager,
                                         ScoreSystem scoreSystem) {
        if (enemy == null || entityManager == null) return;

        // Award kill points to score system
        if (scoreSystem != null) {
            scoreSystem.addKill();
        }

        Vector3f dropPosition = enemy.getPosition().add(
                (FastMath.nextRandomFloat() - 0.5f) * 2f, // Random X offset
                0.2f, // Slightly above ground
                (FastMath.nextRandomFloat() - 0.5f) * 2f  // Random Z offset
        );

        // HIGHEST PRIORITY: Check implemented ammo types first
        for (AmmoType ammoType : IMPLEMENTED_AMMO_TYPES) {
            float dropChance = ammoType.getEnemyDropChance(); // Random chance per ammo type

            if (FastMath.nextRandomFloat() < dropChance) {
                // Create ammo drop at enemy position
                AmmoPickup ammoDrop = new AmmoPickup(dropPosition, ammoType, assetManager, audioManager);
                entityManager.addEntity(ammoDrop);

                System.out.println("Enemy dropped: " + ammoType.displayName);
                return; // Only drop one item per enemy
            }
        }

        // SECOND PRIORITY: 15% chance for health pack drop
        if (FastMath.nextRandomFloat() < 0.15f) {
            HealthPickup healthDrop = new HealthPickup(dropPosition, assetManager, audioManager);
            entityManager.addEntity(healthDrop);
            System.out.println("Enemy dropped: Health Pack");
            return; // Only drop one item per enemy
        }

        // LOWEST PRIORITY: 10% chance for weapon drop
        if (FastMath.nextRandomFloat() < 0.10f) {
            WeaponPickup weaponDrop = createRandomWeaponSpawn(dropPosition, assetManager, audioManager);
            entityManager.addEntity(weaponDrop);
            System.out.println("Enemy dropped: " + weaponDrop.getWeaponType().displayName +
                    " with " + weaponDrop.getStartingAmmo() + " rounds");
            return; // Only drop one item per enemy
        }
    }

    /**
     * Backward compatibility: Original method without score system
     */
    public static void processEnemyDeath(Entity enemy, EntityManager entityManager,
                                         AssetManager assetManager, AudioManager audioManager) {
        processEnemyDeath(enemy, entityManager, assetManager, audioManager, null);
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
     * FIXED: Create random ammo spawn - only for implemented weapons
     */
    public static AmmoPickup createRandomAmmoSpawn(Vector3f position, AssetManager assetManager, AudioManager audioManager) {
        // Calculate total spawn chance for implemented ammo types only
        float totalChance = 0f;
        for (AmmoType ammoType : IMPLEMENTED_AMMO_TYPES) {
            totalChance += ammoType.worldSpawnChance;
        }

        // Pick random ammo type based on spawn chances (only implemented types)
        float roll = FastMath.nextRandomFloat() * totalChance;
        float accumulator = 0f;

        for (AmmoType ammoType : IMPLEMENTED_AMMO_TYPES) {
            accumulator += ammoType.worldSpawnChance;
            if (roll <= accumulator) {
                return new AmmoPickup(position, ammoType, assetManager, audioManager);
            }
        }

        // Fallback to first implemented ammo type
        return new AmmoPickup(position, IMPLEMENTED_AMMO_TYPES[0], assetManager, audioManager);
    }

    /**
     * Create random health pack spawn
     */
    public static HealthPickup createRandomHealthSpawn(Vector3f position, AssetManager assetManager, AudioManager audioManager) {
        // 70% small, 25% medium, 5% large health packs
        float roll = FastMath.nextRandomFloat();

        if (roll < 0.70f) {
            return HealthPickup.createSmallHealthPack(position, assetManager, audioManager);
        } else if (roll < 0.95f) {
            return HealthPickup.createMediumHealthPack(position, assetManager, audioManager);
        } else {
            return HealthPickup.createLargeHealthPack(position, assetManager, audioManager);
        }
    }

    /**
     * ADDED: Get list of implemented ammo types (for debugging)
     */
    public static AmmoType[] getImplementedAmmoTypes() {
        return IMPLEMENTED_AMMO_TYPES.clone();
    }

    /**
     * ADDED: Check if an ammo type is implemented (has a weapon that uses it)
     */
    public static boolean isAmmoTypeImplemented(AmmoType ammoType) {
        for (AmmoType implementedType : IMPLEMENTED_AMMO_TYPES) {
            if (implementedType == ammoType) {
                return true;
            }
        }
        return false;
    }
}