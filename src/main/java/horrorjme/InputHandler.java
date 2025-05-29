package horrorjme;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.math.FastMath;

/**
 * Fixed InputHandler with smooth mouse handling and ground-based footsteps
 */
public class InputHandler implements ActionListener, AnalogListener {

    private InputManager inputManager;
    private GameStateManager stateManager;
    private Player player;
    private MenuSystem menuSystem;
    private HorrorGameJME game;
    private Camera cam;

    // Physics-based movement
    private CharacterControl playerControl;
    private Vector3f walkDirection = new Vector3f();
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    private boolean[] inputFlags = new boolean[4];
    private static final int FORWARD = 0, BACKWARD = 1, LEFT = 2, RIGHT = 3;

    // Scale-adjusted movement
    private float moveSpeed = 0.15f;
    private float sprintMultiplier = 1.67f;
    private boolean isSprinting = false;

    // DIRECT CAMERA CONTROL (like DebugNoclipControl)
    private boolean mouseEnabled = false;
    private float mouseSensitivity = 1.15f;  // Scale-adjusted

    // Direct camera rotation tracking (no physics interference)
    private float yaw = 0f;
    private float pitch = 0f;

    public InputHandler(InputManager inputManager, GameStateManager stateManager, HorrorGameJME game) {
        this.inputManager = inputManager;
        this.stateManager = stateManager;
        this.game = game;
        this.cam = game.getCamera();
        setupInputMappings();

        System.out.println("Fixed InputHandler initialized:");
        System.out.println("  Movement speed: " + moveSpeed);
        System.out.println("  Mouse sensitivity: " + mouseSensitivity);
        System.out.println("  Mouse handling: DIRECT (like noclip)");
        System.out.println("  Ground-based footsteps: ENABLED");
    }

    public void setPlayer(Player player) {
        this.player = player;
        // DON'T sync mouse sensitivity - we handle it directly now

        // IMPORTANT: Pass CharacterControl to Player for ground detection
        if (this.playerControl != null && player != null) {
            player.setCharacterControl(this.playerControl);
            System.out.println("InputHandler: CharacterControl passed to Player for ground detection");
        }
    }

    public void setPlayerControl(CharacterControl playerControl) {
        this.playerControl = playerControl;
        System.out.println("Physics player control connected");

        // IMPORTANT: If player is already set, pass the CharacterControl to it
        if (this.player != null) {
            this.player.setCharacterControl(playerControl);
            System.out.println("InputHandler: CharacterControl passed to existing Player");
        }
    }

    public void setMenuSystem(MenuSystem menuSystem) {
        this.menuSystem = menuSystem;
    }

    private void setupInputMappings() {
        // Movement controls
        inputManager.addMapping("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("MoveBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("StrafeLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("StrafeRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("ToggleTorch", new KeyTrigger(KeyInput.KEY_F));

        // Menu controls
        inputManager.addMapping("MenuUp", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("MenuDown", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("MenuSelect", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("MenuBack", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping("Escape", new KeyTrigger(KeyInput.KEY_ESCAPE));

        // Mouse look - DIRECT CONTROL (like DebugNoclipControl)
        inputManager.addMapping("MouseLookX", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("MouseLookX-", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("MouseLookY", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("MouseLookY-", new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        // Add listeners
        inputManager.addListener(this,
                // Movement
                "MoveForward", "MoveBackward", "StrafeLeft", "StrafeRight",
                "Jump", "Sprint", "ToggleTorch",
                // Menu
                "MenuUp", "MenuDown", "MenuSelect", "MenuBack", "Escape",
                // Mouse look
                "MouseLookX", "MouseLookX-", "MouseLookY", "MouseLookY-"
        );
    }

    public void enableMouseLook() {
        mouseEnabled = true;
        inputManager.setCursorVisible(false);

        // Initialize camera rotation tracking from current camera
        Vector3f direction = cam.getDirection();
        yaw = (float) Math.atan2(-direction.x, -direction.z);
        pitch = (float) Math.asin(direction.y);

        System.out.println("Mouse look enabled - DIRECT camera control active");
    }

    public void disableMouseLook() {
        mouseEnabled = false;
        inputManager.setCursorVisible(true);
        System.out.println("Mouse look disabled");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (stateManager.getCurrentState()) {
            case MAIN_MENU:
            case OPTIONS_MENU:
                handleMenuInput(name, isPressed);
                break;
            case PLAYING:
                handleGameInput(name, isPressed);
                break;
            case PAUSED:
                handlePausedInput(name, isPressed);
                break;
        }
    }

    private void handleMenuInput(String name, boolean isPressed) {
        if (!isPressed) return;

        if (menuSystem != null) {
            switch (name) {
                case "MenuUp":
                    menuSystem.moveSelectionUp();
                    break;
                case "MenuDown":
                    menuSystem.moveSelectionDown();
                    break;
                case "MenuSelect":
                    menuSystem.selectCurrentItem();
                    break;
                case "MenuBack":
                case "Escape":
                    menuSystem.handleBack();
                    break;
            }
        }
    }

    private void handleGameInput(String name, boolean isPressed) {
        // Handle escape first
        if (name.equals("Escape") && !isPressed) {
            game.pauseGame();
            return;
        }

        // Movement controls - Update player movement flags for footstep detection
        switch (name) {
            case "MoveForward":
                inputFlags[FORWARD] = isPressed;
                if (player != null) {
                    player.setMoveForward(isPressed);
                }
                break;
            case "MoveBackward":
                inputFlags[BACKWARD] = isPressed;
                if (player != null) {
                    player.setMoveBackward(isPressed);
                }
                break;
            case "StrafeLeft":
                inputFlags[LEFT] = isPressed;
                if (player != null) {
                    player.setStrafeLeft(isPressed);
                }
                break;
            case "StrafeRight":
                inputFlags[RIGHT] = isPressed;
                if (player != null) {
                    player.setStrafeRight(isPressed);
                }
                break;
            case "Jump":
                if (isPressed && playerControl != null) {
                    playerControl.jump();
                }
                break;
            case "Sprint":
                isSprinting = isPressed;
                break;
            case "ToggleTorch":
                if (!isPressed && player != null) {
                    player.toggleTorch();
                }
                break;
        }

        // Update physics movement
        updatePhysicsMovement();
    }

    private void handlePausedInput(String name, boolean isPressed) {
        if (!isPressed) return;

        if (name.equals("Escape") || name.equals("MenuBack")) {
            game.resumeGame();
        }
    }

    /**
     * Physics movement (unchanged)
     */
    private void updatePhysicsMovement() {
        if (playerControl == null) return;

        // Get FRESH camera direction vectors every frame
        Vector3f camDirection = cam.getDirection().clone();
        Vector3f camLeft = cam.getLeft().clone();

        // Zero out Y components for ground-based movement (crucial for FPS feel)
        camDirection.y = 0;
        camLeft.y = 0;

        // Normalize to prevent speed changes when looking up/down
        camDirection.normalizeLocal();
        camLeft.normalizeLocal();

        // Calculate walk direction based on current camera orientation
        walkDirection.set(0, 0, 0);

        if (inputFlags[FORWARD]) {
            walkDirection.addLocal(camDirection);
        }
        if (inputFlags[BACKWARD]) {
            walkDirection.addLocal(camDirection.negate());
        }
        if (inputFlags[LEFT]) {
            walkDirection.addLocal(camLeft);
        }
        if (inputFlags[RIGHT]) {
            walkDirection.addLocal(camLeft.negate());
        }

        // Apply speed
        float currentMoveSpeed = moveSpeed;
        if (isSprinting) {
            currentMoveSpeed *= sprintMultiplier;
        }

        if (walkDirection.lengthSquared() > 0) {
            walkDirection.normalizeLocal().multLocal(currentMoveSpeed);
        }

        playerControl.setWalkDirection(walkDirection);
    }
    @Override
    public void onAnalog(String name, float value, float tpf) {
        // Only handle mouse look when in game and mouse is enabled
        if (stateManager.getCurrentState() != GameStateManager.GameState.PLAYING ||
                !mouseEnabled) {
            return;
        }

        // FIXED MOUSE INVERSION - Corrected axis handling
        switch (name) {
            case "MouseLookX":
                // Moving mouse RIGHT should look RIGHT (positive yaw)
                yaw -= value * mouseSensitivity;  // FIXED: was +=, now -=
                break;
            case "MouseLookX-":
                // Moving mouse LEFT should look LEFT (negative yaw)
                yaw += value * mouseSensitivity;  // FIXED: was -=, now +=
                break;
            case "MouseLookY":
                // Moving mouse DOWN should look DOWN (positive pitch)
                pitch -= value * mouseSensitivity;  // This was correct
                break;
            case "MouseLookY-":
                // Moving mouse UP should look UP (negative pitch)
                pitch += value * mouseSensitivity;  // This was correct
                break;
        }

        // Clamp pitch to prevent camera flipping
        pitch = Math.max(-1.5f, Math.min(1.5f, pitch));

        // Apply rotation DIRECTLY to camera
        cam.getRotation().fromAngles(pitch, yaw, 0);
    }

    /**
     * Update method - ONLY handle camera position following
     */
    public void update(float tpf) {
        // ONLY update camera POSITION to follow physics player
        // Rotation is handled directly in onAnalog (no interference)
        if (playerControl != null && stateManager.getCurrentState() == GameStateManager.GameState.PLAYING) {
            Vector3f playerPos = playerControl.getPhysicsLocation();
            cam.setLocation(playerPos.add(0, 1.6f, 0)); // Eye level offset
        }
    }

    // Configuration methods
    public void setMoveSpeed(float speed) {
        this.moveSpeed = Math.max(0.05f, Math.min(0.30f, speed));
        System.out.println("Movement speed set to: " + this.moveSpeed);
    }

    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = Math.max(1.05f, Math.min(0.50f, sensitivity));
        System.out.println("Mouse sensitivity set to: " + this.mouseSensitivity);
    }

    public void cleanup() {
        inputManager.removeListener(this);
    }
}