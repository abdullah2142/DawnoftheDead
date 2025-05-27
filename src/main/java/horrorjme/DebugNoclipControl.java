package horrorjme;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * Debug noclip movement - Toggle with F1
 */
public class DebugNoclipControl implements ActionListener, AnalogListener {

    private Camera cam;
    private InputManager inputManager;
    private boolean enabled = false;
    private float moveSpeed = 10f;
    private float originalMoveSpeed;

    // Movement states
    private boolean forward, backward, left, right, up, down;

    public DebugNoclipControl(Camera cam, InputManager inputManager) {
        this.cam = cam;
        this.inputManager = inputManager;
        setupControls();
    }

    private void setupControls() {
        // Toggle noclip
        inputManager.addMapping("ToggleNoclip", new KeyTrigger(KeyInput.KEY_F1));

        // Noclip movement
        inputManager.addMapping("Noclip_Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Noclip_Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Noclip_Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Noclip_Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Noclip_Up", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Noclip_Down", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Noclip_SpeedUp", new KeyTrigger(KeyInput.KEY_LCONTROL));

        // Add listener
        inputManager.addListener(this, "ToggleNoclip");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("ToggleNoclip") && !isPressed) {
            toggle();
            return;
        }

        if (!enabled) return;

        switch (name) {
            case "Noclip_Forward": forward = isPressed; break;
            case "Noclip_Backward": backward = isPressed; break;
            case "Noclip_Left": left = isPressed; break;
            case "Noclip_Right": right = isPressed; break;
            case "Noclip_Up": up = isPressed; break;
            case "Noclip_Down": down = isPressed; break;
            case "Noclip_SpeedUp": moveSpeed = isPressed ? 30f : 10f; break;
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        // Not used
    }

    public void toggle() {
        enabled = !enabled;

        if (enabled) {
            System.out.println("NOCLIP MODE ENABLED (F1 to toggle)");
            // Add movement listeners
            inputManager.addListener(this,
                    "Noclip_Forward", "Noclip_Backward",
                    "Noclip_Left", "Noclip_Right",
                    "Noclip_Up", "Noclip_Down", "Noclip_SpeedUp");
        } else {
            System.out.println("NOCLIP MODE DISABLED");
            // Remove movement listeners
            inputManager.removeListener(this);
            // Keep toggle listener
            inputManager.addListener(this, "ToggleNoclip");
        }
    }

    public void update(float tpf) {
        if (!enabled) return;

        Vector3f camDir = cam.getDirection().clone();
        Vector3f camLeft = cam.getLeft().clone();
        Vector3f walkDirection = new Vector3f(0, 0, 0);

        if (forward) walkDirection.addLocal(camDir);
        if (backward) walkDirection.addLocal(camDir.negate());
        if (left) walkDirection.addLocal(camLeft);
        if (right) walkDirection.addLocal(camLeft.negate());
        if (up) walkDirection.addLocal(Vector3f.UNIT_Y);
        if (down) walkDirection.addLocal(Vector3f.UNIT_Y.negate());

        if (walkDirection.lengthSquared() > 0) {
            walkDirection.normalizeLocal();
            cam.setLocation(cam.getLocation().add(walkDirection.mult(moveSpeed * tpf)));
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void cleanup() {
        inputManager.removeListener(this);
    }
}