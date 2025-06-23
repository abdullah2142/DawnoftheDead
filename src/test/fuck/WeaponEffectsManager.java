package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 * Enhanced Weapon Effects Manager with perfect projectile alignment
 * Integrates with ProjectileAlignmentSystem for accurate bullet placement
 */
public class WeaponEffectsManager {

    private AssetManager assetManager;
    private Node rootNode;
    private Camera camera;
    private BulletAppState bulletAppState;
    private EntityManager entityManager;

    // NEW: Projectile alignment system for perfect accuracy
    private ProjectileAlignmentSystem alignmentSystem;

    // Weapon configuration
    private ProjectileAlignmentSystem.WeaponType currentWeaponType = ProjectileAlignmentSystem.WeaponType.RIFLE;
    private Vector3f weaponScreenPosition = new Vector3f();
    private float weaponScale = 1.0f;

    // Effect settings
    private boolean shellCasingsEnabled = true;
    private boolean bulletTracersEnabled = true;
    private boolean alignmentSystemEnabled = true;

    public WeaponEffectsManager(AssetManager assetManager, Node rootNode, Node guiNode,
                                Camera camera, BulletAppState bulletAppState, EntityManager entityManager) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.camera = camera;
        this.bulletAppState = bulletAppState;
        this.entityManager = entityManager;

        // Initialize projectile alignment system
        //initializeAlignmentSystem();}

    /**
     * Initialize the projectile alignment system

    private void initializeAlignmentSystem() {
        alignmentSystem = new ProjectileAlignmentSystem(camera, null); // Settings will be set later
        alignmentSystem.setWeaponType(currentWeaponType);}
*/

    /**
     * Connect crosshair manager for perfect alignment
     */
    public void setCrosshairManager(CrosshairManager crosshairManager) {
        if (alignmentSystem != null) {
            alignmentSystem.setCrosshairManager(crosshairManager);}
    }

    /**
     * Fire weapon with enhanced effects and perfect alignment
     */
    public void fireWeapon() {
        if (alignmentSystemEnabled && alignmentSystem != null) {
            fireWeaponWithAlignment();
        } else {
            fireWeaponLegacy();
        }
    }

    /**
     * Enhanced weapon firing with perfect projectile alignment
     */
    private void fireWeaponWithAlignment() {
        // Get perfect spawn data from alignment system
        ProjectileAlignmentSystem.BulletSpawnData spawnData = alignmentSystem.calculateBulletSpawn();// 1. Eject shell casing (uses weapon position)
        if (shellCasingsEnabled) {
            ejectShellCasing(spawnData.position, spawnData.direction);
        }

        // 2. Fire bullet tracer with perfect alignment
        if (bulletTracersEnabled) {
            fireBulletTracerAligned(spawnData);
        }}

    /**
     * Legacy firing method (fallback)
     */
    private void fireWeaponLegacy() {
        Vector3f weaponPosition = getWeaponWorldPosition();
        Vector3f cameraDirection = camera.getDirection().clone();

        if (shellCasingsEnabled) {
            ejectShellCasing(weaponPosition, cameraDirection);
        }

        if (bulletTracersEnabled) {
            fireBulletTracerLegacy(weaponPosition, cameraDirection);
        }}

    /**
     * Fire bullet tracer with perfect alignment data
     */
    private void fireBulletTracerAligned(ProjectileAlignmentSystem.BulletSpawnData spawnData) {
        if (entityManager != null) {
            // Create bullet tracer with exact spawn data
            BulletTracer tracer = new BulletTracer(spawnData.position, spawnData.direction, assetManager);

            // Configure bullet based on weapon type
            configureBulletForWeaponType(tracer);

            // Set precise velocity
            tracer.setBulletSpeed(spawnData.speed);

            entityManager.addEntity(tracer);} else {
            System.err.println("Cannot fire aligned bullet - entityManager is null");
        }
    }

    /**
     * Configure bullet properties based on current weapon type
     */
    private void configureBulletForWeaponType(BulletTracer bullet) {
        switch (currentWeaponType) {
            case PISTOL:
                bullet.setBulletPreset(BulletTracer.BulletPreset.PISTOL_ROUND);
                break;
            case RIFLE:
                bullet.setBulletPreset(BulletTracer.BulletPreset.FAST_RIFLE);
                break;
            case SMG:
                bullet.setBulletPreset(BulletTracer.BulletPreset.PISTOL_ROUND);
                bullet.setBulletSpeed(90f); // Slightly faster than pistol
                break;
            case SHOTGUN:
                bullet.setBulletPreset(BulletTracer.BulletPreset.SHOTGUN_PELLET);
                break;
        }
    }

    /**
     * Legacy bullet tracer method
     */
    private void fireBulletTracerLegacy(Vector3f weaponPosition, Vector3f direction) {
        if (entityManager != null) {
            BulletTracer tracer = BulletTracer.createBulletTracer(weaponPosition, direction, assetManager);
            configureBulletForWeaponType(tracer);
            entityManager.addEntity(tracer);} else {
            System.err.println("Cannot fire bullet tracer - entityManager is null");
        }
    }

    /**
     * Eject shell casing (unchanged from original)
     */
    private void ejectShellCasing(Vector3f weaponPosition, Vector3f direction) {
        if (bulletAppState != null && entityManager != null) {
            ShellCasing shell = ShellCasing.createShellCasing(weaponPosition, direction, assetManager, bulletAppState);
            entityManager.addEntity(shell);} else {
            System.err.println("Cannot eject shell casing - physics or entityManager is null");
        }
    }

    /**
     * Update weapon configuration and sync with alignment system
     */
    public void updateWeaponConfiguration(ProjectileAlignmentSystem.WeaponType weaponType,
                                          Vector3f screenPosition, float scale) {
        this.currentWeaponType = weaponType;
        this.weaponScreenPosition.set(screenPosition);
        this.weaponScale = scale;

        // Update alignment system
        if (alignmentSystem != null) {
            alignmentSystem.setupWeapon(weaponType, screenPosition, scale);
        }}

    /**
     * Quick weapon setup with preset configurations
     */
    public void setupWeaponPreset(WeaponPreset preset, Vector3f screenPosition, float scale) {
        switch (preset) {
            case ASSAULT_RIFLE:
                updateWeaponConfiguration(ProjectileAlignmentSystem.WeaponType.RIFLE, screenPosition, scale);
                break;
            case SUBMACHINE_GUN:
                updateWeaponConfiguration(ProjectileAlignmentSystem.WeaponType.SMG, screenPosition, scale);
                break;
            case PISTOL:
                updateWeaponConfiguration(ProjectileAlignmentSystem.WeaponType.PISTOL, screenPosition, scale);
                break;
            case SHOTGUN:
                updateWeaponConfiguration(ProjectileAlignmentSystem.WeaponType.SHOTGUN, screenPosition, scale);
                break;
        }}

    public enum WeaponPreset {
        ASSAULT_RIFLE,   // High accuracy, medium damage
        SUBMACHINE_GUN,  // Medium accuracy, fast fire rate
        PISTOL,          // Good accuracy, moderate damage
        SHOTGUN          // Short range, high damage
    }

    /**
     * Calculate weapon position in world space (legacy method)
     */
    private Vector3f getWeaponWorldPosition() {
        Vector3f cameraPos = camera.getLocation().clone();

        // Get current weapon offset from alignment system if available
        Vector3f offset = alignmentSystem != null ?
                alignmentSystem.getCurrentMuzzleOffset() :
                new Vector3f(0.2f, -0.3f, 0.5f); // Default offset

        Vector3f forward = camera.getDirection().mult(offset.z);
        Vector3f right = camera.getLeft().negate().mult(offset.x);
        Vector3f vertical = Vector3f.UNIT_Y.mult(-offset.y);

        return cameraPos.add(forward).add(right).add(vertical);
    }

    /**
     * Update method
     */
    public void update(float tpf) {
        // Shell casings and bullet tracers update themselves through EntityManager
        // No complex effects to update here
    }

    // ==== CONFIGURATION METHODS ====

    /**
     * Enable/disable shell casing ejection
     */
    public void setShellCasingsEnabled(boolean enabled) {
        this.shellCasingsEnabled = enabled;}

    /**
     * Enable/disable bullet tracers
     */
    public void setBulletTracersEnabled(boolean enabled) {
        this.bulletTracersEnabled = enabled;}

    /**
     * Enable/disable alignment system
     */
    public void setAlignmentSystemEnabled(boolean enabled) {
        this.alignmentSystemEnabled = enabled;if (enabled) {} else {}
    }

    /**
     * Set custom muzzle offset for current weapon
     */
    public void setCustomMuzzleOffset(float rightOffset, float downOffset, float forwardOffset) {
        if (alignmentSystem != null) {
            alignmentSystem.setCustomMuzzleOffset(rightOffset, downOffset, forwardOffset);
        }
    }

    // ==== TESTING AND DEBUG METHODS ====

    /**
     * Test current alignment and print results
     */
    public void testAlignment() {
        if (alignmentSystem != null) {
            alignmentSystem.printAlignmentInfo();

            // Test bullet path at various distances
            float[] testDistances = {10f, 25f, 50f, 100f};
            ProjectileAlignmentSystem.AlignmentTestResult result = alignmentSystem.testAlignment(testDistances);
            result.printResults();
        } else {}
    }

    /**
     * Show bullet path visualization
     */
    public void debugShowBulletPath() {
        if (alignmentSystem != null) {
            alignmentSystem.debugShowBulletPath(100f, 10);
        }
    }

    // ==== GETTERS ====

    public ProjectileAlignmentSystem getAlignmentSystem() {
        return alignmentSystem;
    }

    public ProjectileAlignmentSystem.WeaponType getCurrentWeaponType() {
        return currentWeaponType;
    }

    public boolean isShellCasingsEnabled() {
        return shellCasingsEnabled;
    }

    public boolean isBulletTracersEnabled() {
        return bulletTracersEnabled;
    }

    public boolean isAlignmentSystemEnabled() {
        return alignmentSystemEnabled;
    }

    public boolean isBulletPhysicsAvailable() {
        return bulletAppState != null;
    }

    public boolean isEntityManagerAvailable() {
        return entityManager != null;
    }

    /**
     * Get comprehensive system status
     */
    public String getSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Enhanced WeaponEffectsManager Status:\n");
        status.append("  Physics: ").append(isBulletPhysicsAvailable() ? "Available" : "Missing").append("\n");
        status.append("  EntityManager: ").append(isEntityManagerAvailable() ? "Available" : "Missing").append("\n");
        status.append("  Alignment System: ").append(alignmentSystemEnabled ? "ENABLED" : "DISABLED").append("\n");
        status.append("  Shell Casings: ").append(shellCasingsEnabled ? "ENABLED" : "DISABLED").append("\n");
        status.append("  Bullet Tracers: ").append(bulletTracersEnabled ? "ENABLED" : "DISABLED").append("\n");
        status.append("  Current Weapon: ").append(currentWeaponType).append("\n");

        if (alignmentSystem != null) {
            status.append("  Crosshair Connected: ").append(alignmentSystem.isUsingScreenSpaceAlignment() ? "YES" : "NO");
        }

        return status.toString();
    }

    /**
     * Clean up all effects
     */
    public void cleanup() {}
}