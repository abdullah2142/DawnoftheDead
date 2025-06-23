package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced weapon animation system with proper mouse sway and subframe interpolation
 */
public class ModernWeaponAnimator {

    private float movementSpeed = 0f;
    private float movementDirection = 0f;  // For left/right movement
    private float walkCycleTimer = 0f;

    // Core JME3 references
    private final AssetManager assetManager;
    private final Node guiNode;
    private final int screenWidth;
    private final int screenHeight;

    // Double-buffered Pictures for smooth interpolation
    private Picture currentFramePicture;
    private Picture nextFramePicture;
    private Material currentMaterial;
    private Material nextMaterial;

    // Animation frames
    private Texture2D[] frames;
    private final Map<String, AnimationSequence> animations = new HashMap<>();

    // Current animation state
    private AnimationSequence currentAnimation;
    private int currentFrameIndex = 0;
    private int nextFrameIndex = 0;
    private float interpolationAlpha = 0f;
    private float animationTimer = 0f;
    private boolean isPlaying = false;

    // Weapon positioning
    private Vector3f basePosition;
    private Vector3f currentOffset = new Vector3f();
    private float weaponScale = 6f;

    // Advanced animation features
    private AnimationCurve currentCurve = AnimationCurve.LINEAR;
    private boolean useProceduralMotion = true;
    private float weaponSway = 0f;
    private float weaponBob = 0f;
    private float recoilAmount = 0f;

    // ENHANCED: Mouse sway system
    private float mouseSwayX = 0f;
    private float mouseSwayY = 0f;
    private float mouseSwayDecay = 0.88f; // How fast mouse sway decays
    private float mouseSwayIntensity = 15f; // How strong mouse sway is
    private float mouseSwayMaxX = 25f; // Maximum horizontal sway
    private float mouseSwayMaxY = 20f; // Maximum vertical sway

    /**
     * Set movement velocity for weapon animation
     */
    public void setMovementVelocity(Vector3f velocity) {
        if (velocity == null) {
            movementSpeed = 0f;
            movementDirection = 0f;
            return;
        }

        movementSpeed = velocity.length();
        movementSpeed = FastMath.clamp(movementSpeed, 0f, 8f); // Clamp to reasonable range

        // Calculate left/right movement direction (-1 to 1)
        if (velocity.x != 0) {
            movementDirection = FastMath.clamp(velocity.x / 8f, -1f, 1f); // Normalize by max speed
        } else {
            movementDirection *= 0.9f; // Decay direction when not moving sideways
        }
    }

    /**
     * ENHANCED: Set mouse sway deltas directly from input
     */
    public void setMouseSway(float deltaX, float deltaY) {
        if (!useProceduralMotion) return;

        // Apply mouse deltas to weapon sway with intensity scaling
        mouseSwayX += deltaX * mouseSwayIntensity;
        mouseSwayY += deltaY * mouseSwayIntensity;

        // Clamp sway to maximum values to prevent excessive movement
        mouseSwayX = FastMath.clamp(mouseSwayX, -mouseSwayMaxX, mouseSwayMaxX);
        mouseSwayY = FastMath.clamp(mouseSwayY, -mouseSwayMaxY, mouseSwayMaxY);
    }

    /**
     * Animation sequence definition
     */
    public static class AnimationSequence {
        public final String name;
        public final int startFrame;
        public final int endFrame;
        public final float[] frameDurations;  // Custom duration per frame transition
        public final AnimationCurve[] curves;  // Curve per frame transition
        public final boolean loops;
        public final AnimationSequence nextSequence;  // Chain animations

        public AnimationSequence(String name, int start, int end, float duration, boolean loops) {
            this(name, start, end, createUniformDurations(end - start, duration), null, loops, null);
        }

        public AnimationSequence(String name, int start, int end, float[] durations,
                                 AnimationCurve[] curves, boolean loops, AnimationSequence next) {
            this.name = name;
            this.startFrame = start;
            this.endFrame = end;
            this.frameDurations = durations;
            this.curves = curves;
            this.loops = loops;
            this.nextSequence = next;
        }

        private static float[] createUniformDurations(int transitions, float totalDuration) {
            float[] durations = new float[transitions];
            float perFrame = totalDuration / transitions;
            for (int i = 0; i < transitions; i++) {
                durations[i] = perFrame;
            }
            return durations;
        }
    }

    /**
     * Animation interpolation curves
     */
    public enum AnimationCurve {
        LINEAR {
            @Override
            public float apply(float t) { return t; }
        },
        EASE_IN {
            @Override
            public float apply(float t) { return t * t; }
        },
        EASE_OUT {
            @Override
            public float apply(float t) { return 1f - (1f - t) * (1f - t); }
        },
        EASE_IN_OUT {
            @Override
            public float apply(float t) {
                return t < 0.5f ? 2f * t * t : 1f - 2f * (1f - t) * (1f - t);
            }
        },
        BOUNCE {
            @Override
            public float apply(float t) {
                if (t < 0.5f) {
                    return 8f * t * t;
                } else {
                    t = t - 0.75f;
                    return 1f - 8f * t * t;
                }
            }
        },
        OVERSHOOT {
            @Override
            public float apply(float t) {
                return t * t * (3f - 2f * t);
            }
        };

        public abstract float apply(float t);
    }

    public ModernWeaponAnimator(AssetManager assetManager, Node guiNode, int screenWidth, int screenHeight) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        initializePictures();
        setupDefaultAnimations();




    }

    /**
     * Initialize the double-buffered Picture system
     */
    private void initializePictures() {
        // Current frame picture (back buffer)
        currentFramePicture = new Picture("WeaponCurrentFrame");
        currentFramePicture.setWidth(200);
        currentFramePicture.setHeight(200);

        // Next frame picture (front buffer for blending)
        nextFramePicture = new Picture("WeaponNextFrame");
        nextFramePicture.setWidth(200);
        nextFramePicture.setHeight(200);

        // Calculate base position (bottom center of screen)
        basePosition = new Vector3f(
                (screenWidth - 200) / 2f,
                450,  // Offset from bottom
                0
        );

        // Create materials with proper alpha blending
        currentMaterial = createWeaponMaterial();
        nextMaterial = createWeaponMaterial();

        currentFramePicture.setMaterial(currentMaterial);
        nextFramePicture.setMaterial(nextMaterial);

        // Set initial positions
        updateWeaponPositions();

        // Add to GUI (order matters for alpha blending)
        guiNode.attachChild(currentFramePicture);
        guiNode.attachChild(nextFramePicture);
    }

    /**
     * Create material following JME3 best practices for GUI
     */
    private Material createWeaponMaterial() {
        Material mat = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");
        mat.setColor("Color", ColorRGBA.White);

        // Enable alpha blending
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Disable depth testing for GUI elements
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setDepthWrite(false);

        // Ensure proper render order
        mat.setTransparent(true);

        return mat;
    }

    /**
     * Load weapon frames from PNG files
     * @param basePath Base path like "Textures/Weapons/gun_"
     * @param frameCount Total number of frames (19 for your gun)
     */
    public void loadFrames(String basePath, int frameCount) {
        frames = new Texture2D[frameCount];

        for (int i = 0; i < frameCount; i++) {
            // Build path: "Textures/Weapons/gun_000.png", "gun_001.png", etc.
            String path = basePath + String.format("%03d", i) + ".png";

            try {
                // Load texture through JME3's asset manager
                Texture2D texture = (Texture2D) assetManager.loadTexture(path);

                // Set texture filtering for pixel-perfect rendering
                texture.setMagFilter(Texture.MagFilter.Nearest);
                texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

                frames[i] = texture;

            } catch (Exception e) {
                System.err.println("Failed to load weapon frame: " + path);
                e.printStackTrace();
            }
        }

        // Set initial frame on both Pictures
        if (frames.length > 0) {
            setFrame(0);

        } else {
            System.err.println("No weapon frames were loaded!");
        }
    }

    /**
     * Setup default gun animations with proper timing
     */
    private void setupDefaultAnimations() {
        // SHOOT - frames 1-3
        float[] shootDurations = {0.02f, 0.02f, 0.03f}; // 3 transitions: 1->2, 2->3, 3->done
        AnimationCurve[] shootCurves = {
                AnimationCurve.OVERSHOOT,
                AnimationCurve.LINEAR,
                AnimationCurve.EASE_OUT
        };
        animations.put("shoot", new AnimationSequence("shoot", 1, 4, shootDurations, shootCurves, false, null));

        // RELOAD - frames 4-17 (that's 14 frames, 13 transitions)
        float[] reloadDurations = new float[13]; // 4->5, 5->6, ... 16->17 = 13 transitions
        AnimationCurve[] reloadCurves = new AnimationCurve[13];

        // Magazine ejection (frames 4-7): smooth acceleration
        for (int i = 0; i < 4; i++) {
            reloadDurations[i] = 0.06f;
            reloadCurves[i] = AnimationCurve.EASE_IN_OUT;
        }

        // Magazine out pause (frames 7-8): slower
        reloadDurations[4] = 0.15f;
        reloadCurves[4] = AnimationCurve.LINEAR;

        // New magazine insertion (frames 8-14): deliberate
        for (int i = 5; i < 10; i++) {
            reloadDurations[i] = 0.08f;
            reloadCurves[i] = AnimationCurve.EASE_IN_OUT;
        }

        // Slide release (frames 14-17): snappy
        for (int i = 10; i < 13; i++) {
            reloadDurations[i] = 0.04f;
            reloadCurves[i] = AnimationCurve.OVERSHOOT;
        }

        // RELOAD: frames 4-17 (endFrame 18 means "up to but not including 18")
        animations.put("reload", new AnimationSequence("reload", 4, 18, reloadDurations, reloadCurves, false, null));

    }

    /**
     * Play an animation by name
     */
    public void playAnimation(String name) {
        AnimationSequence anim = animations.get(name);
        if (anim != null) {  // Remove the currentAnimation check

            currentAnimation = anim;
            currentFrameIndex = anim.startFrame;
            nextFrameIndex = Math.min(currentFrameIndex + 1, anim.endFrame);
            interpolationAlpha = 0f;
            animationTimer = 0f;
            isPlaying = true;

            // Set initial frames
            setFrame(currentFrameIndex);

            // Add recoil for shooting
            if ("shoot".equals(name)) {
                recoilAmount = 1f;
            }
        }
    }

    /**
     * Update animation with subframe interpolation and enhanced procedural motion
     */
    public void update(float tpf) {
        // Always update procedural motion (for bob/sway)
        updateProceduralMotion(tpf);

        if (!isPlaying || currentAnimation == null || frames == null) {
            // Still update positions even when not animating
            updateWeaponPositions();
            return;
        }

        // Get current frame transition info
        int transitionIndex = currentFrameIndex - currentAnimation.startFrame;
        if (transitionIndex < 0 || transitionIndex >= currentAnimation.frameDurations.length) {
            handleAnimationComplete();
            return;
        }

        float frameDuration = currentAnimation.frameDurations[transitionIndex];
        AnimationCurve curve = currentAnimation.curves != null ?
                currentAnimation.curves[transitionIndex] : AnimationCurve.LINEAR;

        // Update animation timer
        animationTimer += tpf;

        // Calculate interpolation progress
        float rawAlpha = Math.min(animationTimer / frameDuration, 1f);
        interpolationAlpha = curve.apply(rawAlpha);

        // Update frame blending
        updateFrameBlending();

        // Check for frame advance
        if (animationTimer >= frameDuration) {
            animationTimer = 0f;
            advanceFrame();
        }

        // Update weapon position with all effects combined
        updateWeaponPositions();
    }

    /**
     * Update alpha blending between frames
     */
    private void updateFrameBlending() {
        // Update materials with interpolated alpha
        ColorRGBA currentColor = new ColorRGBA(1f, 1f, 1f, 1f - interpolationAlpha);
        ColorRGBA nextColor = new ColorRGBA(1f, 1f, 1f, interpolationAlpha);

        currentMaterial.setColor("Color", currentColor);
        nextMaterial.setColor("Color", nextColor);

        // Ensure textures are set
        if (currentFrameIndex < frames.length) {
            currentMaterial.setTexture("Texture", frames[currentFrameIndex]);
        }
        if (nextFrameIndex < frames.length && nextFrameIndex != currentFrameIndex) {
            nextMaterial.setTexture("Texture", frames[nextFrameIndex]);
        }
    }

    /**
     * Advance to next frame
     */
    private void advanceFrame() {
        currentFrameIndex = nextFrameIndex;

        if (currentFrameIndex >= currentAnimation.endFrame) {
            // Animation segment complete
            if (currentAnimation.loops) {
                currentFrameIndex = currentAnimation.startFrame;
                nextFrameIndex = currentFrameIndex + 1;
            } else {
                handleAnimationComplete();
                return;
            }
        } else {
            nextFrameIndex = currentFrameIndex + 1;
        }

        interpolationAlpha = 0f;
    }

    /**
     * Handle animation completion
     */
    private void handleAnimationComplete() {
        if (currentAnimation.nextSequence != null) {
            // Chain to next animation
            currentAnimation = currentAnimation.nextSequence;
            currentFrameIndex = currentAnimation.startFrame;
            nextFrameIndex = Math.min(currentFrameIndex + 1, currentAnimation.endFrame);
            interpolationAlpha = 0f;
            animationTimer = 0f;
        } else {
            // Return to static idle state
            isPlaying = false;
            currentAnimation = null;
            setFrame(0); // Static idle on frame 0

        }
    }

    /**
     * ENHANCED: Update procedural weapon motion with mouse sway
     */
    private void updateProceduralMotion(float tpf) {
        if (!useProceduralMotion) return;

        // Walking bob (vertical movement based on movement speed)
        if (movementSpeed > 0.1f) {
            walkCycleTimer += tpf * movementSpeed * 2f; // Walking speed affects bob rate
            weaponBob = FastMath.sin(walkCycleTimer) * movementSpeed * 1f; // Vertical bob
            weaponSway = FastMath.sin(walkCycleTimer * 0.5f) * movementSpeed * 4.5f; // Horizontal sway
        } else {
            // Idle breathing (much smaller)
            weaponBob = FastMath.sin(tpf * 1.5f) * 0.5f;
            weaponSway = FastMath.sin(tpf * 2f) * 0.3f;
            walkCycleTimer = 0f;
        }

        // Add directional tilt based on movement direction
        float directionalTilt = movementDirection * movementSpeed * 2f;

        // Recoil recovery
        if (recoilAmount > 0) {
            recoilAmount = Math.max(0, recoilAmount - tpf * 4f);
        }

        // ENHANCED: Apply mouse sway decay
        mouseSwayX *= mouseSwayDecay;
        mouseSwayY *= mouseSwayDecay;

        // Clamp very small values to zero to prevent tiny movements
        if (Math.abs(mouseSwayX) < 0.1f) mouseSwayX = 0f;
        if (Math.abs(mouseSwayY) < 0.1f) mouseSwayY = 0f;

        // Calculate combined offset with mouse sway
        currentOffset.set(
                weaponSway + directionalTilt + mouseSwayX,  // ENHANCED: Add mouse sway X
                weaponBob - (recoilAmount * 20f) + mouseSwayY,  // ENHANCED: Add mouse sway Y
                0
        );
    }

    /**
     * Update weapon picture positions
     */
    private void updateWeaponPositions() {
        Vector3f finalPos = basePosition.add(currentOffset);

        currentFramePicture.setPosition(finalPos.x, finalPos.y);
        nextFramePicture.setPosition(finalPos.x, finalPos.y);
    }

    /**
     * Set a single frame (no interpolation) - applies texture to Picture via Material
     */
    private void setFrame(int frameIndex) {
        if (frames != null && frameIndex >= 0 && frameIndex < frames.length) {
            currentFrameIndex = frameIndex;

            // Apply texture to the Picture's material
            // This is how the image actually appears on screen
            currentMaterial.setTexture("Texture", frames[frameIndex]);

            // Also set on next frame material (for first frame)
            if (nextMaterial != null) {
                nextMaterial.setTexture("Texture", frames[frameIndex]);
            }

            // Reset to full opacity for single frame display
            currentMaterial.setColor("Color", ColorRGBA.White);
            nextMaterial.setColor("Color", new ColorRGBA(1f, 1f, 1f, 0f)); // Invisible
        }
    }

    // Public API methods

    public void fire() {
        playAnimation("shoot");
    }

    public void reload() {
        playAnimation("reload");
    }

    public void setWeaponPosition(Vector3f offset) {
        this.basePosition.set(offset);
        updateWeaponPositions();
    }

    public void setWeaponScale(float scale) {
        this.weaponScale = scale;
        currentFramePicture.setWidth(128 * scale);
        currentFramePicture.setHeight(128 * scale);
        nextFramePicture.setWidth(128 * scale);
        nextFramePicture.setHeight(128 * scale);
    }

    public void setProceduralMotion(boolean enabled) {
        this.useProceduralMotion = enabled;
        if (!enabled) {
            currentOffset.set(0, 0, 0);
            weaponSway = 0;
            weaponBob = 0;
            recoilAmount = 0;
            mouseSwayX = 0;
            mouseSwayY = 0;
        }
    }

    // ENHANCED: Mouse sway configuration methods
    public void setMouseSwayIntensity(float intensity) {
        this.mouseSwayIntensity = Math.max(0f, intensity); // Remove upper clamp

    }

    public void setMouseSwayDecay(float decay) {
        this.mouseSwayDecay = Math.max(0.1f, Math.min(0.99f, decay)); // Keep decay reasonable

    }

    public void setMouseSwayLimits(float maxX, float maxY) {
        this.mouseSwayMaxX = Math.max(5f, maxX); // Remove upper clamp for X
        this.mouseSwayMaxY = Math.max(5f, maxY); // Remove upper clamp for Y

    }

    public boolean isAnimating() {
        return isPlaying;
    }

    public String getCurrentAnimationName() {
        return currentAnimation != null ? currentAnimation.name : "none";
    }

    public void cleanup() {
        guiNode.detachChild(currentFramePicture);
        guiNode.detachChild(nextFramePicture);
    }

    // Advanced features for external control

    public void addCustomAnimation(String name, AnimationSequence sequence) {
        animations.put(name, sequence);
    }

    // DEPRECATED: Use setMouseSway instead
    @Deprecated
    public void setMouseInfluence(float deltaX, float deltaY) {
        setMouseSway(deltaX, deltaY);
    }

    // Getters for debugging
    public float getCurrentMouseSwayX() { return mouseSwayX; }
    public float getCurrentMouseSwayY() { return mouseSwayY; }
    public float getMouseSwayIntensity() { return mouseSwayIntensity; }
    public float getMouseSwayDecay() { return mouseSwayDecay; }
}