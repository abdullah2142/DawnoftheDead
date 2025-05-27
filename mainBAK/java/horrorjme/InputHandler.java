package horrorjme;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;

/**
 * Handles all input management for different game states
 */
public class InputHandler implements ActionListener, AnalogListener {

    private InputManager inputManager;
    private GameStateManager stateManager;
    private Player player;
    private MenuSystem menuSystem;
    private HorrorGameJME game; // Reference to main game for state transitions

    // Mouse look tracking
    private boolean mouseEnabled = false;

    public InputHandler(InputManager inputManager, GameStateManager stateManager, HorrorGameJME game) {
        this.inputManager = inputManager;
        this.stateManager = stateManager;
        this.game = game;
        setupInputMappings();
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setMenuSystem(MenuSystem menuSystem) {
        this.menuSystem = menuSystem;
    }

    private void setupInputMappings() {
        // Game movement controls
        inputManager.addMapping("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("MoveBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("StrafeLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("StrafeRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("ToggleTorch", new KeyTrigger(KeyInput.KEY_F));

        // Menu controls
        inputManager.addMapping("MenuUp", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("MenuDown", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("MenuSelect", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("MenuBack", new KeyTrigger(KeyInput.KEY_ESCAPE));

        // Universal controls
        inputManager.addMapping("Escape", new KeyTrigger(KeyInput.KEY_ESCAPE));

        // Mouse look
        inputManager.addMapping("MouseLookX", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("MouseLookX-", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("MouseLookY", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("MouseLookY-", new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        // Add listeners
        inputManager.addListener(this,
                // Movement
                "MoveForward", "MoveBackward", "StrafeLeft", "StrafeRight", "ToggleTorch",
                // Menu
                "MenuUp", "MenuDown", "MenuSelect", "MenuBack",
                // Universal
                "Escape",
                // Mouse look
                "MouseLookX", "MouseLookX-", "MouseLookY", "MouseLookY-"
        );
    }

    public void enableMouseLook() {
        mouseEnabled = true;
        inputManager.setCursorVisible(false);
    }

    public void disableMouseLook() {
        mouseEnabled = false;
        inputManager.setCursorVisible(true);
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
        if (player == null) return;

        // Handle escape first
        if (name.equals("Escape") && !isPressed) {
            game.pauseGame();
            return;
        }

        // Movement controls
        switch (name) {
            case "MoveForward":
                player.setMoveForward(isPressed);
                break;
            case "MoveBackward":
                player.setMoveBackward(isPressed);
                break;
            case "StrafeLeft":
                player.setStrafeLeft(isPressed);
                break;
            case "StrafeRight":
                player.setStrafeRight(isPressed);
                break;
            case "ToggleTorch":
                if (!isPressed) {
                    player.toggleTorch();
                }
                break;
        }
    }

    private void handlePausedInput(String name, boolean isPressed) {
        if (!isPressed) return;

        if (name.equals("Escape") || name.equals("MenuBack")) {
            game.resumeGame();
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        // Only handle mouse look when in game and mouse is enabled
        if (stateManager.getCurrentState() != GameStateManager.GameState.PLAYING ||
                !mouseEnabled || player == null) {
            return;
        }

        switch (name) {
            case "MouseLookX":
                player.handleMouseLook(value, 0);
                break;
            case "MouseLookX-":
                player.handleMouseLook(-value, 0);
                break;
            case "MouseLookY":
                player.handleMouseLook(0, value);
                break;
            case "MouseLookY-":
                player.handleMouseLook(0, -value);
                break;
        }
    }

    public void cleanup() {
        inputManager.removeListener(this);
    }
}