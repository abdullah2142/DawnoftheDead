package horrorjme;

import com.jme3.math.ColorRGBA;

/**
 * Updated PickupProcessor - Handles point-based pickup economy
 * Checks for nearby pickups and processes pickup with point costs
 */
public class PickupProcessor {

    private EntityManager entityManager;
    private Player player;
    private HUDManager hudManager;
    private ScoreSystem scoreSystem;

    public PickupProcessor(EntityManager entityManager, Player player, HUDManager hudManager) {
        this.entityManager = entityManager;
        this.player = player;
        this.hudManager = hudManager;
    }

    /**
     * Set score system for point-based economy
     */
    public void setScoreSystem(ScoreSystem scoreSystem) {
        this.scoreSystem = scoreSystem;
        System.out.println("PickupProcessor: Score system connected for point-based pickups");
    }

    /**
     * Update method - checks points before allowing pickup
     */
    public void update(float tpf) {
        if (player == null || entityManager == null) return;

        // Check all pickup entities for collision with player
        for (Entity entity : entityManager.getEntitiesByType(Entity.EntityType.PICKUP)) {
            if (entity instanceof PickupEntity) {
                PickupEntity pickup = (PickupEntity) entity;

                if (!pickup.isPickedUp() && pickup.isPlayerInRange(player)) {
                    // Check if player can afford this pickup
                    if (canAffordPickup(pickup)) {
                        // Attempt pickup
                        boolean success = pickup.attemptPickup(player);

                        if (success) {
                            // Deduct points after successful pickup
                            deductPointsForPickup(pickup);

                            // Show HUD notification
                            if (hudManager != null) {
                                showPickupNotification(pickup);
                            }
                        }
                    } else {
                        // Show "not enough points" message
                        if (hudManager != null) {
                            showInsufficientPointsMessage(pickup);
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if player can afford the pickup
     */
    private boolean canAffordPickup(PickupEntity pickup) {
        if (scoreSystem == null) {
            return true; // If no score system, allow all pickups (fallback)
        }

        if (pickup instanceof WeaponPickup) {
            return scoreSystem.canAffordWeapon();
        } else if (pickup instanceof AmmoPickup) {
            return scoreSystem.canAffordAmmo();
        } else if (pickup instanceof HealthPickup) {
            return scoreSystem.canAffordHealth();
        }

        return true; // Unknown pickup type - allow it
    }

    /**
     * Deduct points after successful pickup
     */
    private void deductPointsForPickup(PickupEntity pickup) {
        if (scoreSystem == null) return;

        if (pickup instanceof WeaponPickup) {
            scoreSystem.purchaseWeapon();
        } else if (pickup instanceof AmmoPickup) {
            scoreSystem.purchaseAmmo();
        } else if (pickup instanceof HealthPickup) {
            scoreSystem.purchaseHealth();
        }
    }

    /**
     * Show pickup notification with point cost
     */
    private void showPickupNotification(PickupEntity pickup) {
        if (pickup instanceof WeaponPickup) {
            WeaponPickup weaponPickup = (WeaponPickup) pickup;
            hudManager.onWeaponPickup(weaponPickup.getWeaponType().displayName,
                    weaponPickup.getStartingAmmo());
            hudManager.showTemporaryMessage("Purchased: " + weaponPickup.getWeaponType().displayName +
                    " (-" + ScoreSystem.getWeaponCost() + " points)", 2f, new ColorRGBA(0f, 1f, 0f, 1f));
        } else if (pickup instanceof AmmoPickup) {
            AmmoPickup ammoPickup = (AmmoPickup) pickup;
            hudManager.onAmmoPickup(ammoPickup.getAmmoType().displayName,
                    ammoPickup.getAmmoAmount());
            hudManager.showTemporaryMessage("Purchased: " + ammoPickup.getAmmoType().displayName +
                    " (-" + ScoreSystem.getAmmoCost() + " points)", 1.5f, new ColorRGBA(1f, 1f, 0f, 1f));
        } else if (pickup instanceof HealthPickup) {
            HealthPickup healthPickup = (HealthPickup) pickup;
            hudManager.showTemporaryMessage("Purchased: Health Pack +" + (int)healthPickup.getHealAmount() + " HP" +
                    " (-" + ScoreSystem.getHealthCost() + " points)", 2f, new ColorRGBA(1f, 0.4f, 0.8f, 1f));
        }
    }

    /**
     * Show insufficient points message
     */
    private void showInsufficientPointsMessage(PickupEntity pickup) {
        int pointsNeeded = 0;
        String itemName = "Item";

        if (pickup instanceof WeaponPickup) {
            pointsNeeded = ScoreSystem.getWeaponCost();
            itemName = "Weapon";
        } else if (pickup instanceof AmmoPickup) {
            pointsNeeded = ScoreSystem.getAmmoCost();
            itemName = "Ammo";
        } else if (pickup instanceof HealthPickup) {
            pointsNeeded = ScoreSystem.getHealthCost();
            itemName = "Health Pack";
        }

        // Only show message occasionally to avoid spam
        if (System.currentTimeMillis() % 2000 < 100) { // Show every 2 seconds for 0.1 seconds
            hudManager.showTemporaryMessage("Need " + pointsNeeded + " points for " + itemName +
                    " (Have: " + scoreSystem.getCurrentPoints() + ")", 1f, new ColorRGBA(1f, 0f, 0f, 1f));
        }
    }

    /**
     * Set new player reference
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Set new HUD manager reference
     */
    public void setHudManager(HUDManager hudManager) {
        this.hudManager = hudManager;
    }
}