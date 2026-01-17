package skaven;

import battlecode.common.*;

/**
 * Interface for bot strategies. Implement this to create different bot behaviors.
 */
public interface BotStrategy {
    /**
     * Execute one turn for the given robot.
     * @param rc The RobotController for this robot
     * @throws GameActionException if a game action fails
     */
    void executeTurn(RobotController rc) throws GameActionException;
}
