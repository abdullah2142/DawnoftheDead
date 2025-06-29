package horrorjme;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;

import java.io.IOException;

/**
 * CORRECTED Player class - REPLACE YOUR EXISTING Player.java with this version
 * Major fixes: Proper weapon system initialization, game instance connection
 */
public class Player implements Control {
    // Player state
    private Vector3f position;
    private boolean torchOn = true;
    private SpotLight torch;
    private float health = 100f;
    private float maxHealth = 100f;
    private boolean isDead = false;
    private Camera camera;
    private Node rootNode;
    private AudioManager audioManager;

    // CRITICAL FIX: Reference to main game for weapon system initialization
    private HorrorGameJME gameInstance;

    // Physics reference for ground detection
    private CharacterControl characterControl;

    // Movement flags - for smooth FPS movement
    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean strafeLeft = false;
    private boolean strafeRight = false;

    // Reusable vectors for performance
    private Vector3f walkDirection = new Vector3f();
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    private Vector3f newPosition = new Vector3f();
    private Vector3f tempVector = new Vector3f();

    // Movement smoothing for FPS feel
    private float footstepTimer = 0f;
    private float footstepInterval = 0.5f;

    // NEW: Weapon and Ammo Inventory Systems
    private WeaponInventory weaponInventory;
    private AmmoInventory ammoInventory;

    // Weapon systems (FIXED: Now conditionally created)
    private ModernWeaponAnimator weaponAnimator;
    private WeaponEffectsManager weaponEffectsManager;
    private ModelBasedMuzzleFlash muzzleFlashSystem;

    // ENHANCED: Mouse tracking for weapon sway
    private float currentMouseDeltaX = 0f;
    private float currentMouseDeltaY = 0f;
    private float mouseDeltaDecay = 0.92f;

    // UPDATED: Weapon properties now managed by inventory
    private boolean isReloading = false;
    private float fireRate = 0.15f; // Time between shots
    private float lastFireTime = 0f;

    // CRITICAL FIX: Flag to track if weapon systems have been initialized
    private boolean weaponSystemsInitialized = false;

    public Player(Camera camera, Node rootNode, int[][] mapData, AudioManager audioManager) {
        this.camera = camera;
        this.rootNode = rootNode;
        this.audioManager = audioManager;
        this.position = new Vector3f(10f, 150f, 20f);

        camera.setLocation(position.clone());
        Vector3f initialLookAt = position.add(new Vector3f(0, 0, -1));
        camera.lookAt(initialLookAt, Vector3f.UNIT_Y);

        // NEW: Initialize inventory systems
        weaponInventory = new WeaponInventory();
        ammoInventory = new AmmoInventory();

        setupTorch();
    }

    public Player(Camera camera, Node rootNode, int[][] mapData) {
        this(camera, rootNode, mapData, null);
    }

    // CRITICAL FIX: Set game instance reference for weapon system initialization
    public void setGameInstance(HorrorGameJME gameInstance) {
        this.gameInstance = gameInstance;
        System.out.println("Player: Game instance reference set - " + (gameInstance != null ? "SUCCESS" : "FAILED"));
    }

    // NEW: Set muzzle flash system (conditionally)
    public void setMuzzleFlashSystem(ModelBasedMuzzleFlash muzzleFlashSystem) {
        this.muzzleFlashSystem = muzzleFlashSystem;
        System.out.println("Player: Muzzle flash system set - " + (muzzleFlashSystem != null ? "SUCCESS" : "NULL"));
    }

    // UPDATED: Set weapon effects manager (conditionally)
    public void setWeaponEffectsManager(WeaponEffectsManager weaponEffectsManager) {
        this.weaponEffectsManager = weaponEffectsManager;
        System.out.println("Player: Weapon effects manager set - " + (weaponEffectsManager != null ? "SUCCESS" : "NULL"));
    }

    public void setCharacterControl(CharacterControl characterControl) {
        this.characterControl = characterControl;
    }

    private void setupTorch() {
        torch = new SpotLight();
        Vector3f torchOffset = camera.getDirection().mult(0.3f);
        torch.setPosition(camera.getLocation().add(torchOffset));
        torch.setDirection(camera.getDirection());

        torch.setSpotRange(15f);
        torch.setSpotInnerAngle(8f * FastMath.DEG_TO_RAD);
        torch.setSpotOuterAngle(18f * FastMath.DEG_TO_RAD);
        torch.setColor(ColorRGBA.White.mult(8.0f));

        rootNode.addLight(torch);
    }

    /**
     * ENHANCED: Set mouse deltas for weapon sway (called from InputHandler)
     */
    public void setMouseDeltas(float deltaX, float deltaY) {
        this.currentMouseDeltaX = deltaX;
        this.currentMouseDeltaY = deltaY;
    }

    // Get linear velocity for weapon movement animation
    public Vector3f getLinearVelocity() {
        float speed = 0f;

        if (moveForward || moveBackward || strafeLeft || strafeRight) {
            speed = 8f; // Your walking speed
        }

        // Return velocity in current movement direction
        Vector3f direction = new Vector3f();
        if (moveForward) direction.addLocal(camera.getDirection());
        if (moveBackward) direction.addLocal(camera.getDirection().negate());
        if (strafeLeft) direction.addLocal(camera.getLeft());
        if (strafeRight) direction.addLocal(camera.getLeft().negate());

        if (direction.length() > 0) {
            direction.normalizeLocal().multLocal(speed);
        }

        return direction;
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        return null;
    }

    @Override
    public void setSpatial(Spatial spatial) {
    }

    public void update(float tpf) {
        // Update fire rate timer
        lastFireTime += tpf;

        updateTorchPosition();
        updateFootstepAudio(tpf);

        // CRITICAL FIX: Only update weapon systems if we have a weapon AND systems are initialized
        if (hasAnyWeapon() && weaponSystemsInitialized) {
            updateWeaponSystems(tpf);
        }
    }

    /**
     * NEW: Update weapon systems only when player has weapons
     */
    private void updateWeaponSystems(float tpf) {
        // Update weapon effects
        if (weaponEffectsManager != null) {
            weaponEffectsManager.update(tpf);
        }

        // Update 3D muzzle flash system
        if (muzzleFlashSystem != null) {
            muzzleFlashSystem.update(tpf);
        }

        if (weaponAnimator != null) {
            // Pass mouse deltas directly to weapon for sway
            weaponAnimator.setMouseSway(currentMouseDeltaX, currentMouseDeltaY);

            // Pass movement velocity to weapon for walking animation
            weaponAnimator.setMovementVelocity(getLinearVelocity());

            // Update weapon animator
            weaponAnimator.update(tpf);

            // Check if reloading animation finished
            if (isReloading && !weaponAnimator.isAnimating()) {
                finishReload();
            }

            // Apply decay to mouse deltas for smooth sway
            currentMouseDeltaX *= mouseDeltaDecay;
            currentMouseDeltaY *= mouseDeltaDecay;

            // Clamp very small values to zero to prevent tiny movements
            if (Math.abs(currentMouseDeltaX) < 0.001f) currentMouseDeltaX = 0f;
            if (Math.abs(currentMouseDeltaY) < 0.001f) currentMouseDeltaY = 0f;
        }
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }

    public void setWeaponAnimator(ModernWeaponAnimator weaponAnimator) {
        this.weaponAnimator = weaponAnimator;
        System.out.println("Player: Weapon animator set - " + (weaponAnimator != null ? "SUCCESS" : "NULL"));
    }

    /**
     * CRITICAL FIX: Add weapon to player inventory with proper system initialization
     */
    public boolean addWeapon(WeaponType weaponType, int startingAmmo) {
        if (weaponType == null) return false;

        System.out.println("Player: Adding weapon " + weaponType.displayName + " with " + startingAmmo + " ammo");

        // Add weapon to inventory
        boolean wasNew = weaponInventory.addWeapon(weaponType);

        // Set starting ammo for the weapon
        ammoInventory.setLoadedAmmo(weaponType, startingAmmo);

        if (wasNew) {
            System.out.println("Player: New weapon acquired - " + weaponType.displayName);

            // CRITICAL FIX: If this is player's first weapon, initialize weapon systems
            if (weaponInventory.getOwnedWeaponCount() == 1 && !weaponSystemsInitialized) {
                System.out.println("Player: First weapon detected - initializing weapon systems...");
                initializeWeaponSystems(weaponType);
            } else {
                // Update weapon animator for new weapon
                updateWeaponAnimatorForCurrentWeapon();
            }
        }

        return wasNew;
    }

    /**
     * NEW: Add ammo to player inventory
     */
    public void addAmmo(AmmoType ammoType, int amount) {
        if (ammoType != null && amount > 0) {
            ammoInventory.addReserveAmmo(ammoType, amount);
            System.out.println("Player gained: " + amount + " " + ammoType.displayName);
        }
    }

    /**
     * NEW: Switch to weapon by number key
     */
    public boolean switchWeapon(int keyNumber) {
        WeaponType newWeapon = weaponInventory.switchByKey(keyNumber);
        return updateWeaponAnimatorForCurrentWeapon();
    }

    /**
     * NEW: Switch to next weapon (mouse wheel up)
     */
    public boolean switchToNextWeapon() {
        WeaponType newWeapon = weaponInventory.switchToNextWeapon();
        return updateWeaponAnimatorForCurrentWeapon();
    }

    /**
     * NEW: Switch to previous weapon (mouse wheel down)
     */
    public boolean switchToPreviousWeapon() {
        WeaponType newWeapon = weaponInventory.switchToPreviousWeapon();
        return updateWeaponAnimatorForCurrentWeapon();
    }

    /**
     * NEW: Quick switch to last weapon
     */
    public boolean quickSwitchWeapon() {
        WeaponType newWeapon = weaponInventory.quickSwitch();
        return updateWeaponAnimatorForCurrentWeapon();
    }

    /**
     * CRITICAL FIX: Update weapon animator when weapon changes
     */
    private boolean updateWeaponAnimatorForCurrentWeapon() {
        WeaponType currentWeapon = weaponInventory.getCurrentWeapon();

        if (currentWeapon == null) {
            // No weapon - hide animator
            if (weaponAnimator != null) {
                weaponAnimator.setProceduralMotion(false);
            }
            return false;
        }

        // Update weapon animator for new weapon
        if (weaponAnimator != null) {
            System.out.println("Player: Loading frames for " + currentWeapon.displayName);
            System.out.println("Frame path: " + currentWeapon.frameBasePath);
            System.out.println("Frame count: " + currentWeapon.frameCount);

            // Pass weapon type for weapon-specific animations and file naming
            weaponAnimator.loadFrames(currentWeapon.frameBasePath, currentWeapon.frameCount, currentWeapon);
            weaponAnimator.setProceduralMotion(true);

            System.out.println("Player: Weapon animator updated for " + currentWeapon.displayName);
        } else {
            System.err.println("Player: Cannot update weapon animator - animator is null!");
        }

        // Update weapon effects manager with current weapon type
        if (weaponEffectsManager != null) {
            weaponEffectsManager.setCurrentHorrorWeapon(currentWeapon);
        }

        return true;
    }

    /**
     * CRITICAL FIX: Initialize weapon systems when player gets first weapon
     */
    private void initializeWeaponSystems(WeaponType firstWeapon) {
        if (gameInstance == null) {
            System.err.println("Player: CRITICAL ERROR - Cannot initialize weapon systems - no game instance reference!");
            System.err.println("Player: Make sure HorrorGameJME calls player.setGameInstance(this) in setupPlayerSystemsWithoutWeapons()");
            return;
        }

        if (weaponSystemsInitialized) {
            System.out.println("Player: Weapon systems already initialized");
            return;
        }

        System.out.println("Player: Calling game instance to initialize weapon systems for " + firstWeapon.displayName);

        try {
            gameInstance.initializePlayerWeaponSystems(firstWeapon);
            weaponSystemsInitialized = true;
            System.out.println("Player: Weapon systems initialization complete!");

            // Update weapon animator for current weapon
            updateWeaponAnimatorForCurrentWeapon();

        } catch (Exception e) {
            System.err.println("Player: CRITICAL ERROR - Failed to initialize weapon systems: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * UPDATED: Fire weapon with new inventory system
     */
    public void fireWeapon() {
        WeaponType currentWeapon = weaponInventory.getCurrentWeapon();

        // Check if we have a weapon
        if (currentWeapon == null) {
            System.out.println("Player: Cannot fire - no weapon equipped");
            return;
        }

        // Check if weapon systems are initialized
        if (!weaponSystemsInitialized) {
            System.err.println("Player: Cannot fire - weapon systems not initialized!");
            return;
        }

        // Check fire rate limit
        if (lastFireTime < fireRate) {
            return;
        }

        // Check if we have ammo
        if (!ammoInventory.canFire(currentWeapon)) {
            // Play empty click sound
            if (audioManager != null) {
                audioManager.playSoundEffect("gun_empty");
            }
            System.out.println("Player: Cannot fire - no ammo");
            return;
        }

        if (isReloading) {
            System.out.println("Player: Cannot fire - reloading");
            return; // Can't fire while reloading
        }

        // Reset fire timer
        lastFireTime = 0f;

        // Consume ammo
        ammoInventory.consumeLoadedAmmo(currentWeapon);

        System.out.println("Player: Firing " + currentWeapon.displayName + " - Ammo remaining: " + ammoInventory.getLoadedAmmo(currentWeapon));

        // 1. Trigger weapon animation
        if (weaponAnimator != null) {
            weaponAnimator.fire();
        }

        // 2. Play fire sound
        if (audioManager != null) {
            audioManager.playSoundEffect("gun_fire");
        }

        // 3. Trigger 3D muzzle flash
        if (muzzleFlashSystem != null) {
            muzzleFlashSystem.createMuzzleFlash();
        }

        // 4. Trigger shell casings and bullet tracers
        if (weaponEffectsManager != null) {
            weaponEffectsManager.fireWeapon();
        }
    }

    /**
     * UPDATED: Reload with new ammo system
     */
    public void reload() {
        WeaponType currentWeapon = weaponInventory.getCurrentWeapon();

        // Check if we have a weapon
        if (currentWeapon == null) {
            System.out.println("Player: Cannot reload - no weapon equipped");
            return;
        }

        // Check if weapon systems are initialized
        if (!weaponSystemsInitialized) {
            System.err.println("Player: Cannot reload - weapon systems not initialized!");
            return;
        }

        // Can't reload if already reloading, or no animator
        if (isReloading || weaponAnimator == null) {
            System.out.println("Player: Cannot reload - already reloading or no animator");
            return;
        }

        // Check if we can reload
        if (!ammoInventory.canReload(currentWeapon)) {
            // Play "no ammo" sound
            if (audioManager != null) {
                audioManager.playSoundEffect("gun_no_ammo");
            }
            System.out.println("Player: Cannot reload - no reserve ammo or magazine full");
            return;
        }

        // Can't reload if weapon is still animating from firing
        if (weaponAnimator.isAnimating()) {
            System.out.println("Player: Cannot reload - weapon still animating");
            return;
        }

        System.out.println("Player: Starting reload for " + currentWeapon.displayName);

        // Start reload
        isReloading = true;
        weaponAnimator.reload();

        if (audioManager != null) {
            audioManager.playSoundEffect("gun_reload");
        }
    }

    /**
     * Finish reload process
     */
    private void finishReload() {
        WeaponType currentWeapon = weaponInventory.getCurrentWeapon();

        if (currentWeapon != null) {
            int ammoReloaded = ammoInventory.reload(currentWeapon);
            System.out.println("Player: Reloaded " + ammoReloaded + " rounds into " + currentWeapon.displayName);
        }

        isReloading = false;
    }

    public void toggleTorch() {
        torchOn = !torchOn;
        if (torchOn) {
            rootNode.addLight(torch);
        } else {
            rootNode.removeLight(torch);
        }

        if (audioManager != null) {
            audioManager.playSoundEffect("torch_toggle");
        }
    }

    private void updateTorchPosition() {
        if (torch != null) {
            Vector3f torchOffset = camera.getDirection().mult(0.4f);
            Vector3f torchPos = camera.getLocation().add(torchOffset);
            torch.setPosition(torchPos);
            torch.setDirection(camera.getDirection());
        }
    }

    private void updateFootstepAudio(float tpf) {
        if (isMovingOnGround() && audioManager != null) {
            footstepTimer += tpf;
            if (footstepTimer >= footstepInterval) {
                audioManager.playSoundEffect("footstep");
                footstepTimer = 0f;
            }
        } else {
            footstepTimer = 0f;
        }
    }

    private boolean isMovingOnGround() {
        boolean isMoving = moveForward || moveBackward || strafeLeft || strafeRight;

        if (characterControl == null) {
            return isMoving;
        }

        boolean onGround = characterControl.onGround();
        return isMoving && onGround;
    }

    public void setPositionOnly(Vector3f physicsPosition) {
        this.position.set(physicsPosition);
    }

    public void syncPositionWithCamera(Vector3f physicsPosition) {
        this.position.set(physicsPosition);
    }

    // Movement setters
    public void setMoveForward(boolean move) { this.moveForward = move; }
    public void setMoveBackward(boolean move) { this.moveBackward = move; }
    public void setStrafeLeft(boolean move) { this.strafeLeft = move; }
    public void setStrafeRight(boolean move) { this.strafeRight = move; }

    // ENHANCED: Getters for mouse delta information
    public float getCurrentMouseDeltaX() { return currentMouseDeltaX; }
    public float getCurrentMouseDeltaY() { return currentMouseDeltaY; }

    // Configuration for mouse sway behavior
    public void setMouseDeltaDecay(float decay) {
        this.mouseDeltaDecay = Math.max(0.5f, Math.min(0.99f, decay));
    }

    // UPDATED: Getters using new inventory system
    public Vector3f getPosition() { return position.clone(); }
    public boolean isTorchOn() { return torchOn; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public boolean isDead() { return isDead; }
    public ModernWeaponAnimator getWeaponAnimator() { return weaponAnimator; }

    // NEW: Inventory-based getters
    public boolean hasAnyWeapon() {
        return weaponInventory.hasAnyWeapon();
    }

    public WeaponType getCurrentWeapon() {
        return weaponInventory.getCurrentWeapon();
    }

    public String getCurrentWeaponName() {
        return weaponInventory.getCurrentWeaponName();
    }

    public int getCurrentAmmo() {
        WeaponType current = weaponInventory.getCurrentWeapon();
        return current != null ? ammoInventory.getLoadedAmmo(current) : 0;
    }

    public int getMaxAmmo() {
        WeaponType current = weaponInventory.getCurrentWeapon();
        return current != null ? current.magazineSize : 0;
    }

    public int getReserveAmmo() {
        WeaponType current = weaponInventory.getCurrentWeapon();
        return current != null ? ammoInventory.getReserveAmmo(current.ammoType) : 0;
    }

    public String getAmmoStatusString() {
        WeaponType current = weaponInventory.getCurrentWeapon();
        return current != null ? ammoInventory.getAmmoStatusForWeapon(current) : "NO WEAPON";
    }

    public boolean isReloading() { return isReloading; }
    public WeaponEffectsManager getWeaponEffectsManager() { return weaponEffectsManager; }
    public ModelBasedMuzzleFlash getMuzzleFlashSystem() { return muzzleFlashSystem; }

    // NEW: Inventory access for debugging
    public WeaponInventory getWeaponInventory() { return weaponInventory; }
    public AmmoInventory getAmmoInventory() { return ammoInventory; }

    // CRITICAL: Check if weapon systems are initialized
    public boolean areWeaponSystemsInitialized() { return weaponSystemsInitialized; }

    public void takeDamage(float damage) {
        health -= damage;
        if (health <= 0) {
            health = 0;
            isDead = true;
        }
    }

    public void heal(float amount) {
        health = Math.min(maxHealth, health + amount);
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public boolean isOnGround() {
        return characterControl != null ? characterControl.onGround() : true;
    }

    // NEW: System status for debugging
    public String getWeaponSystemsStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== Player Weapon Systems Status ===\n");
        status.append("Has Any Weapon: ").append(hasAnyWeapon()).append("\n");
        status.append("Current Weapon: ").append(getCurrentWeaponName()).append("\n");
        status.append("Weapon Count: ").append(weaponInventory.getOwnedWeaponCount()).append("\n");
        status.append("Ammo Status: ").append(getAmmoStatusString()).append("\n");
        status.append("Systems Initialized: ").append(weaponSystemsInitialized).append("\n");
        status.append("Weapon Animator: ").append(weaponAnimator != null ? "Connected" : "Missing").append("\n");
        status.append("Weapon Effects: ").append(weaponEffectsManager != null ? "Connected" : "Missing").append("\n");
        status.append("Muzzle Flash 3D: ").append(muzzleFlashSystem != null ? "Connected" : "Missing").append("\n");
        status.append("Audio Manager: ").append(audioManager != null ? "Connected" : "Missing").append("\n");
        status.append("Game Instance: ").append(gameInstance != null ? "Connected" : "Missing").append("\n");
        status.append("Reloading: ").append(isReloading ? "Yes" : "No").append("\n");
        return status.toString();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
    }

    @Override
    public void read(JmeImporter im) throws IOException {
    }
}