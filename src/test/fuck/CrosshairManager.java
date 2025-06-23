package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.ui.Picture;

/**
 * Crosshair system with dynamic accuracy visualization and weapon alignment
 */
public class CrosshairManager {

    private AssetManager assetManager;
    private Node guiNode;
    private AppSettings settings;

    // Crosshair elements
    private Picture crosshairPicture;
    private Picture accuracyCircle;
    private Node crosshairNode;
    private Material crosshairMaterial;
    private Material accuracyMaterial;

    // Crosshair configuration
    private Vector3f crosshairPosition;
    private float crosshairSize = 32f;
    private boolean isVisible = true;
    private boolean showAccuracyIndicator = true;

    // Dynamic accuracy
    private float currentAccuracy = 1.0f;  // 1.0 = perfect accuracy, 0.0 = very inaccurate
    private float baseAccuracyRadius = 20f;
    private float maxAccuracyRadius = 100f;

    // Weapon alignment
    private Vector3f weaponAlignment = new Vector3f(0, 0, 0); // Offset from screen center
    private boolean useWeaponAlignment = false;

    public CrosshairManager(AssetManager assetManager, Node guiNode, AppSettings settings) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.settings = settings;

        initialize();
    }

    private void initialize() {
        crosshairNode = new Node("CrosshairNode");

        // Calculate center position
        crosshairPosition = new Vector3f(
                settings.getWidth() / 2f,
                settings.getHeight() / 2f,
                0
        );

        createCrosshair();
        createAccuracyIndicator();}

    /**
     * Create the main crosshair graphic
     */
    private void createCrosshair() {
        crosshairPicture = new Picture("Crosshair");
        crosshairPicture.setWidth(crosshairSize);
        crosshairPicture.setHeight(crosshairSize);

        // Create material
        crosshairMaterial = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");

        try {
            // Load crosshair texture
            Texture crosshairTexture = assetManager.loadTexture("Textures/UI/crosshair.png");
            crosshairTexture.setMagFilter(Texture.MagFilter.Nearest);
            crosshairTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            crosshairMaterial.setTexture("Texture", crosshairTexture);} catch (Exception e) {
            // Fallback to colored crosshair
            crosshairMaterial.setColor("Color", new ColorRGBA(1f, 1f, 1f, 0.8f));}

        // Enable alpha blending
        crosshairMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        crosshairMaterial.setTransparent(true);

        crosshairPicture.setMaterial(crosshairMaterial);
        updateCrosshairPosition();

        crosshairNode.attachChild(crosshairPicture);
    }

    /**
     * Create accuracy indicator circle
     */
    private void createAccuracyIndicator() {
        accuracyCircle = new Picture("AccuracyCircle");

        // Create ring material
        accuracyMaterial = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");

        try {
            // Try to load accuracy ring texture (optional)
            Texture ringTexture = assetManager.loadTexture("Textures/UI/accuracy_ring.png");
            ringTexture.setMagFilter(Texture.MagFilter.Bilinear);
            ringTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
            accuracyMaterial.setTexture("Texture", ringTexture);
        } catch (Exception e) {
            // Fallback to colored ring
            accuracyMaterial.setColor("Color", new ColorRGBA(1f, 0f, 0f, 0.3f)); // Semi-transparent red
        }

        accuracyMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        accuracyMaterial.setTransparent(true);

        accuracyCircle.setMaterial(accuracyMaterial);
        updateAccuracyIndicator();

        if (showAccuracyIndicator) {
            crosshairNode.attachChild(accuracyCircle);
        }
    }

    /**
     * Update crosshair position based on weapon alignment
     */
    private void updateCrosshairPosition() {
        Vector3f finalPosition = crosshairPosition.clone();

        if (useWeaponAlignment) {
            finalPosition.addLocal(weaponAlignment);
        }

        // Center the crosshair
        finalPosition.x -= crosshairSize / 2f;
        finalPosition.y -= crosshairSize / 2f;

        crosshairPicture.setPosition(finalPosition.x, finalPosition.y);
    }

    /**
     * Update accuracy indicator size and position
     */
    private void updateAccuracyIndicator() {
        // Calculate current accuracy radius
        float currentRadius = baseAccuracyRadius + (maxAccuracyRadius - baseAccuracyRadius) * (1f - currentAccuracy);

        accuracyCircle.setWidth(currentRadius * 2);
        accuracyCircle.setHeight(currentRadius * 2);

        // Position accuracy circle
        Vector3f circlePos = crosshairPosition.clone();
        if (useWeaponAlignment) {
            circlePos.addLocal(weaponAlignment);
        }

        circlePos.x -= currentRadius;
        circlePos.y -= currentRadius;

        accuracyCircle.setPosition(circlePos.x, circlePos.y);

        // Update color based on accuracy
        ColorRGBA accuracyColor = getAccuracyColor(currentAccuracy);
        accuracyMaterial.setColor("Color", accuracyColor);
    }

    /**
     * Get color based on accuracy level
     */
    private ColorRGBA getAccuracyColor(float accuracy) {
        if (accuracy > 0.8f) {
            return new ColorRGBA(0f, 1f, 0f, 0.3f); // Green - good accuracy
        } else if (accuracy > 0.5f) {
            return new ColorRGBA(1f, 1f, 0f, 0.4f); // Yellow - medium accuracy
        } else {
            return new ColorRGBA(1f, 0f, 0f, 0.5f); // Red - poor accuracy
        }
    }

    /**
     * Get the world position where bullets should be aimed
     * This accounts for weapon alignment and returns the 3D target point
     */
    public Vector3f getAimPoint(float distance) {
        // Get screen position of crosshair
        Vector3f screenPos = crosshairPosition.clone();
        if (useWeaponAlignment) {
            screenPos.addLocal(weaponAlignment);
        }

        // Convert screen coordinates to normalized device coordinates
        float normalizedX = (screenPos.x / settings.getWidth()) * 2f - 1f;
        float normalizedY = ((settings.getHeight() - screenPos.y) / settings.getHeight()) * 2f - 1f;

        // This would require camera and projection matrix math
        // For now, return a simplified calculation
        // In a real implementation, you'd use camera.getWorldCoordinates()

        // Simplified: return a point in front of camera at specified distance
        // The calling code should implement proper screen-to-world conversion
        return new Vector3f(normalizedX * distance * 0.1f, normalizedY * distance * 0.1f, distance);
    }

    /**
     * Get crosshair screen position for bullet spawn alignment
     */
    public Vector3f getCrosshairScreenPosition() {
        Vector3f screenPos = crosshairPosition.clone();
        if (useWeaponAlignment) {
            screenPos.addLocal(weaponAlignment);
        }
        return screenPos;
    }

    /**
     * Update crosshair based on weapon state
     */
    public void update(float tpf, boolean isMoving, boolean isAiming, boolean isReloading) {
        // Update accuracy based on player state
        updateAccuracy(tpf, isMoving, isAiming, isReloading);

        // Update visuals
        updateAccuracyIndicator();
        updateCrosshairPosition();
    }

    /**
     * Update accuracy based on player actions
     */
    private void updateAccuracy(float tpf, boolean isMoving, boolean isAiming, boolean isReloading) {
        float targetAccuracy = 1.0f; // Start with perfect accuracy

        // Reduce accuracy when moving
        if (isMoving) {
            targetAccuracy *= 0.6f;
        }

        // Improve accuracy when aiming (if you implement ADS)
        if (isAiming) {
            targetAccuracy *= 1.2f;
        }

        // No accuracy when reloading
        if (isReloading) {
            targetAccuracy = 0.0f;
        }

        // Smooth transition to target accuracy
        float accuracyChangeSpeed = 3.0f;
        if (targetAccuracy > currentAccuracy) {
            accuracyChangeSpeed = 1.5f; // Slower to improve accuracy
        }

        currentAccuracy = lerp(currentAccuracy, targetAccuracy, accuracyChangeSpeed * tpf);
        currentAccuracy = Math.max(0f, Math.min(1f, currentAccuracy));
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(1f, t);
    }

    // ==== WEAPON ALIGNMENT METHODS ====

    /**
     * Set weapon alignment offset from screen center
     * This should match your weapon's visual position
     */
    public void setWeaponAlignment(float offsetX, float offsetY) {
        this.weaponAlignment.set(offsetX, offsetY, 0);
        this.useWeaponAlignment = true;
        updateCrosshairPosition();}

    /**
     * Align crosshair with weapon position from ModernWeaponAnimator
     */
    public void alignWithWeapon(Vector3f weaponScreenPosition, float weaponScale) {
        // Calculate offset from screen center to weapon center
        Vector3f screenCenter = new Vector3f(settings.getWidth() / 2f, settings.getHeight() / 2f, 0);
        Vector3f weaponCenter = weaponScreenPosition.clone();

        // Adjust for weapon size - aim point should be at weapon muzzle, not center
        weaponCenter.x += (64 * weaponScale); // Adjust based on your weapon sprite
        weaponCenter.y += (32 * weaponScale); // Adjust based on your weapon sprite

        Vector3f alignment = weaponCenter.subtract(screenCenter);
        setWeaponAlignment(alignment.x, alignment.y);}

    /**
     * Reset to center alignment
     */
    public void resetAlignment() {
        this.useWeaponAlignment = false;
        this.weaponAlignment.set(0, 0, 0);
        updateCrosshairPosition();}

    // ==== CONFIGURATION METHODS ====

    public void setCrosshairSize(float size) {
        this.crosshairSize = Math.max(16f, Math.min(64f, size));
        crosshairPicture.setWidth(crosshairSize);
        crosshairPicture.setHeight(crosshairSize);
        updateCrosshairPosition();
    }

    public void setCrosshairColor(ColorRGBA color) {
        crosshairMaterial.setColor("Color", color);
    }

    public void setAccuracyIndicatorVisible(boolean visible) {
        this.showAccuracyIndicator = visible;
        if (visible && accuracyCircle.getParent() == null) {
            crosshairNode.attachChild(accuracyCircle);
        } else if (!visible && accuracyCircle.getParent() != null) {
            crosshairNode.detachChild(accuracyCircle);
        }
    }

    public void setBaseAccuracy(float baseRadius, float maxRadius) {
        this.baseAccuracyRadius = Math.max(10f, baseRadius);
        this.maxAccuracyRadius = Math.max(baseRadius + 10f, maxRadius);
        updateAccuracyIndicator();
    }

    // ==== VISIBILITY METHODS ====

    public void show() {
        if (!isVisible && crosshairNode.getParent() == null) {
            guiNode.attachChild(crosshairNode);
            isVisible = true;
        }
    }

    public void hide() {
        if (isVisible && crosshairNode.getParent() != null) {
            guiNode.detachChild(crosshairNode);
            isVisible = false;
        }
    }

    public void setVisible(boolean visible) {
        if (visible) {
            show();
        } else {
            hide();
        }
    }

    // ==== GETTERS ====

    public boolean isVisible() {
        return isVisible;
    }

    public float getCurrentAccuracy() {
        return currentAccuracy;
    }

    public Vector3f getWeaponAlignment() {
        return weaponAlignment.clone();
    }

    public boolean isUsingWeaponAlignment() {
        return useWeaponAlignment;
    }

    // ==== CLEANUP ====

    public void cleanup() {
        if (crosshairNode.getParent() != null) {
            guiNode.detachChild(crosshairNode);
        }
        crosshairNode.detachAllChildren();}

    // ==== DEBUG METHODS ====

    public String getAlignmentInfo() {
        return "Crosshair Alignment Info:\n" +
                "  Screen Center: " + crosshairPosition + "\n" +
                "  Weapon Alignment: " + weaponAlignment + "\n" +
                "  Using Alignment: " + useWeaponAlignment + "\n" +
                "  Current Accuracy: " + String.format("%.2f", currentAccuracy) + "\n" +
                "  Crosshair Size: " + crosshairSize;
    }
}