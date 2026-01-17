package skaven;

import battlecode.common.*;

/**
 * Default strategy implementation containing the original bot logic.
 */
public class DefaultStrategy implements BotStrategy {

    private final MovementHelper movement;

    public DefaultStrategy(int robotId) {
        this.movement = new MovementHelper(robotId);
    }

    @Override
    public void executeTurn(RobotController rc) throws GameActionException {
        UnitType type = rc.getType();
        
        // Check if we're a rat king
        if (type == UnitType.RAT_KING) {
            runRatKing(rc);
        }
        // Check if we're a cat
        else if (type == UnitType.CAT) {
            runCat(rc);
        }
        // Default: assume we're a rat
        else {
            runRat(rc);
        }
    }

    private void runRat(RobotController rc) throws GameActionException {
        // Check for nearby cats and flee
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies) {
            if (enemy.type == UnitType.CAT) {
                // RUN FROM THE CAT!
                movement.moveAway(rc, enemy.location);
                return;
            }
        }

        // Pick up cheese if nearby
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        for (MapInfo tile : nearbyTiles) {
            if (rc.canPickUpCheese(tile.getMapLocation())) {
                rc.pickUpCheese(tile.getMapLocation());
                break;
            }
        }

        // Move randomly
        movement.moveRandom(rc);
    }

    private void runRatKing(RobotController rc) throws GameActionException {
        // Spawn baby rats while we have > 250 cheese
        if (rc.getAllCheese() > 250) {
            trySpawnRat(rc);
        }

        // Move randomly
        movement.moveRandom(rc);
    }

    private void runCat(RobotController rc) throws GameActionException {
        // Cats hunt rats
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo target = null;
        int minDist = Integer.MAX_VALUE;

        for (RobotInfo enemy : enemies) {
            int dist = rc.getLocation().distanceSquaredTo(enemy.location);
            if (dist < minDist) {
                minDist = dist;
                target = enemy;
            }
        }

        if (target != null) {
            // Attack if in range
            if (rc.canAttack(target.location)) {
                rc.attack(target.location);
            }
            // Move toward target
            movement.moveToward(rc, target.location);
        } else {
            movement.moveRandom(rc);
        }
    }

    /**
     * Attempts to spawn a baby rat at any valid location.
     */
    private boolean trySpawnRat(RobotController rc) throws GameActionException {
        // Try all adjacent locations
        Direction[] dirs = movement.getAllDirections();
        for (Direction dir : dirs) {
            MapLocation spawnLoc = rc.getLocation().add(dir);
            if (rc.canBuildRat(spawnLoc)) {
                rc.buildRat(spawnLoc);
                return true;
            }
        }
        return false;
    }
}