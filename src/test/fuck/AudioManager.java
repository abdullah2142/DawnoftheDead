package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.scene.Node;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages all audio in the game - with improved error handling and JME best practices
 */
public class AudioManager {

    private AssetManager assetManager;
    private Node rootNode;
    private float masterVolume = 1.0f;
    private boolean audioEnabled = true;

    // Audio categories
    private final ConcurrentHashMap<String, AudioNode> backgroundMusic;
    private final ConcurrentHashMap<String, AudioNode> soundEffects;
    private final ConcurrentHashMap<String, AudioNode> ambientSounds;

    // Currently playing - thread-safe references
    private final AtomicReference<AudioNode> currentMusic = new AtomicReference<>();
    private volatile String currentMusicKey;

    public AudioManager(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;

        backgroundMusic = new ConcurrentHashMap<>();
        soundEffects = new ConcurrentHashMap<>();
        ambientSounds = new ConcurrentHashMap<>();
    }
    /**
     * Load and register background music
     */
    public void loadBackgroundMusic(String key, String audioPath) {
        if (!audioEnabled) return;

        try {AudioNode music = new AudioNode(assetManager, audioPath, DataType.Stream);
            music.setLooping(true);
            music.setVolume(masterVolume * 0.7f);
            music.setPositional(false);
            backgroundMusic.put(key, music);
            rootNode.attachChild(music);} catch (Exception e) {
            System.err.println("Failed to load background music " + key + " at path " + audioPath + ": " + e.getMessage());
            // Create a dummy entry so hasBackgroundMusic works correctly

        }
    }

    /**
     * Load and register sound effect with JME best practices for short sounds
     */
    public void loadSoundEffect(String key, String audioPath) {
        if (!audioEnabled) return;

        try {// For short sounds, always use DataType.Buffer for better performance
            AudioNode sfx = new AudioNode(assetManager, audioPath, DataType.Buffer);

            // Standard settings for short sound effects
            sfx.setLooping(false);        // Never loop short effects
            sfx.setPositional(false);     // Global sound (not 3D positioned)
            sfx.setVolume(masterVolume);

            // Test if the audio node was created successfully
            if (sfx.getAudioData() == null) {
                System.err.println("Warning: Audio data is null for " + key);

                return;
            }

            // Store the AudioNode
            soundEffects.put(key, sfx);

            // IMPORTANT: Attach to rootNode for proper audio threading
            rootNode.attachChild(sfx);} catch (Exception e) {
            System.err.println("Failed to load sound effect " + key + " at path " + audioPath + ": " + e.getMessage());
            e.printStackTrace();
            // Create a dummy entry so hasSoundEffect works correctly

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
                    loaded = true;}
            } catch (Exception e) {}
        }

        // Fallback to OGG
        if (!loaded) {
            try {
                loadSoundEffect(key, basePath);
                if (soundEffects.get(key) != null) {
                    loaded = true;}
            } catch (Exception e) {}
        }

        if (!loaded) {
            System.err.println("All format attempts failed for " + key);

        }
    }

    /**
     * Load and register ambient sound
     */
    public void loadAmbientSound(String key, String audioPath) {
        if (!audioEnabled) return;

        try {AudioNode ambient = new AudioNode(assetManager, audioPath, DataType.Stream);
            ambient.setLooping(true);
            ambient.setVolume(masterVolume * 0.5f);
            ambient.setPositional(false);
            ambientSounds.put(key, ambient);
            rootNode.attachChild(ambient);} catch (Exception e) {
            System.err.println("Failed to load ambient sound " + key + " at path " + audioPath + ": " + e.getMessage());
            // Create a dummy entry so hasAmbientSound works correctly

        }
    }

    /**
     * Play background music
     */
    public void playBackgroundMusic(String key) {
        if (!audioEnabled) return;

        // Stop current music atomically
        AudioNode oldMusic = currentMusic.getAndSet(null);
        if (oldMusic != null) {
            try {
                oldMusic.stop();
            } catch (Exception e) {
                System.err.println("Error stopping background music: " + e.getMessage());
            }
        }

        // Start new music
        AudioNode music = backgroundMusic.get(key);
        if (music != null) {
            try {
                music.play();
                currentMusic.set(music);
                currentMusicKey = key;} catch (Exception e) {
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
        AudioNode music = currentMusic.getAndSet(null);
        if (music != null) {
            try {
                music.stop();
            } catch (Exception e) {
                System.err.println("Error stopping background music: " + e.getMessage());
            }
            currentMusicKey = null;
        }
    }

    /**
     * Pause current background music
     */
    public void pauseBackgroundMusic() {
        AudioNode music = currentMusic.get();
        if (music != null) {
            try {
                music.pause();
            } catch (Exception e) {
                System.err.println("Error pausing background music: " + e.getMessage());
            }
        }
    }

    /**
     * Resume current background music
     */
    public void resumeBackgroundMusic() {
        AudioNode music = currentMusic.get();
        if (music != null) {
            try {
                music.play();
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
                    ambient.play();}
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
    public void initializeHorrorSounds() {// Load torch toggle sound with multiple format fallbacks
        loadTorchToggleSound();

        // Background music - these are longer so OGG is fine
        loadBackgroundMusic("horror_ambient", "Sounds/horror_music.ogg");
        loadBackgroundMusic("menu_music", "Sounds/menu_music.ogg");

        // Other sound effects with fallbacks
        loadSoundEffectWithFallbacks("footstep", "Sounds/footstep.wav");
        loadSoundEffectWithFallbacks("door_creak", "Sounds/door_creak.ogg");
        loadSoundEffectWithFallbacks("heartbeat", "Sounds/heartbeat.ogg");
        loadSoundEffectWithFallbacks("gun_fire", "Sounds/gun_fire.wav");
        loadSoundEffectWithFallbacks("gun_reload", "Sounds/gun_reload.wav");
        // Ambient sounds
        loadAmbientSound("wind", "Sounds/wind.ogg");
        loadAmbientSound("whispers", "Sounds/whispers.ogg");}

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
                    torchLoaded = true;}
            } catch (Exception e) {}
        }

        // Fallback to OGG
        if (!torchLoaded) {
            try {
                loadSoundEffect("torch_toggle", "Sounds/torch_toggle.ogg");
                if (soundEffects.get("torch_toggle") != null) {
                    torchLoaded = true;}
            } catch (Exception e) {}
        }

        // Final fallback - try JME built-in sound for testing
        if (!torchLoaded) {
            try {
                loadSoundEffect("torch_toggle", "Sound/Environment/Fire.ogg");
                if (soundEffects.get("torch_toggle") != null) {
                    torchLoaded = true;}
            } catch (Exception e) {
                System.err.println("Built-in sound also failed for torch_toggle: " + e.getMessage());
            }
        }

        if (!torchLoaded) {
            System.err.println("All torch sound loading attempts failed, torch will be silent");

        }
    }

    /**
     * Initialize with minimal/no audio for testing
     */
    public void initializeMinimalAudio() {// Create placeholder entries so the game doesn't crash}

    // Getters
    public float getMasterVolume() {
        return masterVolume;
    }

    public String getCurrentMusicKey() {
        return currentMusicKey;
    }

    public boolean isMusicPlaying() {
        AudioNode music = currentMusic.get();
        return music != null && music.getStatus() == AudioSource.Status.Playing;
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