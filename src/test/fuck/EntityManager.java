package horrorjme;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized EntityManager with efficient spatial grid updates and collision detection
 */
public class EntityManager {

    private Map<String, Entity> entities;
    private Map<Entity.EntityType, List<Entity>> entitiesByType;
    private List<Entity> entitiesToRemove;
    private Node entityNode;

    // Spatial partitioning for optimization
    private static final int GRID_SIZE = 16;
    private Map<String, Set<Entity>> spatialGrid;

    // OPTIMIZATION: Track previous positions to detect movement
    private Map<String, Vector3f> previousPositions;
    private static final float MOVEMENT_THRESHOLD = 0.01f; // Only update if moved more than this

    // OPTIMIZATION: Track processed collision pairs to avoid double processing
    private Set<String> processedCollisionPairs;

    public EntityManager(Node rootNode) {
        entities = new ConcurrentHashMap<>();
        entitiesByType = new EnumMap<>(Entity.EntityType.class);
        entitiesToRemove = new ArrayList<>();
        spatialGrid = new HashMap<>();

        // OPTIMIZATION: Initialize position tracking
        previousPositions = new HashMap<>();
        processedCollisionPairs = new HashSet<>();

        // Initialize type lists
        for (Entity.EntityType type : Entity.EntityType.values()) {
            entitiesByType.put(type, new ArrayList<>());
        }

        // Create entity node
        entityNode = new Node("EntityNode");
        rootNode.attachChild(entityNode);
    }

    /**
     * Add entity to the manager
     */
    public void addEntity(Entity entity) {
        if (entity == null) return;

        String id = entity.getEntityId();
        entities.put(id, entity);
        entitiesByType.get(entity.getType()).add(entity);

        // Initialize and attach to scene
        entity.initializeModel();
        entity.attachToScene(entityNode);

        // Add to spatial grid
        addToSpatialGrid(entity);

        // OPTIMIZATION: Track initial position
        previousPositions.put(id, entity.getPosition().clone());}

    /**
     * Remove entity from the manager
     */
    public void removeEntity(Entity entity) {
        if (entity == null) return;

        String entityId = entity.getEntityId();
        entities.remove(entityId);
        entitiesByType.get(entity.getType()).remove(entity);

        // Remove from spatial grid
        removeFromSpatialGrid(entity);

        // OPTIMIZATION: Clean up position tracking
        previousPositions.remove(entityId);

        // Detach from scene
        entity.detachFromScene();}

    /**
     * OPTIMIZED: Update all entities with efficient spatial grid updates
     */
    public void update(float tpf) {
        // OPTIMIZATION: Clear collision pairs at start of frame
        processedCollisionPairs.clear();

        // Update all active entities
        for (Entity entity : entities.values()) {
            if (entity.isActive() && !entity.isDestroyed()) {
                // OPTIMIZATION: Only update spatial grid if entity actually moved
                updateSpatialGridIfMoved(entity);

                // Update entity logic
                entity.update(tpf);
            }

            // Mark destroyed entities for removal
            if (entity.isDestroyed()) {
                entitiesToRemove.add(entity);
            }
        }

        // Remove destroyed entities
        for (Entity entity : entitiesToRemove) {
            removeEntity(entity);
        }
        entitiesToRemove.clear();

        // Check collisions with optimization
        checkCollisionsOptimized();
    }

    /**
     * OPTIMIZATION: Only update spatial grid if entity actually moved
     */
    private void updateSpatialGridIfMoved(Entity entity) {
        String entityId = entity.getEntityId();
        Vector3f currentPos = entity.getPosition();
        Vector3f previousPos = previousPositions.get(entityId);

        if (previousPos == null) {
            // First time - add to spatial grid and track position
            addToSpatialGrid(entity);
            previousPositions.put(entityId, currentPos.clone());
            return;
        }

        // Check if entity moved significantly
        float distanceMoved = currentPos.distance(previousPos);
        if (distanceMoved > MOVEMENT_THRESHOLD) {
            // Entity moved - update spatial grid
            String oldGridKey = getGridKey(previousPos);
            String newGridKey = getGridKey(currentPos);

            // Only do expensive grid operations if entity changed grid cells
            if (!oldGridKey.equals(newGridKey)) {
                removeFromSpatialGridAtPosition(entity, previousPos);
                addToSpatialGrid(entity);
            }

            // Update tracked position
            previousPos.set(currentPos);
        }
    }

    /**
     * OPTIMIZATION: Avoid double collision processing
     */
    private void checkCollisionsOptimized() {
        for (Entity entity : entities.values()) {
            if (!entity.isActive() || entity.isDestroyed()) continue;

            Set<Entity> nearbyEntities = getNearbyEntities(entity);
            for (Entity other : nearbyEntities) {
                if (entity != other && entity.collidesWith(other)) {

                    // OPTIMIZATION: Create unique pair ID to avoid double processing
                    String pairId = createCollisionPairId(entity, other);

                    if (!processedCollisionPairs.contains(pairId)) {
                        // Process collision only once per pair
                        entity.onCollision(other);
                        other.onCollision(entity);

                        // Mark this pair as processed for this frame
                        processedCollisionPairs.add(pairId);
                    }
                }
            }
        }
    }

    /**
     * OPTIMIZATION: Create consistent collision pair ID regardless of order
     */
    private String createCollisionPairId(Entity a, Entity b) {
        String idA = a.getEntityId();
        String idB = b.getEntityId();

        // Ensure consistent ordering so A-B and B-A produce same ID
        if (idA.compareTo(idB) < 0) {
            return idA + "-" + idB;
        } else {
            return idB + "-" + idA;
        }
    }

    /**
     * OPTIMIZATION: Remove entity from spatial grid at specific position
     */
    private void removeFromSpatialGridAtPosition(Entity entity, Vector3f position) {
        String key = getGridKey(position);
        Set<Entity> grid = spatialGrid.get(key);
        if (grid != null) {
            grid.remove(entity);
            if (grid.isEmpty()) {
                spatialGrid.remove(key);
            }
        }
    }

    // ... Rest of methods remain the same ...

    public List<Entity> getEntitiesByType(Entity.EntityType type) {
        return new ArrayList<>(entitiesByType.get(type));
    }

    public Entity getEntity(String entityId) {
        return entities.get(entityId);
    }

    public Collection<Entity> getAllEntities() {
        return new ArrayList<>(entities.values());
    }

    public List<Entity> getEntitiesInRange(Vector3f position, float range) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities.values()) {
            if (entity.isActive() && !entity.isDestroyed()) {
                if (entity.getPosition().distance(position) <= range) {
                    result.add(entity);
                }
            }
        }
        return result;
    }

    public Entity getClosestEntity(Vector3f position, Entity.EntityType type) {
        Entity closest = null;
        float closestDistance = Float.MAX_VALUE;

        for (Entity entity : entitiesByType.get(type)) {
            if (entity.isActive() && !entity.isDestroyed()) {
                float distance = entity.getPosition().distance(position);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = entity;
                }
            }
        }

        return closest;
    }

    public void clear() {
        for (Entity entity : entities.values()) {
            entity.detachFromScene();
        }
        entities.clear();
        for (List<Entity> list : entitiesByType.values()) {
            list.clear();
        }
        spatialGrid.clear();
        entitiesToRemove.clear();

        // OPTIMIZATION: Clear tracking data
        previousPositions.clear();
        processedCollisionPairs.clear();
    }

    // Spatial Grid Methods
    private String getGridKey(Vector3f position) {
        int x = (int)(position.x / GRID_SIZE);
        int z = (int)(position.z / GRID_SIZE);
        return x + "," + z;
    }

    private void addToSpatialGrid(Entity entity) {
        String key = getGridKey(entity.getPosition());
        spatialGrid.computeIfAbsent(key, k -> new HashSet<>()).add(entity);
    }

    private void removeFromSpatialGrid(Entity entity) {
        String key = getGridKey(entity.getPosition());
        Set<Entity> grid = spatialGrid.get(key);
        if (grid != null) {
            grid.remove(entity);
            if (grid.isEmpty()) {
                spatialGrid.remove(key);
            }
        }
    }

    private Set<Entity> getNearbyEntities(Entity entity) {
        Set<Entity> nearby = new HashSet<>();
        Vector3f pos = entity.getPosition();

        // Check entity's grid cell and adjacent cells
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = (int)(pos.x / GRID_SIZE) + dx;
                int z = (int)(pos.z / GRID_SIZE) + dz;
                String key = x + "," + z;

                Set<Entity> grid = spatialGrid.get(key);
                if (grid != null) {
                    nearby.addAll(grid);
                }
            }
        }

        return nearby;
    }

    // Statistics
    public int getEntityCount() {
        return entities.size();
    }

    public int getEntityCount(Entity.EntityType type) {
        return entitiesByType.get(type).size();
    }

    // OPTIMIZATION: Performance statistics
    public void printPerformanceStats() {int totalEntitiesInGrid = 0;
        for (Set<Entity> cell : spatialGrid.values()) {
            totalEntitiesInGrid += cell.size();
        }}
}