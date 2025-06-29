package horrorjme;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.math.FastMath;

/**
 * Enhanced InputHandler with proper mouse delta tracking for weapon sway AND lighting controls
 * COMPLETE VERSION with weapon switching and debug keys
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

    // ENHANCED: Mouse delta tracking for weapon sway
    private float currentMouseDeltaX = 0f;
    private float currentMouseDeltaY = 0f;
    private float mouseSmoothing = 0.85f; // How much previous deltas affect current (0.0 = no smoothing, 0.9 = heavy smoothing)

    public InputHandler(InputManager inputManager, GameStateManager stateManager, HorrorGameJME game) {
        this.inputManager = inputManager;
        this.stateManager = stateManager;
        this.game = game;
        this.cam = game.getCamera();
        setupInputMappings();
    }

    public void setPlayer(Player player) {
        this.player = player;

        // IMPORTANT: Pass CharacterControl to Player for ground detection
        if (this.playerControl != null && player != null) {
            player.setCharacterControl(this.playerControl);
        }
    }

    public void setPlayerControl(CharacterControl playerControl) {
        this.playerControl = playerControl;

        // IMPORTANT: If player is already set, pass the CharacterControl to it
        if (this.player != null) {
            this.player.setCharacterControl(playerControl);
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
        inputManager.addMapping("Fire", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Reload", new KeyTrigger(KeyInput.KEY_R));

        // NEW: Weapon switching controls
        inputManager.addMapping("Weapon1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("Weapon2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Weapon3", new KeyTrigger(KeyInput.KEY_3));
       // inputManager.addMapping("WeaponNext", new MouseButtonTrigger(MouseInput.BUTTON_WHEEL_UP));
      //  inputManager.addMapping("WeaponPrev", new MouseButtonTrigger(MouseInput.BUTTON_WHEEL_DOWN));

        // Menu controls
        inputManager.addMapping("MenuUp", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("MenuDown", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("MenuSelect", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("MenuBack", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping("Escape", new KeyTrigger(KeyInput.KEY_ESCAPE));

        // LIGHTING CONTROLS - Simplified
        inputManager.addMapping("ToggleShadows", new KeyTrigger(KeyInput.KEY_F9));
        inputManager.addMapping("Lightning", new KeyTrigger(KeyInput.KEY_F10));
        inputManager.addMapping("ToggleFlicker", new KeyTrigger(KeyInput.KEY_F11));

        // NEW: Debug keys for testing revolver
        inputManager.addMapping("SpawnRevolver", new KeyTrigger(KeyInput.KEY_F5));
        inputManager.addMapping("GiveRevolver", new KeyTrigger(KeyInput.KEY_F6));
        inputManager.addMapping("TestRevolver", new KeyTrigger(KeyInput.KEY_F7));

        // Mouse look - DIRECT CONTROL with enhanced delta tracking
        inputManager.addMapping("MouseLookX", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("MouseLookX-", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("MouseLookY", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("MouseLookY-", new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        // Add listeners
        inputManager.addListener(this,
                // Movement
                "MoveForward", "MoveBackward", "StrafeLeft", "StrafeRight",
                "Jump", "Sprint", "ToggleTorch",
                // Weapons
                "Fire", "Reload", "Weapon1", "Weapon2", "Weapon3", "WeaponNext", "WeaponPrev",
                // Menu
                "MenuUp", "MenuDown", "MenuSelect", "MenuBack", "Escape",
                // Mouse look
                "MouseLookX", "MouseLookX-", "MouseLookY", "MouseLookY-",
                // Debug keys
                "SpawnRevolver", "GiveRevolver", "TestRevolver",
                // LIGHTING CONTROLS - Simplified
                "ToggleShadows", "Lightning", "ToggleFlicker"
        );
    }

    public void enableMouseLook() {
        mouseEnabled = true;
        inputManager.setCursorVisible(false);

        // Initialize camera rotation tracking from current camera
        Vector3f direction = cam.getDirection();
        yaw = (float) Math.atan2(-direction.x, -direction.z);
        pitch = (float) Math.asin(direction.y);

        // Reset mouse deltas
        currentMouseDeltaX = 0f;
        currentMouseDeltaY = 0f;
    }

    public void disableMouseLook() {
        mouseEnabled = false;
        inputManager.setCursorVisible(true);

        // Reset mouse deltas when disabling
        currentMouseDeltaX = 0f;
        currentMouseDeltaY = 0f;
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
            case "Fire":
                if (!isPressed && player != null) {
                    player.fireWeapon();
                }
                break;
            case "Reload":
                if (!isPressed && player != null) {
                    player.reload();
                }
                break;

            // NEW: Weapon switching controls
            case "Weapon1":
                if (!isPressed && player != null) {
                    boolean switched = player.switchWeapon(1);
                    if (switched) {
                        System.out.println("Switched to weapon slot 1 (Sten Gun)");
                    }
                }
                break;
            case "Weapon2":
                if (!isPressed && player != null) {
                    boolean switched = player.switchWeapon(2);
                    if (switched) {
                        System.out.println("Switched to weapon slot 2 (Revolver)");
                    }
                }
                break;
            case "Weapon3":
                if (!isPressed && player != null) {
                    boolean switched = player.switchWeapon(3);
                    if (switched) {
                        System.out.println("Switched to weapon slot 3");
                    }
                }
                break;
            case "WeaponNext":
                if (!isPressed && player != null) {
                    boolean switched = player.switchToNextWeapon();
                    if (switched) {
                        System.out.println("Next weapon: " + player.getCurrentWeaponName());
                    }
                }
                break;
            case "WeaponPrev":
                if (!isPressed && player != null) {
                    boolean switched = player.switchToPreviousWeapon();
                    if (switched) {
                        System.out.println("Previous weapon: " + player.getCurrentWeaponName());
                    }
                }
                break;

            // NEW: Debug controls for testing revolver

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

        // ENHANCED: Track raw mouse movement for weapon sway
        float rawMouseDeltaX = 0f;
        float rawMouseDeltaY = 0f;

        // FIXED MOUSE INVERSION - Corrected axis handling
        switch (name) {
            case "MouseLookX":
                // Moving mouse RIGHT should look RIGHT (positive yaw)
                yaw -= value * mouseSensitivity;
                rawMouseDeltaX = -value; // Raw delta for weapon (negative because yaw is negative)
                break;
            case "MouseLookX-":
                // Moving mouse LEFT should look LEFT (negative yaw)
                yaw += value * mouseSensitivity;
                rawMouseDeltaX = value; // Raw delta for weapon (positive because yaw is positive)
                break;
            case "MouseLookY":
                // Moving mouse DOWN should look DOWN (positive pitch)
                pitch -= value * mouseSensitivity;
                rawMouseDeltaY = -value; // Raw delta for weapon
                break;
            case "MouseLookY-":
                // Moving mouse UP should look UP (negative pitch)
                pitch += value * mouseSensitivity;
                rawMouseDeltaY = value; // Raw delta for weapon
                break;
        }

        // ENHANCED: Smooth mouse deltas for weapon sway
        if (rawMouseDeltaX != 0f || rawMouseDeltaY != 0f) {
            // Apply smoothing to reduce jitter while maintaining responsiveness
            currentMouseDeltaX = (currentMouseDeltaX * mouseSmoothing) + (rawMouseDeltaX * (1f - mouseSmoothing));
            currentMouseDeltaY = (currentMouseDeltaY * mouseSmoothing) + (rawMouseDeltaY * (1f - mouseSmoothing));

            // Pass mouse deltas to player for weapon sway
            if (player != null) {
                player.setMouseDeltas(currentMouseDeltaX, currentMouseDeltaY);
            }
        }

        // Clamp pitch to prevent camera flipping
        pitch = Math.max(-1.5f, Math.min(1.5f, pitch));

        // Apply rotation DIRECTLY to camera
        cam.getRotation().fromAngles(pitch, yaw, 0);
    }

    /**
     * Update method - decay mouse deltas over time for smooth weapon sway
     */
    public void update(float tpf) {
        // ONLY update camera POSITION to follow physics player
        if (playerControl != null && stateManager.getCurrentState() == GameStateManager.GameState.PLAYING) {
            Vector3f playerPos = playerControl.getPhysicsLocation();
            cam.setLocation(playerPos.add(0, 1.6f, 0)); // Eye level offset

            // FIX: Continuously update movement direction when moving
            if (inputFlags[FORWARD] || inputFlags[BACKWARD] || inputFlags[LEFT] || inputFlags[RIGHT]) {
                updatePhysicsMovement();
            }
        }

        // ENHANCED: Decay mouse deltas gradually for natural weapon sway
        if (mouseEnabled) {
            float decayRate = 8.0f; // How fast mouse deltas decay (higher = faster decay)
            currentMouseDeltaX *= (1f - decayRate * tpf);
            currentMouseDeltaY *= (1f - decayRate * tpf);

            // Clamp very small values to zero to prevent tiny movements
            if (Math.abs(currentMouseDeltaX) < 0.001f) currentMouseDeltaX = 0f;
            if (Math.abs(currentMouseDeltaY) < 0.001f) currentMouseDeltaY = 0f;

            // Continue passing decaying deltas to player for smooth weapon sway
            if (player != null) {
                player.setMouseDeltas(currentMouseDeltaX, currentMouseDeltaY);
            }
        }
    }

    // ENHANCED: Getters for mouse delta information
    public float getCurrentMouseDeltaX() {
        return currentMouseDeltaX;
    }

    public float getCurrentMouseDeltaY() {
        return currentMouseDeltaY;
    }

    public float getMouseSensitivity() {
        return mouseSensitivity;
    }

    // Configuration methods
    public void setMoveSpeed(float speed) {
        this.moveSpeed = Math.max(0.05f, Math.min(0.30f, speed));
    }

    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = Math.max(0.5f, Math.min(3.0f, sensitivity));
    }

    public void setMouseSmoothingForSway(float smoothing) {
        this.mouseSmoothing = Math.max(0.0f, Math.min(0.95f, smoothing));
    }

    public void cleanup() {
        inputManager.removeListener(this);
    }
}