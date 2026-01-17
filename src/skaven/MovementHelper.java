package skaven;

import battlecode.common.*;
import java.util.Random;

/**
 * Helper class for movement and pathfinding operations.
 * Skaven philosophy: dig through dirt rather than go around it.
 */
public class MovementHelper {
    private static final Direction[] DIRECTIONS = {
        Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
        Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };

    private final Random rng;

    public MovementHelper(int seed) {
        this.rng = new Random(seed);
    }

    public Direction[] getAllDirections() {
        return DIRECTIONS;
    }

    /**
     * Attempts to move in a random direction, digging if needed.
     */
    public boolean moveRandom(RobotController rc) throws GameActionException {
        for (int i = 0; i < DIRECTIONS.length; i++) {
            Direction dir = DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
            if (moveOrDig(rc, dir)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to move toward a target location, digging through dirt if needed.
     */
    public boolean moveToward(RobotController rc, MapLocation target) throws GameActionException {
        if (target == null) {
            return moveRandom(rc); // Fallback to random movement if target is null
        }

        Direction dir = rc.getLocation().directionTo(target);

        // Try direct path first, dig if blocked by dirt
        if (moveOrDig(rc, dir)) {
            return true;
        }

        // Try adjacent directions, still preferring to dig
        Direction left = dir.rotateLeft();
        Direction right = dir.rotateRight();

        if (moveOrDig(rc, left)) {
            return true;
        }
        if (moveOrDig(rc, right)) {
            return true;
        }

        // Fallback to bug pathfinding if direct and adjacent directions fail
        return bugMove(rc, target);
    }

    /**
     * Attempts to move away from a target location, digging if needed.
     */
    public boolean moveAway(RobotController rc, MapLocation target) throws GameActionException {
        if (target == null) {
            return moveRandom(rc); // Fallback to random movement if target is null
        }

        Direction dir = rc.getLocation().directionTo(target).opposite();

        // Try direct away direction first
        if (moveOrDig(rc, dir)) {
            return true;
        }

        // Try adjacent away directions
        Direction[] tryDirs = {
            dir.rotateLeft(),
            dir.rotateRight(),
            dir.rotateLeft().rotateLeft(),
            dir.rotateRight().rotateRight()
        };

        for (Direction tryDir : tryDirs) {
            if (moveOrDig(rc, tryDir)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Attempts to move in direction. If blocked by dirt, dig it instead.
     * @return true if moved or dug successfully
     */
    public boolean moveOrDig(RobotController rc, Direction dir) throws GameActionException {
        if (dir == null || dir == Direction.CENTER) {
            return false;
        }

        MapLocation targetLoc = rc.getLocation().add(dir);

        // If we can move, just move
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // If blocked, try to dig through dirt
        if (canDig(rc, targetLoc)) {
            return dig(rc, targetLoc);
        }

        return false;
    }

    /**
     * Bug pathfinding - tries to go around obstacles.
     */
    public boolean bugMove(RobotController rc, MapLocation target) throws GameActionException {
        if (target == null) {
            return false;
        }

        MapLocation myLoc = rc.getLocation();
        Direction targetDir = myLoc.directionTo(target);

        Direction currentDir = targetDir;
        for (int i = 0; i < 8; i++) {
            if (moveOrDig(rc, currentDir)) {
                return true;
            }
            currentDir = currentDir.rotateRight();
        }

        return false;
    }

    /**
     * Checks if we can dig (remove dirt) at the specified location.
     * Uses Battlecode 2026 API: rc.canRemoveDirt()
     */
    public boolean canDig(RobotController rc, MapLocation loc) throws GameActionException {
        if (loc == null) {
            return false;
        }
        return rc.canRemoveDirt(loc);
    }

    /**
     * Digs (removes dirt) at the specified location if possible.
     * Uses Battlecode 2026 API: rc.removeDirt()
     * @return true if dug successfully
     */
    public boolean dig(RobotController rc, MapLocation loc) throws GameActionException {
        if (rc.canRemoveDirt(loc)) {
            rc.removeDirt(loc);
            return true;
        }
        return false;
    }

    /**
     * Places dirt at the specified location if possible.
     * Uses Battlecode 2026 API: rc.placeDirt()
     * @return true if placed successfully
     */
    public boolean placeDirt(RobotController rc, MapLocation loc) throws GameActionException {
        if (rc.canPlaceDirt(loc)) {
            rc.placeDirt(loc);
            return true;
        }
        return false;
    }
}
