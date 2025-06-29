package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

/**
 * AmmoPickup - 3D rotating ammo boxes with type-specific visuals
 */
public class AmmoPickup extends PickupEntity {

    private AmmoType ammoType;
    private int ammoAmount;

    public AmmoPickup(Vector3f position, AmmoType ammoType, AssetManager assetManager, AudioManager audioManager) {
        super(EntityType.PICKUP, position, assetManager, audioManager);
        this.ammoType = ammoType;
        this.ammoAmount = ammoType.getRandomPickupAmount(); // Random as requested

        // Set ammo-specific properties
        configureAmmoPickup();

        // Faster bob for smaller ammo pickups
        setBobParameters(2.5f, 0.25f);
        setPickupRadius(1.0f); // Slightly smaller pickup radius
    }

    /**
     * Configure ammo-specific properties based on type
     */
    private void configureAmmoPickup() {
        // Set standard box size and rotation for all ammo types
        setBoxSize(0.5f, 0.4f, 0.5f); // Same shape for all ammo
        setRotationSpeed(-1.8f); // Negative for clockwise rotation
        setRotationAxis(0, 1, 0); // Same Y-axis rotation

        // Only change the glow color based on ammo type
        switch (ammoType) {
            case PISTOL_9MM:
                this.glowColor = new ColorRGBA(1.0f, 1.0f, 0.0f, 1.0f); // Yellow
                break;

            case SMG_9MM:
                this.glowColor = new ColorRGBA(0.0f, 1.0f, 0.0f, 1.0f); // Green
                break;

            case RIFLE_556:
                this.glowColor = new ColorRGBA(1.0f, 0.0f, 0.0f, 1.0f); // Red
                break;

            case SHOTGUN_12G:
                this.glowColor = new ColorRGBA(1.0f, 0.5f, 0.0f, 1.0f); // Orange
                break;

            default:
                this.glowColor = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f); // White
                break;
        }

        this.glowIntensity = 8f;
        this.glowRadius = 4f;
    }

    @Override
    protected void create3DVisualModel() {
        if (!(model instanceof Node)) return;
        Node modelNode = (Node) model;

        try {
            // Try to load ammo texture for 3D box
            String texturePath = "Textures/Pickups/ammo_" + ammoType.name().toLowerCase() + ".png";

            Geometry ammoBox = create3DTexturedBox(
                    "ammo_3d_visual",
                    texturePath,
                    boxSize,
                    glowColor // Use glow color as fallback
            );

            modelNode.attachChild(ammoBox);

            System.out.println("Created 3D ammo pickup: " + ammoType.displayName +
                    " (Size: " + boxSize + ", Color: " + glowColor +
                    ", Textured: " + !usingFallbackVisual + ")");

        } catch (Exception e) {
            // Create fallback 3D box matching glow color
            System.out.println("Creating fallback 3D box for ammo: " + ammoType.displayName);
            Geometry fallbackBox = createFallback3DBox(
                    "ammo_3d_box",
                    boxSize,
                    glowColor
            );

            modelNode.attachChild(fallbackBox);
        }
    }

    @Override
    protected String getPickupSound() {
        return "ammo_pickup"; // Will need to be added to AudioManager
    }

    @Override
    protected boolean handlePickup(Player player) {
        if (player == null || ammoType == null) return false;

        // Add ammo to player's inventory
        player.addAmmo(ammoType, ammoAmount);

        System.out.println("Player picked up: " + ammoAmount + " " + ammoType.displayName);
        return true;
    }

    // Getters
    public AmmoType getAmmoType() { return ammoType; }
    public int getAmmoAmount() { return ammoAmount; }
}