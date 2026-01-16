package V1Bot;

import battlecode.common.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
@SuppressWarnings("unused")
public class RobotPlayer {

    /**
     * The bot strategy to use. Can be swapped for different implementations.
     */
    private static BotStrategy strategy;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc The RobotController object. Provides information about the robot's state and
     *           allows issuing commands.
     **/
    public static void run(RobotController rc) {
        // Initialize the strategy - can be swapped for different bot implementations
        strategy = createStrategy(rc);

        // Main game loop
        while (true) {
            try {
                strategy.executeTurn(rc);
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    /**
     * Factory method for creating the bot strategy.
     * Override this to use different strategies.
     */
    private static BotStrategy createStrategy(RobotController rc) {
        // Default strategy - can be replaced with other implementations
        return new DefaultStrategy(rc.getID());
    }
}
