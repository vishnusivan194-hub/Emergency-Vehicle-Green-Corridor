import java.util.*;

 
class Intersection {
    final int id;
    final String name;
    final double x, y;           
    final boolean hospital;

    Intersection(int id, String name, double x, double y, boolean hospital) {
        this.id = id; this.name = name; this.x = x; this.y = y; this.hospital = hospital;
    }
}

 
class SlidingWindow {
    private final int capacity;
    private final ArrayDeque<Integer> values = new ArrayDeque<>();
    private int sum;

    SlidingWindow(int capacity) { this.capacity = capacity; }

    void add(int v) {                        
        values.addLast(v); sum += v;
        if (values.size() > capacity) sum -= values.pollFirst();
    }
    void fill(int v) { values.clear(); sum = 0; for (int i = 0; i < capacity; i++) add(v); }
    double average() { return values.isEmpty() ? 0 : (double) sum / values.size(); }
    @Override public String toString() { return values.toString(); }
}

 
class Road {
    final int id, a, b;
    int baseTime;                  
    int capacity;                  
    boolean available = true;
    final SlidingWindow recent = new SlidingWindow(3);

    Road(int id, int a, int b, int baseTime, int capacity, int density) {
        this.id = id; this.a = a; this.b = b; this.baseTime = baseTime; this.capacity = capacity;
        recent.fill(density);
    }
    int other(int v) { return v == a ? b : a; }
    int density() { return (int) Math.round(recent.average()); }

     
    int travelTime() { return Math.max(1, (int) Math.round(baseTime * (1 + recent.average() / 100.0))); }

     
    int freeCapacity() { return (int) Math.round(capacity * (100 - recent.average()) / 100.0); }
}

 
class Graph {
    static final long INF = Long.MAX_VALUE / 4;
    final List<Intersection> nodes = new ArrayList<>();
    final List<Road> roads = new ArrayList<>();
    final Map<String, Integer> nameToId = new HashMap<>();        
    final Map<Integer, List<Road>> adj = new HashMap<>();         
    final List<int[]> lastSettled = new ArrayList<>();            

     
    static class Route {
        final List<Integer> nodes; final List<Road> roads; final int total;
        Route(List<Integer> nodes, List<Road> roads, int total) { this.nodes = nodes; this.roads = roads; this.total = total; }
        boolean sameAs(Route o) { return nodes.equals(o.nodes); }
    }

    int addNode(String name, double x, double y, boolean hospital) {
        int id = nodes.size();
        nodes.add(new Intersection(id, name, x, y, hospital));
        nameToId.put(name.toLowerCase(), id);
        adj.put(id, new ArrayList<>());
        return id;
    }
    Road addRoad(int a, int b, int time, int cap, int density) {
        Road r = new Road(roads.size(), a, b, time, cap, density);
        roads.add(r); adj.get(a).add(r); adj.get(b).add(r);
        return r;
    }
    Integer find(String name) { return nameToId.get(name.trim().toLowerCase()); }
    int size() { return nodes.size(); }
    String name(int id) { return nodes.get(id).name; }

     

    Route dijkstra(int src, int dst, Road banned) {
        return dijkstraInternal(src, dst, banned, -1);
    }

     



    Route dijkstraAvoidingHeavyTraffic(int src, int dst, int maxAllowedDensity) {
        if (maxAllowedDensity < 1 || maxAllowedDensity > 100)
            throw new IllegalArgumentException("Heavy-traffic threshold must be between 1 and 100.");
        return dijkstraInternal(src, dst, null, maxAllowedDensity);
    }

    private Route dijkstraInternal(int src, int dst, Road banned, int maxAllowedDensity) {
        int n = size();
        int[] dist = new int[n], prev = new int[n]; Road[] via = new Road[n];
        Arrays.fill(dist, Integer.MAX_VALUE); Arrays.fill(prev, -1);
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));
        dist[src] = 0; pq.add(new int[]{0, src});
        lastSettled.clear();
        while (!pq.isEmpty()) {
            int[] top = pq.poll(); int d = top[0], u = top[1];
            if (d > dist[u]) continue;                        
            lastSettled.add(new int[]{u, d});
            if (u == dst) break;
            for (Road r : adj.get(u)) {
                if (!r.available || r == banned) continue;    
                if (maxAllowedDensity >= 0 && r.density() >= maxAllowedDensity) continue;
                int v = r.other(u), nd = d + r.travelTime();
                if (nd < dist[v]) { dist[v] = nd; prev[v] = u; via[v] = r; pq.add(new int[]{nd, v}); }
            }
        }
        if (dist[dst] == Integer.MAX_VALUE) return null;      
        LinkedList<Integer> path = new LinkedList<>(); LinkedList<Road> rs = new LinkedList<>();
        for (int v = dst; v != -1; v = prev[v]) { path.addFirst(v); if (via[v] != null) rs.addFirst(via[v]); }
        return new Route(path, rs, dist[dst]);
    }

     




    List<Route> routesAvoidingHeavyTraffic(int s, int t, int maxAlt, int heavyThreshold) {
        Route best = dijkstraAvoidingHeavyTraffic(s, t, heavyThreshold);
        boolean foundCleanRoute = best != null;
        if (!foundCleanRoute) best = dijkstra(s, t, null);
        if (best == null) return null;

        List<Route> alts = new ArrayList<>();
        for (Road banned : best.roads) {
            Route alt = foundCleanRoute
                    ? dijkstraInternal(s, t, banned, heavyThreshold)
                    : dijkstra(s, t, banned);
            if (alt == null || alt.sameAs(best)) continue;
            boolean dup = false;
            for (Route a : alts) dup |= a.sameAs(alt);
            if (!dup) alts.add(alt);
        }
        alts.sort(Comparator.comparingInt(a -> a.total));
        List<Route> out = new ArrayList<>(); out.add(best);
        out.addAll(alts.subList(0, Math.min(maxAlt, alts.size())));
        return out;
    }

     
    List<Route> routesWithAlternatives(int s, int t, int maxAlt) {
        Route best = dijkstra(s, t, null);
        if (best == null) return null;
        List<Route> alts = new ArrayList<>();
        for (Road banned : best.roads) {
            Route alt = dijkstra(s, t, banned);
            if (alt == null || alt.sameAs(best)) continue;
            boolean dup = false;
            for (Route a : alts) dup |= a.sameAs(alt);
            if (!dup) alts.add(alt);
        }
        alts.sort(Comparator.comparingInt(a -> a.total));
        List<Route> out = new ArrayList<>(); out.add(best);
        out.addAll(alts.subList(0, Math.min(maxAlt, alts.size())));
        return out;
    }

     
    DSU connectivity() {
        DSU d = new DSU(size());
        for (Road r : roads) if (r.available) d.union(r.a, r.b);
        return d;
    }

     
    FlowNetwork toFlowNetwork() {
        FlowNetwork f = new FlowNetwork(size());
        for (Road r : roads) if (r.available && r.freeCapacity() > 0) {
            f.addEdge(r.a, r.b, r.freeCapacity(), r.travelTime());
            f.addEdge(r.b, r.a, r.freeCapacity(), r.travelTime());
        }
        return f;
    }

     
    long[][] allPairs() {
        List<int[]> e = new ArrayList<>();
        for (Road r : roads) if (r.available) {
            e.add(new int[]{r.a, r.b, r.travelTime()}); e.add(new int[]{r.b, r.a, r.travelTime()});
        }
        return johnson(size(), e);
    }

     



    static long[][] johnson(int n, List<int[]> edges) {
        long[] h = new long[n];                            
        for (int pass = 1; pass <= n; pass++) {
            boolean changed = false;
            for (int[] e : edges) if (h[e[0]] + e[2] < h[e[1]]) { h[e[1]] = h[e[0]] + e[2]; changed = true; }
            if (!changed) break;
            if (pass == n) return null;                    
        }
        List<List<long[]>> g = new ArrayList<>();
        for (int i = 0; i < n; i++) g.add(new ArrayList<>());
        for (int[] e : edges) g.get(e[0]).add(new long[]{e[1], e[2] + h[e[0]] - h[e[1]]});
        long[][] d = new long[n][n];
        for (int s = 0; s < n; s++) {
            Arrays.fill(d[s], INF); d[s][s] = 0;
            PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingLong(x -> x[0]));
            pq.add(new long[]{0, s});
            while (!pq.isEmpty()) {
                long[] top = pq.poll(); int u = (int) top[1];
                if (top[0] > d[s][u]) continue;
                for (long[] e : g.get(u)) {
                    int v = (int) e[0];
                    if (d[s][u] + e[1] < d[s][v]) { d[s][v] = d[s][u] + e[1]; pq.add(new long[]{d[s][v], v}); }
                }
            }
            for (int v = 0; v < n; v++) if (d[s][v] < INF) d[s][v] += h[v] - h[s];    
        }
        return d;
    }

     
    static Graph sampleCity() {
        Graph g = new Graph();
        g.addNode("Airport", 0, 6, false);           
        g.addNode("Guindy", 2, 4, false);            
        g.addNode("Saidapet", 4, 5, false);          
        g.addNode("Velachery", 3, 0, false);         
        g.addNode("Adyar", 6, 2, false);             
        g.addNode("T Nagar", 5, 7, false);           
        g.addNode("Anna Salai", 8, 5, false);        
        g.addNode("Anna Nagar", 7, 9, false);        
        g.addNode("Egmore Hospital", 10, 8, true);   
        g.addNode("City Hospital", 11, 4, true);     
        g.addNode("Kovalam", 12, 0, false);          
         
        g.addRoad(0, 1, 180, 20, 10);
        g.addRoad(0, 2, 300, 15, 10);
        g.addRoad(1, 2, 120, 25, 20);
        g.addRoad(1, 3, 210, 10, 30);
        g.addRoad(2, 5, 150, 20, 20);
        g.addRoad(2, 4, 180, 12, 10);
        g.addRoad(3, 4, 180, 15, 10);
        g.addRoad(5, 6, 180, 18, 20);
        g.addRoad(5, 7, 210, 14, 10);
        g.addRoad(4, 6, 150, 16, 30);
        g.addRoad(6, 9, 180, 20, 20);
        g.addRoad(6, 8, 150, 15, 10);
        g.addRoad(7, 8, 120, 12, 10);
        g.addRoad(8, 9, 150, 18, 10);
        g.addRoad(10, 4, 240, 10, 0).available = false;    
        return g;
    }
}
