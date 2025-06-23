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

    // Weapon systems (UPDATED: Split into two systems)
    private ModernWeaponAnimator weaponAnimator;
    private WeaponEffectsManager weaponEffectsManager;     // Shell casings + bullet tracers
    private ModelBasedMuzzleFlash muzzleFlashSystem;       // 3D muzzle flash effects

    // ENHANCED: Mouse tracking for weapon sway
    private float currentMouseDeltaX = 0f;
    private float currentMouseDeltaY = 0f;
    private float mouseDeltaDecay = 0.92f; // How fast mouse deltas decay on the weapon side

    // Weapon properties
    private int currentAmmo = 30;
    private int maxAmmo = 30;
    private boolean isReloading = false;
    private float fireRate = 0.15f; // Time between shots
    private float lastFireTime = 0f;

    public Player(Camera camera, Node rootNode, int[][] mapData, AudioManager audioManager) {
        this.camera = camera;
        this.rootNode = rootNode;
        this.audioManager = audioManager;
        this.position = new Vector3f(10f, 150f, 20f);

        camera.setLocation(position.clone());
        Vector3f initialLookAt = position.add(new Vector3f(0, 0, -1));
        camera.lookAt(initialLookAt, Vector3f.UNIT_Y);

        setupTorch();}

    public Player(Camera camera, Node rootNode, int[][] mapData) {
        this(camera, rootNode, mapData, null);
    }

    // NEW: Set muzzle flash system
    public void setMuzzleFlashSystem(ModelBasedMuzzleFlash muzzleFlashSystem) {
        this.muzzleFlashSystem = muzzleFlashSystem;}

    // UPDATED: Set weapon effects manager (shell casings + tracers)
    public void setWeaponEffectsManager(WeaponEffectsManager weaponEffectsManager) {
        this.weaponEffectsManager = weaponEffectsManager;}

    public void setCharacterControl(CharacterControl characterControl) {
        this.characterControl = characterControl;}

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

        // UPDATED: Update split weapon effects systems
        if (weaponEffectsManager != null) {
            weaponEffectsManager.update(tpf);
        }

        // NEW: Update 3D muzzle flash system
        if (muzzleFlashSystem != null) {
            muzzleFlashSystem.update(tpf);
        }

        if (weaponAnimator != null) {
            // ENHANCED: Pass mouse deltas directly to weapon for sway
            weaponAnimator.setMouseSway(currentMouseDeltaX, currentMouseDeltaY);

            // Pass movement velocity to weapon for walking animation
            weaponAnimator.setMovementVelocity(getLinearVelocity());

            // Update weapon animator
            weaponAnimator.update(tpf);

            // Check if reloading animation finished
            if (isReloading && !weaponAnimator.isAnimating()) {
                finishReload();
            }

            // ENHANCED: Apply decay to mouse deltas for smooth sway
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
        this.weaponAnimator = weaponAnimator;}

    /**
     * ENHANCED: Fire weapon with split effects systems
     */
    public void fireWeapon() {
        // Check fire rate limit
        if (lastFireTime < fireRate) {
            return;
        }

        // Check if we have ammo and not reloading
        if (currentAmmo <= 0) {
            // Play empty click sound
            if (audioManager != null) {}
            return;
        }

        if (isReloading) {
            return; // Can't fire while reloading
        }

        // Reset fire timer
        lastFireTime = 0f;

        // Consume ammo
        currentAmmo--;

        // 1. Trigger weapon animation
        if (weaponAnimator != null) {
            weaponAnimator.fire();
        }

        // 2. Play fire sound
        if (audioManager != null) {
            audioManager.playSoundEffect("gun_fire");
        }

        // 3. NEW: Trigger 3D muzzle flash
        if (muzzleFlashSystem != null) {
            muzzleFlashSystem.createMuzzleFlash();
        }

        // 4. Trigger shell casings and bullet tracers
        if (weaponEffectsManager != null) {
            weaponEffectsManager.fireWeapon();
        }}

    /**
     * ENHANCED: Reload with ammo tracking
     */
    public void reload() {
        // Can't reload if already reloading, already full, or no animator
        if (isReloading || currentAmmo >= maxAmmo || weaponAnimator == null) {
            return;
        }

        // Can't reload if weapon is still animating from firing
        if (weaponAnimator.isAnimating()) {
            return;
        }

        // Start reload
        isReloading = true;
        weaponAnimator.reload();

        if (audioManager != null) {
            audioManager.playSoundEffect("gun_reload");
        }}

    /**
     * Finish reload process
     */
    private void finishReload() {
        isReloading = false;
        currentAmmo = maxAmmo;}

    public void toggleTorch() {
        torchOn = !torchOn;
        if (torchOn) {
            rootNode.addLight(torch);} else {
            rootNode.removeLight(torch);}

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

    private boolean isMoving() {
        return moveForward || moveBackward || strafeLeft || strafeRight;
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
        this.mouseDeltaDecay = Math.max(0.5f, Math.min(0.99f, decay));}

    // Getters and setters
    public Vector3f getPosition() { return position.clone(); }
    public boolean isTorchOn() { return torchOn; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public boolean isDead() { return isDead; }
    public ModernWeaponAnimator getWeaponAnimator() { return weaponAnimator; }

    // UPDATED: Weapon state getters
    public int getCurrentAmmo() { return currentAmmo; }
    public int getMaxAmmo() { return maxAmmo; }
    public boolean isReloading() { return isReloading; }
    public WeaponEffectsManager getWeaponEffectsManager() { return weaponEffectsManager; }
    public ModelBasedMuzzleFlash getMuzzleFlashSystem() { return muzzleFlashSystem; } // NEW

    public void takeDamage(float damage) {
        health -= damage;
        if (health <= 0) {
            health = 0;
            isDead = true;}
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
        status.append("Weapon Animator: ").append(weaponAnimator != null ? "Connected" : "Missing").append("\n");
        status.append("Weapon Effects: ").append(weaponEffectsManager != null ? "Connected" : "Missing").append("\n");
        status.append("Muzzle Flash 3D: ").append(muzzleFlashSystem != null ? "Connected" : "Missing").append("\n");
        status.append("Audio Manager: ").append(audioManager != null ? "Connected" : "Missing").append("\n");
        status.append("Current Ammo: ").append(currentAmmo).append("/").append(maxAmmo).append("\n");
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