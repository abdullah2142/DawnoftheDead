package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;

/**
 * Enhanced PickupEntity with 3D rotating boxes
 * All pickups now use textured 3D boxes that rotate continuously
 */
public abstract class PickupEntity extends Entity {

    protected AssetManager assetManager;
    protected AudioManager audioManager;

    // Visual effects
    protected PointLight glowLight;
    protected ColorRGBA glowColor;
    protected float glowIntensity = 8f;
    protected float glowRadius = 4f;

    // Animation
    protected float bobTimer = 0f;
    protected float bobSpeed = 2f;
    protected float bobHeight = 0.3f;
    protected Vector3f basePosition;

    // NEW: Rotation animation
    protected float rotationSpeed = 1.5f; // Rotation speed in radians per second
    protected Vector3f rotationAxis = new Vector3f(0, 1, 0); // Rotate around Y axis
    protected float rotationTimer = 0f;

    // Pickup mechanics
    protected float pickupRadius = 1.2f;
    protected boolean pickedUp = false;

    // Visual backup
    protected boolean usingFallbackVisual = false;

    // NEW: Box dimensions for different pickup types
    protected Vector3f boxSize = new Vector3f(0.5f, 0.5f, 0.5f);

    public PickupEntity(EntityType type, Vector3f position, AssetManager assetManager, AudioManager audioManager) {
        super(type, position);
        this.assetManager = assetManager;
        this.audioManager = audioManager;
        this.basePosition = position.clone();
        this.boundingRadius = pickupRadius;
    }

    @Override
    public void initializeModel() {
        // Create container node for pickup
        model = new Node("Pickup_" + entityId);

        // Create 3D visual representation
        create3DVisualModel();

        // Create glow effect
        createGlowEffect();

        // Position at base location
        model.setLocalTranslation(basePosition);
    }

    /**
     * Create 3D visual model - implemented by subclasses to set texture path and box size
     */
    protected abstract void create3DVisualModel();

    /**
     * NEW: Create a 3D textured box with rotation
     */
    protected Geometry create3DTexturedBox(String name, String texturePath, Vector3f size, ColorRGBA fallbackColor) {
        // Create 3D box geometry
        Box box = new Box(size.x, size.y, size.z);
        Geometry boxGeom = new Geometry(name, box);

        // Create material
        Material boxMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

        try {
            // Try to load texture
            Texture texture = assetManager.loadTexture(texturePath);
            texture.setMagFilter(Texture.MagFilter.Nearest);
            texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            texture.setWrap(Texture.WrapMode.Repeat); // Allow texture wrapping for box faces

            // Apply texture to diffuse map
            boxMat.setTexture("DiffuseMap", texture);
            boxMat.setColor("Diffuse", ColorRGBA.White);
            boxMat.setColor("Ambient", ColorRGBA.White.mult(0.3f));

            usingFallbackVisual = false;
            System.out.println("Successfully loaded texture for " + name + ": " + texturePath);

        } catch (Exception e) {
            // Fallback to solid color
            boxMat.setColor("Diffuse", fallbackColor);
            boxMat.setColor("Ambient", fallbackColor.mult(0.3f));
            usingFallbackVisual = true;
            System.out.println("Using fallback color for " + name + " - texture not found: " + texturePath);
        }

        // Enable proper lighting
        boxMat.setBoolean("UseMaterialColors", true);

        boxGeom.setMaterial(boxMat);

        return boxGeom;
    }

    /**
     * DEPRECATED: Keep for backward compatibility but redirect to 3D version
     */
    @Deprecated
    protected Geometry createTexturedQuad(String name, String texturePath, float width, float height, ColorRGBA fallbackColor) {
        System.out.println("WARNING: createTexturedQuad is deprecated, using 3D box instead");
        return create3DTexturedBox(name, texturePath, new Vector3f(width/2, height/2, 0.2f), fallbackColor);
    }

    /**
     * Create fallback 3D box visual (same as before but with rotation support)
     */
    protected Geometry createFallback3DBox(String name, Vector3f size, ColorRGBA color) {
        Box fallbackBox = new Box(size.x, size.y, size.z);
        Geometry fallbackGeom = new Geometry(name + "_fallback", fallbackBox);

        Material fallbackMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        fallbackMat.setColor("Diffuse", color);
        fallbackMat.setColor("Ambient", color.mult(0.3f));
        fallbackMat.setBoolean("UseMaterialColors", true);

        fallbackGeom.setMaterial(fallbackMat);
        usingFallbackVisual = true;

        return fallbackGeom;
    }

    /**
     * DEPRECATED: Keep for backward compatibility but redirect to 3D version
     */
    @Deprecated
    protected Geometry createFallbackBox(String name, Vector3f size, ColorRGBA color) {
        return createFallback3DBox(name, size, color);
    }

    /**
     * Create glow light effect
     */
    protected void createGlowEffect() {
        glowLight = new PointLight();
        glowLight.setPosition(basePosition);
        glowLight.setColor(glowColor.mult(glowIntensity));
        glowLight.setRadius(glowRadius);

        // Add light to the model node so it moves with the pickup
        if (model instanceof Node) {
            ((Node) model).addLight(glowLight);
        }
    }

    @Override
    public void update(float tpf) {
        if (!active || destroyed || pickedUp) return;

        // Update bobbing animation
        bobTimer += tpf * bobSpeed;
        float bobOffset = FastMath.sin(bobTimer) * bobHeight;

        // Position box so its bottom sits on ground (add half the box height)
        Vector3f newPosition = basePosition.add(0, bobOffset + boxSize.y, 0);
        position.set(newPosition);

        if (model != null) {
            model.setLocalTranslation(position);
        }

        // NEW: Update rotation animation
        updateRotation(tpf);

        // Update glow light position
        if (glowLight != null) {
            glowLight.setPosition(position);
        }

        // Animate glow intensity slightly
        if (glowLight != null) {
            float glowPulse = 0.8f + 0.2f * FastMath.sin(bobTimer * 1.5f);
            glowLight.setColor(glowColor.mult(glowIntensity * glowPulse));
        }
    }

    /**
     * NEW: Update rotation animation for 3D boxes
     */
    protected void updateRotation(float tpf) {
        rotationTimer += tpf * rotationSpeed;

        // Create rotation quaternion around the specified axis
        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(rotationTimer, rotationAxis);

        // Apply rotation to the model
        if (model != null) {
            model.setLocalRotation(rotation);
        }
    }

    /**
     * Check if player is close enough to pick up
     */
    public boolean isPlayerInRange(Player player) {
        if (player == null || pickedUp) return false;

        float distance = position.distance(player.getPosition());
        return distance <= pickupRadius;
    }

    /**
     * Attempt pickup by player
     */
    public boolean attemptPickup(Player player) {
        if (!isPlayerInRange(player) || pickedUp) {
            return false;
        }

        // Let subclass handle the actual pickup logic
        boolean success = handlePickup(player);

        if (success) {
            pickedUp = true;

            // Play pickup sound
            if (audioManager != null) {
                String soundName = getPickupSound();
                if (soundName != null) {
                    audioManager.playSoundEffect(soundName);
                }
            }

            // Mark for destruction
            destroy();
        }

        return success;
    }

    @Override
    public void onCollision(Entity other) {
        // Pickups use range-based detection, not collision
    }

    @Override
    public void onDestroy() {
        // Remove glow light when destroyed
        if (glowLight != null && model instanceof Node) {
            ((Node) model).removeLight(glowLight);
        }
    }

    // NEW: Configuration methods for rotation
    public void setRotationSpeed(float speed) {
        this.rotationSpeed = speed;
    }

    public void setRotationAxis(Vector3f axis) {
        this.rotationAxis = axis.normalize();
    }

    public void setRotationAxis(float x, float y, float z) {
        this.rotationAxis.set(x, y, z).normalizeLocal();
    }

    // NEW: Set box size for different pickup types
    protected void setBoxSize(float width, float height, float depth) {
        this.boxSize.set(width, height, depth);
    }

    protected void setBoxSize(Vector3f size) {
        this.boxSize.set(size);
    }

    // Configuration methods
    public void setGlowColor(ColorRGBA color) {
        this.glowColor = color.clone();
        if (glowLight != null) {
            glowLight.setColor(glowColor.mult(glowIntensity));
        }
    }

    public void setGlowIntensity(float intensity) {
        this.glowIntensity = intensity;
        if (glowLight != null) {
            glowLight.setColor(glowColor.mult(glowIntensity));
        }
    }

    public void setBobParameters(float speed, float height) {
        this.bobSpeed = speed;
        this.bobHeight = height;
    }

    public void setPickupRadius(float radius) {
        this.pickupRadius = radius;
        this.boundingRadius = radius;
    }

    // Getters
    public boolean isPickedUp() { return pickedUp; }
    public boolean isUsingFallbackVisual() { return usingFallbackVisual; }
    public ColorRGBA getGlowColor() { return glowColor.clone(); }
    public float getRotationSpeed() { return rotationSpeed; }
    public Vector3f getRotationAxis() { return rotationAxis.clone(); }

    // Abstract methods that subclasses must implement
    protected abstract String getPickupSound();
    protected abstract boolean handlePickup(Player player);
}