package horrorjme;

import java.util.EnumSet;
import java.util.Set;

/**
 * WeaponInventory - Manages owned weapons and switching
 * Self-contained weapon management system
 */
public class WeaponInventory {

    private Set<WeaponType> ownedWeapons;
    private WeaponType currentWeapon;
    private WeaponType previousWeapon;

    public WeaponInventory() {
        ownedWeapons = EnumSet.noneOf(WeaponType.class);
        currentWeapon = null;
        previousWeapon = null;
    }

    /**
     * Add weapon to inventory (when picked up)
     */
    public boolean addWeapon(WeaponType weaponType) {
        if (weaponType == null) return false;

        boolean wasNew = !ownedWeapons.contains(weaponType);
        ownedWeapons.add(weaponType);

        // Auto-equip if no weapon currently equipped
        if (currentWeapon == null) {
            switchToWeapon(weaponType);
        }

        return wasNew;
    }

    /**
     * Switch to specific weapon
     */
    public boolean switchToWeapon(WeaponType weaponType) {
        if (weaponType == null || !ownedWeapons.contains(weaponType)) {
            return false;
        }

        if (currentWeapon != weaponType) {
            previousWeapon = currentWeapon;
            currentWeapon = weaponType;
            return true; // Weapon switched
        }

        return false; // Already equipped
    }

    /**
     * Switch to next weapon (mouse wheel or tab)
     */
    public WeaponType switchToNextWeapon() {
        if (ownedWeapons.isEmpty()) return null;

        WeaponType[] allWeapons = WeaponType.values();
        int currentIndex = -1;

        // Find current weapon index
        if (currentWeapon != null) {
            for (int i = 0; i < allWeapons.length; i++) {
                if (allWeapons[i] == currentWeapon) {
                    currentIndex = i;
                    break;
                }
            }
        }

        // Find next owned weapon
        for (int i = 1; i <= allWeapons.length; i++) {
            int nextIndex = (currentIndex + i) % allWeapons.length;
            WeaponType nextWeapon = allWeapons[nextIndex];

            if (ownedWeapons.contains(nextWeapon)) {
                switchToWeapon(nextWeapon);
                return nextWeapon;
            }
        }

        return currentWeapon;
    }

    /**
     * Switch to previous weapon (mouse wheel)
     */
    public WeaponType switchToPreviousWeapon() {
        if (ownedWeapons.isEmpty()) return null;

        WeaponType[] allWeapons = WeaponType.values();
        int currentIndex = allWeapons.length; // Start beyond array if no current weapon

        // Find current weapon index
        if (currentWeapon != null) {
            for (int i = 0; i < allWeapons.length; i++) {
                if (allWeapons[i] == currentWeapon) {
                    currentIndex = i;
                    break;
                }
            }
        }

        // Find previous owned weapon
        for (int i = 1; i <= allWeapons.length; i++) {
            int prevIndex = (currentIndex - i + allWeapons.length) % allWeapons.length;
            WeaponType prevWeapon = allWeapons[prevIndex];

            if (ownedWeapons.contains(prevWeapon)) {
                switchToWeapon(prevWeapon);
                return prevWeapon;
            }
        }

        return currentWeapon;
    }

    /**
     * Switch by number key (1, 2, 3)
     */
    public WeaponType switchByKey(int keyNumber) {
        for (WeaponType weapon : WeaponType.values()) {
            if (weapon.switchKey == keyNumber && ownedWeapons.contains(weapon)) {
                switchToWeapon(weapon);
                return weapon;
            }
        }
        return currentWeapon; // No change if key doesn't match owned weapon
    }

    /**
     * Quick switch to last weapon (like Counter-Strike Q key)
     */
    public WeaponType quickSwitch() {
        if (previousWeapon != null && ownedWeapons.contains(previousWeapon)) {
            switchToWeapon(previousWeapon);
            return currentWeapon;
        }
        return currentWeapon;
    }

    // Getters
    public WeaponType getCurrentWeapon() {
        return currentWeapon;
    }

    public WeaponType getPreviousWeapon() {
        return previousWeapon;
    }

    public boolean hasWeapon(WeaponType weaponType) {
        return ownedWeapons.contains(weaponType);
    }

    public boolean hasAnyWeapon() {
        return !ownedWeapons.isEmpty();
    }

    public Set<WeaponType> getOwnedWeapons() {
        return EnumSet.copyOf(ownedWeapons);
    }

    public int getOwnedWeaponCount() {
        return ownedWeapons.size();
    }

    /**
     * Get weapon status for HUD
     */
    public String getCurrentWeaponName() {
        return currentWeapon != null ? currentWeapon.displayName : "UNARMED";
    }
}