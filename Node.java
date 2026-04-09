/**
 * Node.java
 * 
 * Represents a mobile node in a MANET (Mobile Ad-hoc Network).
 * 
 * Each node has:
 *   - A unique integer ID for identification
 *   - A 2D position (x, y coordinates) representing its location in space
 *   - A set of current neighboring nodes (nodes within radio range)
 * 
 * In a MANET, nodes are mobile and can move around. Their neighbor list
 * changes dynamically as they move closer to or farther from other nodes.
 * 
 * This class is built from scratch without any external networking libraries.
 * Neighbor relationships are determined purely by geometric distance.
 */
import java.util.*;

public class Node {

    // Unique identifier for this node
    private final int id;

    // 2D position coordinates
    private double x;
    private double y;

    // Set of current neighboring nodes (bidirectional)
    // Using Set to avoid duplicates and for fast lookup
    private Set<Node> neighbors;

    /**
     * Constructs a new Node with a given ID and starting position.
     *
     * @param id unique identifier (should be unique across the network)
     * @param x  initial x-coordinate
     * @param y  initial y-coordinate
     */
    public Node(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.neighbors = new HashSet<>();
    }

    // ==================== Getters ====================

    /**
     * Returns the unique ID of this node.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the current x-coordinate of this node.
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the current y-coordinate of this node.
     */
    public double getY() {
        return y;
    }

    /**
     * Returns an unmodifiable view of the current neighbors.
     * This prevents external code from modifying the set directly.
     */
    public Set<Node> getNeighbors() {
        return Collections.unmodifiableSet(neighbors);
    }

    // ==================== Position Management ====================

    /**
     * Updates the position of this node.
     * In a real MANET simulation, nodes would move over time,
     * and neighbor discovery would be re-run to update topology.
     *
     * @param x new x-coordinate
     * @param y new y-coordinate
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // ==================== Neighbor Management ====================

    /**
     * Adds a node as a neighbor of this node.
     * Note: In our Network class, we ensure bidirectional linking.
     *
     * @param neighbor the node to add as neighbor
     */
    public void addNeighbor(Node neighbor) {
        if (neighbor != null && neighbor != this) {
            neighbors.add(neighbor);
        }
    }

    /**
     * Removes a node from this node's neighbors.
     *
     * @param neighbor the node to remove
     */
    public void removeNeighbor(Node neighbor) {
        neighbors.remove(neighbor);
    }

    /**
     * Checks if a given node is currently a neighbor.
     *
     * @param other the node to check
     * @return true if other is in this node's neighbor set
     */
    public boolean isNeighbor(Node other) {
        return neighbors.contains(other);
    }

    /**
     * Clears all neighbors. Called by Network before re-discovering.
     */
    public void clearNeighbors() {
        neighbors.clear();
    }

    // ==================== Utility Methods ====================

    /**
     * Computes the Euclidean distance from this node to another node.
     *
     * @param other the other node
     * @return distance in the same units as x/y coordinates
     */
    public double distanceTo(Node other) {
        if (other == null) return Double.MAX_VALUE;
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Two nodes are equal if they have the same ID.
     * This allows using Nodes in Sets/Maps correctly.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return id == node.id;
    }

    /**
     * Hash code based on ID for consistent Set/Map behavior.
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /**
     * Human-readable string representation of this node.
     * Shows ID, position, and neighbor IDs.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Node[");
        sb.append("id=").append(id);
        sb.append(", pos=(").append(String.format("%.2f", x)).append(", ");
        sb.append(String.format("%.2f", y)).append(")");
        sb.append(", neighbors=");
        if (neighbors.isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[");
            boolean first = true;
            for (Node n : neighbors) {
                if (!first) sb.append(", ");
                sb.append(n.getId());
                first = false;
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
}
