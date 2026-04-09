/**
 * RREQ.java
 * 
 * Represents an AODV Route Request (RREQ) message.
 * 
 * Purpose:
 *   When a source node S wants to send data to a destination D but has no
 *   valid route, S initiates a RREQ flood. The RREQ is broadcast to all
 *   neighbors, who rebroadcast it until it reaches D (or a node with a
 *   fresh enough route to D).
 * 
 * Key fields (simplified AODV):
 *   - sourceId:       the node that originated this RREQ
 *   - destId:         the node we want a route to
 *   - sourceSeqNum:   the originator's current sequence number
 *   - destSeqNum:     last known seq num for dest (0 if unknown)
 *   - hopCount:       how many hops the RREQ has traveled so far
 *   - rreqId:         unique ID per (source, broadcast) to prevent loops
 *   - reversePath:    list of nodes the RREQ has visited (for RREP back)
 * 
 * The reversePath is crucial: as RREQ propagates from S toward D,
 * each intermediate node appends itself. When D (or an intermediate
 * with a route) sends RREP, it follows this path in reverse to reach S.
 * 
 * This class is a simple data carrier (POJO). The AODVRouter handles
 * the logic of creating, forwarding, and responding to RREQs.
 */
import java.util.*;

public class RREQ {

    // --- Core AODV RREQ fields ---

    public final int sourceId;      // Originator node ID
    public final int destId;        // Target destination node ID

    public final int sourceSeqNum;  // Originator's sequence number
    public final int destSeqNum;    // Last known seq num for dest (0 if unknown)

    public int hopCount;            // Incremented at each hop
    public final int rreqId;        // Broadcast ID (unique per source per flood)

    // --- Reverse path: nodes visited from source toward here ---
    // The list is in order: [source, ... intermediate ..., currentPredecessor]
    // When RREP is generated, it traverses this list backwards to source.
    private final List<Node> reversePath;

    /**
     * Creates a new RREQ originating at source, targeting dest.
     *
     * @param sourceId     ID of the node initiating the RREQ
     * @param destId       ID of the destination we seek
     * @param sourceSeqNum Originator's current sequence number
     * @param destSeqNum   Last known dest seq num (use 0 if unknown)
     * @param rreqId       Unique broadcast ID for this RREQ flood
     */
    public RREQ(int sourceId, int destId, int sourceSeqNum, int destSeqNum, int rreqId) {
        this.sourceId = sourceId;
        this.destId = destId;
        this.sourceSeqNum = sourceSeqNum;
        this.destSeqNum = destSeqNum;
        this.hopCount = 0;
        this.rreqId = rreqId;
        this.reversePath = new ArrayList<>();
    }

    /**
     * Copy constructor + increment hop and extend reverse path.
     * Called when a node forwards the RREQ to its neighbors.
     *
     * @param other   the incoming RREQ
     * @param fromNode the node that forwarded this RREQ to us (the "previous hop")
     */
    public RREQ(RREQ other, Node fromNode) {
        this.sourceId = other.sourceId;
        this.destId = other.destId;
        this.sourceSeqNum = other.sourceSeqNum;
        this.destSeqNum = other.destSeqNum;
        this.hopCount = other.hopCount + 1;  // We are one hop further
        this.rreqId = other.rreqId;
        this.reversePath = new ArrayList<>(other.reversePath);
        // Add the node we received from to our reverse path
        if (fromNode != null) {
            this.reversePath.add(fromNode);
        }
    }

    /**
     * Returns the reverse path as an unmodifiable list.
     * The list contains nodes in order from source outward.
     * To send RREP back to source, we traverse this in reverse.
     */
    public List<Node> getReversePath() {
        return Collections.unmodifiableList(reversePath);
    }

    /**
     * Returns the node ID of the last node on the reverse path (the
     * immediate predecessor in the path from source). Useful for
     * determining the next hop when sending RREP back.
     */
    public Node getPredecessor() {
        if (reversePath.isEmpty()) {
            return null;
        }
        return reversePath.get(reversePath.size() - 1);
    }

    /**
     * Returns a human-readable description of this RREQ.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RREQ[src=").append(sourceId);
        sb.append(", dst=").append(destId);
        sb.append(", srcSeq=").append(sourceSeqNum);
        sb.append(", dstSeq=").append(destSeqNum);
        sb.append(", hopCount=").append(hopCount);
        sb.append(", rreqId=").append(rreqId);
        sb.append(", reversePath=");
        if (reversePath.isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[");
            boolean first = true;
            for (Node n : reversePath) {
                if (!first) sb.append("->");
                sb.append(n.getId());
                first = false;
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
}