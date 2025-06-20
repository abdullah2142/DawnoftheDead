package horrorjme;

import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple muzzle flash light system - creates a bright flash that illuminates surroundings
 * No complex 3D models or particles - just pure lighting effect
 */
public class ModelBasedMuzzleFlash {

    private Node rootNode;
    private Camera camera;

    // Flash settings
    private float flashDuration = 0.08f;        // How long the flash lasts
    private float flashIntensity = 50f;         // How bright the flash is
    private float flashRadius = 10f;            // How far the light reaches
    private ColorRGBA flashColor = new ColorRGBA(1f, 0.9f, 0.7f, 1f); // Warm white color

    // Positioning
    private Vector3f muzzleOffset = new Vector3f(0.2f, -0.3f, 0.5f); // right, down, forward from camera

    // Active flashes
    private List<FlashInstance> activeFlashes;

    private static class FlashInstance {
        PointLight light;
        float timer;
        float duration;
        float maxIntensity;

        FlashInstance(PointLight light, float duration, float intensity) {
            this.light = light;
            this.timer = 0f;
            this.duration = duration;
            this.maxIntensity = intensity;
        }
    }

    public ModelBasedMuzzleFlash(Node rootNode, Camera camera) {
        this.rootNode = rootNode;
        this.camera = camera;
        this.activeFlashes = new ArrayList<>();

        System.out.println("Simple Muzzle Flash system initialized");
        System.out.println("Flash settings: Duration=" + flashDuration + "s, Intensity=" + flashIntensity + ", Radius=" + flashRadius);
    }

    /**
     * Create a bright flash of light when firing
     */
    public void createMuzzleFlash() {
        // Calculate flash position
        Vector3f flashPosition = calculateFlashPosition();

        // Create bright point light
        PointLight flashLight = new PointLight();
        flashLight.setPosition(flashPosition);
        flashLight.setColor(flashColor.mult(flashIntensity));
        flashLight.setRadius(flashRadius);

        // Add light to scene
        rootNode.addLight(flashLight);

        // Track this flash for fade-out
        FlashInstance flash = new FlashInstance(flashLight, flashDuration, flashIntensity);
        activeFlashes.add(flash);

        System.out.println("Muzzle flash created - bright light illuminating surroundings");
    }

    /**
     * Calculate where the flash should appear
     */
    private Vector3f calculateFlashPosition() {
        Vector3f cameraPos = camera.getLocation().clone();
        Vector3f forward = camera.getDirection().mult(muzzleOffset.z);   // Forward
        Vector3f right = camera.getLeft().negate().mult(muzzleOffset.x); // Right
        Vector3f vertical = Vector3f.UNIT_Y.mult(-muzzleOffset.y);       // Down

        return cameraPos.add(forward).add(right).add(vertical);
    }

    /**
     * Update all active flashes (fade them out)
     */
    public void update(float tpf) {
        List<FlashInstance> toRemove = new ArrayList<>();

        for (FlashInstance flash : activeFlashes) {
            flash.timer += tpf;

            if (flash.timer >= flash.duration) {
                // Flash finished - remove it
                rootNode.removeLight(flash.light);
                toRemove.add(flash);
            } else {
                // Fade out the flash
                float fadeProgress = flash.timer / flash.duration;
                float currentIntensity = flash.maxIntensity * (1f - fadeProgress);

                // Update light intensity
                ColorRGBA currentColor = flashColor.mult(currentIntensity);
                flash.light.setColor(currentColor);
            }
        }

        // Remove finished flashes
        activeFlashes.removeAll(toRemove);
    }

    /**
     * Clean up all active flashes
     */
    public void cleanup() {
        for (FlashInstance flash : activeFlashes) {
            rootNode.removeLight(flash.light);
        }
        activeFlashes.clear();
        System.out.println("Muzzle flash system cleaned up");
    }

    // ==== CONFIGURATION METHODS ====

    /**
     * Set how long the flash lasts (in seconds)
     */
    public void setFlashDuration(float duration) {
        this.flashDuration = Math.max(0.01f, Math.min(0.5f, duration));
        System.out.println("Flash duration set to: " + this.flashDuration + " seconds");
    }

    /**
     * Set how bright the flash is
     */
    public void setFlashIntensity(float intensity) {
        this.flashIntensity = Math.max(1f, intensity);
        System.out.println("Flash intensity set to: " + this.flashIntensity);
    }

    /**
     * Set how far the light reaches
     */
    public void setFlashRadius(float radius) {
        this.flashRadius = Math.max(1f, radius);
        System.out.println("Flash radius set to: " + this.flashRadius + " units");
    }

    /**
     * Set the color of the flash
     */
    public void setFlashColor(ColorRGBA color) {
        this.flashColor = color.clone();
        System.out.println("Flash color set to: " + color);
    }

    /**
     * Set flash position offset from camera
     */
    public void setMuzzleOffset(float rightOffset, float downOffset, float forwardOffset) {
        this.muzzleOffset.set(rightOffset, downOffset, forwardOffset);
        System.out.println("Flash position set to: Right=" + rightOffset + ", Down=" + downOffset + ", Forward=" + forwardOffset);
    }

    /**
     * Quick presets for different lighting effects
     */
    public void setFlashPreset(FlashPreset preset) {
        switch (preset) {
            case BRIGHT_CAMERA_FLASH:
                setFlashDuration(0.06f);
                setFlashIntensity(25f);
                setFlashRadius(12f);
                setFlashColor(new ColorRGBA(1f, 1f, 1f, 1f)); // Pure white
                break;

            case WARM_GUN_FLASH:
                setFlashDuration(0.08f);
                setFlashIntensity(20f);
                setFlashRadius(10f);
                setFlashColor(new ColorRGBA(1f, 0.9f, 0.7f, 1f)); // Warm white
                break;

            case SUBTLE_ILLUMINATION:
                setFlashDuration(0.12f);
                setFlashIntensity(15f);
                setFlashRadius(8f);
                setFlashColor(new ColorRGBA(1f, 0.95f, 0.8f, 1f)); // Soft warm
                break;

            case DRAMATIC_BRIGHT:
                setFlashDuration(0.05f);
                setFlashIntensity(30f);
                setFlashRadius(15f);
                setFlashColor(new ColorRGBA(1f, 1f, 0.9f, 1f)); // Bright white
                break;
        }
        System.out.println("Applied flash preset: " + preset);
    }

    public enum FlashPreset {
        BRIGHT_CAMERA_FLASH,    // Like a camera flash - very bright and brief
        WARM_GUN_FLASH,         // Realistic gun muzzle flash - warm color
        SUBTLE_ILLUMINATION,    // Gentle lighting effect
        DRAMATIC_BRIGHT         // Very dramatic, bright flash
    }

    // ==== STATUS METHODS ====

    /**
     * Check if any flashes are currently active
     */
    public boolean isFlashActive() {
        return !activeFlashes.isEmpty();
    }

    /**
     * Get number of active flashes
     */
    public int getActiveFlashCount() {
        return activeFlashes.size();
    }

    /**
     * Get current settings summary
     */
    public String getFlashSettings() {
        return "Flash Settings: Duration=" + flashDuration +
                "s, Intensity=" + flashIntensity +
                ", Radius=" + flashRadius +
                ", Color=" + flashColor;
    }
}