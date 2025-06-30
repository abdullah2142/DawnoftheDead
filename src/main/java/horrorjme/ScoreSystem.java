package horrorjme;

/**
 * Simple score and point management system
 * Tracks kills and manages point-based economy for pickups
 */
public class ScoreSystem {

    private int totalKills = 0;
    private int currentPoints = 0;
    private int roundKills = 0;

    // Point values
    private static final int POINTS_PER_KILL = 10;
    private static final int STARTING_POINTS = 100; // Changed from 50 to 100

    // Pickup costs
    private static final int WEAPON_COST = 30;
    private static final int AMMO_COST = 15;
    private static final int HEALTH_COST = 20;

    public ScoreSystem() {
        currentPoints = STARTING_POINTS;
        System.out.println("ScoreSystem: Initialized with " + STARTING_POINTS + " starting points");
        System.out.println("ScoreSystem: Costs - Weapon: " + WEAPON_COST + ", Ammo: " + AMMO_COST + ", Health: " + HEALTH_COST);
    }

    /**
     * Player killed an enemy
     */
    public void addKill() {
        totalKills++;
        roundKills++;
        currentPoints += POINTS_PER_KILL;

        System.out.println("ScoreSystem: Kill! Total: " + totalKills + ", Round: " + roundKills + ", Points: " + currentPoints);
    }

    /**
     * Try to purchase a weapon pickup
     */
    public boolean canAffordWeapon() {
        return currentPoints >= WEAPON_COST;
    }

    public boolean purchaseWeapon() {
        if (canAffordWeapon()) {
            currentPoints -= WEAPON_COST;
            System.out.println("ScoreSystem: Purchased weapon for " + WEAPON_COST + " points. Remaining: " + currentPoints);
            return true;
        }
        System.out.println("ScoreSystem: Cannot afford weapon! Need " + WEAPON_COST + " points, have " + currentPoints);
        return false;
    }

    /**
     * Try to purchase an ammo pickup
     */
    public boolean canAffordAmmo() {
        return currentPoints >= AMMO_COST;
    }

    public boolean purchaseAmmo() {
        if (canAffordAmmo()) {
            currentPoints -= AMMO_COST;
            System.out.println("ScoreSystem: Purchased ammo for " + AMMO_COST + " points. Remaining: " + currentPoints);
            return true;
        }
        System.out.println("ScoreSystem: Cannot afford ammo! Need " + AMMO_COST + " points, have " + currentPoints);
        return false;
    }

    /**
     * Try to purchase a health pickup
     */
    public boolean canAffordHealth() {
        return currentPoints >= HEALTH_COST;
    }

    public boolean purchaseHealth() {
        if (canAffordHealth()) {
            currentPoints -= HEALTH_COST;
            System.out.println("ScoreSystem: Purchased health for " + HEALTH_COST + " points. Remaining: " + currentPoints);
            return true;
        }
        System.out.println("ScoreSystem: Cannot afford health! Need " + HEALTH_COST + " points, have " + currentPoints);
        return false;
    }

    /**
     * Start new round
     */
    public void startNewRound() {
        roundKills = 0;
        System.out.println("ScoreSystem: New round started. Points: " + currentPoints);
    }

    /**
     * Reset to initial state
     */
    public void reset() {
        totalKills = 0;
        currentPoints = STARTING_POINTS;
        roundKills = 0;
        System.out.println("ScoreSystem: Reset to initial state");
    }

    /**
     * Award bonus points (for completing rounds, etc.)
     */
    public void awardBonusPoints(int points, String reason) {
        currentPoints += points;
        System.out.println("ScoreSystem: Bonus! +" + points + " points for " + reason + ". Total: " + currentPoints);
    }

    // Getters
    public int getTotalKills() { return totalKills; }
    public int getCurrentPoints() { return currentPoints; }
    public int getRoundKills() { return roundKills; }

    // Static getters for costs (for HUD display)
    public static int getWeaponCost() { return WEAPON_COST; }
    public static int getAmmoCost() { return AMMO_COST; }
    public static int getHealthCost() { return HEALTH_COST; }
    public static int getPointsPerKill() { return POINTS_PER_KILL; }

    /**
     * Get total score (for high score tracking)
     */
    public int getTotalScore() {
        return totalKills * POINTS_PER_KILL;
    }

    /**
     * Check if player can afford any pickup
     */
    public boolean canAffordAnyPickup() {
        return currentPoints >= Math.min(Math.min(WEAPON_COST, AMMO_COST), HEALTH_COST);
    }
}