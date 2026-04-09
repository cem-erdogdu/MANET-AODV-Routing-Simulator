/**
 * RoutingTable.java
 * 
 * Stores and manages routing entries for a single node in the MANET.
 * 
 * Each entry in the table maps a destination node ID to routing info:
 *   - nextHop: the neighboring Node to forward packets toward the destination
 *   - hopCount: number of hops from here to destination
 *   - destSeqNum: the destination sequence number (ensures route freshness)
 *   - isValid: whether this route is currently usable
 * 
 * AODV uses destination sequence numbers to ensure we don't use stale routes.
 * A higher sequence number means a fresher route. When comparing two routes
 * with the same seqNum, the one with fewer hops is preferred.
 * 
 * This class is kept separate from Node.java (we cannot modify Node).
 * The AODVRouter will associate each Node with its own RoutingTable instance.
 */
import java.util.*;

public class RoutingTable {

    /**
     * Represents a single routing entry toward a destination.
     */
    public static class RouteEntry {
        public final Node nextHop;       // Next node to forward to
        public final int hopCount;       // Hops from this node to dest
        public final int destSeqNum;     // Destination's sequence number
        public final boolean isValid;    // Is this route currently valid?

        public RouteEntry(Node nextHop, int hopCount, int destSeqNum, boolean isValid) {
            this.nextHop = nextHop;
            this.hopCount = hopCount;
            this.destSeqNum = destSeqNum;
            this.isValid = isValid;
        }

        @Override
        public String toString() {
            return "RouteEntry[nextHop=" + (nextHop != null ? nextHop.getId() : "null")
                    + ", hops=" + hopCount
                    + ", seq=" + destSeqNum
                    + ", valid=" + isValid + "]";
        }
    }

    // destination node ID -> route entry
    private Map<Integer, RouteEntry> table;

    public RoutingTable() {
        this.table = new HashMap<>();
    }

    /**
     * Returns the route entry for a given destination ID, or null if none exists.
     */
    public RouteEntry getRoute(int destId) {
        return table.get(destId);
    }

    /**
     * Checks if we have a valid (usable) route to the destination.
     */
    public boolean hasValidRoute(int destId) {
        RouteEntry e = table.get(destId);
        return e != null && e.isValid;
    }

    /**
     * Returns the next hop Node toward the destination, or null if no valid route.
     */
    public Node getNextHop(int destId) {
        RouteEntry e = table.get(destId);
        if (e != null && e.isValid) {
            return e.nextHop;
        }
        return null;
    }

    /**
     * Returns the hop count to destination, or -1 if no valid route.
     */
    public int getHopCount(int destId) {
        RouteEntry e = table.get(destId);
        if (e != null && e.isValid) {
            return e.hopCount;
        }
        return -1;
    }

    /**
     * Returns the known destination sequence number, or -1 if unknown.
     */
    public int getDestSeqNum(int destId) {
        RouteEntry e = table.get(destId);
        if (e != null) {
            return e.destSeqNum;
        }
        return -1;
    }

    /**
     * Updates (or creates) a route entry toward destId.
     * 
     * In AODV, we only update if the new route is "better":
     *   - Higher destSeqNum is always better (fresher info)
     *   - If seqNum is the same, lower hopCount is better
     *   - If we have no existing entry, any new one is accepted
     * 
     * @param destId      destination node ID
     * @param nextHop     the neighbor to forward toward dest
     * @param hopCount    hops from us to dest (via nextHop)
     * @param destSeqNum  the sequence number advertised for dest
     */
    public void updateRoute(int destId, Node nextHop, int hopCount, int destSeqNum) {
        RouteEntry existing = table.get(destId);

        boolean shouldUpdate = false;
        if (existing == null || !existing.isValid) {
            // No existing valid entry -> accept new one
            shouldUpdate = true;
        } else {
            // Compare with existing
            if (destSeqNum > existing.destSeqNum) {
                // Fresher route (higher seq num) wins
                shouldUpdate = true;
            } else if (destSeqNum == existing.destSeqNum && hopCount < existing.hopCount) {
                // Same freshness but shorter path
                shouldUpdate = true;
            }
        }

        if (shouldUpdate) {
            table.put(destId, new RouteEntry(nextHop, hopCount, destSeqNum, true));
        }
    }

    /**
     * Force-updates the route (used when we are the destination and originate RREP,
     * or when we learn a route from a definitive RREP).
     * This bypasses the "is it better?" check because RREP is authoritative.
     */
    public void forceUpdateRoute(int destId, Node nextHop, int hopCount, int destSeqNum) {
        table.put(destId, new RouteEntry(nextHop, hopCount, destSeqNum, true));
    }

    /**
     * Marks a route as invalid (e.g., link break detected, or on RERR).
     * We keep the entry but mark invalid so we don't use it.
     */
    public void invalidateRoute(int destId) {
        RouteEntry e = table.get(destId);
        if (e != null) {
            // Create a new entry that is identical but invalid
            table.put(destId, new RouteEntry(e.nextHop, e.hopCount, e.destSeqNum, false));
        }
    }

    /**
     * Clears all routes (for testing/reset).
     */
    public void clear() {
        table.clear();
    }

    /**
     * Returns a human-readable dump of all routes in this table.
     */
    public String dump() {
        if (table.isEmpty()) {
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        boolean first = true;
        for (Map.Entry<Integer, RouteEntry> e : table.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("dest=").append(e.getKey()).append("->").append(e.getValue());
            first = false;
        }
        sb.append(" }");
        return sb.toString();
    }
}