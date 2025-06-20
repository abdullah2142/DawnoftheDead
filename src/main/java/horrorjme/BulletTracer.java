package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Cylinder;
import com.jme3.texture.Texture;

/**
 * Visible bullet tracer that flies through the air
 * Shows the bullet's path and impact point
 */
public class BulletTracer extends Entity {

    private AssetManager assetManager;
    private float bulletSpeed = 180f;        // Fast bullet speed
    private float maxRange = 100f;          // Maximum bullet travel distance
    private float travelDistance = 0f;      // How far bullet has traveled
    private float lifeTime = 2f;            // Maximum bullet lifetime
    private float currentLifeTime = 0f;     // Current age of bullet
    private boolean hasHit = false;         // Whether bullet hit something
    private Material bulletMaterial;
    private float originalAlpha = 1f;
    private float fadeOutTime = 0.5f;       // How long bullet takes to fade

    // Visual properties
    private static final float BULLET_LENGTH = 0.15f;  // Length of bullet trail
    private static final float BULLET_RADIUS = 0.02f;  // Thickness of bullet

    public BulletTracer(Vector3f startPosition, Vector3f direction, AssetManager assetManager) {
        super(EntityType.DECORATION, startPosition);
        this.assetManager = assetManager;
        this.boundingRadius = BULLET_RADIUS;

        // Normalize direction and set bullet velocity
        Vector3f bulletDirection = direction.normalize();
        this.velocity = bulletDirection.mult(bulletSpeed);

        System.out.println("Bullet tracer created - Speed: " + bulletSpeed + ", Direction: " + bulletDirection);
    }

    @Override
    public void initializeModel() {
        // Create a small cylinder for the bullet tracer
        Cylinder bulletCylinder = new Cylinder(4, 8, BULLET_RADIUS, BULLET_LENGTH, true);
        model = new Geometry("BulletTracer_" + entityId, bulletCylinder);

        // Create glowing bullet material
        bulletMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        try {
            // Try to load a bullet texture (optional)
            Texture bulletTexture = assetManager.loadTexture("Textures/Effects/bullet.png");
            bulletTexture.setMagFilter(Texture.MagFilter.Nearest);
            bulletTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            bulletMaterial.setTexture("ColorMap", bulletTexture);
        } catch (Exception e) {
            // Fallback to bright yellow/white color for visibility
            bulletMaterial.setColor("Color", new ColorRGBA(1f, 1f, 0.8f, 1f)); // Bright yellow-white
        }

        // Enable transparency for fade-out effect
        bulletMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        bulletMaterial.setTransparent(true);

        model.setMaterial(bulletMaterial);
        model.setLocalTranslation(position);

        // Orient bullet in direction of travel
        model.lookAt(position.add(velocity.normalize()), Vector3f.UNIT_Y);

        System.out.println("Bullet tracer model initialized");
    }

    @Override
    public void update(float tpf) {
        if (!active || destroyed) return;

        currentLifeTime += tpf;

        // Move bullet
        if (!hasHit) {
            Vector3f movement = velocity.mult(tpf);
            position.addLocal(movement);
            travelDistance += movement.length();

            // Update model position
            if (model != null) {
                model.setLocalTranslation(position);
            }

            // Check if bullet exceeded max range
            if (travelDistance >= maxRange) {
                hasHit = true;
                velocity.set(0, 0, 0); // Stop bullet
                System.out.println("Bullet reached max range: " + maxRange);
            }
        }

        // Start fading out when bullet hits or near end of life
        if (hasHit || currentLifeTime > lifeTime - fadeOutTime) {
            float fadeStartTime = hasHit ? currentLifeTime : (lifeTime - fadeOutTime);
            float fadeProgress = (currentLifeTime - fadeStartTime) / fadeOutTime;
            fadeProgress = Math.max(0f, Math.min(1f, fadeProgress));

            float alpha = originalAlpha * (1f - fadeProgress);

            // Update material alpha
            ColorRGBA currentColor = bulletMaterial.getParam("Color") != null ?
                    (ColorRGBA) bulletMaterial.getParam("Color").getValue() : ColorRGBA.White;
            bulletMaterial.setColor("Color", new ColorRGBA(currentColor.r, currentColor.g, currentColor.b, alpha));
        }

        // Remove bullet after lifetime
        if (currentLifeTime >= lifeTime) {
            destroy();
        }
    }

    @Override
    public void onCollision(Entity other) {
        // Bullet hit something
        if (!hasHit && other.getType() != EntityType.DECORATION) { // Don't hit other bullets/effects
            hasHit = true;
            velocity.set(0, 0, 0); // Stop bullet

            // Deal damage if it's an enemy
            if (other.getType() == EntityType.ENEMY) {
                other.takeDamage(25f); // Bullet damage
                System.out.println("Bullet hit enemy " + other.getEntityId() + " for 25 damage");
            }

            System.out.println("Bullet tracer hit: " + other.getEntityId());
        }
    }

    @Override
    public void onDestroy() {
        System.out.println("Bullet tracer " + entityId + " destroyed after traveling " +
                String.format("%.1f", travelDistance) + " units");
    }

    /**
     * Static factory method for easy creation from WeaponEffectsManager
     */
    public static BulletTracer createBulletTracer(Vector3f weaponPosition, Vector3f cameraDirection,
                                                  AssetManager assetManager) {
        // Calculate bullet spawn position (slightly forward from weapon)
        Vector3f bulletStartPos = weaponPosition.add(cameraDirection.mult(0.5f));

        return new BulletTracer(bulletStartPos, cameraDirection, assetManager);
    }

    // ==== CONFIGURATION METHODS ====

    /**
     * Set bullet speed (units per second)
     */
    public void setBulletSpeed(float speed) {
        this.bulletSpeed = Math.max(10f, speed);
        // Update current velocity if bullet is still flying
        if (!hasHit) {
            Vector3f direction = velocity.normalize();
            this.velocity = direction.mult(this.bulletSpeed);
        }
        System.out.println("Bullet speed set to: " + this.bulletSpeed);
    }

    /**
     * Set maximum bullet range
     */
    public void setMaxRange(float range) {
        this.maxRange = Math.max(5f, range);
        System.out.println("Bullet max range set to: " + this.maxRange);
    }

    /**
     * Set bullet lifetime (how long it exists)
     */
    public void setLifeTime(float time) {
        this.lifeTime = Math.max(0.1f, time);
        System.out.println("Bullet lifetime set to: " + this.lifeTime + " seconds");
    }

    /**
     * Set bullet color
     */
    public void setBulletColor(ColorRGBA color) {
        if (bulletMaterial != null) {
            bulletMaterial.setColor("Color", color);
            System.out.println("Bullet color set to: " + color);
        }
    }

    /**
     * Preset configurations for different bullet types
     */
    public void setBulletPreset(BulletPreset preset) {
        switch (preset) {
            case FAST_RIFLE:
                setBulletSpeed(120f);
                setMaxRange(150f);
                setLifeTime(2f);
                setBulletColor(new ColorRGBA(1f, 1f, 0.9f, 1f)); // Bright white
                break;

            case PISTOL_ROUND:
                setBulletSpeed(80f);
                setMaxRange(100f);
                setLifeTime(1.5f);
                setBulletColor(new ColorRGBA(1f, 1f, 0.8f, 1f)); // Slightly yellow
                break;

            case SHOTGUN_PELLET:
                setBulletSpeed(60f);
                setMaxRange(50f);
                setLifeTime(1f);
                setBulletColor(new ColorRGBA(1f, 0.9f, 0.7f, 1f)); // Warm white
                break;

            case TRACER_ROUND:
                setBulletSpeed(100f);
                setMaxRange(200f);
                setLifeTime(3f);
                setBulletColor(new ColorRGBA(1f, 0.3f, 0.1f, 1f)); // Bright red tracer
                break;
        }
        System.out.println("Applied bullet preset: " + preset);
    }

    public enum BulletPreset {
        FAST_RIFLE,      // High-velocity rifle round
        PISTOL_ROUND,    // Standard pistol bullet
        SHOTGUN_PELLET,  // Slower, shorter range pellet
        TRACER_ROUND     // Visible tracer ammunition
    }

    // ==== STATUS GETTERS ====

    public boolean hasHitTarget() {
        return hasHit;
    }

    public float getTravelDistance() {
        return travelDistance;
    }

    public float getRemainingRange() {
        return Math.max(0f, maxRange - travelDistance);
    }

    public float getBulletSpeed() {
        return bulletSpeed;
    }

    public Vector3f getCurrentVelocity() {
        return velocity.clone();
    }
}