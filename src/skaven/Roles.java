package skaven;

import battlecode.common.*;

/**
 * Defines different role behaviors for robots.
 * Roles determine how a robot prioritizes actions.
 */
public class Roles {

    public enum RoleType {
        SCOUT,      // Explores map, finds cheese and enemies
        GATHERER,   // Collects cheese and brings it back
        SOLDIER     // Balanced - combat and territory control
    }

    /**
     * Scout role - explores and finds resources/enemies.
     */
    public static class Scout {
        
        /**
         * Execute scout behavior for a turn.
         */
        public static void run(RobotController rc, MovementHelper movement) throws GameActionException {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            
            // Priority 1: Flee from cats!
            for (RobotInfo enemy : enemies) {
                if (enemy.type == UnitType.CAT) {
                    movement.moveAway(rc, enemy.location);
                    return;
                }
            }
            
            // Priority 2: Report enemy positions via squeak
            if (enemies.length > 0) {
                RobotInfo nearest = findNearest(rc, enemies);
                if (nearest != null) {
                    Communication.squeak(rc, Communication.BIT_CAT_SPOTTED, nearest.location);
                }
            }
            
            // Priority 3: Look for cheese and report it
            MapInfo[] nearby = rc.senseNearbyMapInfos();
            for (MapInfo info : nearby) {
                if (info.getCheeseAmount() > 0 || info.hasCheeseMine()) {
                    Communication.squeak(rc, Communication.BIT_CHEESE_FOUND, info.getMapLocation());
                    break;
                }
            }
            
            // Priority 4: Explore unexplored areas
            if (rc.isMovementReady()) {
                MapLocation unexplored = findUnexploredArea(rc);
                if (unexplored != null) {
                    movement.moveToward(rc, unexplored);
                } else {
                    movement.moveRandom(rc);
                }
            }
        }
        
        private static MapLocation findUnexploredArea(RobotController rc) throws GameActionException {
            // Move toward edges of vision or unvisited areas
            MapLocation myLoc = rc.getLocation();
            Direction dir = rc.getDirection();
            
            // Prefer continuing in current direction
            MapLocation ahead = myLoc.add(dir).add(dir).add(dir);
            if (rc.onTheMap(ahead) && !GameState.hasVisited(ahead)) {
                return ahead;
            }
            return null;
        }
    }

    /**
     * Gatherer role - collects cheese and brings it to rat king.
     */
    public static class Gatherer {
        
        /**
         * Execute gatherer behavior for a turn.
         */
        public static void run(RobotController rc, MovementHelper movement) throws GameActionException {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            
            // Priority 1: Flee from cats!
            for (RobotInfo enemy : enemies) {
                if (enemy.type == UnitType.CAT) {
                    movement.moveAway(rc, enemy.location);
                    return;
                }
            }
            
            // Priority 2: Pick up nearby cheese
            if (rc.isActionReady()) {
                MapInfo[] nearby = rc.senseNearbyMapInfos();
                for (MapInfo info : nearby) {
                    MapLocation cheeseLoc = info.getMapLocation();
                    if (info.getCheeseAmount() > 0 && rc.canPickUpCheese(cheeseLoc)) {
                        rc.pickUpCheese(cheeseLoc);
                        break;
                    }
                }
            }
            
            // Priority 3: If carrying cheese, find rat king to deposit
            if (rc.getRawCheese() > 0) {
                RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
                for (RobotInfo ally : allies) {
                    if (ally.type == UnitType.RAT_KING) {
                        if (rc.canTransferCheese(ally.location, rc.getRawCheese())) {
                            rc.transferCheese(ally.location, rc.getRawCheese());
                        } else {
                            movement.moveToward(rc, ally.location);
                        }
                        return;
                    }
                }
            }
            
            // Priority 4: Move toward cheese
            if (rc.isMovementReady()) {
                MapLocation cheeseTarget = findCheeseTarget(rc);
                if (cheeseTarget != null) {
                    movement.moveToward(rc, cheeseTarget);
                } else {
                    movement.moveRandom(rc);
                }
            }
        }
        
        private static MapLocation findCheeseTarget(RobotController rc) throws GameActionException {
            MapInfo[] nearby = rc.senseNearbyMapInfos();
            
            // First look for actual cheese
            for (MapInfo info : nearby) {
                if (info.getCheeseAmount() > 0) {
                    return info.getMapLocation();
                }
            }
            
            // Then look for cheese mines
            for (MapInfo info : nearby) {
                if (info.hasCheeseMine()) {
                    return info.getMapLocation();
                }
            }
            
            // Check remembered cheese locations
            return GameState.findRememberedCheese(rc.getLocation(), 50, rc.getRoundNum());
        }
    }

    /**
     * Soldier role - balanced approach to combat and survival.
     */
    public static class Soldier {
        
        /**
         * Execute soldier behavior for a turn.
         */
        public static void run(RobotController rc, MovementHelper movement) throws GameActionException {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            
            // Priority 1: Flee from cats!
            for (RobotInfo enemy : enemies) {
                if (enemy.type == UnitType.CAT) {
                    movement.moveAway(rc, enemy.location);
                    Communication.squeak(rc, Communication.BIT_CAT_SPOTTED | Communication.BIT_DANGER_ZONE, enemy.location);
                    return;
                }
            }
            
            // Priority 2: Attack if we can
            if (rc.isActionReady() && enemies.length > 0) {
                RobotInfo target = findBestTarget(enemies);
                if (target != null && rc.canAttack(target.location)) {
                    rc.attack(target.location);
                }
            }
            
            // Priority 3: Collect cheese opportunistically
            if (rc.isActionReady()) {
                MapInfo[] nearby = rc.senseNearbyMapInfos();
                for (MapInfo info : nearby) {
                    MapLocation cheeseLoc = info.getMapLocation();
                    if (info.getCheeseAmount() > 0 && rc.canPickUpCheese(cheeseLoc)) {
                        rc.pickUpCheese(cheeseLoc);
                        break;
                    }
                }
            }
            
            // Priority 4: Tactical movement
            if (rc.isMovementReady()) {
                if (enemies.length > 0 && shouldEngage(rc, enemies)) {
                    RobotInfo nearest = findNearest(rc, enemies);
                    movement.moveToward(rc, nearest.location);
                } else {
                    movement.moveRandom(rc);
                }
            }
        }
        
        private static RobotInfo findBestTarget(RobotInfo[] enemies) {
            RobotInfo best = null;
            int lowestHealth = Integer.MAX_VALUE;
            
            for (RobotInfo enemy : enemies) {
                if (enemy.health < lowestHealth) {
                    lowestHealth = enemy.health;
                    best = enemy;
                }
            }
            return best;
        }
        
        private static boolean shouldEngage(RobotController rc, RobotInfo[] enemies) {
            // Never engage cats alone
            for (RobotInfo enemy : enemies) {
                if (enemy.type == UnitType.CAT) {
                    return false;
                }
            }
            
            // Engage other rats if we have health advantage
            int myHealth = rc.getHealth();
            return myHealth > rc.getType().health / 2;
        }
    }

    /**
     * Utility method to find nearest robot.
     */
    private static RobotInfo findNearest(RobotController rc, RobotInfo[] robots) {
        if (robots == null || robots.length == 0) return null;
        
        MapLocation myLoc = rc.getLocation();
        RobotInfo nearest = null;
        int minDist = Integer.MAX_VALUE;
        
        for (RobotInfo robot : robots) {
            int dist = myLoc.distanceSquaredTo(robot.location);
            if (dist < minDist) {
                minDist = dist;
                nearest = robot;
            }
        }
        return nearest;
    }
}
