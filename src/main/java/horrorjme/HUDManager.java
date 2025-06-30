package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * UPDATED HUD Manager - Now includes Timer and Score system displays
 * Shows survival timer, current round, points, and kills
 */
public class HUDManager {
    private AssetManager assetManager;
    private Node guiNode;
    private AppSettings settings;

    // HUD elements
    private BitmapText healthText;
    private BitmapText torchText;
    private BitmapText weaponText;
    private BitmapText ammoText;
    private BitmapText reloadText;
    private BitmapText noWeaponText;
    private BitmapText debugText;

    // NEW: Timer and Score HUD elements
    private BitmapText timerText;         // Shows time remaining in round
    private BitmapText roundText;         // Shows current round number
    private BitmapText scoreText;         // Shows current points
    private BitmapText killsText;         // Shows kills this round / total kills
    private BitmapText phaseText;         // Shows current game phase

    private BitmapFont defaultFont;
    private Node hudNode;

    // Temporary message system
    private List<TemporaryMessage> temporaryMessages;

    // State
    private boolean showDebugInfo = false;

    /**
     * Temporary message data class
     */
    private static class TemporaryMessage {
        BitmapText textNode;
        float remainingTime;

        TemporaryMessage(BitmapText textNode, float duration) {
            this.textNode = textNode;
            this.remainingTime = duration;
        }
    }

    public HUDManager(AssetManager assetManager, Node guiNode, AppSettings settings) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.settings = settings;
        this.temporaryMessages = new ArrayList<>();
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

        // NEW: Timer display (top center) - Large and prominent
        timerText = new BitmapText(defaultFont);
        timerText.setSize(defaultFont.getCharSet().getRenderedSize() * 2.0f);
        timerText.setColor(ColorRGBA.White);
        timerText.setLocalTranslation(settings.getWidth() / 2 - 100, settings.getHeight() - 15, 0);
        hudNode.attachChild(timerText);

        // NEW: Round display (top center, below timer)
        roundText = new BitmapText(defaultFont);
        roundText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.3f);
        roundText.setColor(ColorRGBA.Cyan);
        roundText.setLocalTranslation(settings.getWidth() / 2 - 80, settings.getHeight() - 50, 0);
        hudNode.attachChild(roundText);

        // NEW: Score display (top right)
        scoreText = new BitmapText(defaultFont);
        scoreText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.2f);
        scoreText.setColor(ColorRGBA.Yellow);
        scoreText.setLocalTranslation(settings.getWidth() - 200, settings.getHeight() - 10, 0);
        hudNode.attachChild(scoreText);

        // NEW: Kills display (top right, below score)
        killsText = new BitmapText(defaultFont);
        killsText.setSize(defaultFont.getCharSet().getRenderedSize());
        killsText.setColor(ColorRGBA.Orange);
        killsText.setLocalTranslation(settings.getWidth() - 200, settings.getHeight() - 35, 0);
        hudNode.attachChild(killsText);

        // NEW: Game phase display (top right, below kills)
        phaseText = new BitmapText(defaultFont);
        phaseText.setSize(defaultFont.getCharSet().getRenderedSize());
        phaseText.setColor(ColorRGBA.Magenta);
        phaseText.setLocalTranslation(settings.getWidth() - 200, settings.getHeight() - 60, 0);
        hudNode.attachChild(phaseText);

        // Current weapon name (bottom left)
        weaponText = new BitmapText(defaultFont);
        weaponText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.3f);
        weaponText.setColor(ColorRGBA.White);
        weaponText.setLocalTranslation(10, 100, 0);
        hudNode.attachChild(weaponText);

        // Current weapon ammo only (bottom right)
        ammoText = new BitmapText(defaultFont);
        ammoText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.4f);
        ammoText.setColor(ColorRGBA.White);
        ammoText.setLocalTranslation(settings.getWidth() - 150, 60, 0);
        hudNode.attachChild(ammoText);

        // "UNARMED" indicator (center bottom)
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

        // Debug text (bottom left)
        debugText = new BitmapText(defaultFont);
        debugText.setSize(defaultFont.getCharSet().getRenderedSize() * 0.9f);
        debugText.setColor(ColorRGBA.Cyan);
        debugText.setLocalTranslation(10, 300, 0);
        if (showDebugInfo) {
            hudNode.attachChild(debugText);
        }

        guiNode.attachChild(hudNode);
    }

    /**
     * UPDATED: Main HUD update method - now includes timer and score systems
     */
    public void updateHUD(Player player, float tpf) {
        if (player != null) {
            updateHealthDisplay(player);
            updateTorchStatus(player);
            updateWeaponDisplay(player);
            updateAmmoDisplay(player);
            updateReloadStatus(player);
            updateNoWeaponIndicator(player);

            if (showDebugInfo) {
                updateDebugInfo(player);
            }
        }

        // Update temporary messages with proper time delta
        updateTemporaryMessages(tpf);
    }

    /**
     * NEW: Update HUD with timer and score systems
     */
    public void updateHUD(Player player, TimerSystem timerSystem, ScoreSystem scoreSystem, float tpf) {
        // Update regular player HUD
        updateHUD(player, tpf);

        // Update timer and score displays
        updateTimerDisplay(timerSystem);
        updateScoreDisplay(scoreSystem);
    }

    /**
     * NEW: Update timer display elements
     */
    private void updateTimerDisplay(TimerSystem timerSystem) {
        if (timerSystem == null) {
            timerText.setText("");
            roundText.setText("");
            phaseText.setText("");
            return;
        }

        // Timer display with color coding based on time remaining
        String timeRemaining = timerSystem.getFormattedTimeRemaining();
        timerText.setText("TIME: " + timeRemaining);

        // Color code timer based on time remaining
        float timeLeft = timerSystem.getTimeRemaining();
        if (timeLeft > 60f) {
            timerText.setColor(ColorRGBA.White);
        } else if (timeLeft > 30f) {
            timerText.setColor(ColorRGBA.Yellow);
        } else if (timeLeft > 10f) {
            timerText.setColor(ColorRGBA.Orange);
        } else {
            // Flash red when under 10 seconds
            float alpha = 0.5f + 0.5f * (float)Math.sin(System.currentTimeMillis() * 0.01);
            timerText.setColor(new ColorRGBA(1f, 0f, 0f, alpha));
        }

        // Round display
        roundText.setText("ROUND " + timerSystem.getCurrentRound());

        // Phase display with color coding
        TimerSystem.GamePhase phase = timerSystem.getCurrentPhase();
        switch (phase) {
            case WAITING:
                phaseText.setText("WAITING");
                phaseText.setColor(ColorRGBA.Gray);
                break;
            case ACTIVE:
                phaseText.setText("SURVIVE!");
                phaseText.setColor(ColorRGBA.Green);
                break;
            case COMPLETED:
                phaseText.setText("ROUND COMPLETE");
                phaseText.setColor(ColorRGBA.Cyan);
                break;
            case GAME_OVER:
                phaseText.setText("GAME OVER");
                phaseText.setColor(ColorRGBA.Red);
                break;
        }
    }

    /**
     * NEW: Update score display elements
     */
    private void updateScoreDisplay(ScoreSystem scoreSystem) {
        if (scoreSystem == null) {
            scoreText.setText("");
            killsText.setText("");
            return;
        }

        // Points display
        int currentPoints = scoreSystem.getCurrentPoints();
        scoreText.setText("POINTS: " + currentPoints);

        // Color code points based on amount
        if (currentPoints >= 100) {
            scoreText.setColor(ColorRGBA.Green);
        } else if (currentPoints >= 50) {
            scoreText.setColor(ColorRGBA.Yellow);
        } else if (currentPoints >= 20) {
            scoreText.setColor(ColorRGBA.Orange);
        } else {
            scoreText.setColor(ColorRGBA.Red);
        }

        // Kills display
        int roundKills = scoreSystem.getRoundKills();
        int totalKills = scoreSystem.getTotalKills();
        killsText.setText("KILLS: " + roundKills + "/" + totalKills);
    }

    /**
     * NEW: Show round completion message
     */
    public void showRoundComplete(int roundNumber, int bonusPoints) {
        String message = "ROUND " + roundNumber + " COMPLETE!\nBonus: +" + bonusPoints + " points";
        showTemporaryMessage(message, 3f, ColorRGBA.Cyan);
    }

    /**
     * NEW: Show round start message
     */
    public void showRoundStart(int roundNumber, float duration) {
        String message = "ROUND " + roundNumber + " START!\nSurvive for " + (int)(duration/60) + " minutes!";
        showTemporaryMessage(message, 2f, ColorRGBA.Green);
    }

    /**
     * NEW: Show game over message
     */
    public void showGameOver(int finalRound, float survivalTime, int totalKills) {
        String message = String.format("GAME OVER\nReached Round %d\nSurvived %.1f seconds\nTotal Kills: %d",
                finalRound, survivalTime, totalKills);
        showTemporaryMessage(message, 5f, ColorRGBA.Red);
    }

    /**
     * NEW: Show pickup cost information
     */
    public void showPickupCosts() {
        String message = "PICKUP COSTS:\nWeapon: " + ScoreSystem.getWeaponCost() +
                " pts\nAmmo: " + ScoreSystem.getAmmoCost() +
                " pts\nHealth: " + ScoreSystem.getHealthCost() + " pts";
        showTemporaryMessage(message, 3f, ColorRGBA.White);
    }

    // Update temporary messages timer
    private void updateTemporaryMessages(float tpf) {
        Iterator<TemporaryMessage> iterator = temporaryMessages.iterator();

        while (iterator.hasNext()) {
            TemporaryMessage tempMsg = iterator.next();
            tempMsg.remainingTime -= tpf;

            if (tempMsg.remainingTime <= 0f) {
                // Message expired - remove it
                hudNode.detachChild(tempMsg.textNode);
                iterator.remove();
            } else {
                // Fade out effect in last 0.5 seconds
                if (tempMsg.remainingTime < 0.5f) {
                    float alpha = tempMsg.remainingTime / 0.5f;
                    ColorRGBA currentColor = tempMsg.textNode.getColor();
                    tempMsg.textNode.setColor(new ColorRGBA(
                            currentColor.r, currentColor.g, currentColor.b, alpha
                    ));
                }
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

    private void updateDebugInfo(Player player) {
        if (debugText != null && player != null) {
            StringBuilder debug = new StringBuilder();
            debug.append("=== DEBUG INFO ===\n");
            debug.append("Health: ").append(String.format("%.1f", player.getHealth())).append("\n");

            // Weapon inventory debug
            if (player.hasAnyWeapon()) {
                debug.append("Weapon: ").append(player.getCurrentWeaponName()).append("\n");
                debug.append("Ammo: ").append(player.getAmmoStatusString()).append("\n");
                debug.append("Weapon Count: ").append(player.getWeaponInventory().getOwnedWeaponCount()).append("\n");
                debug.append("Systems Init: ").append(player.areWeaponSystemsInitialized() ? "YES" : "NO").append("\n");
            } else {
                debug.append("Status: UNARMED\n");
            }

            debug.append("Reloading: ").append(player.isReloading() ? "YES" : "NO").append("\n");
            debug.append("Temp Messages: ").append(temporaryMessages.size()).append("\n");

            debugText.setText(debug.toString());
        }
    }

    // ==== PICKUP NOTIFICATION METHODS ====

    public void onWeaponPickup(String weaponName, int ammo) {
        String message = "PICKED UP: " + weaponName + " (" + ammo + " rounds)";
        showTemporaryMessage(message, 1.5f, ColorRGBA.Green);
        System.out.println("HUD: " + message);
    }

    public void onAmmoPickup(String ammoType, int amount) {
        String message = "AMMO: +" + amount + " " + ammoType;
        showTemporaryMessage(message, 1f, ColorRGBA.Yellow);
        System.out.println("HUD: " + message);
    }

    public void onWeaponSwitch(String weaponName) {
        showTemporaryMessage("Switched to: " + weaponName, 1.5f, ColorRGBA.White);
    }

    public void onNoAmmo() {
        showTemporaryMessage("NO AMMO!", 2f, ColorRGBA.Red);
    }

    public void onEmptyWeapon() {
        showTemporaryMessage("*CLICK*", 1f, ColorRGBA.Orange);
    }

    /**
     * Show a temporary message that automatically disappears after duration
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

        // Add to HUD
        hudNode.attachChild(tempText);

        // Add to temporary messages list with timer
        TemporaryMessage tempMsg = new TemporaryMessage(tempText, duration);
        temporaryMessages.add(tempMsg);

        System.out.println("Added temporary message: '" + message + "' for " + duration + " seconds");
    }

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
        if (hudNode.getParent() != null) {
            guiNode.attachChild(hudNode);
        }
    }

    public void cleanup() {
        // Clean up temporary messages
        for (TemporaryMessage tempMsg : temporaryMessages) {
            if (tempMsg.textNode.getParent() != null) {
                hudNode.detachChild(tempMsg.textNode);
            }
        }
        temporaryMessages.clear();

        if (hudNode.getParent() != null) {
            guiNode.detachChild(hudNode);
        }
        hudNode.detachAllChildren();
    }

    public boolean isDebugInfoVisible() {
        return showDebugInfo;
    }
}