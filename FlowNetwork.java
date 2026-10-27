import java.util.*;

 
class FlowNetwork {
    final int n;
    private int edgeCount;
    private int[] from = new int[16];
    private int[] to = new int[16];
    private int[] originalCapacity = new int[16];
    private int[] cost = new int[16];
    private int[] flowOnEdge = new int[16];
    int augmentations;

    FlowNetwork(int n) { this.n = n; }

    void addEdge(int u, int v, int capacity, int edgeCost) {
        ensureCapacity();
        from[edgeCount] = u;
        to[edgeCount] = v;
        originalCapacity[edgeCount] = capacity;
        cost[edgeCount] = edgeCost;
        edgeCount++;
    }

    private void ensureCapacity() {
        if (edgeCount < from.length) return;
        int newCapacity = from.length * 2;
        from = Arrays.copyOf(from, newCapacity);
        to = Arrays.copyOf(to, newCapacity);
        originalCapacity = Arrays.copyOf(originalCapacity, newCapacity);
        cost = Arrays.copyOf(cost, newCapacity);
        flowOnEdge = Arrays.copyOf(flowOnEdge, newCapacity);
    }

    private int forwardCapacity(int edge) { return originalCapacity[edge] - flowOnEdge[edge]; }
    private int reverseCapacity(int edge) { return flowOnEdge[edge]; }

    void reset() { Arrays.fill(flowOnEdge, 0, edgeCount, 0); }

    int fordFulkerson(int s, int t) {
        reset(); augmentations = 0; int totalFlow = 0;
        if (s == t) return 0;
        while (true) {
            int pushed = dfs(s, t, Integer.MAX_VALUE, new boolean[n]);
            if (pushed == 0) break;
            totalFlow += pushed; augmentations++;
        }
        return totalFlow;
    }

    private int dfs(int u, int t, int limit, boolean[] seen) {
        if (u == t) return limit;
        seen[u] = true;
        for (int edge = 0; edge < edgeCount; edge++) {
            if (from[edge] == u && forwardCapacity(edge) > 0 && !seen[to[edge]]) {
                int pushed = dfs(to[edge], t, Math.min(limit, forwardCapacity(edge)), seen);
                if (pushed > 0) { flowOnEdge[edge] += pushed; return pushed; }
            }
            if (to[edge] == u && reverseCapacity(edge) > 0 && !seen[from[edge]]) {
                int pushed = dfs(from[edge], t, Math.min(limit, reverseCapacity(edge)), seen);
                if (pushed > 0) { flowOnEdge[edge] -= pushed; return pushed; }
            }
        }
        return 0;
    }

    int edmondsKarp(int s, int t) {
        reset(); augmentations = 0; int totalFlow = 0;
        if (s == t) return 0;
        while (true) {
            int[] viaEdge = new int[n]; Arrays.fill(viaEdge, -1);
            boolean[] viaForward = new boolean[n];
            boolean[] seen = new boolean[n]; seen[s] = true;
            Queue<Integer> queue = new ArrayDeque<>(); queue.add(s);
            while (!queue.isEmpty() && !seen[t]) {
                int u = queue.poll();
                for (int edge = 0; edge < edgeCount; edge++) {
                    if (from[edge] == u && forwardCapacity(edge) > 0 && !seen[to[edge]]) {
                        seen[to[edge]] = true; viaEdge[to[edge]] = edge; viaForward[to[edge]] = true; queue.add(to[edge]);
                    }
                    if (to[edge] == u && reverseCapacity(edge) > 0 && !seen[from[edge]]) {
                        seen[from[edge]] = true; viaEdge[from[edge]] = edge; viaForward[from[edge]] = false; queue.add(from[edge]);
                    }
                }
            }
            if (!seen[t]) break;
            int bottleneck = Integer.MAX_VALUE;
            for (int v = t; v != s; v = previousNode(v, viaEdge, viaForward)) {
                int edge = viaEdge[v];
                bottleneck = Math.min(bottleneck, viaForward[v] ? forwardCapacity(edge) : reverseCapacity(edge));
            }
            for (int v = t; v != s; v = previousNode(v, viaEdge, viaForward)) {
                int edge = viaEdge[v];
                flowOnEdge[edge] += viaForward[v] ? bottleneck : -bottleneck;
            }
            totalFlow += bottleneck; augmentations++;
        }
        return totalFlow;
    }

    private int previousNode(int node, int[] viaEdge, boolean[] viaForward) {
        int edge = viaEdge[node];
        return viaForward[node] ? from[edge] : to[edge];
    }

    boolean[] sourceSide(int s) {
        boolean[] seen = new boolean[n]; seen[s] = true;
        Queue<Integer> queue = new ArrayDeque<>(); queue.add(s);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int edge = 0; edge < edgeCount; edge++) {
                if (from[edge] == u && forwardCapacity(edge) > 0 && !seen[to[edge]]) {
                    seen[to[edge]] = true; queue.add(to[edge]);
                }
                if (to[edge] == u && reverseCapacity(edge) > 0 && !seen[from[edge]]) {
                    seen[from[edge]] = true; queue.add(from[edge]);
                }
            }
        }
        return seen;
    }

    List<int[]> cutEdges(int s) {
        boolean[] side = sourceSide(s);
        List<int[]> cut = new ArrayList<>();
        for (int edge = 0; edge < edgeCount; edge++) {
            if (side[from[edge]] && !side[to[edge]] && originalCapacity[edge] > 0) {
                cut.add(new int[]{from[edge], to[edge], originalCapacity[edge]});
            }
        }
        return cut;
    }

    long[] minCostMaxFlow(int s, int t) {
        reset(); long totalFlow = 0, totalCost = 0;
        if (s == t) return new long[]{0, 0};
        while (true) {
            long[] distance = new long[n]; Arrays.fill(distance, Graph.INF); distance[s] = 0;
            int[] viaEdge = new int[n]; Arrays.fill(viaEdge, -1);
            boolean[] viaForward = new boolean[n];
            boolean[] inQueue = new boolean[n];
            Queue<Integer> queue = new ArrayDeque<>(); queue.add(s); inQueue[s] = true;
            while (!queue.isEmpty()) {
                int u = queue.poll(); inQueue[u] = false;
                for (int edge = 0; edge < edgeCount; edge++) {
                    if (from[edge] == u && forwardCapacity(edge) > 0
                            && distance[u] + cost[edge] < distance[to[edge]]) {
                        int v = to[edge];
                        distance[v] = distance[u] + cost[edge]; viaEdge[v] = edge; viaForward[v] = true;
                        if (!inQueue[v]) { inQueue[v] = true; queue.add(v); }
                    }
                    if (to[edge] == u && reverseCapacity(edge) > 0
                            && distance[u] - cost[edge] < distance[from[edge]]) {
                        int v = from[edge];
                        distance[v] = distance[u] - cost[edge]; viaEdge[v] = edge; viaForward[v] = false;
                        if (!inQueue[v]) { inQueue[v] = true; queue.add(v); }
                    }
                }
            }
            if (distance[t] >= Graph.INF) break;
            int bottleneck = Integer.MAX_VALUE;
            for (int v = t; v != s; v = previousNode(v, viaEdge, viaForward)) {
                int edge = viaEdge[v];
                bottleneck = Math.min(bottleneck, viaForward[v] ? forwardCapacity(edge) : reverseCapacity(edge));
            }
            for (int v = t; v != s; v = previousNode(v, viaEdge, viaForward)) {
                int edge = viaEdge[v];
                flowOnEdge[edge] += viaForward[v] ? bottleneck : -bottleneck;
            }
            totalFlow += bottleneck; totalCost += (long) bottleneck * distance[t];
        }
        return new long[]{totalFlow, totalCost};
    }

    boolean capacityAccountingValid() {
        for (int edge = 0; edge < edgeCount; edge++) {
            if (flowOnEdge[edge] < 0 || flowOnEdge[edge] > originalCapacity[edge]) return false;
        }
        return true;
    }

    List<int[]> usedEdges() {
        List<int[]> out = new ArrayList<>();
        for (int edge = 0; edge < edgeCount; edge++) {
            if (originalCapacity[edge] > 0 && flowOnEdge[edge] > 0) {
                out.add(new int[]{from[edge], to[edge], flowOnEdge[edge], forwardCapacity(edge), reverseCapacity(edge)});
            }
        }
        return out;
    }
}
