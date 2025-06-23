package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated zombie spawning system
 * Handles all zombie creation and placement logic
 */
public class ZombieSpawner {

    private AssetManager assetManager;
    private Camera camera;
    private BulletAppState bulletAppState;
    private EntityManager entityManager;

    // Spawn configuration
    private int zombieCount = 15;
    private float minSpawnDistance = 18f;
    private float maxSpawnDistance = 100f;
    private float minZombieDistance = 30f; // Minimum distance between zombies
    private float mapScale = 1.5f; // Same as HorrorGameJME.MAP_SCALE

    // Zombie type chances (must add up to 1.0)
    private float fastZombieChance = 0.0f;   // 20%
    private float largeZombieChance = 0.0f;  // 15%
    private float smallZombieChance = 0.0f;  // 10%
    // Normal zombies = remaining 55%

    public ZombieSpawner(AssetManager assetManager, Camera camera,
                         BulletAppState bulletAppState, EntityManager entityManager) {
        this.assetManager = assetManager;
        this.camera = camera;
        this.bulletAppState = bulletAppState;
        this.entityManager = entityManager;
    }

    /**
     * Spawn initial zombies around a specific position
     */
    public void spawnInitialZombies(Vector3f playerStartPosition) {
        if (entityManager == null) {
            System.err.println("Cannot spawn zombies - EntityManager is null");
            return;
        }

        System.out.println("=== SPAWNING DEBUG INFO ===");
        System.out.println("Player start position: " + playerStartPosition);
        System.out.println("Min spawn distance: " + minSpawnDistance);
        System.out.println("Max spawn distance: " + maxSpawnDistance);
        System.out.println("Min zombie distance: " + minZombieDistance);
        System.out.println("Zombie count: " + zombieCount);
        System.out.println("===========================");

        // Track spawned positions to prevent overlap
        List<Vector3f> spawnedPositions = new ArrayList<>();

        for (int i = 0; i < zombieCount; i++) {
            Vector3f spawnPos = generateRandomSpawnPosition(playerStartPosition, spawnedPositions);

            System.out.println("=== ZOMBIE " + (i+1) + " CREATION DEBUG ===");
            System.out.println("Generated spawn position: " + spawnPos);

            ZombieEnemy zombie = createZombie(spawnPos, i);

            System.out.println("After createZombie, position: " + zombie.getPosition());

            // Add to spawned positions list
            spawnedPositions.add(spawnPos.clone());

            entityManager.addEntity(zombie);

            System.out.println("After addEntity, position: " + zombie.getPosition());
            System.out.println("Zombie physics body location: " +
                    (zombie.getRigidBody() != null ? zombie.getRigidBody().getPhysicsLocation() : "NO PHYSICS"));
            System.out.println("==============================");
        }
    }

    /**
     * Spawn additional zombies around the player during gameplay
     */
    public void spawnAdditionalZombies(Vector3f playerPosition, int count) {
        if (entityManager == null || playerPosition == null) {
            return;
        }

        System.out.println("Spawning " + count + " additional zombies around player");

        // Get existing zombie positions to avoid overlap
        List<Vector3f> existingPositions = getExistingZombiePositions();

        for (int i = 0; i < count; i++) {
            Vector3f spawnPos = generateRandomSpawnPosition(playerPosition, existingPositions);
            ZombieEnemy zombie = createZombie(spawnPos, i);

            // Add to existing positions list
            existingPositions.add(spawnPos.clone());

            entityManager.addEntity(zombie);
            System.out.println("Additional zombie spawned at: " + spawnPos);
        }
    }

    /**
     * Create a single zombie with random properties
     */
    private ZombieEnemy createZombie(Vector3f position, int index) {
        ZombieEnemy zombie = new ZombieEnemy(position, assetManager, camera, bulletAppState);

        // Apply random customization
        customizeZombie(zombie, index);

        return zombie;
    }

    /**
     * Generate random spawn position around a center point, avoiding existing positions
     */
    private Vector3f generateRandomSpawnPosition(Vector3f centerPosition, List<Vector3f> existingPositions) {
        Vector3f spawnPos;
        int attempts = 0;

        do {
            // Random angle (0 to 360 degrees)
            float angle = (float)(Math.random() * Math.PI * 2);

            // Random distance between min and max
            float distance = minSpawnDistance +
                    (float)(Math.random() * (maxSpawnDistance - minSpawnDistance));

            // Calculate position using polar coordinates
            float x = centerPosition.x + (float)(Math.cos(angle) * distance);
            float z = centerPosition.z + (float)(Math.sin(angle) * distance);

            spawnPos = new Vector3f(x, 0f, z); // Y will be set by ground detection

            attempts++;

            // Debug output for troubleshooting
            if (attempts <= 3) {
                System.out.println("Attempt " + attempts + ": Generated position " + spawnPos +
                        " (angle: " + Math.toDegrees(angle) + "°, distance: " + distance + ")");
            }

        } while ((isPositionTooClose(spawnPos, centerPosition) ||
                isPositionTooCloseToOthers(spawnPos, existingPositions, minZombieDistance)) &&
                attempts < 50); // Increased max attempts

        if (attempts >= 50) {
            System.out.println("Warning: Could not find valid spawn position after 50 attempts, using last position");
            System.out.println("Final position: " + spawnPos);
        } else {
            System.out.println("Found valid position after " + attempts + " attempts: " + spawnPos);
        }

        return spawnPos;
    }

    /**
     * Check if position is too close to center
     */
    private boolean isPositionTooClose(Vector3f spawnPos, Vector3f centerPos) {
        float distance = spawnPos.distance(centerPos);
        return distance < minSpawnDistance;
    }

    /**
     * Check if position is too close to other zombies
     */
    private boolean isPositionTooCloseToOthers(Vector3f spawnPos, List<Vector3f> existingPositions, float minDistance) {
        for (Vector3f existingPos : existingPositions) {
            float distance = spawnPos.distance(existingPos);
            if (distance < minDistance) {
                System.out.println("Position " + spawnPos + " too close to existing zombie at " + existingPos + " (distance: " + distance + ")");
                return true;
            }
        }
        return false;
    }

    /**
     * Get positions of all existing zombies
     */
    private List<Vector3f> getExistingZombiePositions() {
        List<Vector3f> positions = new ArrayList<>();

        if (entityManager != null) {
            for (Entity entity : entityManager.getEntitiesByType(Entity.EntityType.ENEMY)) {
                if (entity instanceof ZombieEnemy && !entity.isDestroyed()) {
                    positions.add(entity.getPosition());
                }
            }
        }

        System.out.println("Found " + positions.size() + " existing zombie positions");
        return positions;
    }

    /**
     * Apply random customization to a zombie
     */
    private void customizeZombie(ZombieEnemy zombie, int index) {
        // Random basic properties
        float randomSpeed = 8.0f + (float)(Math.random() * 2.0f);        // 1.0 to 3.0
        float randomDetection = 8f + (float)(Math.random() * 7f);        // 8 to 15
        float randomAttackRange = 1.5f + (float)(Math.random() * 1.5f);  // 1.5 to 3.0

        zombie.setSpeed(randomSpeed);
        zombie.setDetectionRange(randomDetection);
        zombie.setAttackRange(randomAttackRange);

        // Determine zombie type based on random chance
        double specialChance = Math.random();

        if (specialChance < fastZombieChance) {
            zombie.makeFastZombie();
            System.out.println("Created FAST zombie #" + (index+1));

        } else if (specialChance < fastZombieChance + largeZombieChance) {
            zombie.makeLargeZombie();
            System.out.println("Created LARGE zombie #" + (index+1));

        } else if (specialChance < fastZombieChance + largeZombieChance + smallZombieChance) {
            zombie.makeSmallZombie();
            System.out.println("Created SMALL zombie #" + (index+1));

        } else {
            // Normal zombie with random scale
            float randomScale = 0.6f + (float)(Math.random() * 0.4f); // 0.8 to 1.2
            zombie.setSpriteScale(randomScale);
            System.out.println("Created NORMAL zombie #" + (index+1) + " (scale: " + String.format("%.2f", randomScale) + ")");
        }
    }

    /**
     * Spawn zombies in a specific pattern (circle, line, etc.)
     */
    public void spawnZombiesInCircle(Vector3f centerPosition, float radius, int count) {
        if (entityManager == null) return;

        float angleStep = (float)(2 * Math.PI / count);
        List<Vector3f> spawnedPositions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            float angle = i * angleStep;
            float x = centerPosition.x + (float)(Math.cos(angle) * radius);
            float z = centerPosition.z + (float)(Math.sin(angle) * radius);

            Vector3f spawnPos = new Vector3f(x, 0f, z);
            ZombieEnemy zombie = createZombie(spawnPos, i);

            spawnedPositions.add(spawnPos.clone());
            entityManager.addEntity(zombie);
        }

        System.out.println("Spawned " + count + " zombies in circle around " + centerPosition + " with radius " + radius);
    }

    /**
     * Create a zombie wave (multiple spawns over time would need a timer system)
     */
    public void spawnZombieWave(Vector3f playerPosition, int waveSize) {
        // For now, just spawn them all at once
        // In a real implementation, you'd want to space them out over time
        spawnAdditionalZombies(playerPosition, waveSize);
        System.out.println("Zombie wave of " + waveSize + " spawned!");
    }

    // ==== CONFIGURATION METHODS ====

    public void setZombieCount(int count) {
        this.zombieCount = Math.max(1, count);
    }

    public void setSpawnDistanceRange(float minDistance, float maxDistance) {
        this.minSpawnDistance = Math.max(1f, minDistance);
        this.maxSpawnDistance = Math.max(minDistance + 1f, maxDistance);
    }

    public void setMinZombieDistance(float distance) {
        this.minZombieDistance = Math.max(0.5f, distance);
    }

    public void setMapScale(float scale) {
        this.mapScale = scale;
    }

    public void setZombieTypeChances(float fastChance, float largeChance, float smallChance) {
        // Ensure chances don't exceed 1.0
        float total = fastChance + largeChance + smallChance;
        if (total > 1.0f) {
            System.err.println("Warning: Zombie type chances exceed 1.0, normalizing...");
            this.fastZombieChance = fastChance / total;
            this.largeZombieChance = largeChance / total;
            this.smallZombieChance = smallChance / total;
        } else {
            this.fastZombieChance = fastChance;
            this.largeZombieChance = largeChance;
            this.smallZombieChance = smallChance;
        }
    }

    // ==== GETTERS ====

    public int getZombieCount() { return zombieCount; }
    public float getMinSpawnDistance() { return minSpawnDistance; }
    public float getMaxSpawnDistance() { return maxSpawnDistance; }
    public float getMinZombieDistance() { return minZombieDistance; }
    public float getMapScale() { return mapScale; }

    /**
     * Get configuration summary for debugging
     */
    public String getConfigurationSummary() {
        StringBuilder config = new StringBuilder();
        config.append("=== Zombie Spawner Configuration ===\n");
        config.append("Zombie Count: ").append(zombieCount).append("\n");
        config.append("Spawn Distance: ").append(minSpawnDistance).append(" to ").append(maxSpawnDistance).append("\n");
        config.append("Min Zombie Distance: ").append(minZombieDistance).append("\n");
        config.append("Map Scale: ").append(mapScale).append("\n");
        config.append("Fast Zombie Chance: ").append(String.format("%.1f%%", fastZombieChance * 100)).append("\n");
        config.append("Large Zombie Chance: ").append(String.format("%.1f%%", largeZombieChance * 100)).append("\n");
        config.append("Small Zombie Chance: ").append(String.format("%.1f%%", smallZombieChance * 100)).append("\n");
        float normalChance = 1.0f - (fastZombieChance + largeZombieChance + smallZombieChance);
        config.append("Normal Zombie Chance: ").append(String.format("%.1f%%", normalChance * 100));
        return config.toString();
    }
}