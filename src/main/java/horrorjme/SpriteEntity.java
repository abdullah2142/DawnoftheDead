package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

import java.util.List;

/**
 * SIMPLE SpriteEntity using kinematic RigidBodyControl - no complex collision detection
 * Just like the original but keeping it simple
 */
public abstract class SpriteEntity extends Entity {

    protected AssetManager assetManager;
    protected Camera camera;
    protected SpriteAnimator spriteAnimator;
    protected BulletAppState bulletAppState;

    // Physics - Simple kinematic RigidBodyControl
    private RigidBodyControl rigidBody;
    private CapsuleCollisionShape collisionShape;

    // Sprite positioning
    private boolean billboardingEnabled = true;
    private Vector3f tempVector = new Vector3f();

    // Physics configuration
    private float collisionRadius = 0.4f;
    private float collisionHeight = 1.8f;

    public SpriteEntity(EntityType type, Vector3f position, AssetManager assetManager,
                        Camera camera, BulletAppState bulletAppState) {
        super(type, position);
        this.assetManager = assetManager;
        this.camera = camera;
        this.bulletAppState = bulletAppState;
    }

    @Override
    public void initializeModel() {
        // 1. Create sprite animator for visuals
        spriteAnimator = new SpriteAnimator(assetManager);
        loadAnimations(); // Implemented by subclasses

        // 2. Create model node containing sprite
        model = new Node("SpriteEntity_" + entityId);
        ((Node) model).attachChild(spriteAnimator.getSpriteNode());

        // 3. Setup bottom-center pivot for sprite
        setupBottomCenterPivot();

        // 4. Create simple kinematic physics body
        createSimpleKinematicBody();

        // 5. Position model at entity position
        model.setLocalTranslation(position);

    }

    /**
     * SIMPLE: Create kinematic RigidBodyControl (can't be pushed, no complex collision)
     */
    private void createSimpleKinematicBody() {
        if (bulletAppState == null) {
            System.err.println("No BulletAppState provided - physics disabled for " + entityId);
            return;
        }

        try {
            System.out.println("Creating physics body for " + entityId + " at position: " + position);

            // Create collision shape
            collisionShape = new CapsuleCollisionShape(collisionRadius, collisionHeight);

            // Create RigidBodyControl with mass=0 (kinematic)
            rigidBody = new RigidBodyControl(collisionShape, 0f);
            rigidBody.setKinematic(true);

            // Add to model FIRST
            model.addControl(rigidBody);

            // Add to physics space SECOND
            bulletAppState.getPhysicsSpace().add(rigidBody);

            // CRITICAL FIX: Set physics location AFTER adding to physics space
            rigidBody.setPhysicsLocation(position);

            System.out.println("Physics body created for " + entityId +
                    " - Physics location: " + rigidBody.getPhysicsLocation());

        } catch (Exception e) {
            System.err.println("Failed to create physics for " + entityId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup bottom-center pivot for sprite visual
     */
    private void setupBottomCenterPivot() {
        Vector2f quadSize = spriteAnimator.getQuadSize();
        if (quadSize != null) {
            Node spriteNode = spriteAnimator.getSpriteNode();
            // Move sprite up by half its height so bottom touches ground
            spriteNode.setLocalTranslation(1, quadSize.y * 0f, 0);
        }
    }

    @Override
    public void update(float tpf) {
        if (!active || destroyed) return;

        // 1. Update AI behavior (subclasses set desired movement)
        updateBehavior(tpf);

        // 2. SIMPLE: Just apply movement directly to physics body
        applyMovement(tpf);

        // 3. Get position from physics
        updatePositionFromPhysics();

        // 4. Update sprite animation
        if (spriteAnimator != null) {
            spriteAnimator.update(tpf);
        }

        // 5. Handle billboarding
        if (billboardingEnabled && camera != null) {
            updateBillboarding();
        }
    }

    /**
     * SIMPLE: Move kinematic body with ground detection (unlimited step up/down)
     */
    private void applyMovement(float tpf) {
        if (rigidBody == null || velocity.lengthSquared() < 0.001f) {
            return;
        }

        Vector3f movement = velocity.mult(tpf);
        Vector3f currentPos = rigidBody.getPhysicsLocation();
        Vector3f newPos = currentPos.add(movement);

        // Check for wall collision at HEAD HEIGHT (not ground level)
        Vector3f headStart = currentPos.add(0, 0.5f, 0);  // Check at head height
        Vector3f headEnd = new Vector3f(newPos.x, currentPos.y + 1.5f, newPos.z);

        if (checkWallCollision(headStart, headEnd)) {
            newPos = currentPos; // Blocked by wall at head height, don't move
        }

        // Always find ground level (allows stepping up/down)
        float groundY = findGroundLevel(newPos);
        newPos.y = groundY;

        rigidBody.setPhysicsLocation(newPos);
    }

    private boolean checkWallCollision(Vector3f from, Vector3f to) {
        if (bulletAppState == null) return false;

        var results = bulletAppState.getPhysicsSpace().rayTest(from, to);

        for (var result : results) {
            var hitObject = result.getCollisionObject();

            if (hitObject != rigidBody && !isEntity(hitObject)) {
                return true; // Hit a wall
            }
        }

        return false; // No wall collision
    }

    /**
     * ADDED: Simple ground detection using raycasting
     */
    private float findGroundLevel(Vector3f position) {
        if (bulletAppState == null) {
            return position.y; // No physics, keep current height
        }

        // Cast ray from high above down to far below to find ground
        Vector3f rayStart = position.add(0, 1.5f, 0);   // Start 10 units above
        Vector3f rayEnd = position.add(0, -50f, 0);    // Check 50 units below

        List<PhysicsRayTestResult> results = bulletAppState.getPhysicsSpace().rayTest(rayStart, rayEnd);

        for (PhysicsRayTestResult result : results) {
            var hitObject = result.getCollisionObject();

            // Skip self and other entities - only hit world geometry
            if (hitObject != rigidBody && !isEntity(hitObject)) {
                // Calculate hit point
                float hitFraction = result.getHitFraction();
                Vector3f rayDirection = rayEnd.subtract(rayStart);
                Vector3f hitPoint = rayStart.add(rayDirection.mult(hitFraction));

                return hitPoint.y; // Return ground level
            }
        }

        return position.y; // No ground found, keep current height
    }

    /**
     * ADDED: Check if collision object is an entity (not world geometry)
     */
    private boolean isEntity(com.jme3.bullet.collision.PhysicsCollisionObject obj) {
        Object userObject = obj.getUserObject();
        if (userObject instanceof Node) {
            Node spatial = (Node) userObject;
            String name = spatial.getName();
            return name != null && (name.contains("SpriteEntity") || name.contains("Player"));
        }
        return false;
    }

    /**
     * Get position from physics body
     */
    private void updatePositionFromPhysics() {
        if (rigidBody != null) {
            Vector3f physicsPosition = rigidBody.getPhysicsLocation();
            position.set(physicsPosition);

            if (model != null) {
                model.setLocalTranslation(position);
            }
        }
    }

    /**
     * Y-axis only billboarding (DOOM style)
     */
    private void updateBillboarding() {
        if (model == null || camera == null) return;

        Vector3f cameraPos = camera.getLocation();
        tempVector.set(cameraPos).subtractLocal(position);
        tempVector.y = 0; // Only rotate around Y axis

        if (tempVector.lengthSquared() > 0.001f) {
            tempVector.normalizeLocal();
            Vector3f lookAtTarget = position.add(tempVector);
            model.lookAt(lookAtTarget, Vector3f.UNIT_Y);
        }
    }

    /**
     * Set position using physics body
     */
    @Override
    public void setPosition(Vector3f newPosition) {
        super.setPosition(newPosition);

        if (rigidBody != null) {
            rigidBody.setPhysicsLocation(newPosition);
        }

        if (model != null) {
            model.setLocalTranslation(position);
        }
    }

    /**
     * Override base movement - we handle it ourselves
     */
    @Override
    protected void updateMovement(float tpf) {
        // Movement is handled by applyMovement()
    }

    // ==== CONFIGURATION METHODS ====

    /**
     * Configure collision shape size
     */
    public void setCollisionSize(float radius, float height) {
        this.collisionRadius = Math.max(0.1f, radius);
        this.collisionHeight = Math.max(0.5f, height);}

    /**
     * Enable/disable billboarding
     */
    public void setBillboardingEnabled(boolean enabled) {
        this.billboardingEnabled = enabled;
    }

    /**
     * Enable physics debug visualization
     */
    public void enablePhysicsDebugVisualization() {
        if (bulletAppState != null) {
            bulletAppState.setDebugEnabled(true);

        } else {

        }
    }

    // ==== ABSTRACT METHODS ====

    /**
     * Load animations - implemented by subclasses
     */
    protected abstract void loadAnimations();

    /**
     * Update behavior - implemented by subclasses for AI
     */
    protected abstract void updateBehavior(float tpf);

    // ==== PUBLIC API ====

    /**
     * Play animation by name
     */
    public void playAnimation(String animationName) {
        if (spriteAnimator != null) {
            spriteAnimator.playAnimation(animationName);
        }
    }

    /**
     * Get current animation name
     */
    public String getCurrentAnimation() {
        return spriteAnimator != null ? spriteAnimator.getCurrentAnimationName() : "none";
    }

    /**
     * Check if currently animating
     */
    public boolean isAnimating() {
        return spriteAnimator != null && spriteAnimator.isAnimating();
    }

    /**
     * Get sprite size for debugging
     */
    public Vector2f getSpriteSize() {
        return spriteAnimator != null ? spriteAnimator.getQuadSize() : null;
    }

    /**
     * Get physics debug info
     */
    public String getPhysicsDebugInfo() {
        StringBuilder debug = new StringBuilder();
        debug.append(String.format("SpriteEntity %s: ", entityId));

        if (rigidBody != null) {
            Vector3f physicsPos = rigidBody.getPhysicsLocation();
            debug.append(String.format("Pos=(%.1f,%.1f,%.1f), ", physicsPos.x, physicsPos.y, physicsPos.z));
            debug.append(String.format("Kinematic=%s", rigidBody.isKinematic() ? "YES" : "NO"));
        } else {
            debug.append("No physics body");
        }

        return debug.toString();
    }

    @Override
    public void onDestroy() {
        // Remove physics body
        if (rigidBody != null && bulletAppState != null) {
            bulletAppState.getPhysicsSpace().remove(rigidBody);
            rigidBody = null;
        }

        // Cleanup sprite animator
        if (spriteAnimator != null) {
            spriteAnimator.cleanup();
        }

    }

    // ==== GETTERS ====
    public SpriteAnimator getSpriteAnimator() { return spriteAnimator; }
    public boolean isBillboardingEnabled() { return billboardingEnabled; }
    public RigidBodyControl getRigidBody() { return rigidBody; }
    public float getCollisionRadius() { return collisionRadius; }
    public float getCollisionHeight() { return collisionHeight; }
}