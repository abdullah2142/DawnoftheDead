package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

/**
 * Enhanced HUD Manager with integrated crosshair system
 */
public class HUDManager {
    private AssetManager assetManager;
    private Node guiNode;
    private AppSettings settings;

    // HUD elements
    private BitmapText healthText;
    private BitmapText torchText;
    private BitmapText ammoText;
    private BitmapText reloadText;
    private BitmapText debugText;        // NEW: Debug information
    private BitmapFont defaultFont;
    private Node hudNode;

    // NEW: Integrated crosshair system
    private CrosshairManager crosshairManager;
    private boolean showCrosshair = true;
    private boolean showDebugInfo = false;

    public HUDManager(AssetManager assetManager, Node guiNode, AppSettings settings) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.settings = settings;
        initialize();
    }

    private void initialize() {
        defaultFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hudNode = new Node("HUD");

        // Initialize crosshair system
        crosshairManager = new CrosshairManager(assetManager, guiNode, settings);

        // Health display
        healthText = new BitmapText(defaultFont);
        healthText.setSize(defaultFont.getCharSet().getRenderedSize());
        healthText.setColor(ColorRGBA.Green);
        healthText.setLocalTranslation(10, settings.getHeight() - 10, 0);
        hudNode.attachChild(healthText);

        // Torch status
        torchText = new BitmapText(defaultFont);
        torchText.setSize(defaultFont.getCharSet().getRenderedSize());
        torchText.setColor(ColorRGBA.Yellow);
        torchText.setLocalTranslation(10, settings.getHeight() - 35, 0);
        hudNode.attachChild(torchText);

        // Ammo display (bottom right)
        ammoText = new BitmapText(defaultFont);
        ammoText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.2f);
        ammoText.setColor(ColorRGBA.White);
        ammoText.setLocalTranslation(settings.getWidth() - 150, 60, 0);
        hudNode.attachChild(ammoText);

        // Reload status (center of screen)
        reloadText = new BitmapText(defaultFont);
        reloadText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.5f);
        reloadText.setColor(ColorRGBA.Yellow);
        reloadText.setLocalTranslation(settings.getWidth() / 2 - 50, settings.getHeight() / 2 - 50, 0);
        hudNode.attachChild(reloadText);

        // NEW: Debug text (top right)
        debugText = new BitmapText(defaultFont);
        debugText.setSize(defaultFont.getCharSet().getRenderedSize() * 0.9f);
        debugText.setColor(ColorRGBA.Cyan);
        debugText.setLocalTranslation(settings.getWidth() - 300, settings.getHeight() - 10, 0);
        if (showDebugInfo) {
            hudNode.attachChild(debugText);
        }

        guiNode.attachChild(hudNode);}

    /**
     * Main HUD update method
     */
    public void updateHUD(Player player) {
        if (player != null) {
            updateHealthDisplay(player);
            updateTorchStatus(player);
            updateAmmoDisplay(player);
            updateReloadStatus(player);

            // NEW: Update crosshair with player state
            updateCrosshair(player);

            // NEW: Update debug info if enabled
            if (showDebugInfo) {
                updateDebugInfo(player);
            }
        }
    }

    private void updateHealthDisplay(Player player) {
        healthText.setText("Health: " + (int)player.getHealth() + "/" + (int)player.getMaxHealth());

        // Change health color based on percentage
        float healthPercent = player.getHealth() / player.getMaxHealth();
        if (healthPercent > 0.6f) {
            healthText.setColor(ColorRGBA.Green);
        } else if (healthPercent > 0.3f) {
            healthText.setColor(ColorRGBA.Yellow);
        } else {
            healthText.setColor(ColorRGBA.Red);
        }
    }

    private void updateTorchStatus(Player player) {
        torchText.setText("Torch: " + (player.isTorchOn() ? "ON" : "OFF"));
    }

    private void updateAmmoDisplay(Player player) {
        int currentAmmo = player.getCurrentAmmo();
        int maxAmmo = player.getMaxAmmo();

        ammoText.setText("AMMO: " + currentAmmo + "/" + maxAmmo);

        // Color code ammo based on amount remaining
        float ammoPercent = (float) currentAmmo / maxAmmo;
        if (ammoPercent > 0.5f) {
            ammoText.setColor(ColorRGBA.White);
        } else if (ammoPercent > 0.2f) {
            ammoText.setColor(ColorRGBA.Yellow);
        } else if (currentAmmo > 0) {
            ammoText.setColor(ColorRGBA.Orange);
        } else {
            ammoText.setColor(ColorRGBA.Red);
        }
    }

    private void updateReloadStatus(Player player) {
        if (player.isReloading()) {
            reloadText.setText("RELOADING...");
            // Make it pulse for visibility
            float alpha = 0.7f + 0.3f * (float)Math.sin(System.currentTimeMillis() * 0.01);
            reloadText.setColor(new ColorRGBA(1f, 1f, 0f, alpha));
        } else {
            reloadText.setText("");
        }
    }

    /**
     * NEW: Update crosshair based on player state
     */
    private void updateCrosshair(Player player) {
        if (crosshairManager != null && showCrosshair) {
            // Determine player movement state for accuracy
            boolean isMoving = isPlayerMoving(player);
            boolean isAiming = false; // You can implement ADS later
            boolean isReloading = player.isReloading();

            // Update crosshair with current state
            crosshairManager.update(0.016f, isMoving, isAiming, isReloading); // Assuming ~60fps
        }
    }

    /**
     * NEW: Check if player is moving (affects crosshair accuracy)
     */
    private boolean isPlayerMoving(Player player) {
        // Check velocity from player if available
        // For now, use a simple heuristic
        return false; // You'll need to implement this based on your player movement system
    }

    /**
     * NEW: Update debug information display
     */
    private void updateDebugInfo(Player player) {
        if (debugText != null && player != null) {
            StringBuilder debug = new StringBuilder();
            debug.append("=== DEBUG INFO ===\n");
            debug.append("Health: ").append(String.format("%.1f", player.getHealth())).append("\n");
            debug.append("Ammo: ").append(player.getCurrentAmmo()).append("/").append(player.getMaxAmmo()).append("\n");
            debug.append("Reloading: ").append(player.isReloading() ? "YES" : "NO").append("\n");

            if (crosshairManager != null) {
                debug.append("Crosshair Accuracy: ").append(String.format("%.2f", crosshairManager.getCurrentAccuracy())).append("\n");
                debug.append("Weapon Aligned: ").append(crosshairManager.isUsingWeaponAlignment() ? "YES" : "NO").append("\n");
            }

            debugText.setText(debug.toString());
        }
    }

    // ==== CROSSHAIR CONTROL METHODS ====

    /**
     * Get the crosshair manager for external configuration
     */
    public CrosshairManager getCrosshairManager() {
        return crosshairManager;
    }

    /**
     * Show/hide crosshair
     */
    public void setCrosshairVisible(boolean visible) {
        this.showCrosshair = visible;
        if (crosshairManager != null) {
            crosshairManager.setVisible(visible);
        }
    }

    /**
     * Configure crosshair for current weapon
     */
    public void configureCrosshairForWeapon(ModernWeaponAnimator weaponAnimator) {
        if (crosshairManager != null && weaponAnimator != null) {
            // Get weapon position information
            // You'll need to add methods to ModernWeaponAnimator to get position info

            // For now, use default alignment
            crosshairManager.resetAlignment();}
    }

    /**
     * Set crosshair alignment to match weapon position
     */
    public void alignCrosshairWithWeapon(float weaponX, float weaponY, float weaponScale) {
        if (crosshairManager != null) {
            // Calculate screen position of weapon
            Vector3f weaponScreenPos = new Vector3f(weaponX, weaponY, 0);
            crosshairManager.alignWithWeapon(weaponScreenPos, weaponScale);
        }
    }

    // ==== DEBUG METHODS ====

    /**
     * Toggle debug information display
     */
    public void toggleDebugInfo() {
        showDebugInfo = !showDebugInfo;

        if (showDebugInfo && debugText.getParent() == null) {
            hudNode.attachChild(debugText);
        } else if (!showDebugInfo && debugText.getParent() != null) {
            hudNode.detachChild(debugText);
        }}

    /**
     * Set debug info visibility
     */
    public void setDebugInfoVisible(boolean visible) {
        if (showDebugInfo != visible) {
            toggleDebugInfo();
        }
    }

    // ==== TEMPORARY MESSAGE SYSTEM ====

    /**
     * Show a temporary message (useful for pickup notifications, etc.)
     */
    public void showTemporaryMessage(String message, float duration, ColorRGBA color) {
        BitmapText tempText = new BitmapText(defaultFont);
        tempText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.3f);
        tempText.setText(message);
        tempText.setColor(color);
        tempText.setLocalTranslation(
                settings.getWidth() / 2 - tempText.getLineWidth() / 2,
                settings.getHeight() / 2 + 100,
                0
        );

        hudNode.attachChild(tempText);

        // In a real implementation, you'd want a timer system to remove this}

    // ==== VISIBILITY METHODS ====

    public void hide() {
        if (hudNode.getParent() != null) {
            guiNode.detachChild(hudNode);
        }
        if (crosshairManager != null) {
            crosshairManager.hide();
        }
    }

    public void show() {
        if (hudNode.getParent() == null) {
            guiNode.attachChild(hudNode);
        }
        if (crosshairManager != null && showCrosshair) {
            crosshairManager.show();
        }
    }

    // ==== CLEANUP ====

    /**
     * Clean up all HUD resources
     */
    public void cleanup() {
        if (crosshairManager != null) {
            crosshairManager.cleanup();
        }

        if (hudNode.getParent() != null) {
            guiNode.detachChild(hudNode);
        }
        hudNode.detachAllChildren();}

    // ==== GETTERS ====

    public boolean isCrosshairVisible() {
        return showCrosshair;
    }

    public boolean isDebugInfoVisible() {
        return showDebugInfo;
    }

    // ==== CONFIGURATION PRESETS ====

    /**
     * Apply HUD preset for different game modes
     */
    public void applyHUDPreset(HUDPreset preset) {
        switch (preset) {
            case MINIMAL:
                setCrosshairVisible(true);
                setDebugInfoVisible(false);
                // Hide some HUD elements for minimal UI
                break;

            case FULL_INFO:
                setCrosshairVisible(true);
                setDebugInfoVisible(true);
                // Show all HUD elements
                break;

            case NO_CROSSHAIR:
                setCrosshairVisible(false);
                setDebugInfoVisible(false);
                // Traditional HUD without crosshair
                break;

            case DEBUG_MODE:
                setCrosshairVisible(true);
                setDebugInfoVisible(true);
                // Maximum information for debugging
                break;
        }}

    public enum HUDPreset {
        MINIMAL,      // Just crosshair and essential info
        FULL_INFO,    // All HUD elements visible
        NO_CROSSHAIR, // Traditional HUD without crosshair
        DEBUG_MODE    // Maximum debug information
    }
}