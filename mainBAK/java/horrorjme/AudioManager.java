package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.scene.Node;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all audio in the game - with improved error handling and JME best practices
 */
public class AudioManager {

    private AssetManager assetManager;
    private Node rootNode;
    private float masterVolume = 1.0f;
    private boolean audioEnabled = true;

    // Audio categories
    private Map<String, AudioNode> backgroundMusic;
    private Map<String, AudioNode> soundEffects;
    private Map<String, AudioNode> ambientSounds;

    // Currently playing
    private AudioNode currentMusic;
    private String currentMusicKey;

    public AudioManager(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;

        backgroundMusic = new HashMap<>();
        soundEffects = new HashMap<>();
        ambientSounds = new HashMap<>();
    }

    /**
     * Load and register background music
     */
    public void loadBackgroundMusic(String key, String audioPath) {
        if (!audioEnabled) return;

        try {
            System.out.println("Loading background music: " + key + " from " + audioPath);
            AudioNode music = new AudioNode(assetManager, audioPath, DataType.Stream);
            music.setLooping(true);
            music.setVolume(masterVolume * 0.7f);
            music.setPositional(false);
            backgroundMusic.put(key, music);
            rootNode.attachChild(music);
            System.out.println("Successfully loaded background music: " + key);
        } catch (Exception e) {
            System.err.println("Failed to load background music " + key + " at path " + audioPath + ": " + e.getMessage());
            // Create a dummy entry so hasBackgroundMusic works correctly
            backgroundMusic.put(key, null);
        }
    }

    /**
     * Load and register sound effect with JME best practices for short sounds
     */
    public void loadSoundEffect(String key, String audioPath) {
        if (!audioEnabled) return;

        try {
            System.out.println("Loading sound effect: " + key + " from " + audioPath);

            // For short sounds, always use DataType.Buffer for better performance
            AudioNode sfx = new AudioNode(assetManager, audioPath, DataType.Buffer);

            // Standard settings for short sound effects
            sfx.setLooping(false);        // Never loop short effects
            sfx.setPositional(false);     // Global sound (not 3D positioned)
            sfx.setVolume(masterVolume);

            // Test if the audio node was created successfully
            if (sfx.getAudioData() == null) {
                System.err.println("Warning: Audio data is null for " + key);
                soundEffects.put(key, null);
                return;
            }

            // Store the AudioNode
            soundEffects.put(key, sfx);

            // IMPORTANT: Attach to rootNode for proper audio threading
            rootNode.attachChild(sfx);

            System.out.println("Successfully loaded sound effect: " + key);

        } catch (Exception e) {
            System.err.println("Failed to load sound effect " + key + " at path " + audioPath + ": " + e.getMessage());
            e.printStackTrace();
            // Create a dummy entry so hasSoundEffect works correctly
            soundEffects.put(key, null);
        }
    }

    /**
     * Safe method to try loading sound with multiple format fallbacks
     */
    public void loadSoundEffectWithFallbacks(String key, String basePath) {
        if (!audioEnabled) return;

        boolean loaded = false;

        // Try WAV first (most reliable for short sounds)
        if (!loaded) {
            try {
                String wavPath = basePath.replace(".ogg", ".wav");
                loadSoundEffect(key, wavPath);
                if (soundEffects.get(key) != null) {
                    loaded = true;
                    System.out.println("Loaded " + key + " as WAV");
                }
            } catch (Exception e) {
                System.out.println("WAV loading failed for " + key + ": " + e.getMessage());
            }
        }

        // Fallback to OGG
        if (!loaded) {
            try {
                loadSoundEffect(key, basePath);
                if (soundEffects.get(key) != null) {
                    loaded = true;
                    System.out.println("Loaded " + key + " as OGG");
                }
            } catch (Exception e) {
                System.out.println("OGG loading failed for " + key + ": " + e.getMessage());
            }
        }

        if (!loaded) {
            System.err.println("All format attempts failed for " + key);
            soundEffects.put(key, null);
        }
    }

    /**
     * Load and register ambient sound
     */
    public void loadAmbientSound(String key, String audioPath) {
        if (!audioEnabled) return;

        try {
            System.out.println("Loading ambient sound: " + key + " from " + audioPath);
            AudioNode ambient = new AudioNode(assetManager, audioPath, DataType.Stream);
            ambient.setLooping(true);
            ambient.setVolume(masterVolume * 0.5f);
            ambient.setPositional(false);
            ambientSounds.put(key, ambient);
            rootNode.attachChild(ambient);
            System.out.println("Successfully loaded ambient sound: " + key);
        } catch (Exception e) {
            System.err.println("Failed to load ambient sound " + key + " at path " + audioPath + ": " + e.getMessage());
            // Create a dummy entry so hasAmbientSound works correctly
            ambientSounds.put(key, null);
        }
    }

    /**
     * Play background music
     */
    public void playBackgroundMusic(String key) {
        if (!audioEnabled) return;

        // Stop current music
        stopBackgroundMusic();

        AudioNode music = backgroundMusic.get(key);
        if (music != null) {
            try {
                music.play();
                currentMusic = music;
                currentMusicKey = key;
                System.out.println("Playing background music: " + key);
            } catch (Exception e) {
                System.err.println("Failed to play background music " + key + ": " + e.getMessage());
            }
        } else {
            System.err.println("Background music not found or failed to load: " + key);
        }
    }

    /**
     * Stop current background music
     */
    public void stopBackgroundMusic() {
        if (currentMusic != null) {
            try {
                currentMusic.stop();
            } catch (Exception e) {
                System.err.println("Error stopping background music: " + e.getMessage());
            }
            currentMusic = null;
            currentMusicKey = null;
        }
    }

    /**
     * Pause current background music
     */
    public void pauseBackgroundMusic() {
        if (currentMusic != null) {
            try {
                currentMusic.pause();
            } catch (Exception e) {
                System.err.println("Error pausing background music: " + e.getMessage());
            }
        }
    }

    /**
     * Resume current background music
     */
    public void resumeBackgroundMusic() {
        if (currentMusic != null) {
            try {
                currentMusic.play();
            } catch (Exception e) {
                System.err.println("Error resuming background music: " + e.getMessage());
            }
        }
    }

    /**
     * Play sound effect using JME best practices - USE playInstance() for short sounds
     */
    public void playSoundEffect(String key) {
        if (!audioEnabled) {
            return;
        }

        AudioNode sfx = soundEffects.get(key);
        if (sfx == null) {
            System.err.println("Sound effect not found or failed to load: " + key);
            return;
        }

        try {
            // KEY: Use playInstance() instead of play() for short, repeatable sounds
            // This allows multiple instances to play simultaneously without conflicts
            sfx.playInstance();

        } catch (Exception e) {
            System.err.println("Error playing sound effect " + key + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Play ambient sound
     */
    public void playAmbientSound(String key) {
        if (!audioEnabled) return;

        AudioNode ambient = ambientSounds.get(key);
        if (ambient != null) {
            try {
                if (ambient.getStatus() != AudioSource.Status.Playing) {
                    ambient.play();
                    System.out.println("Playing ambient sound: " + key);
                }
            } catch (Exception e) {
                System.err.println("Error playing ambient sound " + key + ": " + e.getMessage());
            }
        } else {
            System.err.println("Ambient sound not found or failed to load: " + key);
        }
    }

    /**
     * Stop ambient sound
     */
    public void stopAmbientSound(String key) {
        AudioNode ambient = ambientSounds.get(key);
        if (ambient != null) {
            try {
                ambient.stop();
            } catch (Exception e) {
                System.err.println("Error stopping ambient sound " + key + ": " + e.getMessage());
            }
        }
    }

    /**
     * Set master volume (0.0 to 1.0)
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0f, Math.min(1f, volume));

        // Update all audio volumes
        updateAllVolumes();
    }

    private void updateAllVolumes() {
        if (!audioEnabled) return;

        try {
            // Update background music
            for (AudioNode music : backgroundMusic.values()) {
                if (music != null) {
                    music.setVolume(masterVolume * 0.7f);
                }
            }

            // Update sound effects
            for (AudioNode sfx : soundEffects.values()) {
                if (sfx != null) {
                    sfx.setVolume(masterVolume);
                }
            }

            // Update ambient sounds
            for (AudioNode ambient : ambientSounds.values()) {
                if (ambient != null) {
                    ambient.setVolume(masterVolume * 0.5f);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating audio volumes: " + e.getMessage());
        }
    }

    /**
     * Stop all audio
     */
    public void stopAllAudio() {
        stopBackgroundMusic();

        for (AudioNode sfx : soundEffects.values()) {
            if (sfx != null) {
                try {
                    sfx.stop();
                } catch (Exception e) {
                    System.err.println("Error stopping sound effect: " + e.getMessage());
                }
            }
        }

        for (AudioNode ambient : ambientSounds.values()) {
            if (ambient != null) {
                try {
                    ambient.stop();
                } catch (Exception e) {
                    System.err.println("Error stopping ambient sound: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Initialize common horror game sounds with JME best practices and fallbacks
     */
    public void initializeHorrorSounds() {
        System.out.println("Initializing horror sounds with JME best practices...");

        // Load torch toggle sound with multiple format fallbacks
        loadTorchToggleSound();

        // Background music - these are longer so OGG is fine
        loadBackgroundMusic("horror_ambient", "Sounds/horror_music.ogg");
        loadBackgroundMusic("menu_music", "Sounds/menu_music.ogg");

        // Other sound effects with fallbacks
        loadSoundEffectWithFallbacks("footstep", "Sounds/footstep.ogg");
        loadSoundEffectWithFallbacks("door_creak", "Sounds/door_creak.ogg");
        loadSoundEffectWithFallbacks("heartbeat", "Sounds/heartbeat.ogg");

        // Ambient sounds
        loadAmbientSound("wind", "Sounds/wind.ogg");
        loadAmbientSound("whispers", "Sounds/whispers.ogg");

        System.out.println("Audio initialization complete. Missing files will be skipped gracefully.");
    }

    /**
     * Special method to load torch toggle sound with multiple fallbacks
     */
    private void loadTorchToggleSound() {
        boolean torchLoaded = false;

        // Try WAV first (most reliable for short sounds)
        if (!torchLoaded) {
            try {
                loadSoundEffect("torch_toggle", "Sounds/torch_toggle.wav");
                if (soundEffects.get("torch_toggle") != null) {
                    torchLoaded = true;
                    System.out.println("Loaded torch sound as WAV");
                }
            } catch (Exception e) {
                System.out.println("WAV loading failed for torch_toggle: " + e.getMessage());
            }
        }

        // Fallback to OGG
        if (!torchLoaded) {
            try {
                loadSoundEffect("torch_toggle", "Sounds/torch_toggle.ogg");
                if (soundEffects.get("torch_toggle") != null) {
                    torchLoaded = true;
                    System.out.println("Loaded torch sound as OGG");
                }
            } catch (Exception e) {
                System.out.println("OGG loading failed for torch_toggle: " + e.getMessage());
            }
        }

        // Final fallback - try JME built-in sound for testing
        if (!torchLoaded) {
            try {
                loadSoundEffect("torch_toggle", "Sound/Environment/Fire.ogg");
                if (soundEffects.get("torch_toggle") != null) {
                    torchLoaded = true;
                    System.out.println("Using built-in JME fire sound for torch toggle");
                }
            } catch (Exception e) {
                System.err.println("Built-in sound also failed for torch_toggle: " + e.getMessage());
            }
        }

        if (!torchLoaded) {
            System.err.println("All torch sound loading attempts failed, torch will be silent");
            soundEffects.put("torch_toggle", null);
        }
    }

    /**
     * Initialize with minimal/no audio for testing
     */
    public void initializeMinimalAudio() {
        System.out.println("Initializing minimal audio setup...");

        // Create placeholder entries so the game doesn't crash
        backgroundMusic.put("horror_ambient", null);
        backgroundMusic.put("menu_music", null);

        soundEffects.put("footstep", null);
        soundEffects.put("door_creak", null);
        soundEffects.put("heartbeat", null);
        soundEffects.put("torch_toggle", null);

        ambientSounds.put("wind", null);
        ambientSounds.put("whispers", null);

        System.out.println("Minimal audio setup complete - audio disabled.");
    }

    // Getters
    public float getMasterVolume() {
        return masterVolume;
    }

    public String getCurrentMusicKey() {
        return currentMusicKey;
    }

    public boolean isMusicPlaying() {
        return currentMusic != null && currentMusic.getStatus() == AudioSource.Status.Playing;
    }

    public boolean hasSoundEffect(String key) {
        return soundEffects.containsKey(key) && soundEffects.get(key) != null;
    }

    public boolean hasBackgroundMusic(String key) {
        return backgroundMusic.containsKey(key) && backgroundMusic.get(key) != null;
    }

    public boolean hasAmbientSound(String key) {
        return ambientSounds.containsKey(key) && ambientSounds.get(key) != null;
    }

    public boolean isAudioEnabled() {
        return audioEnabled;
    }

    public void setAudioEnabled(boolean enabled) {
        this.audioEnabled = enabled;
        if (!enabled) {
            stopAllAudio();
        }
    }

    /**
     * Cleanup all audio resources
     */
    public void cleanup() {
        stopAllAudio();

        // Detach all audio nodes
        for (AudioNode music : backgroundMusic.values()) {
            if (music != null) {
                try {
                    rootNode.detachChild(music);
                } catch (Exception e) {
                    System.err.println("Error detaching music node: " + e.getMessage());
                }
            }
        }
        for (AudioNode sfx : soundEffects.values()) {
            if (sfx != null) {
                try {
                    rootNode.detachChild(sfx);
                } catch (Exception e) {
                    System.err.println("Error detaching sfx node: " + e.getMessage());
                }
            }
        }
        for (AudioNode ambient : ambientSounds.values()) {
            if (ambient != null) {
                try {
                    rootNode.detachChild(ambient);
                } catch (Exception e) {
                    System.err.println("Error detaching ambient node: " + e.getMessage());
                }
            }
        }

        backgroundMusic.clear();
        soundEffects.clear();
        ambientSounds.clear();
    }
}