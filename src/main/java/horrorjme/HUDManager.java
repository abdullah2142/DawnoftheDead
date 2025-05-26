package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

public class HUDManager {
    private AssetManager assetManager;
    private Node guiNode;
    private AppSettings settings;

    private BitmapText healthText;
    private BitmapText torchText;
    private BitmapFont defaultFont;
    private Node hudNode;

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

        guiNode.attachChild(hudNode);
    }

    public void updateHUD(Player player) {
        if (player != null) {
            healthText.setText("Health: " + (int)player.getHealth() + "/" + (int)player.getMaxHealth());
            torchText.setText("Torch: " + (player.isTorchOn() ? "ON" : "OFF"));

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
}