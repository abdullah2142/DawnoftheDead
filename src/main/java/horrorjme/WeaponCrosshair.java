package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;

/**
 * Crosshair that shows exactly where the weapon camera is aiming.
 * Updates position based on weapon camera direction, not player camera.
 */
public class WeaponCrosshair {

    private AssetManager assetManager;
    private Node guiNode;
    private Camera playerCamera; // For screen projection
    private WeaponCamera weaponCamera; // For aim direction

    // Crosshair visual
    private Picture crosshair;
    private boolean visible = true;

    // Configuration
    private float crosshairSize = 3f;
    private ColorRGBA crosshairColor = ColorRGBA.White;
    private float aimDistance = 25f; // How far ahead to project the crosshair

    /**
     * Create weapon-aligned crosshair
     * @param assetManager Asset manager for loading textures
     * @param guiNode GUI node to attach crosshair to
     * @param playerCamera Player camera for screen projection
     * @param weaponCamera Weapon camera for aim direction
     */
    public WeaponCrosshair(AssetManager assetManager, Node guiNode, Camera playerCamera, WeaponCamera weaponCamera) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.playerCamera = playerCamera;
        this.weaponCamera = weaponCamera;

        initializeCrosshair();

        System.out.println("WeaponCrosshair initialized - aligned to weapon camera");
    }

    /**
     * Initialize the crosshair visual
     */
    private void initializeCrosshair() {
        crosshair = new Picture("WeaponCrosshair");
        crosshair.setWidth(crosshairSize);
        crosshair.setHeight(crosshairSize);

        // Try to load crosshair texture, fallback to colored square
        Material crosshairMat = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");

        try {
            // Try to load a crosshair texture
            crosshairMat.setTexture("Texture", assetManager.loadTexture("Textures/GUI/crosshair.png"));
        } catch (Exception e) {
            // Fallback to simple colored crosshair
            crosshairMat.setColor("Color", crosshairColor);
        }

        crosshair.setMaterial(crosshairMat);

        if (visible) {
            guiNode.attachChild(crosshair);
        }
    }

    /**
     * Update crosshair position based on weapon camera aim (call every frame)
     * @param tpf Time per frame
     */
    public void update(float tpf) {
        if (!visible || crosshair == null || weaponCamera == null) {
            return;
        }

        // Get weapon camera aim point in 3D world space
        Vector3f weaponPos = weaponCamera.getFiringPosition();
        Vector3f weaponDir = weaponCamera.getFiringDirection();

        // Project weapon aim forward to get target point
        Vector3f aimPoint = weaponPos.add(weaponDir.mult(aimDistance));

        // Convert 3D aim point to 2D screen coordinates using player camera
        Vector3f screenCoords = playerCamera.getScreenCoordinates(aimPoint);

        // Position crosshair at screen coordinates (centered)
        float crosshairX = screenCoords.x - (crosshairSize / 2f);
        float crosshairY = screenCoords.y - (crosshairSize / 2f);

        crosshair.setPosition(crosshairX, crosshairY);
    }

    // ==== CONFIGURATION METHODS ====

    /**
     * Set crosshair visibility
     * @param visible Whether crosshair should be shown
     */
    public void setVisible(boolean visible) {
        this.visible = visible;

        if (crosshair != null) {
            if (visible && crosshair.getParent() == null) {
                guiNode.attachChild(crosshair);
            } else if (!visible && crosshair.getParent() != null) {
                guiNode.detachChild(crosshair);
            }
        }

        System.out.println("Weapon crosshair visibility: " + visible);
    }

    /**
     * Set crosshair size
     * @param size Size in pixels
     */
    public void setCrosshairSize(float size) {
        this.crosshairSize = size;

        if (crosshair != null) {
            crosshair.setWidth(size);
            crosshair.setHeight(size);
        }

        System.out.println("Crosshair size set to: " + size);
    }

    /**
     * Set crosshair color (only works if no texture is loaded)
     * @param color Color for the crosshair
     */
    public void setCrosshairColor(ColorRGBA color) {
        this.crosshairColor = color;

        if (crosshair != null && crosshair.getMaterial() != null) {
            crosshair.getMaterial().setColor("Color", color);
        }

        System.out.println("Crosshair color set to: " + color);
    }

    /**
     * Set how far ahead to project the crosshair
     * @param distance Distance in world units
     */
    public void setAimDistance(float distance) {
        this.aimDistance = Math.max(1f, distance);
        System.out.println("Crosshair aim distance set to: " + this.aimDistance);
    }

    // ==== STATUS METHODS ====

    /**
     * Check if crosshair is visible
     * @return True if crosshair is currently visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Get current crosshair screen position
     * @return Screen coordinates of crosshair center
     */
    public Vector2f getCrosshairPosition() {
        if (crosshair != null) {
            float centerX = crosshair.getLocalTranslation().x + (crosshairSize / 2f);
            float centerY = crosshair.getLocalTranslation().y + (crosshairSize / 2f);
            return new Vector2f(centerX, centerY);
        }
        return new Vector2f(0, 0);
    }

    /**
     * Get current crosshair size
     * @return Crosshair size in pixels
     */
    public float getCrosshairSize() {
        return crosshairSize;
    }

    // ==== CLEANUP ====

    /**
     * Clean up crosshair resources
     */
    public void cleanup() {
        if (crosshair != null && crosshair.getParent() != null) {
            guiNode.detachChild(crosshair);
        }

        crosshair = null;
        weaponCamera = null;
        playerCamera = null;

        System.out.println("WeaponCrosshair cleaned up");
    }
}