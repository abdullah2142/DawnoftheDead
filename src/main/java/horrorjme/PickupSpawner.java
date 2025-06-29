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
 * FIXED PickupSpawner - Handles random placement of weapons and ammo throughout the game world
 * Now properly spawns weapons at reasonable distances from player
 */
public class PickupSpawner {

    private AssetManager assetManager;
    private AudioManager audioManager;
    private BulletAppState bulletAppState;

    // Spawn configuration
    private List<Vector3f> validSpawnPoints;
    private List<Vector3f> weaponSpawnPoints;    // Premium locations for weapons
    private List<Vector3f> ammoSpawnPoints;      // Regular locations for ammo

    // Spawn parameters (all randomized as requested)
    private int minWeapons = 2;
    private int maxWeapons = 5;
    private int minAmmoPickups = 8;
    private int maxAmmoPickups = 15;

    // FIXED: Map scanning parameters with proper spawn distances
    private float groundScanHeight = 50f;      // How high above ground to start ray
    private float minSpawnDistance = 5f;       // Minimum distance between pickups
    private float maxSpawnDistance = 25f;      // ADDED: Maximum distance for spawns (reasonable for pickups)
    private float minPickupDistance = 3f;      // ADDED: Minimum distance between different pickups
    private float playerStartRadius = 8f;      // Don't spawn too close to player start
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
     * IMPROVED: Scan the loaded map for valid pickup spawn points
     */
    public void scanMapForSpawnPoints(Spatial map, Vector3f playerStart) {
        this.playerStartPosition = playerStart.clone();

        System.out.println("Scanning map for pickup spawn points...");
        System.out.println("Spawn distance range: " + minSpawnDistance + " to " + maxSpawnDistance);

        // Clear existing spawn points
        validSpawnPoints.clear();
        weaponSpawnPoints.clear();
        ammoSpawnPoints.clear();

        // IMPROVED: Use radial scanning around player instead of full map grid
        scanRadialAroundPlayer();

        System.out.printf("Scan complete: %d valid spawn points found%n", validSpawnPoints.size());
        System.out.printf("Weapon locations: %d, Ammo locations: %d%n",
                weaponSpawnPoints.size(), ammoSpawnPoints.size());
    }

    /**
     * NEW: Scan in a circle around the player at reasonable distances
     */
    private void scanRadialAroundPlayer() {
        int totalChecks = 0;
        int validPoints = 0;

        // Generate spawn points in rings around the player
        for (float radius = minSpawnDistance; radius <= maxSpawnDistance; radius += 2f) {
            // Calculate how many points to check at this radius
            int pointsAtRadius = Math.max(8, (int)(radius * 0.8f)); // More points for larger radius

            for (int i = 0; i < pointsAtRadius; i++) {
                float angle = (float)(2 * Math.PI * i / pointsAtRadius);

                float x = playerStartPosition.x + radius * FastMath.cos(angle);
                float z = playerStartPosition.z + radius * FastMath.sin(angle);

                totalChecks++;

                // Find ground level at this position
                Vector3f groundPos = findGroundLevel(x, z, playerStartPosition.y + groundScanHeight);

                if (groundPos != null && isValidSpawnLocation(groundPos)) {
                    validSpawnPoints.add(groundPos);
                    validPoints++;

                    // IMPROVED: Categorize spawn points based on radius and position
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
            return new Vector3f(x, 0, z); // Fallback to ground level
        }

        Vector3f rayStart = new Vector3f(x, startY, z);
        Vector3f rayEnd = new Vector3f(x, startY - 100f, z); // Cast down 100 units

        List<PhysicsRayTestResult> results = bulletAppState.getPhysicsSpace().rayTest(rayStart, rayEnd);

        for (PhysicsRayTestResult result : results) {
            // Hit something solid - calculate hit point
            float hitFraction = result.getHitFraction();
            Vector3f rayDirection = rayEnd.subtract(rayStart);
            Vector3f hitPoint = rayStart.add(rayDirection.mult(hitFraction));

            // Add small offset above ground
            return hitPoint.add(0, 0.2f, 0);
        }

        return null; // No ground found
    }

    /**
     * IMPROVED: Check if a location is valid for spawning pickups
     */
    private boolean isValidSpawnLocation(Vector3f position) {
        // Don't spawn too close to player start
        if (playerStartPosition != null) {
            float distanceFromStart = position.distance(playerStartPosition);
            if (distanceFromStart < playerStartRadius) {
                return false;
            }
        }

        // Don't spawn too close to other spawn points
        for (Vector3f existingPoint : validSpawnPoints) {
            if (position.distance(existingPoint) < minPickupDistance) {
                return false;
            }
        }

        // Check if there's enough space above the spawn point (not inside geometry)
        if (bulletAppState != null) {
            Vector3f checkStart = position.clone();
            Vector3f checkEnd = position.add(0, 2f, 0); // 2 units above ground

            List<PhysicsRayTestResult> results = bulletAppState.getPhysicsSpace().rayTest(checkStart, checkEnd);
            if (!results.isEmpty()) {
                return false; // Something blocking above
            }
        }

        return true;
    }

    /**
     * FIXED: Determine if a location is good for weapon spawning
     * Now uses radius and variety instead of just distance from center
     */
    private boolean isGoodWeaponLocation(Vector3f position, float radius) {
        // Weapons get medium-distance locations (not too close, not too far)
        // Sweet spot for weapons: 10-20 units from player
        boolean goodDistance = radius >= 10f && radius <= 20f;

        // Add some randomness so not all weapons are at the same distance
        boolean randomFactor = FastMath.nextRandomFloat() < 0.4f; // 40% chance

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

        // Random number of weapons to spawn
        int weaponRange = maxWeapons - minWeapons + 1;
        int weaponCount = minWeapons + (int)(FastMath.nextRandomFloat() * weaponRange);

        System.out.println("Spawning " + weaponCount + " weapons...");

        // Shuffle spawn points for randomness
        List<Vector3f> shuffledPoints = new ArrayList<>(weaponSpawnPoints);

        for (int i = 0; i < weaponCount && i < shuffledPoints.size(); i++) {
            // Pick random spawn point
            int randomIndex = (int)(FastMath.nextRandomFloat() * shuffledPoints.size());
            Vector3f spawnPos = shuffledPoints.remove(randomIndex);

            // Create random weapon pickup
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

        // Random number of ammo pickups to spawn
        int ammoRange = maxAmmoPickups - minAmmoPickups + 1;
        int ammoCount = minAmmoPickups + (int)(FastMath.nextRandomFloat() * ammoRange);

        System.out.println("Spawning " + ammoCount + " ammo pickups...");

        // Shuffle spawn points for randomness
        List<Vector3f> shuffledPoints = new ArrayList<>(ammoSpawnPoints);

        for (int i = 0; i < ammoCount && i < shuffledPoints.size(); i++) {
            // Pick random spawn point
            int randomIndex = (int)(FastMath.nextRandomFloat() * shuffledPoints.size());
            Vector3f spawnPos = shuffledPoints.remove(randomIndex);

            // Create random ammo pickup
            AmmoPickup ammo = DropSystem.createRandomAmmoSpawn(spawnPos, assetManager, audioManager);
            entityManager.addEntity(ammo);

            System.out.printf("Spawned %s at (%.1f, %.1f, %.1f) - Distance from player: %.1f%n",
                    ammo.getAmmoType().displayName,
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    spawnPos.distance(playerStartPosition));
        }
    }

    /**
     * Spawn both weapons and ammo in one call
     */
    public void spawnAllPickups(EntityManager entityManager) {
        spawnRandomWeapons(entityManager);
        spawnRandomAmmo(entityManager);

        System.out.println("All pickups spawned successfully!");
    }

    // IMPROVED: Configuration methods with proper spawn distances
    public void applySpawnPreset(SpawnPreset preset) {
        switch (preset) {
            case SCARCE_RESOURCES:
                setWeaponSpawnRange(1, 2);
                setAmmoSpawnRange(3, 6);
                setSpawnDistanceRange(8f, 20f);      // Closer spawns for scarce mode
                setMinSpawnDistance(8f);
                setPlayerStartRadius(6f);
                break;

            case NORMAL_RESOURCES:
                setWeaponSpawnRange(2, 4);
                setAmmoSpawnRange(6, 10);
                setSpawnDistanceRange(6f, 25f);      // Reasonable distance
                setMinSpawnDistance(5f);
                setPlayerStartRadius(8f);
                break;

            case ABUNDANT_RESOURCES:
                setWeaponSpawnRange(3, 6);
                setAmmoSpawnRange(10, 15);
                setSpawnDistanceRange(5f, 30f);      // Slightly farther for more variety
                setMinSpawnDistance(3f);
                setPlayerStartRadius(5f);
                break;

            case TESTING_MODE:
                setWeaponSpawnRange(5, 8);
                setAmmoSpawnRange(15, 25);
                setSpawnDistanceRange(3f, 15f);      // Very close for testing
                setMinSpawnDistance(2f);
                setPlayerStartRadius(3f);
                break;
        }

        System.out.println("Applied spawn preset: " + preset);
        System.out.println("Spawn distance: " + minSpawnDistance + " to " + maxSpawnDistance);
    }

    public enum SpawnPreset {
        SCARCE_RESOURCES,    // Survival horror - very limited resources
        NORMAL_RESOURCES,    // Balanced gameplay
        ABUNDANT_RESOURCES,  // More action-oriented
        TESTING_MODE        // Lots of pickups for testing
    }

    // IMPROVED: Configuration methods
    public void setWeaponSpawnRange(int min, int max) {
        this.minWeapons = Math.max(0, min);
        this.maxWeapons = Math.max(min, max);
    }

    public void setAmmoSpawnRange(int min, int max) {
        this.minAmmoPickups = Math.max(0, min);
        this.maxAmmoPickups = Math.max(min, max);
    }

    public void setMinSpawnDistance(float distance) {
        this.minSpawnDistance = Math.max(1f, distance);
    }

    // NEW: Set spawn distance range
    public void setSpawnDistanceRange(float minDistance, float maxDistance) {
        this.minSpawnDistance = Math.max(1f, minDistance);
        this.maxSpawnDistance = Math.max(minDistance + 1f, maxDistance);
    }

    // NEW: Set minimum distance between pickups
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

    // IMPROVED: Better statistics with distance info
    public String getSpawnStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== PICKUP SPAWN STATISTICS ===\n");
        stats.append("Total valid spawn points: ").append(validSpawnPoints.size()).append("\n");
        stats.append("Weapon spawn points: ").append(weaponSpawnPoints.size()).append("\n");
        stats.append("Ammo spawn points: ").append(ammoSpawnPoints.size()).append("\n");
        stats.append("Weapon spawn range: ").append(minWeapons).append("-").append(maxWeapons).append("\n");
        stats.append("Ammo spawn range: ").append(minAmmoPickups).append("-").append(maxAmmoPickups).append("\n");
        stats.append("Spawn distance range: ").append(minSpawnDistance).append("-").append(maxSpawnDistance).append("\n");
        stats.append("Min pickup distance: ").append(minPickupDistance).append("\n");
        stats.append("Player start radius: ").append(playerStartRadius).append("\n");

        // Add distance analysis
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

    // NEW: Get current spawn distance settings
    public float getMinSpawnDistance() { return minSpawnDistance; }
    public float getMaxSpawnDistance() { return maxSpawnDistance; }
    public float getMinPickupDistance() { return minPickupDistance; }
    public float getPlayerStartRadius() { return playerStartRadius; }
}