package V1Bot;

import battlecode.common.*;
import java.util.Random;

/**
 * Helper class for spawning rats (only Rat Kings can spawn).
 */
public class SpawnHelper {

    private final Random rng;
    private static final Direction[] DIRECTIONS = {
        Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
        Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };

    public SpawnHelper(int seed) {
        this.rng = new Random(seed);
    }

    /**
     * Attempts to spawn a baby rat at a random valid location.
     * Only Rat Kings can spawn rats.
     */
    public boolean trySpawnRat(RobotController rc) throws GameActionException {
        Direction dir = DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
        MapLocation spawnLoc = rc.getLocation().add(dir);
        
        if (rc.canBuildRat(spawnLoc)) {
            rc.buildRat(spawnLoc);
            return true;
        }
        
        // Try all directions if random fails
        for (Direction d : DIRECTIONS) {
            spawnLoc = rc.getLocation().add(d);
            if (rc.canBuildRat(spawnLoc)) {
                rc.buildRat(spawnLoc);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if spawning is worthwhile based on cheese reserves.
     * @param minCheese Minimum cheese to keep in reserve
     */
    public boolean shouldSpawn(RobotController rc, int minCheese) throws GameActionException {
        return rc.getAllCheese() > minCheese;
    }

    /**
     * Gets the current cost to spawn a rat.
     */
    public int getSpawnCost(RobotController rc) {
        return rc.getCurrentRatCost();
    }
}
