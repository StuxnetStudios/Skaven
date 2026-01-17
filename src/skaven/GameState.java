package skaven;

import battlecode.common.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds game state information for decision making.
 */
public class GameState {
    // Shared map data (static so all robots can access)
    private static Map<MapLocation, TileMemory> sharedMap = new HashMap<>();
    private static int mapWidth = -1;
    private static int mapHeight = -1;

    // Instance fields
    public final MapLocation currentLocation;
    public final Direction facing;
    public final RobotInfo[] nearbyAllies;
    public final RobotInfo[] nearbyEnemies;
    public final MapInfo[] nearbyTiles;
    public final int roundNum;
    public final int health;
    public final int rawCheese;
    public final int globalCheese;
    public final int allCheese;
    public final int dirt;
    public final Team team;
    public final UnitType unitType;
    public final boolean isCooperation;
    public final boolean isBeingCarried;
    public final boolean isActionReady;
    public final boolean isMovementReady;
    public final RobotInfo carrying;

    public GameState(RobotController rc) throws GameActionException {
        // Initialize map dimensions once
        if (mapWidth < 0) {
            mapWidth = rc.getMapWidth();
            mapHeight = rc.getMapHeight();
        }

        this.currentLocation = rc.getLocation();
        this.facing = rc.getDirection();
        this.nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        this.nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        this.nearbyTiles = rc.senseNearbyMapInfos();
        this.roundNum = rc.getRoundNum();
        this.health = rc.getHealth();
        this.rawCheese = rc.getRawCheese();
        this.globalCheese = rc.getGlobalCheese();
        this.allCheese = rc.getAllCheese();
        this.dirt = rc.getDirt();
        this.team = rc.getTeam();
        this.unitType = rc.getType();
        this.isCooperation = rc.isCooperation();
        this.isBeingCarried = rc.isBeingCarried();
        this.isActionReady = rc.isActionReady();
        this.isMovementReady = rc.isMovementReady();
        this.carrying = rc.getCarrying();

        // Update shared map with what we see
        updateSharedMap(rc);
    }

    /**
     * Updates the shared map with currently visible tiles.
     */
    private void updateSharedMap(RobotController rc) throws GameActionException {
        for (MapInfo tile : nearbyTiles) {
            MapLocation loc = tile.getMapLocation();
            
            TileMemory memory = new TileMemory(
                tile.isPassable(),
                tile.isWall(),
                tile.isDirt(),
                tile.getCheeseAmount(),
                tile.hasCheeseMine(),
                tile.getTrap(),
                roundNum
            );
            sharedMap.put(loc, memory);
        }

        // Also record enemy positions
        for (RobotInfo enemy : nearbyEnemies) {
            TileMemory existing = sharedMap.get(enemy.location);
            if (existing != null) {
                existing.lastSeenEnemy = roundNum;
                existing.enemyType = enemy.type;
            }
        }
    }

    // ===== SHARED MAP ACCESS METHODS =====

    /**
     * Gets remembered tile info at a location.
     */
    public static TileMemory getMemory(MapLocation loc) {
        return sharedMap.get(loc);
    }

    /**
     * Checks if we've ever seen a location.
     */
    public static boolean hasVisited(MapLocation loc) {
        return sharedMap.containsKey(loc);
    }

    /**
     * Gets the map width.
     */
    public static int getMapWidth() {
        return mapWidth;
    }

    /**
     * Gets the map height.
     */
    public static int getMapHeight() {
        return mapHeight;
    }

    /**
     * Finds remembered cheese locations.
     */
    public static MapLocation findRememberedCheese(MapLocation from, int maxAge, int currentRound) {
        MapLocation best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Map.Entry<MapLocation, TileMemory> entry : sharedMap.entrySet()) {
            TileMemory mem = entry.getValue();
            if ((mem.cheeseAmount > 0 || mem.hasCheeseMine) && (currentRound - mem.lastSeen) <= maxAge) {
                int dist = from.distanceSquaredTo(entry.getKey());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = entry.getKey();
                }
            }
        }
        return best;
    }

    /**
     * Finds last known enemy locations.
     */
    public static MapLocation findLastKnownEnemy(MapLocation from, int maxAge, int currentRound) {
        MapLocation best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Map.Entry<MapLocation, TileMemory> entry : sharedMap.entrySet()) {
            TileMemory mem = entry.getValue();
            if (mem.lastSeenEnemy > 0 && (currentRound - mem.lastSeenEnemy) <= maxAge) {
                int dist = from.distanceSquaredTo(entry.getKey());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = entry.getKey();
                }
            }
        }
        return best;
    }

    /**
     * Clears old entries from the map to save memory.
     */
    public static void pruneOldEntries(int currentRound, int maxAge) {
        sharedMap.entrySet().removeIf(entry -> 
            (currentRound - entry.getValue().lastSeen) > maxAge
        );
    }

    /**
     * Checks if there are enemies nearby.
     */
    public boolean hasEnemiesNearby() {
        return nearbyEnemies != null && nearbyEnemies.length > 0;
    }

    /**
     * Checks if there are allies nearby.
     */
    public boolean hasAlliesNearby() {
        return nearbyAllies != null && nearbyAllies.length > 0;
    }

    /**
     * Finds the nearest enemy.
     */
    public RobotInfo getNearestEnemy() {
        if (!hasEnemiesNearby()) {
            return null;
        }
        
        RobotInfo nearest = null;
        int minDist = Integer.MAX_VALUE;
        
        for (RobotInfo enemy : nearbyEnemies) {
            int dist = currentLocation.distanceSquaredTo(enemy.location);
            if (dist < minDist) {
                minDist = dist;
                nearest = enemy;
            }
        }
        return nearest;
    }

    /**
     * Finds the nearest ally.
     */
    public RobotInfo getNearestAlly() {
        if (!hasAlliesNearby()) {
            return null;
        }
        
        RobotInfo nearest = null;
        int minDist = Integer.MAX_VALUE;
        
        for (RobotInfo ally : nearbyAllies) {
            int dist = currentLocation.distanceSquaredTo(ally.location);
            if (dist < minDist) {
                minDist = dist;
                nearest = ally;
            }
        }
        return nearest;
    }

    /**
     * Checks if robot is carrying another robot.
     */
    public boolean isCarryingRobot() {
        return carrying != null;
    }

    /**
     * Memory of a tile we've seen.
     */
    public static class TileMemory {
        public final boolean isPassable;
        public final boolean isWall;
        public final boolean isDirt;
        public int cheeseAmount;
        public boolean hasCheeseMine;
        public TrapType trap;
        public int lastSeen;
        public int lastSeenEnemy;
        public UnitType enemyType;

        public TileMemory(boolean isPassable, boolean isWall, boolean isDirt,
                          int cheeseAmount, boolean hasCheeseMine, TrapType trap,
                          int lastSeen) {
            this.isPassable = isPassable;
            this.isWall = isWall;
            this.isDirt = isDirt;
            this.cheeseAmount = cheeseAmount;
            this.hasCheeseMine = hasCheeseMine;
            this.trap = trap;
            this.lastSeen = lastSeen;
            this.lastSeenEnemy = 0;
            this.enemyType = null;
        }

        public boolean hasCheese() {
            return cheeseAmount > 0;
        }

        public boolean hasRatTrap() {
            return trap == TrapType.RAT_TRAP;
        }

        public boolean hasCatTrap() {
            return trap == TrapType.CAT_TRAP;
        }

        public boolean hasTrap() {
            return trap != null && trap != TrapType.NONE;
        }
    }
}
