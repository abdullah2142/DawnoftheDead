package horrorjme;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Base class for all game entities (enemies, items, pickups, etc.)
 * Updated with bounding height for better collision detection
 */
public abstract class Entity {

    public enum EntityType {
        ENEMY,
        ITEM,
        PICKUP,
        DECORATION,
        TRIGGER
    }

    protected Vector3f position;
    protected Vector3f velocity;
    protected float health;
    protected float maxHealth;
    protected boolean active;
    protected boolean destroyed;
    protected EntityType type;
    protected Spatial model;
    protected Node parentNode;

    protected float boundingRadius;
    protected float boundingHeight;  // ADDED: Height for better collision detection

    // Unique identifier
    protected String entityId;
    private static int nextId = 0;

    public Entity(EntityType type, Vector3f position) {
        this.type = type;
        this.position = position.clone();
        this.velocity = new Vector3f(0, 0, 0);
        this.active = true;
        this.destroyed = false;
        this.health = 100f;
        this.maxHealth = 100f;

        // Default bounding volume
        this.boundingRadius = 0.5f;
        this.boundingHeight = 1.0f;  // ADDED: Default height

        this.entityId = type.name() + "_" + (nextId++);
    }

    /**
     * Called every frame to update entity logic
     */
    public abstract void update(float tpf);

    /**
     * Called when this entity collides with another entity
     */
    public abstract void onCollision(Entity other);

    /**
     * Called when this entity is destroyed
     */
    public abstract void onDestroy();

    /**
     * Initialize the entity's visual representation
     */
    public abstract void initializeModel();

    /**
     * Basic movement update - can be overridden
     */
    protected void updateMovement(float tpf) {
        if (velocity.lengthSquared() > 0) {
            position.addLocal(velocity.x * tpf, velocity.y * tpf, velocity.z * tpf);

            if (model != null) {
                model.setLocalTranslation(position);
            }
        }
    }

    /**
     * UPDATED: Check collision using radius + height instead of just radius
     */
    public boolean collidesWith(Entity other) {
        if (other == null || !other.isActive() || other.isDestroyed()) {
            return false;
        }

        // Check horizontal distance (2D, ignoring Y)
        float dx = this.position.x - other.position.x;
        float dz = this.position.z - other.position.z;
        float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);
        float combinedRadius = this.boundingRadius + other.getBoundingRadius();

        if (horizontalDistance >= combinedRadius) {
            return false; // Too far apart horizontally
        }

        // Check vertical overlap
        float thisBottom = this.position.y;
        float thisTop = this.position.y + this.boundingHeight;
        float otherBottom = other.position.y;
        float otherTop = other.position.y + other.boundingHeight;

        // Entities overlap if: thisBottom < otherTop AND otherBottom < thisTop
        return thisBottom < otherTop && otherBottom < thisTop;
    }

    /**
     * Damage this entity
     */
    public void takeDamage(float damage) {
        health -= damage;
        if (health <= 0) {
            destroy();
        }
    }

    /**
     * Heal this entity
     */
    public void heal(float amount) {
        health = Math.min(maxHealth, health + amount);
    }

    /**
     * Mark entity for destruction
     */
    public void destroy() {
        destroyed = true;
        active = false;
        onDestroy();

        if (model != null && parentNode != null) {
            parentNode.detachChild(model);
        }
    }

    /**
     * Attach entity to scene graph
     */
    public void attachToScene(Node parent) {
        this.parentNode = parent;
        if (model != null) {
            parent.attachChild(model);
        }
    }

    /**
     * Detach entity from scene graph
     */
    public void detachFromScene() {
        if (model != null && parentNode != null) {
            parentNode.detachChild(model);
        }
        parentNode = null;
    }

    // Getters and Setters

    public Vector3f getPosition() { return position.clone(); }
    public void setPosition(Vector3f position) {
        this.position.set(position);
        if (model != null) {
            model.setLocalTranslation(position);
        }
    }

    public Vector3f getVelocity() { return velocity.clone(); }
    public void setVelocity(Vector3f velocity) { this.velocity.set(velocity); }

    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = Math.max(0, Math.min(maxHealth, health)); }

    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
        if (health > maxHealth) {
            health = maxHealth;
        }
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isDestroyed() { return destroyed; }

    public EntityType getType() { return type; }

    public String getEntityId() { return entityId; }

    public Spatial getModel() { return model; }

    // Bounding volume getters and setters
    public float getBoundingRadius() { return boundingRadius; }
    public void setBoundingRadius(float radius) {
        this.boundingRadius = Math.max(0.1f, radius);
    }

    public float getBoundingHeight() { return boundingHeight; }  // ADDED
    public void setBoundingHeight(float height) {               // ADDED
        this.boundingHeight = Math.max(0.1f, height);
    }

    public Node getParentNode() { return parentNode; }
}