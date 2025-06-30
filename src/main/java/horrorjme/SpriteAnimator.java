package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

import java.util.HashMap;
import java.util.Map;

/**
 * FIXED: Single-geometry sprite animator - no more blinking/flickering
 */
public class SpriteAnimator {

    private AssetManager assetManager;
    private Node spriteNode;

    // SIMPLIFIED: Single geometry approach
    private Geometry spriteGeometry;
    private Material spriteMaterial;

    // Animation frames storage
    private Map<String, Texture2D[]> animationFrames = new HashMap<>();
    private Map<String, AnimationSequence> animations = new HashMap<>();

    // Current animation state
    private AnimationSequence currentAnimation;
    private int currentFrameIndex = 0;
    private float animationTimer = 0f;
    private boolean isPlaying = false;

    // Quad sizing
    private float pixelToWorldScale = 0.05f;
    private Vector2f currentQuadSize;
    private boolean manualSizeOverride = false;
    private Vector2f manualSize;

    /**
     * Animation sequence definition
     */
    public static class AnimationSequence {
        public final String name;
        public final int startFrame;
        public final int endFrame;
        public final float frameDuration;
        public final boolean loops;

        public AnimationSequence(String name, int startFrame, int endFrame, float frameDuration, boolean loops) {
            this.name = name;
            this.startFrame = startFrame;
            this.endFrame = endFrame;
            this.frameDuration = frameDuration;
            this.loops = loops;
        }
    }

    public SpriteAnimator(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.spriteNode = new Node("SpriteAnimatorNode");
        initializeGeometry();
    }

    /**
     * SIMPLIFIED: Initialize single geometry
     */
    private void initializeGeometry() {
        // Create initial quad
        Quad quad = new Quad(1f, 1f);
        spriteGeometry = new Geometry("SpriteQuad", quad);

        // Create material
        spriteMaterial = createSpriteMaterial();
        spriteGeometry.setMaterial(spriteMaterial);

        // Add to sprite node
        spriteNode.attachChild(spriteGeometry);
    }

    /**
     * Create material for sprite rendering
     */
    private Material createSpriteMaterial() {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        // Enable alpha blending
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.setFloat("AlphaDiscardThreshold", 0.1f);
        mat.setTransparent(true);

        // Set default color
        mat.setColor("Color", ColorRGBA.White);

        return mat;
    }

    /**
     * Load animation state from individual files
     */
    public void loadAnimationState(String animationName, String basePath, int frameCount) {

        Texture2D[] frames = new Texture2D[frameCount];

        for (int i = 0; i < frameCount; i++) {
            String filename = basePath + animationName + " (" + (i + 1) + ").png";

            try {
                Texture2D texture = (Texture2D) assetManager.loadTexture(filename);
                texture.setMagFilter(Texture.MagFilter.Nearest);
                texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                frames[i] = texture;

            } catch (Exception e) {
                System.err.println("Failed to load frame: " + filename);
                e.printStackTrace();
                return;
            }
        }

        // Store frames
        animationFrames.put(animationName, frames);

        // Create animation sequence
        float defaultFrameDuration = 0.1f;
        AnimationSequence sequence = new AnimationSequence(
                animationName, 0, frameCount, defaultFrameDuration, true
        );
        animations.put(animationName, sequence);

        // Auto-size quad based on first frame if no manual override
        if (!manualSizeOverride && frames.length > 0) {
            updateQuadSizeFromTexture(frames[0]);
        }

    }

    /**
     * Auto-size quad from texture dimensions
     */
    private void updateQuadSizeFromTexture(Texture2D texture) {
        float textureWidth = texture.getImage().getWidth();
        float textureHeight = texture.getImage().getHeight();

        float worldWidth = textureWidth * pixelToWorldScale;
        float worldHeight = textureHeight * pixelToWorldScale;

        currentQuadSize = new Vector2f(worldWidth, worldHeight);

        // Update geometry with new size
        updateGeometryQuad();}

    /**
     * Update quad geometry size
     */
    private void updateGeometryQuad() {
        Vector2f size = manualSizeOverride ? manualSize : currentQuadSize;
        if (size == null) return;

        // Create new quad with bottom-center pivot
        Quad newQuad = new Quad(size.x, size.y);
        spriteGeometry.setMesh(newQuad);

        // Move geometry so bottom edge is at y=0, centered on x=0
        spriteGeometry.setLocalTranslation(-size.x/2, 0, 0);
    }

    /**
     * Play animation by name
     */
    public void playAnimation(String animationName) {
        AnimationSequence anim = animations.get(animationName);
        if (anim == null) {
            System.err.println("Animation not found: " + animationName);
            return;
        }

        currentAnimation = anim;
        currentFrameIndex = anim.startFrame;
        animationTimer = 0f;
        isPlaying = true;

        // Set initial frame
        updateCurrentFrame();

    }

    /**
     * SIMPLIFIED: Update animation - just advance frames, no interpolation
     */
    public void update(float tpf) {
        if (!isPlaying || currentAnimation == null) {
            return;
        }

        // Update animation timer
        animationTimer += tpf;

        // Check for frame advance
        if (animationTimer >= currentAnimation.frameDuration) {
            advanceFrame();
            animationTimer = 0f;
        }
    }

    /**
     * Advance to next frame
     */
    private void advanceFrame() {
        currentFrameIndex++;

        if (currentFrameIndex >= currentAnimation.endFrame) {
            if (currentAnimation.loops) {
                currentFrameIndex = currentAnimation.startFrame;
            } else {
                // Animation finished
                isPlaying = false;
                return;
            }
        }

        updateCurrentFrame();
    }

    /**
     * SIMPLIFIED: Update current frame texture - no blending
     */
    private void updateCurrentFrame() {
        Texture2D[] frames = animationFrames.get(currentAnimation.name);
        if (frames == null || currentFrameIndex >= frames.length) return;

        // Simply set the texture - no complex blending
        spriteMaterial.setTexture("ColorMap", frames[currentFrameIndex]);
    }

    /**
     * Set manual quad size
     */
    public void setQuadSize(float width, float height) {
        this.manualSize = new Vector2f(width, height);
        this.manualSizeOverride = true;
        updateGeometryQuad();

    }

    /**
     * Get the sprite node for attachment
     */
    public Node getSpriteNode() {
        return spriteNode;
    }

    /**
     * Get current animation name
     */
    public String getCurrentAnimationName() {
        return currentAnimation != null ? currentAnimation.name : "none";
    }

    /**
     * Check if animation is playing
     */
    public boolean isAnimating() {
        return isPlaying;
    }

    /**
     * Get quad size for debugging
     */
    public Vector2f getQuadSize() {
        return manualSizeOverride ? manualSize : currentQuadSize;
    }

    /**
     * FIXED: Set animation speed
     */
    public void setAnimationSpeed(String animationName, float frameDuration) {
        AnimationSequence anim = animations.get(animationName);
        if (anim != null) {
            // Create new sequence with updated duration
            AnimationSequence newAnim = new AnimationSequence(
                    anim.name, anim.startFrame, anim.endFrame, frameDuration, anim.loops
            );
            animations.put(animationName, newAnim);

        }
    }
    public void loadAnimationState(String animationName, String basePath, int frameCount, boolean loops) {
        Texture2D[] frames = new Texture2D[frameCount];

        for (int i = 0; i < frameCount; i++) {
            String filename = basePath + animationName + " (" + (i + 1) + ").png";

            try {
                Texture2D texture = (Texture2D) assetManager.loadTexture(filename);
                texture.setMagFilter(Texture.MagFilter.Nearest);
                texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                frames[i] = texture;
            } catch (Exception e) {
                System.err.println("Failed to load frame: " + filename);
                e.printStackTrace();
                return;
            }
        }

        animationFrames.put(animationName, frames);

        float defaultFrameDuration = 0.1f;
        AnimationSequence sequence = new AnimationSequence(
                animationName, 0, frameCount, defaultFrameDuration, loops
        );
        animations.put(animationName, sequence);

        if (!manualSizeOverride && frames.length > 0) {
            updateQuadSizeFromTexture(frames[0]);
        }
    }

    public void setAnimationLooping(String animationName, boolean loops) {
        AnimationSequence existing = animations.get(animationName);
        if (existing != null) {
            AnimationSequence newSequence = new AnimationSequence(
                    existing.name, existing.startFrame, existing.endFrame,
                    existing.frameDuration, loops
            );
            animations.put(animationName, newSequence);
        }
    }

    /**
     * Stop current animation
     */
    public void stopAnimation() {
        isPlaying = false;
        currentAnimation = null;

    }

    /**
     * Set specific frame manually
     */
    public void setFrame(String animationName, int frameIndex) {
        Texture2D[] frames = animationFrames.get(animationName);
        if (frames != null && frameIndex >= 0 && frameIndex < frames.length) {
            spriteMaterial.setTexture("ColorMap", frames[frameIndex]);
            currentFrameIndex = frameIndex;
            isPlaying = false; // Stop any current animation
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        spriteNode.detachAllChildren();
        animationFrames.clear();
        animations.clear();
    }
}