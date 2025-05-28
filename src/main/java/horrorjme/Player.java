package horrorjme;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 * FPS-tuned Player class with proper horror game controls and ground-based footsteps
 */
public class Player {

    // FPS-tuned movement constants
    private static final float MOVE_SPEED = 0.50f;        // Reduced from too fast
    private static final float ROTATION_SPEED = 2f;      // Smooth rotation
    private static final float COLLISION_BUFFER = 0.3f;  // Wall collision buffer

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

    // Physics reference for ground detection - ADDED THIS LINE
    private CharacterControl characterControl;

    // Movement flags - for smooth FPS movement
    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean strafeLeft = false;
    private boolean strafeRight = false;

    // FPS Mouse look settings
    private float mouseSensitivity = 0.15f;  // REDUCED - was too sensitive
    private float yaw = 0f;
    private float pitch = 0f;
    private float maxLookUp = 0.4f;    // Limit looking up
    private float maxLookDown = -0.4f; // Limit looking down

    // Reusable vectors for performance
    private Vector3f walkDirection = new Vector3f();
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    private Vector3f newPosition = new Vector3f();
    private Vector3f tempVector = new Vector3f();

    // Movement smoothing for FPS feel
    private float footstepTimer = 0f;
    private float footstepInterval = 0.5f;

    // FPS movement dampening
    private float movementSmoothing = 0.1f;

    public Player(Camera camera, Node rootNode, int[][] mapData, AudioManager audioManager) {
        this.camera = camera;
        this.rootNode = rootNode;
        this.audioManager = audioManager;
        this.position = new Vector3f(10f, 150f, 20f); // Use your working spawn

        // Set initial camera position and look direction
        camera.setLocation(position.clone());

        // Look forward initially (not inverted)
        Vector3f initialLookAt = position.add(new Vector3f(0, 0, -1)); // Look forward (negative Z)
        camera.lookAt(initialLookAt, Vector3f.UNIT_Y);

        setupTorch();

        System.out.println("FPS Player initialized with ground-based footstep system");
    }

    // Backward compatibility constructor
    public Player(Camera camera, Node rootNode, int[][] mapData) {
        this(camera, rootNode, mapData, null);
    }

    /**
     * Set the CharacterControl reference for ground detection
     * This should be called from InputHandler after physics setup
     */
    public void setCharacterControl(CharacterControl characterControl) {
        this.characterControl = characterControl;
        System.out.println("Player: CharacterControl set for ground detection");
    }

    private void setupTorch() {
        torch = new SpotLight();

        // Position flashlight in front of camera
        Vector3f torchOffset = camera.getDirection().mult(0.3f);
        torch.setPosition(camera.getLocation().add(torchOffset));
        torch.setDirection(camera.getDirection());

        // Horror game flashlight settings
        torch.setSpotRange(15f);                               // Not too far
        torch.setSpotInnerAngle(8f * FastMath.DEG_TO_RAD);    // Focused beam
        torch.setSpotOuterAngle(18f * FastMath.DEG_TO_RAD);   // Soft edges
        torch.setColor(ColorRGBA.White.mult(8.0f));           // Dimmer for horror

        rootNode.addLight(torch);
    }

    /**
     * Update method with FPS-tuned movement (called when not using physics)
     */
    public void update(float tpf) {
        updateTorchPosition();
        updateFootstepAudio(tpf);
        // Note: Movement handled by physics system now
    }

    /**
     * FPS Mouse look - FIXED INVERSION
     */
    public void handleMouseLook(float deltaX, float deltaY) {
        // FIXED: Remove negative signs that were causing inversion
        yaw += deltaX * mouseSensitivity;      // WAS: yaw -= deltaX (inverted)
        pitch += deltaY * mouseSensitivity;    // WAS: pitch -= deltaY (inverted)

        // Clamp pitch to prevent camera flipping
        pitch = Math.max(maxLookDown, Math.min(maxLookUp, pitch));

        // Apply rotation to camera - FPS style
        Quaternion yawRot = new Quaternion();
        yawRot.fromAngleAxis(yaw, Vector3f.UNIT_Y);

        Quaternion pitchRot = new Quaternion();
        pitchRot.fromAngleAxis(pitch, Vector3f.UNIT_X);

        // Combine rotations properly for FPS
        camera.setRotation(yawRot.mult(pitchRot));
    }

    /**
     * Toggle flashlight with horror game audio
     */
    public void toggleTorch() {
        torchOn = !torchOn;
        if (torchOn) {
            rootNode.addLight(torch);
            System.out.println("Flashlight ON");
        } else {
            rootNode.removeLight(torch);
            System.out.println("Flashlight OFF");
        }

        // Play torch toggle sound
        if (audioManager != null) {
            audioManager.playSoundEffect("torch_toggle");
        }
    }

    /**
     * Update torch to follow camera properly
     */
    private void updateTorchPosition() {
        if (torch != null) {
            // Position flashlight slightly in front of camera
            Vector3f torchOffset = camera.getDirection().mult(0.4f);
            Vector3f torchPos = camera.getLocation().add(torchOffset);
            torch.setPosition(torchPos);
            torch.setDirection(camera.getDirection());
        }
    }

    /**
     * FIXED: Ground-based footstep audio system
     * Only plays footsteps when moving AND on the ground
     */
    private void updateFootstepAudio(float tpf) {
        // Check if player is moving AND on the ground
        if (isMovingOnGround() && audioManager != null) {
            footstepTimer += tpf;
            if (footstepTimer >= footstepInterval) {
                audioManager.playSoundEffect("footstep");
                footstepTimer = 0f;
            }
        } else {
            // Reset timer when not walking on ground
            footstepTimer = 0f;
        }
    }

    /**
     * FIXED: Check if player is moving AND on the ground
     * This prevents footstep sounds during jumping/falling
     */
    private boolean isMovingOnGround() {
        boolean isMoving = moveForward || moveBackward || strafeLeft || strafeRight;

        // If we don't have character control reference, fall back to old behavior
        if (characterControl == null) {
            return isMoving;
        }

        // Only play footsteps if moving AND on the ground
        boolean onGround = characterControl.onGround();

        // Optional: Add debug output for testing
        if (isMoving && !onGround) {
            // Uncomment for debugging: System.out.println("Moving but not on ground - no footsteps");
        }

        return isMoving && onGround;
    }

    /**
     * Legacy method - kept for compatibility
     */
    private boolean isMoving() {
        return moveForward || moveBackward || strafeLeft || strafeRight;
    }

    /**
     * Sync player position with physics (called from physics system)
     */
    public void syncPositionWithCamera(Vector3f physicsPosition) {
        this.position.set(physicsPosition);

        // Update camera rotation tracking based on current camera
        Vector3f camDir = camera.getDirection();
        this.yaw = (float) Math.atan2(-camDir.x, -camDir.z);
        this.pitch = (float) Math.asin(camDir.y);

        // Clamp pitch after sync
        pitch = Math.max(maxLookDown, Math.min(maxLookUp, pitch));
    }

    // Movement setters - kept for compatibility but not used with physics
    public void setMoveForward(boolean move) { this.moveForward = move; }
    public void setMoveBackward(boolean move) { this.moveBackward = move; }
    public void setStrafeLeft(boolean move) { this.strafeLeft = move; }
    public void setStrafeRight(boolean move) { this.strafeRight = move; }

    // Getters and setters
    public Vector3f getPosition() { return position.clone(); }
    public boolean isTorchOn() { return torchOn; }

    public void setMouseSensitivity(float sensitivity) {
        // Clamp sensitivity to reasonable FPS values
        this.mouseSensitivity = Math.max(0.1f, Math.min(2.0f, sensitivity));
        System.out.println("Mouse sensitivity set to: " + this.mouseSensitivity);
    }

    public float getMouseSensitivity() { return mouseSensitivity; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public boolean isDead() { return isDead; }

    public void takeDamage(float damage) {
        health -= damage;
        if (health <= 0) {
            health = 0;
            isDead = true;
            System.out.println("Player died!");
        }
    }

    public void heal(float amount) {
        health = Math.min(maxHealth, health + amount);
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    /**
     * Check if player is currently on the ground (for external use)
     */
    public boolean isOnGround() {
        return characterControl != null ? characterControl.onGround() : true;
    }
}