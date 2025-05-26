package horrorjme;

/**
 * Manages all game options and settings
 */
public class OptionsManager {

    // Default values
    private static final float DEFAULT_MASTER_VOLUME = 1.0f;
    private static final float DEFAULT_MOUSE_SENSITIVITY = 1.0f;
    private static final boolean DEFAULT_INVERT_MOUSE_Y = false;
    private static final boolean DEFAULT_COLOR_BLIND_MODE = false;
    private static final float DEFAULT_TEXT_SIZE = 1.0f;

    // Current settings
    private float masterVolume;
    private float mouseSensitivity;
    private boolean invertMouseY;
    private boolean colorBlindMode;
    private float textSize;

    public OptionsManager() {
        loadDefaultSettings();
    }

    private void loadDefaultSettings() {
        masterVolume = DEFAULT_MASTER_VOLUME;
        mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
        invertMouseY = DEFAULT_INVERT_MOUSE_Y;
        colorBlindMode = DEFAULT_COLOR_BLIND_MODE;
        textSize = DEFAULT_TEXT_SIZE;
    }

    // Getters
    public float getMasterVolume() {
        return masterVolume;
    }

    public float getMouseSensitivity() {
        return mouseSensitivity;
    }

    public boolean isInvertMouseY() {
        return invertMouseY;
    }

    public boolean isColorBlindMode() {
        return colorBlindMode;
    }

    public float getTextSize() {
        return textSize;
    }

    // Setters with validation
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0f, Math.min(1f, volume));
    }

    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = Math.max(0.1f, Math.min(3f, sensitivity));
    }

    public void setInvertMouseY(boolean invert) {
        this.invertMouseY = invert;
    }

    public void setColorBlindMode(boolean enabled) {
        this.colorBlindMode = enabled;
    }

    public void setTextSize(float size) {
        this.textSize = Math.max(0.5f, Math.min(2f, size));
    }

    // Convenience methods for incrementing/decrementing values
    public void adjustMasterVolume(float delta) {
        setMasterVolume(masterVolume + delta);
    }

    public void adjustMouseSensitivity(float delta) {
        setMouseSensitivity(mouseSensitivity + delta);
    }

    public void toggleInvertMouseY() {
        invertMouseY = !invertMouseY;
    }

    public void toggleColorBlindMode() {
        colorBlindMode = !colorBlindMode;
    }

    public void adjustTextSize(float delta) {
        setTextSize(textSize + delta);
    }

    // Reset to defaults
    public void resetToDefaults() {
        loadDefaultSettings();
    }

    // Future: Save/Load from file
    public void saveSettings() {
        // TODO: Implement saving to preferences file
        System.out.println("Settings would be saved here");
    }

    public void loadSettings() {
        // TODO: Implement loading from preferences file
        System.out.println("Settings would be loaded here");
    }

    // Apply settings to game systems
    public void applySettings(Player player, AudioManager audioManager) {
        if (player != null) {
            player.setMouseSensitivity(mouseSensitivity);
        }

        if (audioManager != null) {
            audioManager.setMasterVolume(masterVolume);
        }
    }
}