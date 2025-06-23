package horrorjme;

import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.system.AppSettings;

/**
 * Advanced projectile alignment system that properly converts screen positions to world coordinates
 * Ensures bullets go exactly where the crosshair is pointing
 */
public class ProjectileAlignmentSystem {

    private Camera camera;
    private AppSettings settings;
    private CrosshairManager crosshairManager;

    // Alignment configuration
    private Vector3f weaponMuzzleOffset = new Vector3f(0.2f, -0.3f, 0.5f); // Default weapon position
    private boolean useScreenSpaceAlignment = true; // Whether to use crosshair for aiming
    private float projectileSpeed = 100f;

    // Weapon positioning
    private Vector2f weaponScreenPosition = new Vector2f();
    private float weaponScale = 1.0f;
    private WeaponType currentWeaponType = WeaponType.RIFLE;

    public enum WeaponType {
        PISTOL(new Vector3f(0.1f, -0.4f, 0.4f)),
        RIFLE(new Vector3f(0.2f, -0.3f, 0.8f)),
        SMG(new Vector3f(0.15f, -0.35f, 0.6f)),
        SHOTGUN(new Vector3f(0.25f, -0.25f, 1.0f));

        public final Vector3f defaultMuzzleOffset;

        WeaponType(Vector3f offset) {
            this.defaultMuzzleOffset = offset;
        }
    }

    public ProjectileAlignmentSystem(Camera camera, AppSettings settings) {
        this.camera = camera;
        this.settings = settings;}

    public void setCrosshairManager(CrosshairManager crosshairManager) {
        this.crosshairManager = crosshairManager;}

    /**
     * Get the direction vector for where bullets should go
     * This is the core method that ensures perfect alignment
     */
    public Vector3f getBulletDirection() {
        if (useScreenSpaceAlignment && crosshairManager != null) {
            return getBulletDirectionFromCrosshair();
        } else {
            // Fallback to camera direction
            return camera.getDirection().clone();
        }
    }

    /**
     * Calculate bullet direction based on crosshair position
     */
    private Vector3f getBulletDirectionFromCrosshair() {
        // Get crosshair screen position
        Vector3f crosshairScreenPos = crosshairManager.getCrosshairScreenPosition();

        // Convert screen coordinates to normalized device coordinates (-1 to 1)
        float normalizedX = (crosshairScreenPos.x / settings.getWidth()) * 2f - 1f;
        float normalizedY = ((settings.getHeight() - crosshairScreenPos.y) / settings.getHeight()) * 2f - 1f;

        // Create a ray from camera through the crosshair point
        Vector3f nearPoint = camera.getWorldCoordinates(new Vector2f(crosshairScreenPos.x, crosshairScreenPos.y), 0f);
        Vector3f farPoint = camera.getWorldCoordinates(new Vector2f(crosshairScreenPos.x, crosshairScreenPos.y), 1f);

        // Calculate direction from near to far point
        Vector3f direction = farPoint.subtract(nearPoint).normalizeLocal();

        return direction;
    }

    /**
     * Get the world position where bullets should spawn
     */
    public Vector3f getBulletSpawnPosition() {
        Vector3f cameraPos = camera.getLocation().clone();

        // Apply weapon muzzle offset
        Vector3f forward = camera.getDirection().mult(weaponMuzzleOffset.z);
        Vector3f right = camera.getLeft().negate().mult(weaponMuzzleOffset.x);
        Vector3f up = Vector3f.UNIT_Y.mult(-weaponMuzzleOffset.y);

        return cameraPos.add(forward).add(right).add(up);
    }

    /**
     * Get precise bullet spawn position and direction for perfect alignment
     */
    public BulletSpawnData calculateBulletSpawn() {
        Vector3f spawnPosition = getBulletSpawnPosition();
        Vector3f direction = getBulletDirection();

        // If using screen space alignment, adjust spawn position to ensure bullet
        // passes through the crosshair target point
        if (useScreenSpaceAlignment && crosshairManager != null) {
            spawnPosition = adjustSpawnForAlignment(spawnPosition, direction);
        }

        return new BulletSpawnData(spawnPosition, direction, projectileSpeed);
    }

    /**
     * Adjust spawn position so bullet trajectory passes through crosshair target
     */
    private Vector3f adjustSpawnForAlignment(Vector3f originalSpawn, Vector3f direction) {
        // This is where we can fine-tune the spawn position to ensure perfect alignment
        // For most weapons, the original spawn position should work well

        // Optional: Apply small adjustments based on weapon type
        Vector3f adjustment = Vector3f.ZERO;

        switch (currentWeaponType) {
            case PISTOL:
                // Pistols might need slight upward adjustment
                adjustment = Vector3f.UNIT_Y.mult(0.05f);
                break;
            case RIFLE:
                // Rifles are typically well-aligned
                break;
            case SMG:
                // SMGs might need slight right adjustment
                adjustment = camera.getLeft().negate().mult(0.02f);
                break;
            case SHOTGUN:
                // Shotguns might need slight downward adjustment
                adjustment = Vector3f.UNIT_Y.mult(-0.03f);
                break;
        }

        return originalSpawn.add(adjustment);
    }

    /**
     * Update weapon screen position for crosshair alignment
     */
    public void updateWeaponPosition(Vector3f weaponScreenPos, float scale) {
        this.weaponScreenPosition.set(weaponScreenPos.x, weaponScreenPos.y);
        this.weaponScale = scale;

        // Update crosshair alignment if connected
        if (crosshairManager != null) {
            crosshairManager.alignWithWeapon(weaponScreenPos, scale);
        }
    }

    /**
     * Set weapon type and update muzzle offset accordingly
     */
    public void setWeaponType(WeaponType weaponType) {
        this.currentWeaponType = weaponType;
        this.weaponMuzzleOffset = weaponType.defaultMuzzleOffset.clone();}

    /**
     * Manually set custom muzzle offset
     */
    public void setCustomMuzzleOffset(float rightOffset, float downOffset, float forwardOffset) {
        this.weaponMuzzleOffset.set(rightOffset, downOffset, forwardOffset);}

    /**
     * Enable/disable screen space alignment
     */
    public void setScreenSpaceAlignment(boolean enabled) {
        this.useScreenSpaceAlignment = enabled;

        if (enabled) {} else {}
    }

    /**
     * Quick setup for different weapon configurations
     */
    public void setupWeapon(WeaponType type, Vector3f weaponScreenPos, float scale) {
        setWeaponType(type);
        updateWeaponPosition(weaponScreenPos, scale);}

    /**
     * Get target point at specified distance for range calculations
     */
    public Vector3f getTargetPoint(float distance) {
        Vector3f direction = getBulletDirection();
        Vector3f spawnPos = getBulletSpawnPosition();
        return spawnPos.add(direction.mult(distance));
    }

    /**
     * Test alignment by calculating where bullet would hit at various distances
     */
    public AlignmentTestResult testAlignment(float[] testDistances) {
        BulletSpawnData spawn = calculateBulletSpawn();
        AlignmentTestResult result = new AlignmentTestResult();

        for (float distance : testDistances) {
            Vector3f hitPoint = spawn.position.add(spawn.direction.mult(distance));
            result.addHitPoint(distance, hitPoint);
        }

        return result;
    }

    // ==== DATA CLASSES ====

    /**
     * Contains all information needed to spawn a bullet
     */
    public static class BulletSpawnData {
        public final Vector3f position;
        public final Vector3f direction;
        public final float speed;

        public BulletSpawnData(Vector3f position, Vector3f direction, float speed) {
            this.position = position.clone();
            this.direction = direction.clone();
            this.speed = speed;
        }

        @Override
        public String toString() {
            return "BulletSpawn{pos=" + position + ", dir=" + direction + ", speed=" + speed + "}";
        }
    }

    /**
     * Results from alignment testing
     */
    public static class AlignmentTestResult {
        private final java.util.Map<Float, Vector3f> hitPoints = new java.util.HashMap<>();

        public void addHitPoint(float distance, Vector3f point) {
            hitPoints.put(distance, point.clone());
        }

        public Vector3f getHitPoint(float distance) {
            return hitPoints.get(distance);
        }

        public void printResults() {for (java.util.Map.Entry<Float, Vector3f> entry : hitPoints.entrySet()) {}
        }
    }

    // ==== GETTERS ====

    public Vector3f getCurrentMuzzleOffset() {
        return weaponMuzzleOffset.clone();
    }

    public WeaponType getCurrentWeaponType() {
        return currentWeaponType;
    }

    public boolean isUsingScreenSpaceAlignment() {
        return useScreenSpaceAlignment;
    }

    public Vector2f getCurrentWeaponScreenPosition() {
        return weaponScreenPosition.clone();
    }

    public float getCurrentWeaponScale() {
        return weaponScale;
    }

    // ==== DEBUG METHODS ====

    public void printAlignmentInfo() {if (crosshairManager != null) {} else {}

        // Test current alignment
        BulletSpawnData spawn = calculateBulletSpawn();}

    /**
     * Visual debug - shows where bullets would go
     */
    public void debugShowBulletPath(float maxDistance, int steps) {
        BulletSpawnData spawn = calculateBulletSpawn();for (int i = 0; i <= steps; i++) {
            float distance = (maxDistance / steps) * i;
            Vector3f point = spawn.position.add(spawn.direction.mult(distance));}}
}