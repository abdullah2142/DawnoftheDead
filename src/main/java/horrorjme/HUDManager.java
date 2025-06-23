package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

/**
 * HUD Manager for the game interface.
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

    // State
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

        guiNode.attachChild(hudNode);

    }

    /**
     * Main HUD update method
     */
    public void updateHUD(Player player) {
        if (player != null) {
            updateHealthDisplay(player);
            updateTorchStatus(player);
            updateAmmoDisplay(player);
            updateReloadStatus(player);

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
     * NEW: Update debug information display
     */
    private void updateDebugInfo(Player player) {
        if (debugText != null && player != null) {
            StringBuilder debug = new StringBuilder();
            debug.append("=== DEBUG INFO ===\n");
            debug.append("Health: ").append(String.format("%.1f", player.getHealth())).append("\n");
            debug.append("Ammo: ").append(player.getCurrentAmmo()).append("/").append(player.getMaxAmmo()).append("\n");
            debug.append("Reloading: ").append(player.isReloading() ? "YES" : "NO").append("\n");

            debugText.setText(debug.toString());
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
        }

    }

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

        // In a real implementation, you'd want a timer system to remove this

    }

    // ==== VISIBILITY METHODS ====

    public void hide() {
        if (hudNode.getParent() != null) {
            guiNode.detachChild(hudNode);
        }
    }

    public void show() {
        if (hudNode.getParent() == null) {
            guiNode.attachChild(hudNode);
        }
    }

    // ==== CLEANUP ====

    /**
     * Clean up all HUD resources
     */
    public void cleanup() {
        if (hudNode.getParent() != null) {
            guiNode.detachChild(hudNode);
        }
        hudNode.detachAllChildren();

    }

    // ==== GETTERS ====

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
                setDebugInfoVisible(false);
                // Hide some HUD elements for minimal UI
                break;

            case FULL_INFO:
                setDebugInfoVisible(false); // Default to false unless debugging
                // Show all HUD elements
                break;

            case DEBUG_MODE:
                setDebugInfoVisible(true);
                // Maximum information for debugging
                break;
        }

    }

    public enum HUDPreset {
        MINIMAL,      // Essential info
        FULL_INFO,    // All HUD elements visible
        DEBUG_MODE    // Maximum debug information
    }
}