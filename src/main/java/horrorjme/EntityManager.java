package horrorjme;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all entities in the game world
 */
public class EntityManager {

    private Map<String, Entity> entities;
    private Map<Entity.EntityType, List<Entity>> entitiesByType;
    private List<Entity> entitiesToRemove;
    private Node entityNode;

    // Spatial partitioning for optimization (simple grid)
    private static final int GRID_SIZE = 16;
    private Map<String, Set<Entity>> spatialGrid;

    public EntityManager(Node rootNode) {
        entities = new ConcurrentHashMap<>();
        entitiesByType = new EnumMap<>(Entity.EntityType.class);
        entitiesToRemove = new ArrayList<>();
        spatialGrid = new HashMap<>();

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

        System.out.println("Added entity: " + id + " of type " + entity.getType());
    }

    /**
     * Remove entity from the manager
     */
    public void removeEntity(String entityId) {
        Entity entity = entities.get(entityId);
        if (entity != null) {
            removeEntity(entity);
        }
    }

    /**
     * Remove entity from the manager
     */
    public void removeEntity(Entity entity) {
        if (entity == null) return;

        entities.remove(entity.getEntityId());
        entitiesByType.get(entity.getType()).remove(entity);

        // Remove from spatial grid
        removeFromSpatialGrid(entity);

        // Detach from scene
        entity.detachFromScene();

        System.out.println("Removed entity: " + entity.getEntityId());
    }

    /**
     * Update all entities
     */
    public void update(float tpf) {
        // Update all active entities
        for (Entity entity : entities.values()) {
            if (entity.isActive() && !entity.isDestroyed()) {
                // Update spatial grid position if entity moved
                updateSpatialGrid(entity);

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

        // Check collisions
        checkCollisions();
    }

    /**
     * Check collisions between entities
     */
    private void checkCollisions() {
        // Use spatial grid for efficient collision detection
        for (Entity entity : entities.values()) {
            if (!entity.isActive() || entity.isDestroyed()) continue;

            Set<Entity> nearbyEntities = getNearbyEntities(entity);
            for (Entity other : nearbyEntities) {
                if (entity != other && entity.collidesWith(other)) {
                    entity.onCollision(other);
                    other.onCollision(entity);
                }
            }
        }
    }

    /**
     * Get entities of a specific type
     */
    public List<Entity> getEntitiesByType(Entity.EntityType type) {
        return new ArrayList<>(entitiesByType.get(type));
    }

    /**
     * Get entity by ID
     */
    public Entity getEntity(String entityId) {
        return entities.get(entityId);
    }

    /**
     * Get all entities
     */
    public Collection<Entity> getAllEntities() {
        return new ArrayList<>(entities.values());
    }

    /**
     * Get entities within a certain distance of a position
     */
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

    /**
     * Get the closest entity of a specific type to a position
     */
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

    /**
     * Clear all entities
     */
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
    }

    // Spatial Grid Methods for optimization
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

    private void updateSpatialGrid(Entity entity) {
        // Simple approach: remove and re-add
        // In production, you'd track previous position
        removeFromSpatialGrid(entity);
        addToSpatialGrid(entity);
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
}