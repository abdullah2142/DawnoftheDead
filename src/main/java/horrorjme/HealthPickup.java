package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

/**
 * HealthPickup - Pink healing pickups that restore player health
 */
public class HealthPickup extends PickupEntity {

    private float healAmount;

    public HealthPickup(Vector3f position, AssetManager assetManager, AudioManager audioManager) {
        this(position, 25f, assetManager, audioManager); // Default 25 HP heal
    }

    public HealthPickup(Vector3f position, float healAmount, AssetManager assetManager, AudioManager audioManager) {
        super(EntityType.PICKUP, position, assetManager, audioManager);
        this.healAmount = healAmount;

        // Set pink glow color as requested
        this.glowColor = new ColorRGBA(1.0f, 0.4f, 0.8f, 1.0f); // Bright pink
        this.glowIntensity = 10f;
        this.glowRadius = 5f;

        // Health pack specific settings
        setBoxSize(0.6f, 0.3f, 0.4f); // Medical kit shape - wider and flatter
        setRotationSpeed(2.0f); // Positive for counter-clockwise rotation
        setRotationAxis(0, 1, 0); // Rotate around Y-axis

        // Medium bob speed for health packs
        setBobParameters(2.0f, 0.35f);
        setPickupRadius(1.1f);
    }

    @Override
    protected void create3DVisualModel() {
        if (!(model instanceof Node)) return;
        Node modelNode = (Node) model;

        try {
            // Try to load health pack texture
            String texturePath = "Textures/Pickups/health_pack.png";

            Geometry healthBox = create3DTexturedBox(
                    "health_3d_visual",
                    texturePath,
                    boxSize,
                    glowColor // Pink fallback color
            );

            modelNode.attachChild(healthBox);

            System.out.println("Created 3D health pickup: +" + healAmount + " HP" +
                    " (Size: " + boxSize + ", Pink glow, Textured: " + !usingFallbackVisual + ")");

        } catch (Exception e) {
            // Create fallback pink 3D box
            System.out.println("Creating fallback pink 3D box for health pickup");
            Geometry fallbackBox = createFallback3DBox(
                    "health_3d_box",
                    boxSize,
                    glowColor // Pink color
            );

            modelNode.attachChild(fallbackBox);
        }
    }

    @Override
    protected String getPickupSound() {
        return "health_pickup"; // Will need to be added to AudioManager
    }

    @Override
    protected boolean handlePickup(Player player) {
        if (player == null) return false;

        // Check if player needs healing
        if (player.getHealth() >= player.getMaxHealth()) {
            System.out.println("Player health already full - cannot pickup health pack");
            return false; // Don't allow pickup if health is full
        }

        // Heal the player
        float oldHealth = player.getHealth();
        player.heal(healAmount);
        float newHealth = player.getHealth();
        float actualHealing = newHealth - oldHealth;

        System.out.println("Player healed: " + oldHealth + " -> " + newHealth + " (+" + actualHealing + " HP)");
        return true;
    }

    // Getters
    public float getHealAmount() { return healAmount; }

    /**
     * Factory method for different health pack sizes
     */
    public static HealthPickup createSmallHealthPack(Vector3f position, AssetManager assetManager, AudioManager audioManager) {
        return new HealthPickup(position, 15f, assetManager, audioManager);
    }

    public static HealthPickup createMediumHealthPack(Vector3f position, AssetManager assetManager, AudioManager audioManager) {
        return new HealthPickup(position, 25f, assetManager, audioManager);
    }

    public static HealthPickup createLargeHealthPack(Vector3f position, AssetManager assetManager, AudioManager audioManager) {
        return new HealthPickup(position, 50f, assetManager, audioManager);
    }
}