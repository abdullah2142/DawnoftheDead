package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

/**
 * CORRECTED HUD Manager - REPLACE YOUR EXISTING HUDManager.java with this version
 * Updated to work with new weapon/ammo inventory system
 */
public class HUDManager {
    private AssetManager assetManager;
    private Node guiNode;
    private AppSettings settings;

    // HUD elements
    private BitmapText healthText;
    private BitmapText torchText;
    private BitmapText weaponText;        // NEW: Current weapon name
    private BitmapText ammoText;          // UPDATED: Current weapon ammo only
    private BitmapText reloadText;
    private BitmapText noWeaponText;      // NEW: "UNARMED" indicator
    private BitmapText debugText;
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

        // Health display (top left)
        healthText = new BitmapText(defaultFont);
        healthText.setSize(defaultFont.getCharSet().getRenderedSize());
        healthText.setColor(ColorRGBA.Green);
        healthText.setLocalTranslation(10, settings.getHeight() - 10, 0);
        hudNode.attachChild(healthText);

        // Torch status (below health)
        torchText = new BitmapText(defaultFont);
        torchText.setSize(defaultFont.getCharSet().getRenderedSize());
        torchText.setColor(ColorRGBA.Yellow);
        torchText.setLocalTranslation(10, settings.getHeight() - 35, 0);
        hudNode.attachChild(torchText);

        // NEW: Current weapon name (bottom left)
        weaponText = new BitmapText(defaultFont);
        weaponText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.3f);
        weaponText.setColor(ColorRGBA.White);
        weaponText.setLocalTranslation(10, 100, 0);
        hudNode.attachChild(weaponText);

        // UPDATED: Current weapon ammo only (bottom right)
        ammoText = new BitmapText(defaultFont);
        ammoText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.4f);
        ammoText.setColor(ColorRGBA.White);
        ammoText.setLocalTranslation(settings.getWidth() - 150, 60, 0);
        hudNode.attachChild(ammoText);

        // NEW: "UNARMED" indicator (center bottom)
        noWeaponText = new BitmapText(defaultFont);
        noWeaponText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.8f);
        noWeaponText.setColor(ColorRGBA.Orange);
        noWeaponText.setText("UNARMED");
        noWeaponText.setLocalTranslation(
                settings.getWidth() / 2 - noWeaponText.getLineWidth() / 2,
                80,
                0
        );
        hudNode.attachChild(noWeaponText);

        // Reload status (center of screen)
        reloadText = new BitmapText(defaultFont);
        reloadText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.5f);
        reloadText.setColor(ColorRGBA.Yellow);
        reloadText.setLocalTranslation(settings.getWidth() / 2 - 50, settings.getHeight() / 2 - 50, 0);
        hudNode.attachChild(reloadText);

        // Debug text (top right)
        debugText = new BitmapText(defaultFont);
        debugText.setSize(defaultFont.getCharSet().getRenderedSize() * 0.9f);
        debugText.setColor(ColorRGBA.Cyan);
        debugText.setLocalTranslation(settings.getWidth() - 400, settings.getHeight() - 10, 0);
        if (showDebugInfo) {
            hudNode.attachChild(debugText);
        }

        guiNode.attachChild(hudNode);
    }

    /**
     * UPDATED: Main HUD update method for new weapon system
     */
    public void updateHUD(Player player) {
        if (player != null) {
            updateHealthDisplay(player);
            updateTorchStatus(player);
            updateWeaponDisplay(player);        // NEW
            updateAmmoDisplay(player);          // UPDATED
            updateReloadStatus(player);
            updateNoWeaponIndicator(player);    // NEW

            // Update debug info if enabled
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

    /**
     * NEW: Display current weapon name
     */
    private void updateWeaponDisplay(Player player) {
        String weaponName = player.getCurrentWeaponName();
        weaponText.setText(weaponName);

        // Color code weapon name
        if (player.hasAnyWeapon()) {
            weaponText.setColor(ColorRGBA.White);
        } else {
            weaponText.setColor(ColorRGBA.Gray);
        }
    }

    /**
     * UPDATED: Display current weapon ammo only (as requested)
     */
    private void updateAmmoDisplay(Player player) {
        if (!player.hasAnyWeapon()) {
            // Hide ammo display when no weapon
            ammoText.setText("");
            return;
        }

        // Show current weapon ammo in format: LOADED/RESERVE
        String ammoStatus = player.getAmmoStatusString();
        ammoText.setText("AMMO: " + ammoStatus);

        // Color code ammo based on loaded ammo amount
        int currentAmmo = player.getCurrentAmmo();
        int maxAmmo = player.getMaxAmmo();

        if (maxAmmo > 0) {
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
        } else {
            ammoText.setColor(ColorRGBA.Gray);
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
     * NEW: Show/hide "UNARMED" indicator
     */
    private void updateNoWeaponIndicator(Player player) {
        if (!player.hasAnyWeapon()) {
            // Show unarmed indicator
            if (noWeaponText.getParent() == null) {
                hudNode.attachChild(noWeaponText);
            }

            // Make it pulse to draw attention
            float alpha = 0.6f + 0.4f * (float)Math.sin(System.currentTimeMillis() * 0.008);
            noWeaponText.setColor(new ColorRGBA(1f, 0.5f, 0f, alpha)); // Orange pulse
        } else {
            // Hide unarmed indicator
            if (noWeaponText.getParent() != null) {
                hudNode.detachChild(noWeaponText);
            }
        }
    }

    /**
     * UPDATED: Debug information display with new weapon system
     */
    private void updateDebugInfo(Player player) {
        if (debugText != null && player != null) {
            StringBuilder debug = new StringBuilder();
            debug.append("=== DEBUG INFO ===\n");
            debug.append("Health: ").append(String.format("%.1f", player.getHealth())).append("\n");

            // NEW: Weapon inventory debug
            if (player.hasAnyWeapon()) {
                debug.append("Weapon: ").append(player.getCurrentWeaponName()).append("\n");
                debug.append("Ammo: ").append(player.getAmmoStatusString()).append("\n");
                debug.append("Weapon Count: ").append(player.getWeaponInventory().getOwnedWeaponCount()).append("\n");
                debug.append("Systems Init: ").append(player.areWeaponSystemsInitialized() ? "YES" : "NO").append("\n");
            } else {
                debug.append("Status: UNARMED\n");
            }

            debug.append("Reloading: ").append(player.isReloading() ? "YES" : "NO").append("\n");

            debugText.setText(debug.toString());
        }
    }

    // ==== PICKUP NOTIFICATION METHODS ====

    /**
     * NEW: Show weapon pickup notification
     */
    public void onWeaponPickup(String weaponName, int ammo) {
        String message = "PICKED UP: " + weaponName + " (" + ammo + " rounds)";
        showTemporaryMessage(message, 3f, ColorRGBA.Green);
        System.out.println("HUD: " + message);
    }

    /**
     * NEW: Show ammo pickup notification
     */
    public void onAmmoPickup(String ammoType, int amount) {
        String message = "AMMO: +" + amount + " " + ammoType;
        showTemporaryMessage(message, 2f, ColorRGBA.Yellow);
        System.out.println("HUD: " + message);
    }

    /**
     * NEW: Show weapon switch notification
     */
    public void onWeaponSwitch(String weaponName) {
        showTemporaryMessage("Switched to: " + weaponName, 1.5f, ColorRGBA.White);
    }

    /**
     * NEW: Show no ammo message
     */
    public void onNoAmmo() {
        showTemporaryMessage("NO AMMO!", 2f, ColorRGBA.Red);
    }

    /**
     * NEW: Show empty weapon message
     */
    public void onEmptyWeapon() {
        showTemporaryMessage("*CLICK*", 1f, ColorRGBA.Orange);
    }

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

        // TODO: In a real implementation, you'd want a timer system to remove this
        // For now, it will stay on screen (you could implement a cleanup system)
    }

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

    public void cleanup() {
        if (hudNode.getParent() != null) {
            guiNode.detachChild(hudNode);
        }
        hudNode.detachAllChildren();
    }

    public boolean isDebugInfoVisible() {
        return showDebugInfo;
    }
}