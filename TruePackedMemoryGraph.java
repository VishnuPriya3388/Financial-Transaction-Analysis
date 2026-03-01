import java.util.*;

// Node and edge representation
public class TruePackedMemoryGraph {
    private Map<Integer, List<Edge>> adjList;
    private Map<Integer, Integer> nodeCapacity; // optional for your insertNode
    private int edgeCount;

    public TruePackedMemoryGraph(int capacity) {
        adjList = new HashMap<>();
        nodeCapacity = new HashMap<>();
        edgeCount = 0;
    }

    public void insertNode(int nodeId, int capacity) {
        adjList.putIfAbsent(nodeId, new ArrayList<>());
        nodeCapacity.putIfAbsent(nodeId, capacity);
    }

    public void addEdge(int from, int to, double amount) {
        insertNode(from, 10);
        insertNode(to, 10);
        adjList.get(from).add(new Edge(to, amount));
        edgeCount++;
    }

    public int getNodeCount() {
        return adjList.size();
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public double getDensity() {
        int n = getNodeCount();
        return n <= 1 ? 0 : (double) edgeCount / (n * (n - 1));
    }

    public Map<Integer, Double> computeOutgoingAmounts() {
        Map<Integer, Double> outgoing = new HashMap<>();
        for (Map.Entry<Integer, List<Edge>> entry : adjList.entrySet()) {
            double sum = 0;
            for (Edge e : entry.getValue()) sum += e.amount;
            outgoing.put(entry.getKey(), sum);
        }
        return outgoing;
    }

    // --- NEW METHODS FOR GUI ---
    public Set<Integer> getNodes() {
        return adjList.keySet();
    }

    public List<Edge> getEdges(int from) {
        return adjList.getOrDefault(from, Collections.emptyList());
    }

    public Set<Integer> bfs(int start) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> q = new LinkedList<>();
        visited.add(start);
        q.add(start);

        while (!q.isEmpty()) {
            int curr = q.poll();
            for (Edge e : getEdges(curr)) {
                if (!visited.contains(e.to)) {
                    visited.add(e.to);
                    q.add(e.to);
                }
            }
        }
        return visited;
    }

    // Edge inner class
    public static class Edge {
        public int to;
        public double amount;

        public Edge(int to, double amount) {
            this.to = to;
            this.amount = amount;
        }
    }
}
