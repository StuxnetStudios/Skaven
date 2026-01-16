package Skaven.V1Bot;

import battlecode.common.*;

/**
 * Helper class for combat operations.
 */
public class CombatHelper {

    /**
     * Finds the best enemy target to attack.
     */
    public RobotInfo findBestTarget(RobotInfo[] enemies) {
        if (enemies == null || enemies.length == 0) {
            return null;
        }
        
        RobotInfo bestTarget = null;
        int lowestHealth = Integer.MAX_VALUE;
        
        for (RobotInfo enemy : enemies) {
            if (enemy.health < lowestHealth) {
                lowestHealth = enemy.health;
                bestTarget = enemy;
            }
        }
        return bestTarget;
    }

    /**
     * Attempts to attack an enemy at the given location.
     */
    public boolean tryAttack(RobotController rc, MapLocation target) throws GameActionException {
        if (target != null && rc.canAttack(target)) {
            rc.attack(target);
            return true;
        }
        return false;
    }

    /**
     * Finds nearest enemy robot.
     */
    public RobotInfo findNearestEnemy(RobotController rc, RobotInfo[] enemies) {
        if (enemies == null || enemies.length == 0) {
            return null;
        }

        MapLocation myLoc = rc.getLocation();
        RobotInfo nearest = null;
        int minDist = Integer.MAX_VALUE;

        for (RobotInfo enemy : enemies) {
            int dist = myLoc.distanceSquaredTo(enemy.location);
            if (dist < minDist) {
                minDist = dist;
                nearest = enemy;
            }
        }
        return nearest;
    }

    /**
     * Check if we should retreat based on health.
     */
    public boolean shouldRetreat(RobotController rc) {
        return rc.getHealth() < rc.getType().health / 4;
    }
}
