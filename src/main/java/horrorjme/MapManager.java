package horrorjme;

import com.jme3.math.Vector3f;

/**
 * Manages map switching and transitions between rounds
 */
public class MapManager {

    public enum MapTransitionMode {
        SEQUENTIAL,  // Go through maps in order
        RANDOM,      // Pick random map each round
        PLAYER_CHOICE // Let player choose (menu selection)
    }

    private MapInfo currentMap;
    private MapInfo nextMap;
    private MapTransitionMode transitionMode = MapTransitionMode.SEQUENTIAL;

    // Transition timing
    private boolean transitionPending = false;
    private float transitionCountdown = 10f; // 10 seconds to next map
    private float maxTransitionTime = 10f;
    private boolean countdownActive = false;

    // Map selection for menu
    private MapInfo selectedMapInMenu;

    public MapManager() {
        this.currentMap = MapInfo.getDefaultMap();
        this.nextMap = currentMap.getNextMap();
        this.selectedMapInMenu = currentMap;

        System.out.println("MapManager: Initialized with " + currentMap.getDisplayName());
    }

    /**
     * Set the starting map based on player selection and set mode to PLAYER_CHOICE.
     * This is intended to be called once at game startup when a player has chosen a map.
     */
    public void setStartMap(MapInfo map) {
        if (map != null) {
            this.currentMap = map;
            this.selectedMapInMenu = map;
            this.nextMap = map; // When chosen, the next map is the same until a transition is triggered
            setTransitionMode(MapTransitionMode.PLAYER_CHOICE);
            System.out.println("MapManager: Player selected start map set to " + map.getDisplayName());
        }
    }

    /**
     * Start map transition countdown after round completion
     */
    public void startMapTransition() {
        if (!transitionPending) {
            transitionPending = true;
            countdownActive = true;
            transitionCountdown = maxTransitionTime;

            // Determine next map based on mode
            switch (transitionMode) {
                case SEQUENTIAL:
                    nextMap = currentMap.getNextMap();
                    break;
                case RANDOM:
                    nextMap = currentMap.getRandomMap();
                    break;
                case PLAYER_CHOICE:
                    // In player choice mode, the map doesn't automatically change.
                    // Another system would need to set the next map.
                    // For now, we can make it select a random one or stay the same.
                    nextMap = selectedMapInMenu; // Stays on the chosen map unless changed elsewhere
                    break;
            }

            System.out.println("MapManager: Starting transition from " + currentMap.getDisplayName() +
                    " to " + nextMap.getDisplayName() + " in " + maxTransitionTime + " seconds");
        }
    }

    /**
     * Update transition countdown (call every frame)
     */
    public void update(float tpf) {
        if (countdownActive) {
            transitionCountdown -= tpf;

            if (transitionCountdown <= 0f) {
                completeTransition();
            }
        }
    }

    /**
     * Complete the map transition
     */
    private void completeTransition() {
        currentMap = nextMap;
        transitionPending = false;
        countdownActive = false;
        transitionCountdown = maxTransitionTime;

        System.out.println("MapManager: Transition complete - now on " + currentMap.getDisplayName());
    }

    /**
     * Force immediate map transition (skip countdown)
     */
    public void forceTransition() {
        if (transitionPending) {
            completeTransition();
        }
    }

    /**
     * Cancel pending transition
     */
    public void cancelTransition() {
        transitionPending = false;
        countdownActive = false;
        transitionCountdown = maxTransitionTime;
        System.out.println("MapManager: Transition cancelled");
    }

    /**
     * Check if map transition should happen now
     */
    public boolean shouldTransitionMap() {
        return transitionPending && transitionCountdown <= 0f;
    }

    /**
     * Set map for menu selection
     */
    public void setSelectedMapInMenu(MapInfo map) {
        if (map != null) {
            this.selectedMapInMenu = map;
            // If the mode is player choice, update the next map immediately
            if (transitionMode == MapTransitionMode.PLAYER_CHOICE) {
                this.nextMap = selectedMapInMenu;
            }
            System.out.println("MapManager: Menu map selection set to " + map.getDisplayName());
        }
    }

    /**
     * Manually set current map (for menu-based map selection)
     */
    public void setCurrentMap(MapInfo map) {
        if (map != null) {
            this.currentMap = map;
            this.selectedMapInMenu = map;
            System.out.println("MapManager: Current map manually set to " + map.getDisplayName());
        }
    }

    // ==== CONFIGURATION METHODS ====

    public void setTransitionMode(MapTransitionMode mode) {
        this.transitionMode = mode;
        System.out.println("MapManager: Transition mode set to " + mode);
    }

    public void setTransitionTime(float seconds) {
        this.maxTransitionTime = Math.max(1f, seconds);
        this.transitionCountdown = this.maxTransitionTime;
        System.out.println("MapManager: Transition time set to " + seconds + " seconds");
    }

    // ==== GETTERS ====

    public MapInfo getCurrentMap() {
        return currentMap;
    }

    public MapInfo getNextMap() {
        return nextMap;
    }

    public MapInfo getSelectedMapInMenu() {
        return selectedMapInMenu;
    }

    public String getCurrentMapName() {
        return currentMap.getDisplayName();
    }

    public String getNextMapName() {
        return nextMap != null ? nextMap.getDisplayName() : "Unknown";
    }

    public Vector3f getCurrentMapSpawnPosition() {
        return currentMap.getStartPosition();
    }

    public String getCurrentMapPath() {
        return currentMap.getModelPath();
    }

    public boolean isTransitionPending() {
        return transitionPending;
    }

    public boolean isCountdownActive() {
        return countdownActive;
    }

    public float getTransitionCountdown() {
        return Math.max(0f, transitionCountdown);
    }

    public int getTransitionCountdownSeconds() {
        return Math.max(0, (int)Math.ceil(transitionCountdown));
    }

    public MapTransitionMode getTransitionMode() {
        return transitionMode;
    }

    public float getTransitionProgress() {
        if (!countdownActive) return 0f;
        return 1f - (transitionCountdown / maxTransitionTime);
    }

    /**
     * Get formatted countdown string for HUD
     */
    public String getFormattedCountdown() {
        int seconds = getTransitionCountdownSeconds();
        return String.format("Map Change: %d", seconds);
    }

    /**
     * Get transition status for HUD
     */
    public String getTransitionStatus() {
        if (!transitionPending) {
            return "Current Map: " + getCurrentMapName();
        } else {
            return "Changing to: " + getNextMapName() + " in " + getTransitionCountdownSeconds() + "s";
        }
    }

    /**
     * Reset manager state
     */
    public void reset() {
        transitionPending = false;
        countdownActive = false;
        transitionCountdown = maxTransitionTime;
        // Keep current map and selected map - don't reset those
        System.out.println("MapManager: Reset completed");
    }

    /**
     * Get all available maps for menu display
     */
    public static MapInfo[] getAllMaps() {
        return MapInfo.values();
    }

    /**
     * Get map selection summary for debugging
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== MapManager Debug Info ===\n");
        info.append("Current Map: ").append(getCurrentMapName()).append("\n");
        info.append("Next Map: ").append(getNextMapName()).append("\n");
        info.append("Selected in Menu: ").append(selectedMapInMenu.getDisplayName()).append("\n");
        info.append("Transition Mode: ").append(transitionMode).append("\n");
        info.append("Transition Pending: ").append(transitionPending).append("\n");
        info.append("Countdown Active: ").append(countdownActive).append("\n");
        info.append("Countdown Remaining: ").append(String.format("%.1f", transitionCountdown)).append("s\n");
        info.append("Transition Time: ").append(maxTransitionTime).append("s");
        return info.toString();
    }
}