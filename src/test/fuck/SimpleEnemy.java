package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 * Example enemy implementation showing how to use the Entity system
 */
public class SimpleEnemy extends Entity {

    private AssetManager assetManager;
    private Vector3f playerPosition;
    private float speed = 1.5f;
    private float detectionRange = 5f;
    private float attackRange = 1f;
    private float attackDamage = 10f;
    private float lastAttackTime = 0f;
    private float attackCooldown = 2f; // 2 seconds between attacks

    // AI states
    public enum EnemyState {
        IDLE,
        CHASING,
        ATTACKING,
        DEAD
    }

    private EnemyState currentState = EnemyState.IDLE;

    public SimpleEnemy(Vector3f position, AssetManager assetManager) {
        super(EntityType.ENEMY, position);
        this.assetManager = assetManager;
        this.health = 50f;
        this.maxHealth = 50f;
        this.boundingRadius = 0.5f;
    }

    @Override
    public void initializeModel() {
        // Create a simple red box as the enemy model
        Box enemyBox = new Box(0.3f, 0.6f, 0.3f);
        model = new Geometry("Enemy_" + entityId, enemyBox);

        Material enemyMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        enemyMat.setColor("Diffuse", ColorRGBA.Red);
        enemyMat.setColor("Ambient", ColorRGBA.Red.mult(0.3f));
        enemyMat.setBoolean("UseMaterialColors", true);

        model.setMaterial(enemyMat);
        model.setLocalTranslation(position);
    }

    @Override
    public void update(float tpf) {
        if (!active || destroyed) return;

        updateAI(tpf);
        updateMovement(tpf);

        lastAttackTime += tpf;
    }

    private void updateAI(float tpf) {
        if (playerPosition == null) return;

        float distanceToPlayer = position.distance(playerPosition);

        switch (currentState) {
            case IDLE:
                if (distanceToPlayer <= detectionRange) {
                    currentState = EnemyState.CHASING;}
                break;

            case CHASING:
                if (distanceToPlayer <= attackRange) {
                    currentState = EnemyState.ATTACKING;
                } else if (distanceToPlayer > detectionRange * 1.5f) {
                    // Lost player
                    currentState = EnemyState.IDLE;
                    velocity.set(0, 0, 0);
                } else {
                    // Move towards player
                    Vector3f direction = playerPosition.subtract(position).normalizeLocal();
                    velocity.set(direction.mult(speed));
                }
                break;

            case ATTACKING:
                if (distanceToPlayer > attackRange) {
                    currentState = EnemyState.CHASING;
                } else {
                    // Attack if cooldown is ready
                    if (lastAttackTime >= attackCooldown) {
                        attack();
                        lastAttackTime = 0f;
                    }
                    velocity.set(0, 0, 0); // Don't move while attacking
                }
                break;

            case DEAD:
                velocity.set(0, 0, 0);
                break;
        }
    }

    private void attack() {// In a real game, you'd apply damage to the player here
        // For now, just print the attack
    }

    @Override
    public void onCollision(Entity other) {
        if (other.getType() == EntityType.ENEMY) {
            // Push away from other enemies to prevent clustering
            Vector3f pushDirection = position.subtract(other.getPosition()).normalizeLocal();
            position.addLocal(pushDirection.mult(0.1f));

            if (model != null) {
                model.setLocalTranslation(position);
            }
        }
    }

    @Override
    public void onDestroy() {
        currentState = EnemyState.DEAD;// Change color to indicate death
        if (model != null && model instanceof Geometry) {
            Geometry geom = (Geometry) model;
            Material mat = geom.getMaterial();
            if (mat != null) {
                mat.setColor("Diffuse", ColorRGBA.Black);
                mat.setColor("Ambient", ColorRGBA.Black);
            }
        }
    }

    /**
     * Set the player's position for AI tracking
     */
    public void setPlayerPosition(Vector3f playerPos) {
        this.playerPosition = playerPos.clone();
    }

    /**
     * Take damage and change color to indicate hurt
     */
    @Override
    public void takeDamage(float damage) {
        super.takeDamage(damage);

        // Flash red when taking damage
        if (model != null && !destroyed && model instanceof Geometry) {
            Geometry geom = (Geometry) model;
            Material mat = geom.getMaterial();
            if (mat != null) {
                float healthPercent = health / maxHealth;
                ColorRGBA color = ColorRGBA.Red.mult(1f - healthPercent * 0.5f);
                mat.setColor("Diffuse", color);
            }
        }
    }

    // Getters
    public EnemyState getCurrentState() { return currentState; }
    public float getDetectionRange() { return detectionRange; }
    public float getAttackRange() { return attackRange; }
    public float getAttackDamage() { return attackDamage; }

    // Setters
    public void setSpeed(float speed) { this.speed = speed; }
    public void setDetectionRange(float range) { this.detectionRange = range; }
    public void setAttackRange(float range) { this.attackRange = range; }
    public void setAttackDamage(float damage) { this.attackDamage = damage; }
}