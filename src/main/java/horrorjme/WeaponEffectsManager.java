package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 * Simplified Weapon Effects Manager without a projectile alignment system.
 * Bullets fire from the camera's direction.
 */
public class WeaponEffectsManager {

    private WeaponCamera weaponCamera;
    private AssetManager assetManager;
    private Node rootNode;
    private Camera camera;
    private BulletAppState bulletAppState;
    private EntityManager entityManager;
    private WeaponCrosshair weaponCrosshair;

    // The WeaponType enum is moved here from the deleted ProjectileAlignmentSystem.java
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

    // Weapon configuration
    private WeaponType currentWeaponType = WeaponType.RIFLE;

    // Effect settings
    private boolean shellCasingsEnabled = true;
    private boolean bulletTracersEnabled = true;

    public WeaponEffectsManager(AssetManager assetManager, Node rootNode, Node guiNode,
                                Camera camera, BulletAppState bulletAppState, EntityManager entityManager) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.camera = camera;
        this.bulletAppState = bulletAppState;
        this.entityManager = entityManager;
        this.weaponCamera = new WeaponCamera(camera);
        this.weaponCrosshair = new WeaponCrosshair(assetManager, guiNode, camera, weaponCamera);

        System.out.println("Simplified WeaponEffectsManager initialized.");
    }

    /**
     * Fire weapon using legacy camera-based direction.
     */
    public void fireWeapon() {
        Vector3f weaponPosition = getWeaponWorldPosition();
        Vector3f cameraDirection = camera.getDirection().clone();

        if (shellCasingsEnabled) {
            ejectShellCasing(weaponPosition, cameraDirection);
        }

        if (bulletTracersEnabled) {
            fireBulletTracer(weaponPosition, cameraDirection);
        }
    }

    /**
     * Fires a bullet tracer.
     */
    private void fireBulletTracer(Vector3f weaponPosition, Vector3f direction) {
        if (entityManager != null) {
            // OLD CODE (comment out):
            // BulletTracer tracer = BulletTracer.createBulletTracer(weaponPosition, direction, assetManager);

            // NEW CODE (replace with):
            Vector3f firingPos = weaponCamera.getFiringPosition();
            Vector3f firingDir = weaponCamera.getFiringDirection();
            BulletTracer tracer = BulletTracer.createBulletTracer(firingPos, firingDir, assetManager);

            configureBulletForWeaponType(tracer);
            entityManager.addEntity(tracer);
        } else {
            System.err.println("Cannot fire bullet tracer - entityManager is null");
        }
    }

    /**
     * Configure bullet properties based on current weapon type.
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
     * Eject shell casing.
     */
    private void ejectShellCasing(Vector3f weaponPosition, Vector3f direction) {
        if (bulletAppState != null && entityManager != null) {
            // OLD CODE (comment out):
            // ShellCasing shell = ShellCasing.createShellCasing(weaponPosition, direction, assetManager, bulletAppState);

            // NEW CODE (replace with):
            Vector3f firingPos = weaponCamera.getFiringPosition();
            Vector3f firingDir = weaponCamera.getFiringDirection();
            ShellCasing shell = ShellCasing.createShellCasing(firingPos, firingDir, assetManager, bulletAppState);

            entityManager.addEntity(shell);
        } else {
            System.err.println("Cannot eject shell casing - physics or entityManager is null");
        }
    }

    // ADD: Configuration method for manual tuning
    public void setWeaponOffset(float rightOffset, float downOffset, float forwardOffset) {
        if (weaponCamera != null) {
            weaponCamera.setWeaponOffset(rightOffset, downOffset, forwardOffset);
            System.out.println("Weapon camera offset set to: " + rightOffset + ", " + downOffset + ", " + forwardOffset);
        }
    }

    // ADD: Debug method
    public void enableWeaponCameraDebug(boolean enabled) {
        if (weaponCamera != null) {
            weaponCamera.setDebugEnabled(enabled);
        }
    }


    /**
     * Simplified weapon setup.
     */
    public void setupWeaponPreset(WeaponType weaponType) {
        this.currentWeaponType = weaponType;
        System.out.println("Applied weapon preset: " + weaponType);
    }


    /**
     * Calculate weapon position in world space.
     */
    private Vector3f getWeaponWorldPosition() {
        Vector3f cameraPos = camera.getLocation().clone();
        Vector3f offset = currentWeaponType.defaultMuzzleOffset;

        Vector3f forward = camera.getDirection().mult(offset.z);
        Vector3f right = camera.getLeft().negate().mult(offset.x);
        Vector3f vertical = Vector3f.UNIT_Y.mult(-offset.y);

        return cameraPos.add(forward).add(right).add(vertical);
    }

    /**
     * Update method
     */
    public void update(float tpf) {
        weaponCamera.update(tpf);
        weaponCrosshair.update(tpf);

    }

    // ==== CONFIGURATION METHODS ====
    public void setShellCasingsEnabled(boolean enabled) {
        this.shellCasingsEnabled = enabled;
    }

    public void setBulletTracersEnabled(boolean enabled) {
        this.bulletTracersEnabled = enabled;
    }

    // ==== GETTERS ====
    public WeaponType getCurrentWeaponType() {
        return currentWeaponType;
    }

    public boolean isShellCasingsEnabled() {
        return shellCasingsEnabled;
    }

    public boolean isBulletTracersEnabled() {
        return bulletTracersEnabled;
    }
    public void setCrosshairVisible(boolean visible) {
        if (weaponCrosshair != null) {
            weaponCrosshair.setVisible(visible);
        }
    }

    public void setCrosshairSize(float size) {
        if (weaponCrosshair != null) {
            weaponCrosshair.setCrosshairSize(size);
        }
    }

    public void setCrosshairColor(ColorRGBA color) {
        if (weaponCrosshair != null) {
            weaponCrosshair.setCrosshairColor(color);
        }
    }

    /**
     * Clean up all effects
     */
    public void cleanup() {
        // ADD: Cleanup weapon camera
        if (weaponCamera != null) {
            weaponCamera.cleanup();
        }
        // ADD: Cleanup crosshair
        if (weaponCrosshair != null) {
            weaponCrosshair.cleanup();
        }

        System.out.println("WeaponEffectsManager cleaned up");
    }
}