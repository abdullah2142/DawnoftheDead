package horrorjme;

import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 * Handles all player-related functionality
 */
public class Player{
    private static final float MOVE_SPEED = 3f;
    private static final float ROTATION_SPEED = 2f;
    private static final float COLLISION_BUFFER = 0.3f;

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

    // Movement flags
    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean strafeLeft = false;
    private boolean strafeRight = false;

    // Mouse look
    private float mouseSensitivity = 1.0f;
    private float yaw = 0f;
    private float pitch = 0f;

    // Reusable vectors to avoid garbage collection
    private Vector3f walkDirection = new Vector3f();
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    private Vector3f newPosition = new Vector3f();
    private Vector3f tempVector = new Vector3f();

    // Map reference for collision
    private int[][] mapData;

    // Movement tracking for footsteps
    private float lastFootstepTime = 0f;
    private float footstepInterval = 0.5f;

    public Player(Camera camera, Node rootNode, int[][] mapData, AudioManager audioManager) {
        this.camera = camera;
        this.rootNode = rootNode;
        this.mapData = mapData;
        this.audioManager = audioManager;
        this.position = new Vector3f(2.5f, 0.5f, 2.5f);

        // Set initial camera position and orientation
        camera.setLocation(position.clone());
        camera.lookAt(new Vector3f(3.5f, 0.5f, 2.5f), Vector3f.UNIT_Y);

        setupTorch();
    }

    // Overloaded constructor for backward compatibility
    public Player(Camera camera, Node rootNode, int[][] mapData) {
        this(camera, rootNode, mapData, null);
    }

    private void setupTorch() {
        torch = new SpotLight();

        // Position the flashlight slightly in front of the player
        Vector3f torchOffset = camera.getDirection().mult(0.2f);
        torch.setPosition(position.add(torchOffset));

        // Set the direction to match camera direction
        torch.setDirection(camera.getDirection());

        // Configure flashlight properties
        torch.setSpotRange(25f);
        torch.setSpotInnerAngle(12f * FastMath.DEG_TO_RAD);
        torch.setSpotOuterAngle(25f * FastMath.DEG_TO_RAD);

        // Light intensity and color
        torch.setColor(ColorRGBA.White.mult(25.0f));

        rootNode.addLight(torch);
    }

    // ADDED: The missing update method
    public void update(float tpf) {
        updateMovement(tpf);
        updateTorchPosition();
        updateFootstepAudio(tpf);
    }

    private void updateMovement(float tpf) {
        // Get camera directions (reuse vectors)
        camera.getDirection(camDir);
        camera.getLeft(camLeft);
        camDir.y = 0;
        camLeft.y = 0;
        camDir.normalizeLocal();
        camLeft.normalizeLocal();

        // Calculate walk direction
        walkDirection.set(0, 0, 0);

        if (moveForward) {
            tempVector.set(camDir).multLocal(tpf * MOVE_SPEED);
            walkDirection.addLocal(tempVector);
        }
        if (moveBackward) {
            tempVector.set(camDir).multLocal(-tpf * MOVE_SPEED);
            walkDirection.addLocal(tempVector);
        }
        if (strafeLeft) {
            tempVector.set(camLeft).multLocal(tpf * MOVE_SPEED);
            walkDirection.addLocal(tempVector);
        }
        if (strafeRight) {
            tempVector.set(camLeft).multLocal(-tpf * MOVE_SPEED);
            walkDirection.addLocal(tempVector);
        }

        // Apply movement with collision detection
        newPosition.set(position).addLocal(walkDirection);
        if (!isColliding(newPosition)) {
            position.set(newPosition);
            camera.setLocation(position);
        } else {
            // Try sliding along walls
            trySlideMovement();
        }
    }

    private void updateFootstepAudio(float tpf) {
        // Play footstep sounds when moving
        if (isMoving() && audioManager != null) {
            lastFootstepTime += tpf;
            if (lastFootstepTime >= footstepInterval) {
                audioManager.playSoundEffect("footstep");
                lastFootstepTime = 0f;
            }
        }
    }

    private boolean isMoving() {
        return moveForward || moveBackward || strafeLeft || strafeRight;
    }

    private void trySlideMovement() {
        // Try X movement only
        newPosition.set(position);
        newPosition.x += walkDirection.x;
        if (!isColliding(newPosition)) {
            position.x = newPosition.x;
            camera.setLocation(position);
        }

        // Try Z movement only
        newPosition.set(position);
        newPosition.z += walkDirection.z;
        if (!isColliding(newPosition)) {
            position.z = newPosition.z;
            camera.setLocation(position);
        }
    }

    private boolean isColliding(Vector3f pos) {
        int mapX = (int)pos.x;
        int mapZ = (int)pos.z;

        // Check bounds
        if (mapX < 0 || mapX >= mapData[0].length || mapZ < 0 || mapZ >= mapData.length) {
            return true;
        }

        // Check current cell
        if (mapData[mapZ][mapX] == 1) {
            return true;
        }

        // Check buffer zones around player
        if (pos.x - mapX < COLLISION_BUFFER) {
            if (mapX > 0 && mapData[mapZ][mapX - 1] == 1) {
                return true;
            }
        }
        if (mapX + 1 - pos.x < COLLISION_BUFFER) {
            if (mapX < mapData[0].length - 1 && mapData[mapZ][mapX + 1] == 1) {
                return true;
            }
        }
        if (pos.z - mapZ < COLLISION_BUFFER) {
            if (mapZ > 0 && mapData[mapZ - 1][mapX] == 1) {
                return true;
            }
        }
        if (mapZ + 1 - pos.z < COLLISION_BUFFER) {
            if (mapZ < mapData.length - 1 && mapData[mapZ + 1][mapX] == 1) {
                return true;
            }
        }

        return false;
    }

    public void handleMouseLook(float deltaX, float deltaY) {
        yaw -= deltaX * mouseSensitivity * 1.105f;
        pitch -= deltaY * mouseSensitivity * 1.105f;

        // Clamp pitch to prevent camera flipping
        pitch = Math.max(-1.5f, Math.min(1.5f, pitch));

        // Apply rotation to camera
        Quaternion yawRot = new Quaternion();
        yawRot.fromAngleAxis(yaw, Vector3f.UNIT_Y);

        Quaternion pitchRot = new Quaternion();
        pitchRot.fromAngleAxis(pitch, Vector3f.UNIT_X);

        camera.setRotation(yawRot.mult(pitchRot));
    }

    public void toggleTorch() {
        torchOn = !torchOn;
        if (torchOn) {
            rootNode.addLight(torch);
        } else {
            rootNode.removeLight(torch);
        }

        // Play torch toggle sound
        if (audioManager != null) {
            audioManager.playSoundEffect("torch_toggle");
        }
    }

    // FIXED: Updated to handle both position and direction for flashlight
    private void updateTorchPosition() {
        if (torch != null) {
            // Position the flashlight slightly in front of the player
            Vector3f torchOffset = camera.getDirection().mult(0.3f);
            torch.setPosition(position.add(torchOffset));

            // Update direction to match where the player is looking
            torch.setDirection(camera.getDirection());
        }
    }

    // Movement setters
    public void setMoveForward(boolean move) { this.moveForward = move; }
    public void setMoveBackward(boolean move) { this.moveBackward = move; }
    public void setStrafeLeft(boolean move) { this.strafeLeft = move; }
    public void setStrafeRight(boolean move) { this.strafeRight = move; }

    // Getters
    public Vector3f getPosition() { return position.clone(); }
    public boolean isTorchOn() { return torchOn; }
    public void setMouseSensitivity(float sensitivity) { this.mouseSensitivity = sensitivity; }
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

    // Audio manager setter
    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }
}