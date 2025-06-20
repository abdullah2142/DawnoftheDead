package horrorjme;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * Dedicated weapon camera for accurate bullet firing.
 * Creates a virtual camera positioned at the weapon location instead of player's face.
 *
 * Usage:
 * 1. Create WeaponCamera with player camera reference
 * 2. Configure weapon position with setWeaponOffset()
 * 3. Call update() each frame
 * 4. Use getFiringPosition() and getFiringDirection() for bullets
 */
public class WeaponCamera {

    // Reference to player camera
    private Camera playerCamera;

    // Weapon camera (virtual camera for firing calculations)
    private Camera weaponCamera;

    // Weapon positioning offsets from player camera
    private Vector3f weaponOffset = new Vector3f(0.3f, -0.2f, 0.4f); // right, down, forward

    // Debug
    private boolean debugEnabled = false;

    /**
     * Create weapon camera system
     * @param playerCamera Reference to the player's main camera
     */
    public WeaponCamera(Camera playerCamera) {
        this.playerCamera = playerCamera;
        initializeWeaponCamera();

        System.out.println("WeaponCamera initialized with offset: " + weaponOffset);
    }

    /**
     * Create weapon camera with custom offset
     * @param playerCamera Reference to the player's main camera
     * @param rightOffset How far right from player camera (negative = left)
     * @param downOffset How far down from player camera (negative = up)
     * @param forwardOffset How far forward from player camera (negative = back)
     */
    public WeaponCamera(Camera playerCamera, float rightOffset, float downOffset, float forwardOffset) {
        this.playerCamera = playerCamera;
        this.weaponOffset = new Vector3f(rightOffset, -downOffset, forwardOffset);
        initializeWeaponCamera();

        System.out.println("WeaponCamera initialized with custom offset: " + weaponOffset);
    }

    /**
     * Initialize the weapon camera with same properties as player camera
     */
    private void initializeWeaponCamera() {
        // Create weapon camera with same viewport properties as player camera
        weaponCamera = new Camera(playerCamera.getWidth(), playerCamera.getHeight());

        // Copy camera properties from player camera
        float aspectRatio = (float) playerCamera.getWidth() / playerCamera.getHeight();
        weaponCamera.setFrustumPerspective(
                playerCamera.getFov(),
                aspectRatio,
                playerCamera.getFrustumNear(),
                playerCamera.getFrustumFar()
        );

        // Initial positioning
        updateWeaponCameraPosition();
    }

    /**
     * Update weapon camera position and rotation (call this every frame)
     * @param tpf Time per frame
     */
    public void update(float tpf) {
        updateWeaponCameraPosition();

        if (debugEnabled) {
            printDebugInfo();
        }
    }

    /**
     * Calculate and apply weapon camera position based on player camera
     */
    private void updateWeaponCameraPosition() {
        // Get player camera vectors
        Vector3f playerPos = playerCamera.getLocation().clone();
        Vector3f playerDirection = playerCamera.getDirection().clone();
        Vector3f playerLeft = playerCamera.getLeft().clone();
        Vector3f playerUp = playerCamera.getUp().clone();

        // Calculate weapon position using offsets
        Vector3f weaponPosition = playerPos.clone();

        // Apply forward/back offset
        weaponPosition.addLocal(playerDirection.mult(weaponOffset.z));

        // Apply right/left offset
        weaponPosition.addLocal(playerLeft.negate().mult(weaponOffset.x));

        // Apply up/down offset
        weaponPosition.addLocal(playerUp.mult(weaponOffset.y));

        // Set weapon camera position and rotation
        weaponCamera.setLocation(weaponPosition);
        weaponCamera.setRotation(playerCamera.getRotation().clone());
    }

    // ==== PUBLIC API FOR FIRING SYSTEM ====

    /**
     * Get the position where bullets should spawn
     * @return World position for bullet creation
     */
    public Vector3f getFiringPosition() {
        return weaponCamera.getLocation().clone();
    }

    /**
     * Get the direction bullets should travel
     * @return Normalized direction vector for bullet velocity
     */
    public Vector3f getFiringDirection() {
        return weaponCamera.getDirection().clone();
    }

    // ==== CONFIGURATION METHODS ====

    /**
     * Set weapon position offset from player camera
     * @param rightOffset Positive = right, negative = left
     * @param downOffset Positive = down, negative = up
     * @param forwardOffset Positive = forward, negative = back
     */
    public void setWeaponOffset(float rightOffset, float downOffset, float forwardOffset) {
        this.weaponOffset.set(rightOffset, -downOffset, forwardOffset);

        if (debugEnabled) {
            System.out.println("Weapon offset updated to: " + weaponOffset);
        }
    }

    /**
     * Adjust current weapon offset by delta values
     * @param deltaRight Change in right offset
     * @param deltaDown Change in down offset
     * @param deltaForward Change in forward offset
     */
    public void adjustWeaponOffset(float deltaRight, float deltaDown, float deltaForward) {
        weaponOffset.addLocal(deltaRight, -deltaDown, deltaForward);

        if (debugEnabled) {
            System.out.println("Weapon offset adjusted to: " + weaponOffset);
        }
    }

    // ==== DEBUG METHODS ====

    /**
     * Enable debug output
     * @param enabled Whether to print debug information
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;

        if (enabled) {
            System.out.println("WeaponCamera debug enabled");
        }
    }

    /**
     * Print current weapon camera state
     */
    public void printDebugInfo() {
        System.out.println("=== WeaponCamera Debug ===");
        System.out.println("Weapon Offset: " + weaponOffset);
        System.out.println("Weapon Position: " + weaponCamera.getLocation());
        System.out.println("Weapon Direction: " + weaponCamera.getDirection());
        System.out.println("Player Position: " + playerCamera.getLocation());
        System.out.println("Distance from Player: " +
                weaponCamera.getLocation().distance(playerCamera.getLocation()));
    }

    /**
     * Get current weapon offset
     * @return Current weapon offset vector
     */
    public Vector3f getWeaponOffset() {
        return weaponOffset.clone();
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        weaponCamera = null;
        playerCamera = null;

        System.out.println("WeaponCamera cleaned up");
    }
}