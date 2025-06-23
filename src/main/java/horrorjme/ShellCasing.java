package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * Physics-based shell casing that ejects from weapon and falls to ground
 */
public class ShellCasing extends Entity {

    private AssetManager assetManager;
    private BulletAppState bulletAppState;
    private RigidBodyControl rigidBody;
    private float lifeTime = 10f; // How long shell stays on ground
    private float currentLifeTime = 0f;
    private boolean hasLanded = false;
    private float fadeOutTime = 2f; // How long it takes to fade out
    private Material shellMaterial;
    private float originalAlpha = 1f;

    // Shell physics properties
    private static final float SHELL_MASS = 0.02f; // Very light
    private static final float SHELL_SIZE = 0.03f; // Small shell

    public ShellCasing(Vector3f startPosition, Vector3f ejectionVelocity,
                       AssetManager assetManager, BulletAppState bulletAppState) {
        super(EntityType.DECORATION, startPosition);
        this.assetManager = assetManager;
        this.bulletAppState = bulletAppState;
        this.boundingRadius = SHELL_SIZE;

        // Add some randomness to ejection
        Vector3f randomizedVelocity = ejectionVelocity.clone();
        randomizedVelocity.x += (FastMath.nextRandomFloat() - 0.5f) * 2f;
        randomizedVelocity.y += FastMath.nextRandomFloat() * 1f;
        randomizedVelocity.z += (FastMath.nextRandomFloat() - 0.5f) * 2f;

        this.velocity = randomizedVelocity;
    }

    @Override
    public void initializeModel() {
        // Create a small quad for the shell casing
        Quad shellQuad = new Quad(SHELL_SIZE, SHELL_SIZE * 1.5f); // Slightly elongated
        model = new Geometry("ShellCasing_" + entityId, shellQuad);

        // Create material for shell casing
        shellMaterial = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");

        try {
            // Try to load shell casing texture
            Texture shellTexture = assetManager.loadTexture("Textures/Weapons/shell_casing.png");
            shellTexture.setMagFilter(Texture.MagFilter.Nearest);
            shellTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            shellMaterial.setTexture("Texture", shellTexture);
        } catch (Exception e) {

            // Fallback to golden color
            shellMaterial.setColor("Color", new ColorRGBA(0.8f, 0.6f, 0.2f, 1f));
        }

        // Enable alpha blending for fade-out effect
        shellMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        shellMaterial.setTransparent(true);

        model.setMaterial(shellMaterial);
        model.setLocalTranslation(position);

        // Create physics body
        setupPhysics();
    }

    private void setupPhysics() {
        // Create a small box collision shape for the shell
        BoxCollisionShape shellShape = new BoxCollisionShape(new Vector3f(SHELL_SIZE/2, SHELL_SIZE*0.75f, SHELL_SIZE/2));

        // Create rigid body with mass (so it falls)
        rigidBody = new RigidBodyControl(shellShape, SHELL_MASS);

        // CRITICAL: Prevent shell casings from affecting player
        rigidBody.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
        rigidBody.setCollideWithGroups(
                PhysicsCollisionObject.COLLISION_GROUP_01 |  // World/terrain
                        PhysicsCollisionObject.COLLISION_GROUP_02    // Other shell casings
                // NOT including player collision group
        );

        // Set initial velocity (ejection from gun)
        rigidBody.setLinearVelocity(velocity);

        // Add some random angular velocity for realistic tumbling
        Vector3f angularVel = new Vector3f(
                (FastMath.nextRandomFloat() - 0.5f) * 20f,
                (FastMath.nextRandomFloat() - 0.5f) * 20f,
                (FastMath.nextRandomFloat() - 0.5f) * 20f
        );
        rigidBody.setAngularVelocity(angularVel);

        // Reduce bouncing
        rigidBody.setRestitution(0.3f);
        rigidBody.setFriction(0.7f);

        model.addControl(rigidBody);

        // Add to physics space
        if (bulletAppState != null) {
            bulletAppState.getPhysicsSpace().add(rigidBody);
        }
    }

    @Override
    public void update(float tpf) {
        if (!active || destroyed) return;

        currentLifeTime += tpf;

        // Update position from physics
        if (rigidBody != null) {
            Vector3f physicsPos = rigidBody.getPhysicsLocation();
            position.set(physicsPos);

            // Check if shell has landed (low velocity)
            if (!hasLanded && rigidBody.getLinearVelocity().length() < 1f) {
                hasLanded = true;
                // Reduce physics activity when landed
                rigidBody.setAngularVelocity(Vector3f.ZERO);
            }
        }

        // Start fading out near end of life
        if (currentLifeTime > lifeTime - fadeOutTime) {
            float fadeProgress = (currentLifeTime - (lifeTime - fadeOutTime)) / fadeOutTime;
            float alpha = originalAlpha * (1f - fadeProgress);

            // Update material alpha
            ColorRGBA currentColor = shellMaterial.getParam("Color") != null ?
                    (ColorRGBA) shellMaterial.getParam("Color").getValue() : ColorRGBA.White;
            shellMaterial.setColor("Color", new ColorRGBA(currentColor.r, currentColor.g, currentColor.b, alpha));
        }

        // Remove shell after lifetime
        if (currentLifeTime >= lifeTime) {
            destroy();
        }
    }

    @Override
    public void onCollision(Entity other) {
        // Shell casings don't interact with other entities
    }

    @Override
    public void onDestroy() {
        // Remove from physics
        if (rigidBody != null && bulletAppState != null) {
            bulletAppState.getPhysicsSpace().remove(rigidBody);
        }

    }

    // Static factory method for easy creation
    public static ShellCasing createShellCasing(Vector3f weaponPosition, Vector3f cameraDirection,
                                                AssetManager assetManager, BulletAppState bulletAppState) {
        // Calculate ejection position (slightly to the right and forward of weapon)
        Vector3f rightDirection = cameraDirection.cross(Vector3f.UNIT_Y).normalizeLocal();
        Vector3f ejectionPos = weaponPosition.add(rightDirection.mult(0.2f)).add(cameraDirection.mult(0.1f));

        // Calculate ejection velocity (right and slightly up)
        Vector3f ejectionVel = rightDirection.mult(3f).add(Vector3f.UNIT_Y.mult(2f)).add(cameraDirection.mult(1f));

        return new ShellCasing(ejectionPos, ejectionVel, assetManager, bulletAppState);
    }

    public boolean hasLanded() {
        return hasLanded;
    }

    public float getLifeTime() {
        return currentLifeTime;
    }
}