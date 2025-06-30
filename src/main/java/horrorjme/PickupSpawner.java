package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import java.util.ArrayList;
import java.util.List;

/**
 * Updated PickupSpawner - Now includes health pack spawning for point economy
 */
public class PickupSpawner {

    private AssetManager assetManager;
    private AudioManager audioManager;
    private BulletAppState bulletAppState;

    // Spawn configuration
    private List<Vector3f> validSpawnPoints;
    private List<Vector3f> weaponSpawnPoints;
    private List<Vector3f> ammoSpawnPoints;

    // Spawn parameters (reduced for point economy)
    private int minWeapons = 2;
    private int maxWeapons = 3;
    private int minAmmoPickups = 4;
    private int maxAmmoPickups = 8;
    private int minHealthPickups = 2;
    private int maxHealthPickups = 4;

    // Map scanning parameters
    private float groundScanHeight = 50f;
    private float minSpawnDistance = 5f;
    private float maxSpawnDistance = 25f;
    private float minPickupDistance = 3f;
    private float playerStartRadius = 8f;
    private Vector3f playerStartPosition;

    public PickupSpawner(AssetManager assetManager, AudioManager audioManager, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.audioManager = audioManager;
        this.bulletAppState = bulletAppState;
        this.validSpawnPoints = new ArrayList<>();
        this.weaponSpawnPoints = new ArrayList<>();
        this.ammoSpawnPoints = new ArrayList<>();
    }

    /**
     * Scan the loaded map for valid pickup spawn points
     */
    public void scanMapForSpawnPoints(Spatial map, Vector3f playerStart) {
        this.playerStartPosition = playerStart.clone();

        System.out.println("Scanning map for pickup spawn points...");
        System.out.println("Spawn distance range: " + minSpawnDistance + " to " + maxSpawnDistance);

        validSpawnPoints.clear();
        weaponSpawnPoints.clear();
        ammoSpawnPoints.clear();

        scanRadialAroundPlayer();

        System.out.printf("Scan complete: %d valid spawn points found%n", validSpawnPoints.size());
        System.out.printf("Weapon locations: %d, Ammo locations: %d%n",
                weaponSpawnPoints.size(), ammoSpawnPoints.size());
    }

    /**
     * Scan in a circle around the player at reasonable distances
     */
    private void scanRadialAroundPlayer() {
        int totalChecks = 0;
        int validPoints = 0;

        for (float radius = minSpawnDistance; radius <= maxSpawnDistance; radius += 2f) {
            int pointsAtRadius = Math.max(8, (int)(radius * 0.8f));

            for (int i = 0; i < pointsAtRadius; i++) {
                float angle = (float)(2 * Math.PI * i / pointsAtRadius);

                float x = playerStartPosition.x + radius * FastMath.cos(angle);
                float z = playerStartPosition.z + radius * FastMath.sin(angle);

                totalChecks++;

                Vector3f groundPos = findGroundLevel(x, z, playerStartPosition.y + groundScanHeight);

                if (groundPos != null && isValidSpawnLocation(groundPos)) {
                    validSpawnPoints.add(groundPos);
                    validPoints++;

                    if (isGoodWeaponLocation(groundPos, radius)) {
                        weaponSpawnPoints.add(groundPos);
                    } else {
                        ammoSpawnPoints.add(groundPos);
                    }
                }
            }
        }

        System.out.printf("Radial scan: %d valid spawn points found from %d locations checked%n",
                validPoints, totalChecks);
    }

    /**
     * Find ground level at specific X,Z coordinates using raycasting
     */
    private Vector3f findGroundLevel(float x, float z, float startY) {
        if (bulletAppState == null) {
            return new Vector3f(x, 0, z);
        }

        Vector3f rayStart = new Vector3f(x, startY, z);
        Vector3f rayEnd = new Vector3f(x, startY - 100f, z);

        List<PhysicsRayTestResult> results = bulletAppState.getPhysicsSpace().rayTest(rayStart, rayEnd);

        for (PhysicsRayTestResult result : results) {
            float hitFraction = result.getHitFraction();
            Vector3f rayDirection = rayEnd.subtract(rayStart);
            Vector3f hitPoint = rayStart.add(rayDirection.mult(hitFraction));

            return hitPoint.add(0, 0.2f, 0);
        }

        return null;
    }

    /**
     * Check if a location is valid for spawning pickups
     */
    private boolean isValidSpawnLocation(Vector3f position) {
        if (playerStartPosition != null) {
            float distanceFromStart = position.distance(playerStartPosition);
            if (distanceFromStart < playerStartRadius) {
                return false;
            }
        }

        for (Vector3f existingPoint : validSpawnPoints) {
            if (position.distance(existingPoint) < minPickupDistance) {
                return false;
            }
        }

        if (bulletAppState != null) {
            Vector3f checkStart = position.clone();
            Vector3f checkEnd = position.add(0, 2f, 0);

            List<PhysicsRayTestResult> results = bulletAppState.getPhysicsSpace().rayTest(checkStart, checkEnd);
            if (!results.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determine if a location is good for weapon spawning
     */
    private boolean isGoodWeaponLocation(Vector3f position, float radius) {
        boolean goodDistance = radius >= 10f && radius <= 20f;
        boolean randomFactor = FastMath.nextRandomFloat() < 0.4f;
        return goodDistance || randomFactor;
    }

    /**
     * Spawn random weapons throughout the map
     */
    public void spawnRandomWeapons(EntityManager entityManager) {
        if (weaponSpawnPoints.isEmpty()) {
            System.err.println("No weapon spawn points available!");
            return;
        }

        int weaponRange = maxWeapons - minWeapons + 1;
        int weaponCount = minWeapons + (int)(FastMath.nextRandomFloat() * weaponRange);

        System.out.println("Spawning " + weaponCount + " weapons...");

        List<Vector3f> shuffledPoints = new ArrayList<>(weaponSpawnPoints);

        for (int i = 0; i < weaponCount && i < shuffledPoints.size(); i++) {
            int randomIndex = (int)(FastMath.nextRandomFloat() * shuffledPoints.size());
            Vector3f spawnPos = shuffledPoints.remove(randomIndex);

            WeaponPickup weapon = DropSystem.createRandomWeaponSpawn(spawnPos, assetManager, audioManager);
            entityManager.addEntity(weapon);

            System.out.printf("Spawned %s at (%.1f, %.1f, %.1f) - Distance from player: %.1f%n",
                    weapon.getWeaponType().displayName,
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    spawnPos.distance(playerStartPosition));
        }
    }

    /**
     * Spawn random ammo pickups throughout the map
     */
    public void spawnRandomAmmo(EntityManager entityManager) {
        if (ammoSpawnPoints.isEmpty()) {
            System.err.println("No ammo spawn points available!");
            return;
        }

        int ammoRange = maxAmmoPickups - minAmmoPickups + 1;
        int ammoCount = minAmmoPickups + (int)(FastMath.nextRandomFloat() * ammoRange);

        System.out.println("Spawning " + ammoCount + " ammo pickups...");

        List<Vector3f> shuffledPoints = new ArrayList<>(ammoSpawnPoints);

        for (int i = 0; i < ammoCount && i < shuffledPoints.size(); i++) {
            int randomIndex = (int)(FastMath.nextRandomFloat() * shuffledPoints.size());
            Vector3f spawnPos = shuffledPoints.remove(randomIndex);

            AmmoPickup ammo = DropSystem.createRandomAmmoSpawn(spawnPos, assetManager, audioManager);
            entityManager.addEntity(ammo);

            System.out.printf("Spawned %s at (%.1f, %.1f, %.1f) - Distance from player: %.1f%n",
                    ammo.getAmmoType().displayName,
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    spawnPos.distance(playerStartPosition));
        }
    }

    /**
     * Spawn random health pickups throughout the map
     */
    public void spawnRandomHealthPacks(EntityManager entityManager) {
        if (ammoSpawnPoints.isEmpty()) {
            System.err.println("No health spawn points available!");
            return;
        }

        int healthRange = maxHealthPickups - minHealthPickups + 1;
        int healthCount = minHealthPickups + (int)(FastMath.nextRandomFloat() * healthRange);

        System.out.println("Spawning " + healthCount + " health pickups...");

        List<Vector3f> shuffledPoints = new ArrayList<>(ammoSpawnPoints);

        for (int i = 0; i < healthCount && i < shuffledPoints.size(); i++) {
            int randomIndex = (int)(FastMath.nextRandomFloat() * shuffledPoints.size());
            Vector3f spawnPos = shuffledPoints.remove(randomIndex);

            HealthPickup health = DropSystem.createRandomHealthSpawn(spawnPos, assetManager, audioManager);
            entityManager.addEntity(health);

            System.out.printf("Spawned Health Pack at (%.1f, %.1f, %.1f) - Distance from player: %.1f%n",
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    spawnPos.distance(playerStartPosition));
        }
    }

    /**
     * Spawn all pickups including health packs
     */
    public void spawnAllPickups(EntityManager entityManager) {
        spawnRandomWeapons(entityManager);
        spawnRandomAmmo(entityManager);
        spawnRandomHealthPacks(entityManager);

        System.out.println("All pickups (including health packs) spawned successfully!");
    }

    /**
     * Configuration presets
     */
    public void applySpawnPreset(SpawnPreset preset) {
        switch (preset) {
            case SCARCE_RESOURCES:
                setWeaponSpawnRange(1, 2);
                setAmmoSpawnRange(3, 5);
                setHealthSpawnRange(1, 2);
                setSpawnDistanceRange(8f, 20f);
                setMinSpawnDistance(8f);
                setPlayerStartRadius(6f);
                break;

            case NORMAL_RESOURCES:
                setWeaponSpawnRange(2, 3);
                setAmmoSpawnRange(4, 8);
                setHealthSpawnRange(2, 3);
                setSpawnDistanceRange(6f, 25f);
                setMinSpawnDistance(5f);
                setPlayerStartRadius(8f);
                break;

            case ABUNDANT_RESOURCES:
                setWeaponSpawnRange(3, 4);
                setAmmoSpawnRange(6, 10);
                setHealthSpawnRange(3, 5);
                setSpawnDistanceRange(5f, 30f);
                setMinSpawnDistance(3f);
                setPlayerStartRadius(5f);
                break;

            case TESTING_MODE:
                setWeaponSpawnRange(4, 6);
                setAmmoSpawnRange(8, 12);
                setHealthSpawnRange(4, 6);
                setSpawnDistanceRange(3f, 15f);
                setMinSpawnDistance(2f);
                setPlayerStartRadius(3f);
                break;
        }

        System.out.println("Applied spawn preset: " + preset);
        System.out.println("Spawn distance: " + minSpawnDistance + " to " + maxSpawnDistance);
    }

    public enum SpawnPreset {
        SCARCE_RESOURCES,
        NORMAL_RESOURCES,
        ABUNDANT_RESOURCES,
        TESTING_MODE
    }

    // Configuration methods
    public void setWeaponSpawnRange(int min, int max) {
        this.minWeapons = Math.max(0, min);
        this.maxWeapons = Math.max(min, max);
    }

    public void setAmmoSpawnRange(int min, int max) {
        this.minAmmoPickups = Math.max(0, min);
        this.maxAmmoPickups = Math.max(min, max);
    }

    public void setHealthSpawnRange(int min, int max) {
        this.minHealthPickups = Math.max(0, min);
        this.maxHealthPickups = Math.max(min, max);
    }

    public void setMinSpawnDistance(float distance) {
        this.minSpawnDistance = Math.max(1f, distance);
    }

    public void setSpawnDistanceRange(float minDistance, float maxDistance) {
        this.minSpawnDistance = Math.max(1f, minDistance);
        this.maxSpawnDistance = Math.max(minDistance + 1f, maxDistance);
    }

    public void setMinPickupDistance(float distance) {
        this.minPickupDistance = Math.max(1f, distance);
    }

    public void setPlayerStartRadius(float radius) {
        this.playerStartRadius = Math.max(0f, radius);
    }

    public void clearSpawnPoints() {
        validSpawnPoints.clear();
        weaponSpawnPoints.clear();
        ammoSpawnPoints.clear();
        System.out.println("Cleared all spawn points");
    }

    public String getSpawnStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== PICKUP SPAWN STATISTICS ===\n");
        stats.append("Total valid spawn points: ").append(validSpawnPoints.size()).append("\n");
        stats.append("Weapon spawn points: ").append(weaponSpawnPoints.size()).append("\n");
        stats.append("Ammo spawn points: ").append(ammoSpawnPoints.size()).append("\n");
        stats.append("Weapon spawn range: ").append(minWeapons).append("-").append(maxWeapons).append("\n");
        stats.append("Ammo spawn range: ").append(minAmmoPickups).append("-").append(maxAmmoPickups).append("\n");
        stats.append("Health spawn range: ").append(minHealthPickups).append("-").append(maxHealthPickups).append("\n");
        stats.append("Spawn distance range: ").append(minSpawnDistance).append("-").append(maxSpawnDistance).append("\n");
        stats.append("Min pickup distance: ").append(minPickupDistance).append("\n");
        stats.append("Player start radius: ").append(playerStartRadius).append("\n");

        if (!validSpawnPoints.isEmpty() && playerStartPosition != null) {
            float minDist = Float.MAX_VALUE;
            float maxDist = 0f;
            for (Vector3f point : validSpawnPoints) {
                float dist = point.distance(playerStartPosition);
                minDist = Math.min(minDist, dist);
                maxDist = Math.max(maxDist, dist);
            }
            stats.append("Actual spawn distances: ").append(String.format("%.1f", minDist))
                    .append("-").append(String.format("%.1f", maxDist)).append("\n");
        }

        return stats.toString();
    }

    // Getters
    public float getMinSpawnDistance() { return minSpawnDistance; }
    public float getMaxSpawnDistance() { return maxSpawnDistance; }
    public float getMinPickupDistance() { return minPickupDistance; }
    public float getPlayerStartRadius() { return playerStartRadius; }
}