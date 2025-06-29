package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector2f;    // For quadSize
import com.jme3.math.Vector3f;   // For spriteOffset
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;      // For spriteNode
/**
 * Enhanced ZombieEnemy with sprite scaling and positioning controls
 */
public class ZombieEnemy extends SpriteEntity {

    // Player tracking
    private Vector3f playerPosition;

    // AI properties
    private float speed = 1.5f;
    private float detectionRange = 5f;
    private float attackRange = 2f;
    private float attackDamage = 10f;
    private float lastAttackTime = 0f;
    private float attackCooldown = 2f;

    private boolean dropProcessed = false;

    private float deathTimer = 0f;
    private static final float DEATH_ANIMATION_TIME = 1.2f;

    // ADDED: Sprite appearance settings
    private float spriteScale = 0.5f;           // Scale factor for zombie sprites (0.8 = 80% size)
    private Vector3f spriteOffset = new Vector3f(0f, 0, 0); // Offset for fine-tuning position

    public boolean hasProcessedDrop() {
        return dropProcessed;
    }

    public void setDropProcessed(boolean processed) {
        this.dropProcessed = processed;
        if (processed) {
            System.out.println("Zombie " + entityId + " drop marked as processed");
        }
    }

    private void adjustSpritePosition() {
        if (spriteAnimator != null) {
            Vector2f quadSize = spriteAnimator.getQuadSize();
            if (quadSize != null) {
                Node spriteNode = spriteAnimator.getSpriteNode();

                // Manual fine-tuning per zombie type
                // Format: (x-offset, y-offset, z-offset)
                spriteNode.setLocalTranslation(spriteOffset.x, spriteOffset.y, spriteOffset.z);

            }
        }
    }

    // AI states
    public enum ZombieState {
        IDLE,
        WALKING,
        ATTACKING,
        DEAD
    }

    private ZombieState currentState = ZombieState.IDLE;
    private ZombieState previousState = ZombieState.IDLE;

    // Movement
    private Vector3f desiredDirection = new Vector3f();

    public ZombieEnemy(Vector3f position, AssetManager assetManager, Camera camera, BulletAppState bulletAppState) {
        super(EntityType.ENEMY, position, assetManager, camera, bulletAppState);

        // Configure zombie properties
        this.health = 50f;
        this.maxHealth = 50f;
        this.boundingRadius = 1.8f;
        setBoundingHeight(3.0f);

        // Configure collision size (radius, height)
        setCollisionSize(1.0f, 4.5f); //zombie capsule

        // TEMPORARY DEBUG: Enable to see physics capsules
        // enablePhysicsDebugVisualization();  // ← Uncomment this line to see capsules

    }

    @Override
    protected void loadAnimations() {

        // Load all zombie animation states
        String basePath = "Textures/Zombies/";

        try {
            spriteAnimator.loadAnimationState("Idle", basePath, 15);
            spriteAnimator.loadAnimationState("Walk", basePath, 10);
            spriteAnimator.loadAnimationState("Attack", basePath, 8);
            spriteAnimator.loadAnimationState("Dead", basePath, 12);

            // ADDED: Apply zombie-specific scaling after loading animations
            applySpriteScaling();

            // Start with idle animation
            playAnimation("Idle");

        } catch (Exception e) {
            System.err.println("Failed to load zombie animations: " + e.getMessage());
            // Continue without animations
        }
    }

    /**
     * ADDED: Apply zombie-specific sprite scaling
     */
    private void applySpriteScaling() {
        if (spriteAnimator != null) {
            // Get current auto-sized dimensions
            var currentSize = spriteAnimator.getQuadSize();
            if (currentSize != null) {
                // Apply zombie scale factor
                float newWidth = currentSize.x * spriteScale;
                float newHeight = currentSize.y * spriteScale;

                spriteAnimator.setQuadSize(newWidth, newHeight);}

            // Apply any position offset for fine-tuning
            if (spriteOffset.lengthSquared() > 0) {
                var spriteNode = spriteAnimator.getSpriteNode();
                spriteNode.setLocalTranslation(spriteOffset);

            }
        }
    }

    @Override
    protected void updateBehavior(float tpf) {
        // Handle death state specially
        if (currentState == ZombieState.DEAD) {
            deathTimer += tpf;
            desiredDirection.set(0, 0, 0);
            velocity.set(0, 0, 0);

            // Destroy zombie after death animation has had time to play
            if (deathTimer >= DEATH_ANIMATION_TIME) {
                if (!destroyed) {
                    System.out.println("Zombie " + entityId + " death animation complete - destroying");
                    destroy();
                }
            }
            return; // Don't do normal AI when dead
        }

        // Normal behavior for living zombies
        lastAttackTime += tpf;
        updateAI(tpf);
        updateAnimation();
        velocity.set(desiredDirection.mult(speed));
    }

    /**
     * Simple AI state machine
     */
    private void updateAI(float tpf) {
        if (currentState == ZombieState.DEAD) {
            desiredDirection.set(0, 0, 0);
            return;
        }

        if (playerPosition == null) {
            currentState = ZombieState.IDLE;
            desiredDirection.set(0, 0, 0);
            return;
        }

        float distanceToPlayer = position.distance(playerPosition);

        switch (currentState) {
            case IDLE:
                desiredDirection.set(0, 0, 0);

                if (distanceToPlayer <= detectionRange) {
                    currentState = ZombieState.WALKING;

                }
                break;

            case WALKING:
                if (distanceToPlayer <= attackRange) {
                    currentState = ZombieState.ATTACKING;
                    desiredDirection.set(0, 0, 0);
                } else if (distanceToPlayer > detectionRange * 1.5f) {
                    // Lost player
                    currentState = ZombieState.IDLE;
                    desiredDirection.set(0, 0, 0);
                } else {
                    // Move towards player
                    Vector3f direction = playerPosition.subtract(position).normalizeLocal();
                    direction.y = 0; // Keep movement on ground plane
                    desiredDirection.set(direction);
                }
                break;

            case ATTACKING:
                desiredDirection.set(0, 0, 0);

                if (distanceToPlayer > attackRange) {
                    currentState = ZombieState.WALKING;
                } else {
                    // Attack if cooldown is ready
                    if (lastAttackTime >= attackCooldown) {
                        performAttack();
                        lastAttackTime = 0f;
                    }
                }
                break;
        }
    }

    /**
     * Update animation based on current state
     */
    private void updateAnimation() {
        if (currentState != previousState) {
            String newAnimation = getAnimationForState(currentState);

            if (!newAnimation.equals(getCurrentAnimation())) {
                playAnimation(newAnimation);

            }

            previousState = currentState;
        }
    }

    /**
     * Get animation name for AI state
     */
    private String getAnimationForState(ZombieState state) {
        switch (state) {
            case IDLE:
                return "Idle";
            case WALKING:
                return "Walk";
            case ATTACKING:
                return "Attack";
            case DEAD:
                return "Dead";
            default:
                return "Idle";
        }
    }

    /**
     * Perform attack action
     */
    private void performAttack() {

        // Trigger attack animation if not already playing
        if (!isAnimating() || !getCurrentAnimation().equals("Attack")) {
            playAnimation("Attack");
        }

        // In a real game, you would apply damage to player here
        // For now, just log the attack
    }

    @Override
    public void onCollision(Entity other) {
        // Kinematic bodies handle collision automatically through physics
        // We don't need complex collision logic here

        if (other.getType() == EntityType.ENEMY) {
            // Other zombies - physics handles this

        }
    }

    @Override
    public void takeDamage(float damage) {
        if (currentState == ZombieState.DEAD) return;

        // Check if this damage would kill the zombie BEFORE calling super.takeDamage
        boolean willDie = (health - damage) <= 0;

        if (willDie) {
            // Handle death without calling super.takeDamage (which would call destroy())
            health = 0;
            currentState = ZombieState.DEAD;
            playAnimation("Dead");

            // Stop movement
            desiredDirection.set(0, 0, 0);
            velocity.set(0, 0, 0);

            System.out.println("Zombie " + entityId + " is dying - playing death animation");
        } else {
            // Normal damage - won't kill the zombie
            super.takeDamage(damage);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    // ==== SPRITE APPEARANCE METHODS ====

    /**
     * Set zombie sprite scale (1.0 = normal size, 0.5 = half size, 2.0 = double size)
     */
    public void setSpriteScale(float scale) {
        this.spriteScale = Math.max(0.1f, scale); // Minimum 10% size

        // If animations are already loaded, reapply scaling
        if (spriteAnimator != null) {
            applySpriteScaling();
        }

    }
    public void destroy() {
        // Only allow destruction if we're already in death state and timer has elapsed
        if (currentState == ZombieState.DEAD && deathTimer >= DEATH_ANIMATION_TIME) {
            System.out.println("Zombie " + entityId + " final destruction");
            super.destroy();
        } else if (currentState != ZombieState.DEAD) {
            // Allow immediate destruction if not dying (e.g., game cleanup)
            super.destroy();
        }
        // Otherwise, death in progress - don't destroy yet
    }

    /**
     * Set sprite position offset for fine-tuning alignment
     */
    public void setSpriteOffset(float x, float y, float z) {
        this.spriteOffset.set(x, y, z);

        // Apply immediately if sprite exists
        if (spriteAnimator != null) {
            var spriteNode = spriteAnimator.getSpriteNode();
            spriteNode.setLocalTranslation(spriteOffset);
        }

    }

    /**
     * Quick preset for small zombies
     */
    public void makeSmallZombie() {
        setSpriteScale(0.6f);           // 60% normal size
        setSpriteOffset(0, 0.1f, 0);    // Slightly raised
        setCollisionSize(0.3f, 1.4f);   // Smaller collision

    }

    /**
     * Quick preset for large zombies
     */
    public void makeLargeZombie() {
        setSpriteScale(1.4f);           // 140% normal size
        setSpriteOffset(0, -0.1f, 0);   // Slightly lowered
        setCollisionSize(0.5f, 2.2f);   // Larger collision
        setAttackDamage(15f);           // More damage
        setDetectionRange(6f);          // Better detection

    }

    /**
     * Quick preset for skinny/fast zombies
     */
    public void makeFastZombie() {
        setSpriteScale(0.8f);           // Slightly smaller
        setSpriteOffset(0, 0.05f, 0);   // Slightly raised
        setSpeed(2.5f);                 // Much faster
        setCollisionSize(0.3f, 1.8f);   // Thinner collision

    }

    // ==== PUBLIC API ====

    /**
     * Set player position for AI tracking
     */
    public void setPlayerPosition(Vector3f playerPos) {
        if (playerPos != null) {
            this.playerPosition = playerPos.clone();
        }
    }

    /**
     * Get debug status string
     */
    public String getDebugStatus() {
        String baseStatus = String.format("Zombie %s: State=%s, Anim=%s, Health=%.1f, Scale=%.2f",
                entityId, currentState, getCurrentAnimation(), health, spriteScale);

        String distanceStatus = "";
        if (playerPosition != null) {
            float distance = position.distance(playerPosition);
            distanceStatus = String.format(", PlayerDist=%.1f", distance);
        }

        String movementStatus = String.format(", Speed=%.1f", desiredDirection.length() * speed);
        String physicsStatus = isMovementBlocked() ? " [BLOCKED]" : "";

        return baseStatus + distanceStatus + movementStatus + physicsStatus;
    }

    // Check if movement is blocked (method needs to be implemented in SpriteEntity if not exists)
    private boolean isMovementBlocked() {
        // Placeholder - implement in SpriteEntity if needed
        return false;
    }

    // ==== CONFIGURATION METHODS ====

    public void setSpeed(float speed) {
        this.speed = Math.max(0.1f, speed);

    }

    public void setDetectionRange(float range) {
        this.detectionRange = Math.max(1f, range);

    }

    public void setAttackRange(float range) {
        this.attackRange = Math.max(0.5f, range);

    }

    public void setAttackDamage(float damage) {
        this.attackDamage = Math.max(1f, damage);

    }

    @Override
    public void initializeModel() {
        // Call parent's initialization first
        super.initializeModel();

        // Then apply our custom zombie positioning (this overrides the parent's positioning)
        if (spriteAnimator != null) {
            adjustSpritePosition();
        }

    }
    // ==== GETTERS ====

    public ZombieState getCurrentState() { return currentState; }
    public float getSpeed() { return speed; }
    public float getDetectionRange() { return detectionRange; }
    public float getAttackRange() { return attackRange; }
    public float getAttackDamage() { return attackDamage; }
    public float getSpriteScale() { return spriteScale; }
    public Vector3f getSpriteOffset() { return spriteOffset.clone(); }
    public Vector3f getPlayerPosition() { return playerPosition != null ? playerPosition.clone() : null; }

    /**
     * Check if zombie can see player (simple line-of-sight)
     */
    public boolean canSeePlayer() {
        if (playerPosition == null) return false;

        float distance = position.distance(playerPosition);
        return distance <= detectionRange;
    }

    /**
     * Check if zombie is in attack range
     */
    public boolean isInAttackRange() {
        if (playerPosition == null) return false;

        float distance = position.distance(playerPosition);
        return distance <= attackRange;
    }
}