package skaven;

import battlecode.common.*;

/**
 * Handles robot communication via squeaks.
 * Battlecode 2026 uses rc.squeak() to send 4-byte (32-bit) messages.
 * 
 * Bit layout (using lower 10 bits for compatibility):
 *   Bit 0: Cat spotted (DANGER!)
 *   Bit 1: Need help
 *   Bit 2: Attacking
 *   Bit 3: Retreating
 *   Bit 4: Rally point
 *   Bit 5: Cheese found
 *   Bit 6: Rat trap nearby
 *   Bit 7: Cat trap placed
 *   Bit 8: Danger zone
 *   Bit 9: Re-squeak (this message was heard and repeated)
 * 
 * Bits 10-25: Location encoding (x: bits 10-17, y: bits 18-25)
 */
public class Communication {
    
    // Bit flags for squeak types (lower 10 bits)
    public static final int BIT_CAT_SPOTTED   = 1 << 0;  // 1
    public static final int BIT_NEED_HELP     = 1 << 1;  // 2
    public static final int BIT_ATTACKING     = 1 << 2;  // 4
    public static final int BIT_RETREATING    = 1 << 3;  // 8
    public static final int BIT_RALLY         = 1 << 4;  // 16
    public static final int BIT_CHEESE_FOUND  = 1 << 5;  // 32
    public static final int BIT_RAT_TRAP      = 1 << 6;  // 64
    public static final int BIT_CAT_TRAP      = 1 << 7;  // 128
    public static final int BIT_DANGER_ZONE   = 1 << 8;  // 256
    public static final int BIT_RESQUEAK      = 1 << 9;  // 512

    // Bit positions for location encoding
    private static final int X_SHIFT = 10;
    private static final int Y_SHIFT = 18;
    private static final int COORD_MASK = 0xFF; // 8 bits for each coordinate (0-255)

    /**
     * Encodes a squeak message with bits and location.
     */
    private static int encodeMessage(int bits, MapLocation location) {
        int x = location.x & COORD_MASK;
        int y = location.y & COORD_MASK;
        return (bits & 0x3FF) | (x << X_SHIFT) | (y << Y_SHIFT);
    }

    /**
     * Decodes location from a squeak message.
     */
    private static MapLocation decodeLocation(int message) {
        int x = (message >> X_SHIFT) & COORD_MASK;
        int y = (message >> Y_SHIFT) & COORD_MASK;
        return new MapLocation(x, y);
    }

    /**
     * Decodes bits from a squeak message.
     */
    private static int decodeBits(int message) {
        return message & 0x3FF;
    }

    /**
     * Sends a squeak message to nearby teammates.
     * @param rc RobotController
     * @param bits Bitwise OR of BIT_* constants
     * @param location Location relevant to the squeak
     * @return true if squeak was sent successfully
     */
    public static boolean squeak(RobotController rc, int bits, MapLocation location) throws GameActionException {
        int message = encodeMessage(bits, location);
        return rc.squeak(message);
    }

    /**
     * Sends a squeak at the robot's current location.
     */
    public static boolean squeak(RobotController rc, int bits) throws GameActionException {
        return squeak(rc, bits, rc.getLocation());
    }

    /**
     * Re-squeaks a heard message, adding the RESQUEAK bit.
     * Use this when a rat has nothing new to report but wants to relay info.
     */
    public static boolean resqueak(RobotController rc, Squeak heard) throws GameActionException {
        if (heard != null) {
            int newBits = heard.bits | BIT_RESQUEAK;
            return squeak(rc, newBits, heard.location);
        }
        return false;
    }

    /**
     * Reads all recent squeaks from teammates (within last 5 rounds).
     * @return Array of Squeak objects, or empty array if none
     */
    public static Squeak[] hearSqueaks(RobotController rc) throws GameActionException {
        Message[] messages = rc.readSqueaks(-1); // -1 = all messages from past 5 rounds
        
        if (messages == null || messages.length == 0) {
            return new Squeak[0];
        }

        Squeak[] squeaks = new Squeak[messages.length];
        for (int i = 0; i < messages.length; i++) {
            int content = messages[i].getBytes();
            int bits = decodeBits(content);
            MapLocation loc = decodeLocation(content);
            int round = messages[i].getRound();
            int senderID = messages[i].getSenderID();
            squeaks[i] = new Squeak(bits, loc, round, senderID);
        }
        return squeaks;
    }

    /**
     * Reads squeaks from a specific round.
     */
    public static Squeak[] hearSqueaks(RobotController rc, int roundNum) throws GameActionException {
        Message[] messages = rc.readSqueaks(roundNum);
        
        if (messages == null || messages.length == 0) {
            return new Squeak[0];
        }

        Squeak[] squeaks = new Squeak[messages.length];
        for (int i = 0; i < messages.length; i++) {
            int content = messages[i].getBytes();
            int bits = decodeBits(content);
            MapLocation loc = decodeLocation(content);
            int round = messages[i].getRound();
            int senderID = messages[i].getSenderID();
            squeaks[i] = new Squeak(bits, loc, round, senderID);
        }
        return squeaks;
    }

    /**
     * Finds the most recent squeak with a specific flag.
     */
    public static Squeak findSqueak(RobotController rc, int flagBit) throws GameActionException {
        Squeak[] squeaks = hearSqueaks(rc);
        Squeak mostRecent = null;
        
        for (Squeak s : squeaks) {
            if (s.hasFlag(flagBit)) {
                if (mostRecent == null || s.round > mostRecent.round) {
                    mostRecent = s;
                }
            }
        }
        return mostRecent;
    }

    /**
     * Checks if any cat has been spotted recently.
     */
    public static Squeak findCatAlert(RobotController rc) throws GameActionException {
        return findSqueak(rc, BIT_CAT_SPOTTED);
    }

    /**
     * Checks if any cheese has been found recently.
     */
    public static Squeak findCheeseAlert(RobotController rc) throws GameActionException {
        return findSqueak(rc, BIT_CHEESE_FOUND);
    }

    /**
     * Checks if there's a rally point.
     */
    public static Squeak findRallyPoint(RobotController rc) throws GameActionException {
        return findSqueak(rc, BIT_RALLY);
    }

    /**
     * Data class representing a squeak message.
     */
    public static class Squeak {
        public final int bits;
        public final MapLocation location;
        public final int round;
        public final int senderID;

        public Squeak(int bits, MapLocation location, int round, int senderID) {
            this.bits = bits;
            this.location = location;
            this.round = round;
            this.senderID = senderID;
        }

        public boolean hasFlag(int flag) {
            return (bits & flag) != 0;
        }

        public boolean isResqueak() {
            return hasFlag(BIT_RESQUEAK);
        }

        public boolean hasCatSpotted() { return hasFlag(BIT_CAT_SPOTTED); }
        public boolean hasNeedHelp() { return hasFlag(BIT_NEED_HELP); }
        public boolean hasAttacking() { return hasFlag(BIT_ATTACKING); }
        public boolean hasRetreating() { return hasFlag(BIT_RETREATING); }
        public boolean hasRally() { return hasFlag(BIT_RALLY); }
        public boolean hasCheeseFound() { return hasFlag(BIT_CHEESE_FOUND); }
        public boolean hasRatTrap() { return hasFlag(BIT_RAT_TRAP); }
        public boolean hasCatTrap() { return hasFlag(BIT_CAT_TRAP); }
        public boolean hasDangerZone() { return hasFlag(BIT_DANGER_ZONE); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Squeak[");
            if (hasCatSpotted()) sb.append("CAT! ");
            if (hasNeedHelp()) sb.append("HELP ");
            if (hasAttacking()) sb.append("ATTACK ");
            if (hasRetreating()) sb.append("RETREAT ");
            if (hasRally()) sb.append("RALLY ");
            if (hasCheeseFound()) sb.append("CHEESE ");
            if (hasRatTrap()) sb.append("RAT_TRAP ");
            if (hasCatTrap()) sb.append("CAT_TRAP ");
            if (hasDangerZone()) sb.append("DANGER ");
            if (isResqueak()) sb.append("(RE) ");
            sb.append("@ ").append(location);
            sb.append(" r").append(round);
            sb.append(" from:").append(senderID);
            sb.append("]");
            return sb.toString();
        }
    }
}
