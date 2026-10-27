import java.util.*;

 

class Tests {
    private static int passed, total;
    private static boolean verbose;

    private static void check(String name, boolean ok, String detail) {
        total++; if (ok) passed++;
        if (verbose || !ok) System.out.printf("  [%s] Verification check %d%s%n", ok ? "PASS" : "FAIL", total, detail.isEmpty() ? "" : "  (" + detail + ")");
    }
    private static boolean throwsBad(Runnable r) { try { r.run(); return false; } catch (IllegalArgumentException e) { return true; } }

     
    static int[] runAll(boolean show) {
        passed = 0; total = 0; verbose = show;
        if (show) System.out.println("\nAUTOMATED TESTS (each result below is computed, not assumed):");
        routingTests(); signalTests(); flowTests(); johnsonTests(); structureTests(); inputTests();
        if (show) System.out.println("RESULT: " + passed + " / " + total + " checks passed.");
        return new int[]{passed, total};
    }

     
    static FlowNetwork textbookNetwork() {                        
        FlowNetwork f = new FlowNetwork(6);
        int[][] e = {{0, 1, 16}, {0, 2, 13}, {1, 2, 10}, {2, 1, 4}, {1, 3, 12}, {3, 2, 9}, {2, 4, 14}, {4, 3, 7}, {3, 5, 20}, {4, 5, 4}};
        for (int[] x : e) f.addEdge(x[0], x[1], x[2], 1);
        return f;
    }
    static FlowNetwork tinyCostNetwork() {                        
        FlowNetwork f = new FlowNetwork(4);
        f.addEdge(0, 1, 2, 1); f.addEdge(0, 2, 1, 2); f.addEdge(1, 3, 1, 1); f.addEdge(1, 2, 1, 1); f.addEdge(2, 3, 2, 1);
        return f;
    }
     
    static FlowNetwork reverseEdgeNetwork() {
        FlowNetwork f = new FlowNetwork(4);
        f.addEdge(0, 1, 1, 1); f.addEdge(0, 2, 1, 4); f.addEdge(1, 2, 1, 1); f.addEdge(1, 3, 1, 4); f.addEdge(2, 3, 1, 1);
        return f;
    }
    private static Graph.Route sampleRoute(Graph g) { return g.dijkstra(g.find("Airport"), g.find("City Hospital"), null); }

     
    private static void routingTests() {
        Graph g = Graph.sampleCity(); int s = g.find("Airport"), t = g.find("City Hospital");
        Graph.Route r = sampleRoute(g);
        check("Route starts at source and ends at destination", r != null && r.nodes.get(0) == s && r.nodes.get(r.nodes.size() - 1) == t, "");
        int sum = 0; for (Road x : r.roads) sum += x.travelTime();
        check("Route cost equals the sum of its road costs", sum == r.total, sum + " vs " + r.total);
        boolean edgesOk = r.roads.size() == r.nodes.size() - 1;
        for (int i = 0; i < r.roads.size() && edgesOk; i++) {
            Road x = r.roads.get(i); int u = r.nodes.get(i), v = r.nodes.get(i + 1);
            edgesOk = x.available && x.other(u) == v && g.adj.get(u).contains(x);
        }
        check("Every route road exists, is open and joins consecutive intersections", edgesOk, "");

        Road first = r.roads.get(0); first.available = false;
        Graph.Route detour = g.dijkstra(s, t, null);
        check("Unavailable road is never used (a detour is found)", detour != null && !detour.roads.contains(first), "");
        first.available = true;

        SlidingWindow w = new SlidingWindow(3); for (int v : new int[]{10, 20, 30, 40}) w.add(v);
        check("Sliding window keeps only the last 3 readings", Math.abs(w.average() - 30.0) < 1e-9, w.toString());
        Road probe = new Road(0, 0, 1, 100, 10, 0); probe.recent.fill(50);
        check("Dynamic edge weight: 100 s at 50% density -> 150 s", probe.travelTime() == 150, probe.travelTime() + " s");

        int before = r.total; for (Road x : r.roads) x.recent.fill(100);
        int oldRouteNow = 0; for (Road x : r.roads) oldRouteNow += x.travelTime();
        Graph.Route re = g.dijkstra(s, t, null);
        check("Congested roads raise the estimated time", oldRouteNow > before, before + " -> " + oldRouteNow);
        check("Re-routing never does worse than staying on the congested route", re != null && re.total <= oldRouteNow, "");

        Graph g2 = Graph.sampleCity();
        check("Disconnected intersection: destination Kovalam is unreachable", g2.dijkstra(s, g2.find("Kovalam"), null) == null, "");
        List<Graph.Route> rs = g2.routesWithAlternatives(s, t, 2);
        check("Alternative routes differ from the best and are not faster", rs != null && rs.size() >= 2 && !rs.get(1).sameAs(rs.get(0)) && rs.get(1).total >= rs.get(0).total, rs == null ? "" : rs.size() + " routes");

        Graph gh = Graph.sampleCity(); int hs = gh.find("Airport"), ht = gh.find("City Hospital");
        Road fastButHeavy = gh.roads.get(1);  
        fastButHeavy.baseTime = 30; fastButHeavy.recent.fill(100);
        Graph.Route unrestricted = gh.dijkstra(hs, ht, null);
        Graph.Route clean = gh.dijkstraAvoidingHeavyTraffic(hs, ht, 70);
        check("Heavy road is avoided when a lower-density open detour exists",
                unrestricted != null && unrestricted.roads.contains(fastButHeavy) && clean != null && !clean.roads.contains(fastButHeavy), "");

        Graph gf = Graph.sampleCity();
        for (Road road : gf.roads) if (road.available) road.recent.fill(100);
        Graph.Route noClean = gf.dijkstraAvoidingHeavyTraffic(gf.find("Airport"), gf.find("City Hospital"), 70);
        List<Graph.Route> fallback = gf.routesAvoidingHeavyTraffic(gf.find("Airport"), gf.find("City Hospital"), 3, 70);
        check("Fastest open-road fallback is used when every route contains heavy traffic",
                noClean == null && fallback != null && !fallback.isEmpty(), "");
    }

     
    private static void signalTests() {
        Graph g = Graph.sampleCity(); SignalControl sc = new SignalControl(g); sc.loadSampleReservations(g);
        SignalScheduler sch = new SignalScheduler(sc); Graph.Route r = sampleRoute(g);
        SignalScheduler.Plan p = sch.schedule(r, VehicleType.AMBULANCE, true);
        check("A feasible signal plan exists for the sample trip", p != null, "");
        if (p == null) return;
        List<String> bad = sch.validate(p, r);
        check("Plan: no overlaps, waits within limit, times consistent with road times", bad.isEmpty(), bad.toString());

        SignalScheduler.Plan brute = sch.schedule(r, VehicleType.AMBULANCE, false);
        check("Branch and Bound finds the same optimal cost as plain backtracking", brute != null && brute.cost == p.cost, p.cost + " vs " + (brute == null ? "none" : brute.cost));
        check("Branch and Bound explores fewer states than plain backtracking", brute != null && p.states < brute.states, p.states + " vs " + (brute == null ? 0 : brute.states));

         
        Random rnd = new Random(7); int compared = 0; boolean allEqual = true;
        for (int trial = 0; trial < 60; trial++) {
            Graph gx = Graph.sampleCity(); SignalControl scx = new SignalControl(gx);
            scx.settings.cycle = 40 + rnd.nextInt(60); scx.settings.naturalGreen = 15 + rnd.nextInt(25); scx.settings.maxHeld = rnd.nextInt(40);
            SignalScheduler sx = new SignalScheduler(scx);
            for (int i = 0; i < 4; i++) { int a = rnd.nextInt(10), b = rnd.nextInt(10), st0 = rnd.nextInt(1000); if (a != b) scx.addReservation(a, st0, st0 + 20 + rnd.nextInt(60), "r"); }
            int a = rnd.nextInt(10), b = rnd.nextInt(10); if (a == b) continue;
            Graph.Route rx = gx.dijkstra(a, b, null);
            if (rx == null || rx.nodes.size() - 2 > 5) continue;
            VehicleType vt = VehicleType.values()[rnd.nextInt(3)];
            SignalScheduler.Plan fast = sx.schedule(rx, vt, true), slow = sx.schedule(rx, vt, false);
            compared++;
            allEqual &= (fast == null) == (slow == null) && (fast == null || fast.cost == slow.cost);
        }
        check("Branch and Bound = plain backtracking on randomized scenarios", allEqual && compared >= 30, compared + " scenarios compared");

         
        Graph gb = Graph.sampleCity(); SignalControl scb = new SignalControl(gb); scb.loadSampleReservations(gb); scb.settings.maxHeld = 8;
        SignalScheduler sb = new SignalScheduler(scb); Graph.Route rb = sampleRoute(gb); SignalScheduler.Plan pb = sb.schedule(rb, VehicleType.AMBULANCE, true);
        int heldTotal = 0; if (pb != null) for (SignalScheduler.Step x : pb.steps) heldTotal += x.held;
        check("Disruption budget (max 8 vehicles held) is respected", pb != null && heldTotal <= 8 && sb.validate(pb, rb).isEmpty(), "held " + heldTotal);
        scb.settings.maxHeld = 0; scb.settings.maxWait = 0;
        check("Impossible limits (no waiting, no preemption) -> no feasible plan reported", sb.schedule(rb, VehicleType.AMBULANCE, true) == null, "");

        int[][] tl = Simulator.run(g, sc, r, p, Greedy.Rule.LONGEST_QUEUE, false);
        boolean same = true, chain = true;
        for (int k = 1; k < r.nodes.size() - 1; k++) {
            same &= tl[0][k] == p.steps[k].arrive && tl[1][k] == p.steps[k].greenStart && tl[2][k] == p.steps[k].depart;
            chain &= tl[0][k + 1] == tl[2][k] + r.roads.get(k).travelTime();
        }
        check("Simulated journey follows the calculated signal schedule", same && tl[0][r.nodes.size() - 1] == p.destArrival, "");
        check("Arrival/departure times agree with road travel times", chain && tl[0][1] == r.roads.get(0).travelTime(), "");

         
        SignalScheduler.Step s1 = p.steps[1];
        sc.reservations.get(s1.node).insert(s1.greenStart - 3, s1.release + 3, "Competing convoy");
        SignalScheduler.Plan p2 = sch.schedule(r, VehicleType.AMBULANCE, true);
        check("Competing reservation: scheduler moves the green away from it", p2 != null && sch.validate(p2, r).isEmpty() && p2.steps[1].greenStart != s1.greenStart, "");
        check("Interval Tree rejects a conflicting reservation", sc.addReservation(s1.node, s1.greenStart, s1.greenStart + 5, "x") != null, "");
        check("Touching reservations [a,b) and [b,c) are allowed", new IntervalTree() {{ insert(10, 20, "a"); }}.overlapping(20, 30).isEmpty(), "");

        sc.reservations.get(s1.node).insert(0, 100000, "Total blackout");
        check("Fully blocked signal: no feasible schedule is reported (null)", sch.schedule(r, VehicleType.AMBULANCE, true) == null, "");

        Graph g3 = Graph.sampleCity(); SignalControl sc3 = new SignalControl(g3);
        int node = g3.find("Saidapet"); int prev = g3.find("Guindy"), next = g3.find("T Nagar");
        Approach pick = Greedy.choose(sc3.approaches.get(node), prev, next, Greedy.Rule.LONGEST_QUEUE);
        int maxOther = 0; for (Approach a : sc3.approaches.get(node)) if (a.from != prev && a.from != next) maxOther = Math.max(maxOther, a.queue.size());
        check("Greedy picks the longest non-emergency queue", pick != null && pick.queue.size() == maxOther, "");
    }

     
    private static void flowTests() {
        FlowNetwork f = textbookNetwork();
        int ff = f.fordFulkerson(0, 5), ek = f.edmondsKarp(0, 5);
        check("Ford-Fulkerson and Edmonds-Karp agree (textbook network = 23)", ff == ek && ek == 23, ff + " vs " + ek);
        int cut = 0; for (int[] e : f.cutEdges(0)) cut += e[2];
        boolean[] side = f.sourceSide(0);
        check("Max flow equals min-cut capacity; source in S, sink not in S", cut == ek && side[0] && !side[5], "cut " + cut + ", flow " + ek);
        check("Flow accounting stays within each road's capacity", f.capacityAccountingValid(), "");

        Graph g = Graph.sampleCity(); int s = g.find("Airport"), t = g.find("City Hospital");
        FlowNetwork fs = g.toFlowNetwork();
        int a = fs.fordFulkerson(s, t), b = fs.edmondsKarp(s, t);
        long[] mc = fs.minCostMaxFlow(s, t);
        check("Sample city: FF = EK = MCMF flow", a == b && b == mc[0] && b > 0, a + ", " + b + ", " + mc[0]);
        check("Disconnected network: max flow to Kovalam is 0", fs.edmondsKarp(s, g.find("Kovalam")) == 0 && fs.fordFulkerson(s, g.find("Kovalam")) == 0, "");
        long[] tiny = tinyCostNetwork().minCostMaxFlow(0, 3);
        check("Min-Cost Max-Flow on a hand-solved network: flow 3, cost 8", tiny[0] == 3 && tiny[1] == 8, tiny[0] + ", " + tiny[1]);
        long[] rev = reverseEdgeNetwork().minCostMaxFlow(0, 3);
        check("Min-Cost Max-Flow must use a reverse edge (cost -c): flow 2, cost 10", rev[0] == 2 && rev[1] == 10, rev[0] + ", " + rev[1]);
    }

     
    private static void johnsonTests() {
        Graph g = Graph.sampleCity(); long[][] j = g.allPairs(); boolean ok = true;
        for (int i = 0; i < g.size(); i++) for (int k = 0; k < g.size(); k++) {
            Graph.Route r = g.dijkstra(i, k, null);
            long expect = r == null ? Graph.INF : r.total;
            ok &= j[i][k] == expect;
        }
        check("Johnson distances equal Dijkstra for all 121 city pairs (incl. unreachable)", ok, "");

        List<int[]> edges = List.of(new int[]{0, 1, 4}, new int[]{1, 2, -2}, new int[]{0, 2, 3}, new int[]{2, 3, 2}, new int[]{3, 1, 1});
        long[][] d = Graph.johnson(4, edges), fw = new long[4][4];
        for (long[] row : fw) Arrays.fill(row, Graph.INF);
        for (int i = 0; i < 4; i++) fw[i][i] = 0;
        for (int[] e : edges) fw[e[0]][e[1]] = Math.min(fw[e[0]][e[1]], e[2]);
        for (int m = 0; m < 4; m++) for (int u = 0; u < 4; u++) for (int v = 0; v < 4; v++) if (fw[u][m] < Graph.INF && fw[m][v] < Graph.INF) fw[u][v] = Math.min(fw[u][v], fw[u][m] + fw[m][v]);
        check("Johnson with a negative edge equals Floyd-Warshall", d != null && Arrays.deepEquals(d, fw), "");
        check("Johnson detects a negative cycle", Graph.johnson(3, List.of(new int[]{0, 1, 1}, new int[]{1, 2, -3}, new int[]{2, 0, 1})) == null, "");
    }

     
    private static void structureTests() {
        Graph g = Graph.sampleCity();
        DSU d = g.connectivity();
        check("DSU: closed bridge leaves 2 groups (Kovalam isolated)", d.components == 2 && !d.connected(0, g.find("Kovalam")), "groups " + d.components);
        g.roads.get(g.roads.size() - 1).available = true;
        check("DSU: opening the road joins everything into 1 group", g.connectivity().components == 1, "");

        Random rnd = new Random(42);
        List<Intersection> pts = new ArrayList<>();
        for (int i = 0; i < 200; i++) pts.add(new Intersection(i, "p" + i, rnd.nextInt(100), rnd.nextInt(100), false));
        KDTree kd = new KDTree(pts); boolean kdOk = true;
        for (int q = 0; q < 300; q++) {
            double x = rnd.nextDouble() * 100, y = rnd.nextDouble() * 100, bestD = Double.MAX_VALUE;
            for (Intersection p : pts) bestD = Math.min(bestD, (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y));
            Intersection n = kd.nearest(x, y);
            kdOk &= Math.abs((n.x - x) * (n.x - x) + (n.y - y) * (n.y - y) - bestD) < 1e-9;
        }
        check("KD Tree nearest = brute force on 300 random queries", kdOk, "");

        Road capacityProbe = new Road(0, 0, 1, 100, 80, 25);
        boolean capacityOk = capacityProbe.freeCapacity() == 60;
        capacityProbe.recent.fill(100);
        capacityOk &= capacityProbe.freeCapacity() == 0;
        check("Traffic-adjusted spare capacity reflects road density", capacityOk, "");

        Directory dir = new Directory(g); boolean prefixOk = true;
        for (Intersection n : g.nodes) for (int len = 1; len <= Math.min(3, n.name.length()); len++) {
            String pre = n.name.substring(0, len).toLowerCase();
            List<String> expect = new ArrayList<>();
            for (Intersection m : g.nodes) if (m.name.toLowerCase().startsWith(pre)) expect.add(m.name);
            List<String> got = dir.prefix(pre); Collections.sort(expect);
            prefixOk &= expect.equals(got);
        }
        check("Location prefix search matches case-insensitive name filtering", prefixOk, "");

        IntervalTree it = new IntervalTree(); List<int[]> all = new ArrayList<>(); boolean itOk = true;
        for (int i = 0; i < 150; i++) { int s = rnd.nextInt(1000), e = s + 1 + rnd.nextInt(40); it.insert(s, e, "r"); all.add(new int[]{s, e}); }
        for (int q = 0; q < 300; q++) {
            int s = rnd.nextInt(1000), e = s + 1 + rnd.nextInt(60), expect = 0;
            for (int[] iv : all) if (iv[0] < e && s < iv[1]) expect++;
            itOk &= it.overlapping(s, e).size() == expect;
        }
        check("Interval Tree overlap query = brute force on 300 queries", itOk, "");
    }

     
    private static void inputTests() {
        Graph g = Graph.sampleCity(); Directory dir = new Directory(g);
        check("Empty input rejected", throwsBad(() -> Input.parseInt("", 0, 100)), "");
        check("Non-numeric input rejected", throwsBad(() -> Input.parseInt("abc", 0, 100)), "");
        check("Negative travel time rejected", throwsBad(() -> Input.parseInt("-30", 1, 3600)), "");
        check("Capacity 0 rejected", throwsBad(() -> Input.parseInt("0", 1, 500)), "");
        check("Density above 100 rejected", throwsBad(() -> Input.parseInt("101", 0, 100)), "");
        check("Valid number accepted", Input.parseInt(" 50 ", 0, 100) == 50, "");
        check("Unknown intersection name matches nothing", dir.find("Atlantis").isEmpty() && dir.find("").isEmpty() && dir.find("999").isEmpty(), "");
        check("Prefix 'gu' finds exactly Guindy; number 1 finds Airport", dir.find("gu").equals(List.of(g.find("Guindy"))) && dir.find("1").equals(List.of(0)), "");
    }
}
