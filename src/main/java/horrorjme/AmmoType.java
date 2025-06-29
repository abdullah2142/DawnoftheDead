package horrorjme;

import com.jme3.math.FastMath;

/**
 * AmmoType - Defines all ammunition types in the game
 */
public enum AmmoType {
    PISTOL_9MM("9mm Rounds", 15, "pistol rounds", 0.6f),
    SMG_9MM("9mm SMG Mag", 30, "SMG magazine", 0.4f),
    RIFLE_556("5.56mm Rounds", 20, "rifle magazine", 0.3f),
    SHOTGUN_12G("12 Gauge", 8, "shotgun shells", 0.5f);

    public final String displayName;
    public final int standardMagSize;
    public final String pickupDescription;
    public final float worldSpawnChance; // How often this ammo spawns in world

    AmmoType(String displayName, int standardMagSize, String pickupDescription, float worldSpawnChance) {
        this.displayName = displayName;
        this.standardMagSize = standardMagSize;
        this.pickupDescription = pickupDescription;
        this.worldSpawnChance = worldSpawnChance;
    }

    /**
     * Get random ammo amount for pickup (partial to full magazine)
     */
    public int getRandomPickupAmount() {
        // Random between 30% and 100% of standard magazine size
        int minAmount = Math.max(1, (int)(standardMagSize * 0.3f));
        int range = standardMagSize - minAmount + 1;
        return minAmount + (int)(FastMath.nextRandomFloat() * range);
    }

    /**
     * Get ammo drop chance from enemies (all random as requested)
     */
    public float getEnemyDropChance() {
        // Base 30% chance with ±10% randomization
        return 0.2f + (FastMath.nextRandomFloat() * 0.2f);
    }
}