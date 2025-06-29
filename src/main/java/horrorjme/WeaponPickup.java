package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

/**
 * WeaponPickup - 3D rotating weapon boxes with enhanced visuals
 */
public class WeaponPickup extends PickupEntity {

    private WeaponType weaponType;
    private int startingAmmo;

    public WeaponPickup(Vector3f position, WeaponType weaponType, AssetManager assetManager, AudioManager audioManager) {
        super(EntityType.PICKUP, position, assetManager, audioManager);
        this.weaponType = weaponType;
        this.startingAmmo = weaponType.getRandomStartingAmmo(); // Random as requested

        // Set weapon-specific glow color (white/blue as requested)
        this.glowColor = new ColorRGBA(0.7f, 0.9f, 1.0f, 1.0f); // Light blue-white
        this.glowIntensity = 12f; // Brighter for weapons
        this.glowRadius = 6f;

        // Weapon-specific settings
        configureWeaponPickup();

        // Slower, more dramatic bob for weapons
        setBobParameters(1.5f, 0.4f);
    }

    /**
     * Configure weapon-specific properties
     */
    private void configureWeaponPickup() {
        switch (weaponType) {
            case STEN_GUN:
                // SMG is longer and thinner
                setBoxSize(1.2f, 0.4f, 0.3f);
                setRotationSpeed(-1.0f); // Negative for clockwise rotation
                setRotationAxis(0, 1, 0); // Pure Y-axis rotation
                break;

            // Future weapons can have different configurations
            default:
                // Default weapon box size
                setBoxSize(1.0f, 0.5f, 0.3f);
                setRotationSpeed(-1.2f); // Negative for clockwise rotation
                setRotationAxis(0, 1, 0); // Pure Y-axis rotation
                break;
        }
    }

    @Override
    protected void create3DVisualModel() {
        if (!(model instanceof Node)) return;
        Node modelNode = (Node) model;

        try {
            // Try to load weapon texture for 3D box
            String texturePath = "Textures/Pickups/weapon_" + weaponType.name().toLowerCase() + ".png";

            Geometry weaponBox = create3DTexturedBox(
                    "weapon_3d_visual",
                    texturePath,
                    boxSize,
                    new ColorRGBA(0.8f, 0.8f, 1.0f, 1.0f) // Fallback blue-white
            );

            modelNode.attachChild(weaponBox);

            System.out.println("Created 3D weapon pickup: " + weaponType.displayName +
                    " (Size: " + boxSize + ", Textured: " + !usingFallbackVisual + ")");

        } catch (Exception e) {
            // Create fallback 3D box
            System.out.println("Creating fallback 3D box for weapon: " + weaponType.displayName);
            Geometry fallbackBox = createFallback3DBox(
                    "weapon_3d_box",
                    boxSize,
                    new ColorRGBA(0.9f, 0.9f, 1.0f, 1.0f) // White with slight blue tint
            );

            modelNode.attachChild(fallbackBox);
        }
    }

    @Override
    protected String getPickupSound() {
        return "weapon_pickup"; // Will need to be added to AudioManager
    }

    @Override
    protected boolean handlePickup(Player player) {
        if (player == null || weaponType == null) return false;

        // Add weapon to player's inventory
        boolean weaponAdded = player.addWeapon(weaponType, startingAmmo);

        if (weaponAdded) {
            System.out.println("Player picked up: " + weaponType.displayName +
                    " with " + startingAmmo + " rounds");
        } else {
            // FIXED: If player already has this weapon, give them ammo instead
            player.addAmmo(weaponType.ammoType, startingAmmo);
            System.out.println("Player already has " + weaponType.displayName +
                    " - gained " + startingAmmo + " " + weaponType.ammoType.displayName + " instead");
        }

        // FIXED: Always return true so pickup disappears
        return true;
    }
    // Getters
    public WeaponType getWeaponType() { return weaponType; }
    public int getStartingAmmo() { return startingAmmo; }
}