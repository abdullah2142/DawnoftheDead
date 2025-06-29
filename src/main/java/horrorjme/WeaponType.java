package horrorjme;

import com.jme3.math.FastMath;

/**
 * WeaponType - Defines all weapons and their properties
 * FINAL VERSION - Replace your WeaponType.java with this
 */
public enum WeaponType {
    // Existing weapons
    STEN_GUN("Sten Gun", AmmoType.SMG_9MM, "Textures/Weapons/sten_frame_", 19, 30, 1),

    // NEW: Revolver - your sprWeaponRevolver_XX.png files
    REVOLVER("Revolver", AmmoType.PISTOL_9MM, "Textures/Weapons/sprWeaponRevolver_", 32, 6, 2);

    public final String displayName;
    public final AmmoType ammoType;
    public final String frameBasePath;
    public final int frameCount;     // 32 frames for revolver
    public final int magazineSize;   // 6 bullets (revolver cylinder)
    public final int switchKey;      // Press "2" key to select

    WeaponType(String displayName, AmmoType ammoType, String frameBasePath,
               int frameCount, int magazineSize, int switchKey) {
        this.displayName = displayName;
        this.ammoType = ammoType;
        this.frameBasePath = frameBasePath;
        this.frameCount = frameCount;
        this.magazineSize = magazineSize;
        this.switchKey = switchKey;
    }

    /**
     * Random starting ammo when weapon is picked up
     */
    public int getRandomStartingAmmo() {
        if (FastMath.nextRandomFloat() < 0.4f) {
            return 0; // 40% chance weapon is empty when found
        } else {
            // 60% chance weapon has some ammo (10% to 100% of magazine)
            int minAmmo = Math.max(1, magazineSize / 10);
            int range = magazineSize - minAmmo + 1;
            return minAmmo + (int)(FastMath.nextRandomFloat() * range);
        }
    }
}