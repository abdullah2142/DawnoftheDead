package horrorjme;

import java.util.EnumMap;
import java.util.Map;

/**
 * AmmoInventory - Manages all ammunition for the player
 * Self-contained and handles all ammo-related logic
 */
public class AmmoInventory {

    // Reserve ammunition (stored magazines/rounds)
    private Map<AmmoType, Integer> reserveAmmo;

    // Currently loaded ammunition per weapon
    private Map<WeaponType, Integer> loadedAmmo;

    public AmmoInventory() {
        reserveAmmo = new EnumMap<>(AmmoType.class);
        loadedAmmo = new EnumMap<>(WeaponType.class);

        // Initialize with zero ammo (player starts with nothing)
        for (AmmoType ammoType : AmmoType.values()) {
            reserveAmmo.put(ammoType, 0);
        }

        for (WeaponType weaponType : WeaponType.values()) {
            loadedAmmo.put(weaponType, 0);
        }
    }

    /**
     * Add ammo to reserve (from pickups or drops)
     */
    public void addReserveAmmo(AmmoType ammoType, int amount) {
        if (amount <= 0) return;

        int current = reserveAmmo.get(ammoType);
        reserveAmmo.put(ammoType, current + amount);
    }

    /**
     * Set loaded ammo for a weapon (when weapon is picked up)
     */
    public void setLoadedAmmo(WeaponType weaponType, int amount) {
        int clampedAmount = Math.max(0, Math.min(amount, weaponType.magazineSize));
        loadedAmmo.put(weaponType, clampedAmount);
    }

    /**
     * Get currently loaded ammo for weapon
     */
    public int getLoadedAmmo(WeaponType weaponType) {
        return loadedAmmo.get(weaponType);
    }

    /**
     * Get reserve ammo for ammo type
     */
    public int getReserveAmmo(AmmoType ammoType) {
        return reserveAmmo.get(ammoType);
    }

    /**
     * Check if weapon can fire (has loaded ammo)
     */
    public boolean canFire(WeaponType weaponType) {
        return getLoadedAmmo(weaponType) > 0;
    }

    /**
     * Consume one round of loaded ammo (when firing)
     */
    public boolean consumeLoadedAmmo(WeaponType weaponType) {
        int current = getLoadedAmmo(weaponType);
        if (current > 0) {
            loadedAmmo.put(weaponType, current - 1);
            return true;
        }
        return false;
    }

    /**
     * Check if weapon can be reloaded
     */
    public boolean canReload(WeaponType weaponType) {
        // Can reload if: weapon isn't full AND we have reserve ammo
        int currentLoaded = getLoadedAmmo(weaponType);
        int reserveAvailable = getReserveAmmo(weaponType.ammoType);

        return currentLoaded < weaponType.magazineSize && reserveAvailable > 0;
    }

    /**
     * Reload weapon from reserve ammo
     * Returns amount actually reloaded
     */
    public int reload(WeaponType weaponType) {
        if (!canReload(weaponType)) {
            return 0;
        }

        int currentLoaded = getLoadedAmmo(weaponType);
        int reserveAvailable = getReserveAmmo(weaponType.ammoType);
        int spaceInMagazine = weaponType.magazineSize - currentLoaded;

        // Take minimum of what we need and what we have
        int ammoToLoad = Math.min(spaceInMagazine, reserveAvailable);

        // Update loaded and reserve
        loadedAmmo.put(weaponType, currentLoaded + ammoToLoad);
        reserveAmmo.put(weaponType.ammoType, reserveAvailable - ammoToLoad);

        return ammoToLoad;
    }

    /**
     * Get total ammo for a weapon type (loaded + reserve)
     */
    public int getTotalAmmo(WeaponType weaponType) {
        return getLoadedAmmo(weaponType) + getReserveAmmo(weaponType.ammoType);
    }

    /**
     * Check if player has any ammo at all
     */
    public boolean hasAnyAmmo() {
        // Check loaded ammo
        for (int loaded : loadedAmmo.values()) {
            if (loaded > 0) return true;
        }

        // Check reserve ammo
        for (int reserve : reserveAmmo.values()) {
            if (reserve > 0) return true;
        }

        return false;
    }

    /**
     * Get ammo status string for HUD display
     */
    public String getAmmoStatusForWeapon(WeaponType weaponType) {
        if (weaponType == null) {
            return "NO WEAPON";
        }

        int loaded = getLoadedAmmo(weaponType);
        int reserve = getReserveAmmo(weaponType.ammoType);

        return loaded + "/" + reserve;
    }

    /**
     * Debug method - get full inventory status
     */
    public String getFullInventoryStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== AMMO INVENTORY ===\n");

        for (WeaponType weapon : WeaponType.values()) {
            int loaded = getLoadedAmmo(weapon);
            int reserve = getReserveAmmo(weapon.ammoType);
            status.append(weapon.displayName).append(": ").append(loaded)
                    .append("/").append(reserve).append("\n");
        }

        return status.toString();
    }
}