/**
 * RREP.java
 * 
 * Represents an AODV Route Reply (RREP) message.
 * 
 * Purpose:
 *   When a RREQ reaches the destination D (or an intermediate node I that
 *   already has a fresh enough route to D), that node generates an RREP.
 *   The RREP travels back along the reverse path (established by the RREQ)
 *   to the original source S.
 * 
 * As the RREP travels back:
 *   - Each node that forwards it learns a route to the destination D:
 *     nextHop = the node it received RREP from (closer to D),
 *     hopCount = RREP.hopCount + 1
 *   - Eventually S receives the RREP and now has a complete route to D.
 * 
 * Key fields (simplified AODV):
 *   - destId:        the destination this route is for (the node that
 *                    generated or is represented by this RREP)
 *   - destSeqNum:    destination's current sequence number (fresh)
 *   - sourceId:      the original RREQ originator (final recipient of RREP)
 *   - hopCount:      hops from whoever currently holds this RREP to destId
 *                    (increments as RREP travels toward source)
 * 
 * In our simulator, we also carry the reverse path (list of nodes from
 * source to dest) so that the AODVRouter can easily forward the RREP
 * hop-by-hop and print a clear trace.
 * 
 * The RREP is created at the node that can answer the RREQ (dest or
 * intermediate). It then follows the RREQ's reversePath backwards.
 */
import java.util.*;

public class RREP {

    // --- Core AODV RREP fields ---

    public final int destId;        // Destination node ID (who this route reaches)
    public final int destSeqNum;    // Fresh sequence number for dest
    public final int sourceId;      // Originator of the original RREQ (final target)

    public int hopCount;            // Hops from this RREP's current holder to dest

    // --- Forward path toward source (reverse of RREQ's reversePath) ---
    // This is the list of nodes from dest back to source.
    // We traverse it to deliver RREP. The list is in order [dest, ..., source].
    // Actually, for simplicity we store the path that RREP must still traverse.
    // When RREP is at node N, the "remainingPath" tells where to go next.
    // We'll model it as: list of nodes in order from current position back to source.
    // Simpler: just store the full reverse path from RREQ; RREP starts at dest
    // and we consume it backwards.

    // To keep it simple and traceable, RREP will carry a "pathToSource" list
    // which is the reversePath from RREQ (nodes in order source<-...<-dest).
    // When processing, the node at the "head" of this list is the next hop.
    private final List<Node> pathToSource;

    /**
     * Creates an RREP originating from the node that answers the RREQ.
     *
     * @param destId      the destination (this node or one it has route to)
     * @param destSeqNum  the destination's sequence number
     * @param sourceId    the original RREQ source (who needs this RREP)
     * @param hopCount    0 if we are the dest; otherwise hopCount from our route
     * @param pathToSource the reversePath list from the RREQ (source->...->dest order)
     */
    public RREP(int destId, int destSeqNum, int sourceId, int hopCount, List<Node> pathToSource) {
        this.destId = destId;
        this.destSeqNum = destSeqNum;
        this.sourceId = sourceId;
        this.hopCount = hopCount;
        // Defensive copy; pathToSource is the RREQ reversePath which goes source->...->pred
        // For RREP, we need to go from dest back toward source.
        // We store it as-is; AODVRouter will handle direction.
        this.pathToSource = new ArrayList<>(pathToSource);
    }

    /**
     * Copy constructor for forwarding: increments hopCount.
     * The pathToSource stays the same (we just traverse it differently in logic).
     */
    public RREP(RREP other) {
        this.destId = other.destId;
        this.destSeqNum = other.destSeqNum;
        this.sourceId = other.sourceId;
        this.hopCount = other.hopCount + 1;
        this.pathToSource = new ArrayList<>(other.pathToSource);
    }

    /**
     * Returns the path-to-source list (as stored; order depends on how it was built).
     * In our design, pathToSource is the RREQ reversePath which is [source, ..., pred].
     * The AODVRouter uses this to know the next hop toward source.
     */
    public List<Node> getPathToSource() {
        return Collections.unmodifiableList(pathToSource);
    }

    /**
     * Returns a human-readable description.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RREP[dest=").append(destId);
        sb.append(", destSeq=").append(destSeqNum);
        sb.append(", src=").append(sourceId);
        sb.append(", hopCount=").append(hopCount);
        sb.append("]");
        return sb.toString();
    }
}