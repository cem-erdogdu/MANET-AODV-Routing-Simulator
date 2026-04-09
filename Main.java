// Main.java
import java.util.List;

/**
 * Entry point for the MANET AODV Routing Simulator foundation demo.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  MANET AODV Simulator - Foundation Demo  ");
        System.out.println("==========================================\n");

        double radioRange = 10.0;
        Network network = new Network(radioRange);

        Node n0 = new Node(0, 0.0, 0.0);
        Node n1 = new Node(1, 5.0, 0.0);
        Node n2 = new Node(2, 12.0, 0.0);
        Node n3 = new Node(3, 7.0, 7.0);
        Node n4 = new Node(4, 20.0, 20.0);

        network.addNode(n0);
        network.addNode(n1);
        network.addNode(n2);
        network.addNode(n3);
        network.addNode(n4);

        System.out.println("Created " + network.size() + " nodes and added to network.\n");

        System.out.println("Running neighbor discovery (radioRange=" + radioRange + ")...\n");
        network.discoverNeighbors();
        network.printNetworkState();

        System.out.println("\n--- Demonstrating mobility ---");
        System.out.println("Moving Node 4 from (20,20) to (8,1) ...\n");
        n4.setPosition(8.0, 1.0);

        network.discoverNeighbors();
        network.printNetworkState();

        System.out.println("\n--- Sanity Checks ---");
        System.out.println("Is Node 0 a neighbor of Node 1? " + n1.isNeighbor(n0));
        System.out.println("Is Node 1 a neighbor of Node 0? " + n0.isNeighbor(n1));
        System.out.println("Is Node 0 a neighbor of Node 2? " + n0.isNeighbor(n2));
        System.out.println("Is Node 4 a neighbor of Node 1? " + n4.isNeighbor(n1));

        System.out.println("\nDistance from Node 0 to Node 3: " +
                String.format("%.2f", n0.distanceTo(n3)));

        System.out.println("\n==========================================");
        System.out.println("  Foundation demo complete. Ready for AODV!");
        System.out.println("==========================================");

        System.out.println("\n\n==========================================");
        System.out.println("  AODV Route Discovery Demo");
        System.out.println("==========================================\n");

        System.out.println("--- Setting up multi-hop scenario ---");
        System.out.println("Moving Node 4 from (8,1) to (20,0) (reachable via 0->1->2->4)...\n");
        n4.setPosition(20.0, 0.0);
        network.discoverNeighbors();
        network.printNetworkState();

        System.out.println("\nIs Node 0 a direct neighbor of Node 4? " + n0.isNeighbor(n4));
        System.out.println("(Should be false for multi-hop demo)\n");

        AODVRouter router = new AODVRouter(network);

        System.out.println("--- Starting AODV route discovery ---");
        System.out.println("Source: Node " + n0.getId() + "  Destination: Node " + n4.getId() + "\n");

        List<Node> route = router.discoverRoute(n0, n4.getId());

        if (route != null) {
            System.out.println("\n--- Route Discovery Successful ---");
            System.out.print("Path from " + n0.getId() + " to " + n4.getId() + ": ");
            for (int i = 0; i < route.size(); i++) {
                if (i > 0) System.out.print(" -> ");
                System.out.print(route.get(i).getId());
            }
            System.out.println();
            System.out.println("Hop count: " + (route.size() - 1));
        } else {
            System.out.println("\n--- Route Discovery Failed ---");
            System.out.println("No route from Node " + n0.getId() + " to Node " + n4.getId());
        }

        System.out.println("\n--- Sample Routing Tables After Discovery ---");
        System.out.println("Node 0 routing table: " + router.getRoutingTable(n0).dump());
        System.out.println("Node 1 routing table: " + router.getRoutingTable(n1).dump());
        System.out.println("Node 2 routing table: " + router.getRoutingTable(n2).dump());
        System.out.println("Node 3 routing table: " + router.getRoutingTable(n3).dump());
        System.out.println("Node 4 routing table: " + router.getRoutingTable(n4).dump());

        System.out.println("\n==========================================");
        System.out.println("  AODV Demo Complete");
        System.out.println("==========================================");
    }
}