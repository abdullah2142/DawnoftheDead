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
 * Enhanced bullet tracer with improved damage detection and raycast collision
 * FIXED: Better collision detection for fast-moving bullets to prevent tunneling
 */
public class BulletTracer extends Entity {

    private AssetManager assetManager;
    private EntityManager entityManager; // ADDED: Reference for improved collision detection

    private float bulletSpeed = 80f;        // Reduced from 3800f for better collision detection
    private float bulletDamage = 25f;       // ADDED: Configurable damage
    private float maxRange = 100f;
    private float travelDistance = 0f;
    private float lifeTime = 2f;
    private float currentLifeTime = 0f;
    private boolean hasHit = false;
    private Material bulletMaterial;
    private float originalAlpha = 1f;
    private float fadeOutTime = 0.5f;

    // ADDED: For raycast collision detection
    private Vector3f previousPosition;
    private boolean useRaycastCollision = true;

    // Visual properties
    private static final float BULLET_LENGTH = 0.03f;
    private static final float BULLET_RADIUS = 0.03f;  // INCREASED: Better collision detection

    public BulletTracer(Vector3f startPosition, Vector3f direction, AssetManager assetManager) {
        super(EntityType.DECORATION, startPosition);
        this.assetManager = assetManager;
        this.boundingRadius = BULLET_RADIUS * 1f; // INCREASED: Better collision detection
        this.previousPosition = startPosition.clone(); // ADDED: Track previous position

        // Normalize direction and set bullet velocity
        Vector3f bulletDirection = direction.normalize();
        this.velocity = bulletDirection.mult(bulletSpeed);
    }

    // ADDED: Constructor with EntityManager reference for improved collision
    public BulletTracer(Vector3f startPosition, Vector3f direction, AssetManager assetManager, EntityManager entityManager) {
        this(startPosition, direction, assetManager);
        this.entityManager = entityManager;
    }

    @Override
    public void initializeModel() {
        // Create a small cylinder for the bullet tracer
        Cylinder bulletCylinder = new Cylinder(2, 3, BULLET_RADIUS, BULLET_LENGTH, true);
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
    }

    @Override
    public void update(float tpf) {
        if (!active || destroyed) return;

        currentLifeTime += tpf;

        // Move bullet
        if (!hasHit) {
            // ADDED: Store previous position for raycast collision
            previousPosition.set(position);

            Vector3f movement = velocity.mult(tpf);
            position.addLocal(movement);
            travelDistance += movement.length();

            // ADDED: Check for raycast collision if moving fast
            if (useRaycastCollision && entityManager != null) {
                checkRaycastCollision();
            }

            // Update model position
            if (model != null) {
                model.setLocalTranslation(position);
            }

            // Check if bullet exceeded max range
            if (travelDistance >= maxRange) {
                hasHit = true;
                velocity.set(0, 0, 0); // Stop bullet
                System.out.println("DEBUG: Bullet " + entityId + " reached max range: " + maxRange);
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

    /**
     * ADDED: Improved collision detection using raycast to prevent tunneling
     */
    private void checkRaycastCollision() {
        if (hasHit || entityManager == null) return;

        // Check all entities for collision along bullet path
        for (Entity entity : entityManager.getAllEntities()) {
            if (entity == this || !entity.isActive() || entity.isDestroyed()) {
                continue;
            }

            // Skip other decoration entities (bullets, effects, etc.)
            if (entity.getType() == EntityType.DECORATION) {
                continue;
            }

            // Check if bullet path intersects with entity
            if (checkLineIntersection(previousPosition, position, entity)) {
                // Hit detected!
                handleCollisionWithEntity(entity);
                return; // Stop checking after first hit
            }
        }
    }

    /**
     * ADDED: Check if line segment intersects with entity's bounding sphere
     */
    private boolean checkLineIntersection(Vector3f lineStart, Vector3f lineEnd, Entity entity) {
        Vector3f entityPos = entity.getPosition();
        float entityRadius = entity.getBoundingRadius();

        // Vector from line start to entity center
        Vector3f toEntity = entityPos.subtract(lineStart);

        // Line direction vector
        Vector3f lineDir = lineEnd.subtract(lineStart);
        float lineLength = lineDir.length();

        if (lineLength < 0.001f) return false; // No movement

        lineDir.normalizeLocal();

        // Project entity center onto line
        float projection = toEntity.dot(lineDir);

        // Clamp projection to line segment
        projection = Math.max(0f, Math.min(lineLength, projection));

        // Find closest point on line to entity center
        Vector3f closestPoint = lineStart.add(lineDir.mult(projection));

        // Check distance from entity center to closest point
        float distance = closestPoint.distance(entityPos);

        return distance <= entityRadius;
    }

    /**
     * ADDED: Handle collision with specific entity
     */
    private void handleCollisionWithEntity(Entity entity) {
        if (hasHit) return;

        hasHit = true;
        velocity.set(0, 0, 0); // Stop bullet

        System.out.println("DEBUG: Bullet " + entityId + " hit " + entity.getEntityId());

        // Deal damage based on entity type
        if (entity.getType() == EntityType.ENEMY) {
            System.out.println("DEBUG: Dealing " + bulletDamage + " damage to " + entity.getEntityId());
            entity.takeDamage(bulletDamage);

            // ADDED: Visual feedback for successful hit
            createHitEffect(entity);
        }

        // ADDED: Mark bullet for quick destruction after hit
        currentLifeTime = lifeTime - fadeOutTime; // Start fading immediately
    }

    /**
     * ADDED: Create visual effect when bullet hits target
     */
    private void createHitEffect(Entity target) {
        // Change bullet color to indicate hit
        if (bulletMaterial != null) {
            bulletMaterial.setColor("Color", new ColorRGBA(1f, 0.2f, 0.2f, 1f)); // Red hit color
        }

        // In a full implementation, you could spawn particles, blood effects, etc.
        System.out.println("EFFECT: Hit effect created for " + target.getEntityId());
    }

    @Override
    public void onCollision(Entity other) {
        // This method is called by EntityManager's collision system
        // We now also have raycast collision, but keep this as backup

        if (!hasHit && other.getType() != EntityType.DECORATION) {
            System.out.println("DEBUG: Standard collision - Bullet " + entityId + " hit " + other.getEntityId());
            handleCollisionWithEntity(other);
        }
    }

    @Override
    public void onDestroy() {
        System.out.println("DEBUG: Bullet " + entityId + " destroyed after traveling " +
                String.format("%.1f", travelDistance) + " units");
    }

    /**
     * UPDATED: Static factory method with EntityManager reference
     */
    public static BulletTracer createBulletTracer(Vector3f weaponPosition, Vector3f cameraDirection,
                                                  AssetManager assetManager, EntityManager entityManager) {
        // Calculate bullet spawn position (slightly forward from weapon)
        Vector3f bulletStartPos = weaponPosition.add(cameraDirection.mult(0.5f));

        return new BulletTracer(bulletStartPos, cameraDirection, assetManager, entityManager);
    }

    /**
     * Original factory method (for backward compatibility)
     */
    public static BulletTracer createBulletTracer(Vector3f weaponPosition, Vector3f cameraDirection,
                                                  AssetManager assetManager) {
        Vector3f bulletStartPos = weaponPosition.add(cameraDirection.mult(0.5f));
        return new BulletTracer(bulletStartPos, cameraDirection, assetManager);
    }

    // ==== ENHANCED CONFIGURATION METHODS ====

    /**
     * ADDED: Set bullet damage
     */
    public void setBulletDamage(float damage) {
        this.bulletDamage = Math.max(1f, damage);
    }

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
    }

    /**
     * Set maximum bullet range
     */
    public void setMaxRange(float range) {
        this.maxRange = Math.max(5f, range);
    }

    /**
     * Set bullet lifetime (how long it exists)
     */
    public void setLifeTime(float time) {
        this.lifeTime = Math.max(0.1f, time);
    }

    /**
     * Set bullet color
     */
    public void setBulletColor(ColorRGBA color) {
        if (bulletMaterial != null) {
            bulletMaterial.setColor("Color", color);
        }
    }

    /**
     * ADDED: Enable/disable raycast collision detection
     */
    public void setRaycastCollisionEnabled(boolean enabled) {
        this.useRaycastCollision = enabled;
    }

    /**
     * UPDATED: Preset configurations with damage values
     */
    public void setBulletPreset(BulletPreset preset) {
        switch (preset) {
            case FAST_RIFLE:
                setBulletSpeed(120f);
                setBulletDamage(35f);       // ADDED: High damage
                setMaxRange(150f);
                setLifeTime(2f);
                setBulletColor(new ColorRGBA(1f, 1f, 0.9f, 1f));
                break;

            case PISTOL_ROUND:
                setBulletSpeed(80f);
                setBulletDamage(25f);       // ADDED: Medium damage
                setMaxRange(100f);
                setLifeTime(1.5f);
                setBulletColor(new ColorRGBA(1f, 1f, 0.8f, 1f));
                break;

            case SHOTGUN_PELLET:
                setBulletSpeed(60f);
                setBulletDamage(15f);       // ADDED: Lower damage per pellet
                setMaxRange(50f);
                setLifeTime(1f);
                setBulletColor(new ColorRGBA(1f, 0.9f, 0.7f, 1f));
                break;

            case TRACER_ROUND:
                setBulletSpeed(100f);
                setBulletDamage(30f);       // ADDED: Good damage
                setMaxRange(200f);
                setLifeTime(3f);
                setBulletColor(new ColorRGBA(1f, 0.3f, 0.1f, 1f));
                break;
        }
    }

    public enum BulletPreset {
        FAST_RIFLE,      // High-velocity, high-damage rifle round
        PISTOL_ROUND,    // Standard pistol bullet
        SHOTGUN_PELLET,  // Shorter range, moderate damage pellet
        TRACER_ROUND     // Visible tracer ammunition with good damage
    }

    // ==== ENHANCED STATUS GETTERS ====

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

    public float getBulletDamage() {
        return bulletDamage;
    }

    public Vector3f getCurrentVelocity() {
        return velocity.clone();
    }

    public boolean isRaycastCollisionEnabled() {
        return useRaycastCollision;
    }

    /**
     * ADDED: Get debug information about bullet state
     */
    public String getDebugInfo() {
        return String.format("Bullet %s: Speed=%.1f, Damage=%.1f, Range=%.1f/%.1f, Hit=%s, Age=%.2fs",
                entityId, bulletSpeed, bulletDamage, travelDistance, maxRange, hasHit, currentLifeTime);
    }
}