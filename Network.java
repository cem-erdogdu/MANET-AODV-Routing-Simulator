/**
 * Network.java
 * 
 * Represents the entire MANET (Mobile Ad-hoc Network) in our simulation.
 * 
 * Responsibilities:
 *   - Holds a collection of all Node objects in the simulation
 *   - Manages a shared radio range parameter (in same units as node coordinates)
 *   - Implements neighbor discovery: for each pair of nodes, if their
 *     Euclidean distance <= radioRange, they are considered neighbors
 * 
 * How neighbor discovery works:
 *   - In a real wireless ad-hoc network, nodes discover neighbors by
 *     broadcasting "HELLO" packets. Nodes within radio range can hear
 *     each other and establish a link.
 *   - In our simulator, we simulate this geometrically:
 *     if distance(nodeA, nodeB) <= radioRange, they are mutual neighbors.
 *   - We re-compute this on demand (e.g., after node movement).
 * 
 * This class uses only plain Java (no external libs) as required.
 */
import java.util.*;

public class Network {

    // All nodes in the simulation
    private List<Node> nodes;

    // Radio range: maximum distance at which two nodes can communicate
    // If two nodes are within this distance, they are neighbors.
    private double radioRange;

    /**
     * Creates a new Network with a given radio range.
     *
     * @param radioRange the maximum distance for two nodes to be neighbors
     */
    public Network(double radioRange) {
        this.nodes = new ArrayList<>();
        this.radioRange = radioRange;
    }

    // ==================== Node Management ====================

    /**
     * Adds a node to the network.
     * Note: The caller is responsible for ensuring node IDs are unique.
     *
     * @param node the node to add
     */
    public void addNode(Node node) {
        if (node != null && !nodes.contains(node)) {
            nodes.add(node);
        }
    }

    /**
     * Returns an unmodifiable list of all nodes in the network.
     */
    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Returns the number of nodes currently in the network.
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Returns the radio range used for neighbor discovery.
     */
    public double getRadioRange() {
        return radioRange;
    }

    // ==================== Neighbor Discovery ====================

    /**
     * Performs neighbor discovery across the entire network.
     * 
     * Algorithm (O(n^2) for simplicity, fine for small networks):
     *   1. Clear all existing neighbor relationships
     *   2. For every unique pair of nodes (i, j) where i < j:
     *        - Compute distance d = nodes[i].distanceTo(nodes[j])
     *        - If d <= radioRange:
     *            - nodes[i].addNeighbor(nodes[j])
     *            - nodes[j].addNeighbor(nodes[i])  // bidirectional
     * 
     * This simulates the wireless broadcast nature: if A can hear B,
     * then B can also hear A (assuming symmetric radio).
     * 
     * In a real MANET, this would be triggered periodically or when
     * nodes move, to keep the topology up-to-date.
     */
    public void discoverNeighbors() {
        // Step 1: Clear existing neighbor links for all nodes
        for (Node node : nodes) {
            node.clearNeighbors();
        }

        // Step 2: Check all unique pairs
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Node a = nodes.get(i);
                Node b = nodes.get(j);
                double distance = a.distanceTo(b);

                if (distance <= radioRange) {
                    // They can communicate -> mutual neighbors
                    a.addNeighbor(b);
                    b.addNeighbor(a);
                }
                // If distance > radioRange, they are NOT neighbors
                // (no explicit action needed since we cleared first)
            }
        }
    }

    // ==================== Utility / Debugging ====================

    /**
     * Prints a summary of the network: number of nodes, radio range,
     * and each node's position and current neighbors.
     */
    public void printNetworkState() {
        System.out.println("=== MANET Network State ===");
        System.out.println("Nodes: " + nodes.size());
        System.out.println("Radio Range: " + radioRange);
        System.out.println("---------------------------");
        for (Node node : nodes) {
            System.out.println("  " + node);
        }
        System.out.println("===========================");
    }
}