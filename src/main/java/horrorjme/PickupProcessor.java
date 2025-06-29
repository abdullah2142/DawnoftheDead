package horrorjme;

/**
 * PickupProcessor - Handles player interaction with pickups during gameplay
 * Checks for nearby pickups and processes automatic pickup
 */
public class PickupProcessor {

    private EntityManager entityManager;
    private Player player;
    private HUDManager hudManager;

    public PickupProcessor(EntityManager entityManager, Player player, HUDManager hudManager) {
        this.entityManager = entityManager;
        this.player = player;
        this.hudManager = hudManager;
    }

    /**
     * Update method - call every frame to check for pickups
     */
    public void update(float tpf) {
        if (player == null || entityManager == null) return;

        // Check all pickup entities for collision with player
        for (Entity entity : entityManager.getEntitiesByType(Entity.EntityType.PICKUP)) {
            if (entity instanceof PickupEntity) {
                PickupEntity pickup = (PickupEntity) entity;

                if (!pickup.isPickedUp() && pickup.isPlayerInRange(player)) {
                    // Attempt automatic pickup
                    boolean success = pickup.attemptPickup(player);

                    if (success && hudManager != null) {
                        // Show HUD notification
                        if (pickup instanceof WeaponPickup) {
                            WeaponPickup weaponPickup = (WeaponPickup) pickup;
                            hudManager.onWeaponPickup(weaponPickup.getWeaponType().displayName,
                                    weaponPickup.getStartingAmmo());
                        } else if (pickup instanceof AmmoPickup) {
                            AmmoPickup ammoPickup = (AmmoPickup) pickup;
                            hudManager.onAmmoPickup(ammoPickup.getAmmoType().displayName,
                                    ammoPickup.getAmmoAmount());
                        }
                    }
                }
            }
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