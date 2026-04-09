/**
 * AODVRouter.java
 * 
 * Coordinates the AODV route discovery process across all nodes in the network.
 * 
 * This class is the "brain" of our AODV simulator. It:
 *   - Maintains per-node state that we cannot store in Node.java:
 *       * RoutingTable (routes to destinations)
 *       * Sequence number (each node's own counter)
 *       * Seen RREQs (to suppress duplicate broadcasts)
 *       * Reverse-path predecessor map (for RREP forwarding)
 *       * RREP sender tracking (who last forwarded RREP, for route learning)
 *   - Implements the RREQ flood: when a source has no route to dest,
 *     it broadcasts RREQ; intermediates rebroadcast until dest (or
 *     a node with a fresh route) is reached.
 *   - Implements RREP propagation: the answering node sends RREP back
 *     along the reverse path; each intermediate learns a route to dest.
 *   - Prints a clear trace so you can follow the discovery step-by-step.
 * 
 * Key AODV concepts demonstrated:
 *   - On-demand: routes discovered only when needed
 *   - Flooding: RREQ is broadcast to all neighbors
 *   - Reverse path: RREQ builds a path back to source
 *   - Forward path: RREP builds routes toward destination
 *   - Sequence numbers: ensure we use fresh routes, not stale ones
 * 
 * Usage from Main:
 *   AODVRouter router = new AODVRouter(network);
 *   List<Node> route = router.discoverRoute(sourceNode, destNodeId);
 *   // route is [source, hop1, hop2, ..., dest] or null if unreachable
 */
import java.util.*;

public class AODVRouter {

    // --- Per-node state (external to Node.java) ---

    // Each node has its own routing table
    private Map<Node, RoutingTable> tables;

    // Each node has its own sequence number (incremented on RREQ/RREP origin)
    private Map<Node, Integer> seqNums;

    // Seen RREQs per node: key = "sourceId:rreqId", value = true if processed
    // Used to avoid re-broadcasting the same RREQ
    private Map<Node, Set<String>> seenRreqs;

    // Reverse-path predecessor: for each node, map (sourceId:rreqId) -> predecessor Node
    // When RREP comes, we forward it to this predecessor toward source
    private Map<Node, Map<String, Node>> reversePredecessors;

    // RREP sender tracking: key = "destId:sourceId" -> who last forwarded RREP
    // Used to know nextHop toward dest when learning route from RREP
    private Map<String, Node> rrepSender;

    // The network we operate on
    private Network network;

    // Global RREQ ID counter (unique across all floods)
    private int nextRreqId = 1;

    /**
     * Creates a new AODVRouter bound to a Network.
     * Call this after nodes are added and neighbor discovery is done.
     */
    public AODVRouter(Network network) {
        this.network = network;
        this.tables = new HashMap<>();
        this.seqNums = new HashMap<>();
        this.seenRreqs = new HashMap<>();
        this.reversePredecessors = new HashMap<>();
        this.rrepSender = new HashMap<>();

        // Initialize per-node state for all existing nodes
        for (Node n : network.getNodes()) {
            tables.put(n, new RoutingTable());
            seqNums.put(n, 0);
            seenRreqs.put(n, new HashSet<>());
            reversePredecessors.put(n, new HashMap<>());
        }
    }

    // ==================== Public API ====================

    /**
     * Main entry point: discover a route from source to destId.
     * 
     * If source already has a valid route, returns it immediately.
     * Otherwise, initiates RREQ flood, waits for RREP, and returns the
     * discovered route as a list of Nodes [source, hop1, ..., dest].
     * 
     * @return route list, or null if destination is unreachable
     */
    public List<Node> discoverRoute(Node source, int destId) {
        System.out.println("\n>>> AODV: Source Node " + source.getId() +
                " wants a route to destination " + destId);

        // If source IS the destination, trivial route
        if (source.getId() == destId) {
            System.out.println("    (Source is destination; trivial route)");
            List<Node> r = new ArrayList<>();
            r.add(source);
            return r;
        }

        // Check if we already have a valid route
        RoutingTable rt = tables.get(source);
        if (rt != null && rt.hasValidRoute(destId)) {
            System.out.println("    (Valid route already in routing table)");
            return buildRouteList(source, destId);
        }

        // No route -> initiate RREQ flood
        System.out.println("    No valid route. Initiating RREQ flood...");

        // Increment our own sequence number (we are originating a RREQ)
        int mySeq = seqNums.get(source) + 1;
        seqNums.put(source, mySeq);

        // Create the RREQ
        int rreqId = nextRreqId++;
        // destSeqNum: last known by source (we use 0 meaning unknown)
        int knownDestSeq = rt != null ? rt.getDestSeqNum(destId) : -1;
        if (knownDestSeq < 0) knownDestSeq = 0;

        RREQ rreq = new RREQ(source.getId(), destId, mySeq, knownDestSeq, rreqId);

        // Mark as seen by source (so we don't reprocess if we hear it back)
        markSeen(source, source.getId(), rreqId);

        // Broadcast RREQ from source
        broadcastRREQ(source, rreq);

        // After flood, check if we now have a route
        rt = tables.get(source); // may have been updated
        if (rt != null && rt.hasValidRoute(destId)) {
            System.out.println("\n>>> Route discovery complete!");
            List<Node> route = buildRouteList(source, destId);
            printRoute(route);
            return route;
        } else {
            System.out.println("\n>>> No route found to destination " + destId);
            return null;
        }
    }

    /**
     * Returns the routing table for a node (for inspection/debug).
     */
    public RoutingTable getRoutingTable(Node node) {
        return tables.get(node);
    }

    /**
     * Returns a node's current sequence number.
     */
    public int getSeqNum(Node node) {
        return seqNums.getOrDefault(node, 0);
    }

    // ==================== RREQ Handling ====================

    /**
     * Broadcasts (simulates) a RREQ from a node to all its neighbors.
     * Each neighbor will process it via receiveRREQ().
     */
    private void broadcastRREQ(Node fromNode, RREQ rreq) {
        System.out.println("    [Node " + fromNode.getId() + "] BROADCASTS " + rreq);

        for (Node neighbor : fromNode.getNeighbors()) {
            // Simulate receiving at neighbor
            receiveRREQ(neighbor, rreq, fromNode);
        }
    }

    /**
     * Called when a node receives a RREQ from one of its neighbors.
     * 
     * Logic:
     *   1. Check if we've already processed this (sourceId, rreqId) pair.
     *      If yes, ignore (suppress duplicate).
     *   2. Record the predecessor (fromNeighbor) for reverse path.
     *   3. If WE are the destination -> generate RREP.
     *   4. Else if we have a fresh enough route to dest -> generate RREP.
     *   5. Else -> rebroadcast to our neighbors (after incrementing hopCount).
     */
    private void receiveRREQ(Node node, RREQ incoming, Node fromNeighbor) {
        // 1. Duplicate suppression
        String key = incoming.sourceId + ":" + incoming.rreqId;
        Set<String> seen = seenRreqs.get(node);
        if (seen.contains(key)) {
            // Already processed this RREQ; ignore
            return;
        }
        // Mark as seen
        seen.add(key);

        // 2. Record reverse predecessor: from whom we received this RREQ
        //    This is the next hop toward source when we forward RREP.
        reversePredecessors.get(node).put(key, fromNeighbor);

        // Build our version of RREQ (with incremented hopCount and extended reversePath)
        RREQ rreq = new RREQ(incoming, fromNeighbor);

        System.out.println("    [Node " + node.getId() + "] RECEIVES " + rreq +
                " from Node " + fromNeighbor.getId());

        int destId = rreq.destId;

        // 3. Are we the destination?
        if (node.getId() == destId) {
            System.out.println("    [Node " + node.getId() + "] IS THE DESTINATION. Generating RREP.");

            // Increment our sequence number (we are replying)
            int mySeq = seqNums.get(node) + 1;
            seqNums.put(node, mySeq);

            // Dest seq num in RREP: max(our seq, RREQ's destSeqNum)
            int replySeq = Math.max(mySeq, rreq.destSeqNum);

            // RREP hopCount = 0 (we are dest)
            RREP rrep = new RREP(destId, replySeq, rreq.sourceId, 0, rreq.getReversePath());

            // Send RREP back toward source
            sendRREP(node, rrep, rreq.rreqId);
            return;
        }

        // 4. Do we have a fresh enough route to dest?
        RoutingTable rt = tables.get(node);
        if (rt != null && rt.hasValidRoute(destId)) {
            int ourSeq = rt.getDestSeqNum(destId);
            int ourHops = rt.getHopCount(destId);
            // "Fresh enough": our destSeqNum >= RREQ.destSeqNum
            if (ourSeq >= rreq.destSeqNum) {
                System.out.println("    [Node " + node.getId() +
                        "] HAS A FRESH ROUTE to dest (seq=" + ourSeq + ", hops=" + ourHops +
                        "). Generating RREP.");

                // Use our known seq (or max with RREQ's)
                int replySeq = Math.max(ourSeq, rreq.destSeqNum);

                // hopCount in RREP = our hops to dest
                RREP rrep = new RREP(destId, replySeq, rreq.sourceId, ourHops, rreq.getReversePath());

                sendRREP(node, rrep, rreq.rreqId);
                return;
            }
        }

        // 5. Otherwise, rebroadcast to our neighbors
        System.out.println("    [Node " + node.getId() + "] REBROADCASTS RREQ (no reply possible here).");
        broadcastRREQ(node, rreq);
    }

    // ==================== RREP Handling ====================

    /**
     * Sends an RREP from a node toward the source, following the reverse path.
     * 
     * @param fromNode  the node originating/forwarding this RREP
     * @param rrep      the RREP message
     * @param rreqId    the RREQ ID this RREP is answering (needed for reverse path lookup)
     */
    private void sendRREP(Node fromNode, RREP rrep, int rreqId) {
        System.out.println("    [Node " + fromNode.getId() + "] SENDS " + rrep + " toward source " + rrep.sourceId);

        // Record who is sending (for route learning at recipient)
        String rkey = rrep.destId + ":" + rrep.sourceId;
        rrepSender.put(rkey, fromNode);

        // Find next hop toward source using the reverse path stored in RREQ time
        // We stored reversePredecessors[node].get(sourceId:rreqId) = predecessor
        String rkeyPre = rrep.sourceId + ":" + rreqId;
        Node nextTowardSource = reversePredecessors.get(fromNode).get(rkeyPre);

        // If fromNode is the destination, its predecessor is the last of reversePath
        if (nextTowardSource == null && fromNode.getId() == rrep.destId) {
            List<Node> path = rrep.getPathToSource();
            if (!path.isEmpty()) {
                nextTowardSource = path.get(path.size() - 1);
            }
        }

        if (nextTowardSource != null) {
            // Forward RREP to next hop toward source
            // The recipient will learn: nextHop to dest = fromNode
            forwardRREP(nextTowardSource, rrep, fromNode, rreqId);
        } else if (fromNode.getId() == rrep.sourceId) {
            // We are the source; RREP delivered
            System.out.println("    [Node " + fromNode.getId() + "] RREP REACHED SOURCE.");
        }
    }

    /**
     * Forwards the RREP toward the source.
     * 
     * @param node       the node receiving/forwarding the RREP
     * @param rrep       the RREP
     * @param fromNode   who sent RREP to us (this is our nextHop toward dest)
     * @param rreqId     the RREQ ID for reverse path lookup
     */
    private void forwardRREP(Node node, RREP rrep, Node fromNode, int rreqId) {
        int myId = node.getId();
        String rkey = rrep.destId + ":" + rrep.sourceId;

        // Record: fromNode is the sender of RREP to us = nextHop toward dest
        rrepSender.put(rkey, fromNode);

        // Update our routing table: learn route to dest via fromNode
        RoutingTable rt = tables.get(node);
        if (rt != null) {
            int newHopCount = rrep.hopCount + 1;
            rt.forceUpdateRoute(rrep.destId, fromNode, newHopCount, rrep.destSeqNum);
        }
        System.out.println("    [Node " + myId + "] LEARNS route to dest " + rrep.destId +
                " via Node " + fromNode.getId() + " (hops=" + (rrep.hopCount + 1) + ")");

        // If we are the source, we're done
        if (myId == rrep.sourceId) {
            System.out.println("    [Node " + myId + "] RREP REACHED SOURCE. Route established.");
            return;
        }

        // Otherwise, forward toward source
        String rkeyPre = rrep.sourceId + ":" + rreqId;
        Node nextTowardSource = reversePredecessors.get(node).get(rkeyPre);

        // Fallback: use pathToSource to find predecessor
        if (nextTowardSource == null) {
            List<Node> path = rrep.getPathToSource();
            nextTowardSource = findPredecessorTowardSource(node, path);
        }

        if (nextTowardSource != null) {
            // Create forwarded RREP (increment hopCount)
            RREP forwarded = new RREP(rrep);
            // Record that WE are now the sender for the next hop
            rrepSender.put(rkey, node);
            forwardRREP(nextTowardSource, forwarded, node, rreqId);
        }
    }

    // ==================== Helper methods ====================

    /**
     * Mark a RREQ (sourceId, rreqId) as seen at a node.
     */
    private void markSeen(Node node, int sourceId, int rreqId) {
        String key = sourceId + ":" + rreqId;
        seenRreqs.get(node).add(key);
    }

    /**
     * Find the predecessor toward source in the RREQ reversePath.
     * path = [source, n1, n2, ..., predOfDest]
     * If node N is at index i in path, predecessor = path[i-1] (toward source).
     * If N is not in path (e.g., dest itself), return last element (predOfDest).
     */
    private Node findPredecessorTowardSource(Node node, List<Node> path) {
        int myId = node.getId();
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).getId() == myId) {
                if (i > 0) return path.get(i - 1);
                return null; // we are the source
            }
        }
        // Not in path: we are likely the dest or a node not on the recorded path.
        // If we are dest, predecessor toward source = last of path (predOfDest).
        if (path.size() > 0) return path.get(path.size() - 1);
        return null;
    }

    private Node findNodeById(int id) {
        for (Node n : network.getNodes()) {
            if (n.getId() == id) return n;
        }
        return null;
    }

    /**
     * Build a hop-by-hop route list from source to dest using routing tables.
     * Follows nextHop pointers until reaching dest.
     */
    private List<Node> buildRouteList(Node source, int destId) {
        List<Node> route = new ArrayList<>();
        Set<Integer> visited = new HashSet<>(); // cycle guard
        Node cur = source;
        route.add(cur);
        visited.add(cur.getId());

        while (cur.getId() != destId) {
            RoutingTable rt = tables.get(cur);
            if (rt == null || !rt.hasValidRoute(destId)) {
                return null; // incomplete route
            }
            Node next = rt.getNextHop(destId);
            if (next == null || visited.contains(next.getId())) {
                return null; // broken or loop
            }
            route.add(next);
            visited.add(next.getId());
            cur = next;
        }
        return route;
    }

    /**
     * Print a route in a readable hop-by-hop format.
     */
    private void printRoute(List<Node> route) {
        if (route == null || route.isEmpty()) {
            System.out.println("    (no route)");
            return;
        }
        System.out.print("    Final route: ");
        for (int i = 0; i < route.size(); i++) {
            if (i > 0) System.out.print(" -> ");
            System.out.print(route.get(i).getId());
        }
        System.out.println();
    }
}