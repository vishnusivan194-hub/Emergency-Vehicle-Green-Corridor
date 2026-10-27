import java.util.*;

 
enum VehicleType {
    AMBULANCE("Ambulance", 5), FIRE_ENGINE("Fire Engine", 4), POLICE("Police Vehicle", 2);
    final String label; final int urgency;
    VehicleType(String label, int urgency) { this.label = label; this.urgency = urgency; }
}

 
class Fmt { static String t(int s) { return String.format("%02d:%02d", s / 60, s % 60); } }

 
class SignalSettings {
    int cycle = 60;            
    int naturalGreen = 30;     
    int crossTime = 10;        
    int buffer = 5;            
    int safetyGap = 3;         
    int step = 5;              
    int maxWait = 60;          
    int maxHeld = 25;          

    int hold() { return crossTime + buffer; }                     
    int offset(int node) { return (node * 23) % cycle; }          
     
    boolean naturalGreenCovers(int node, int s) { return Math.floorMod(s - offset(node), cycle) + hold() <= naturalGreen; }
}

 
class Approach {
    final int from; final int headWait;                           
    final ArrayDeque<String> queue = new ArrayDeque<>();
    Approach(int node, int from, int size, int headWait) {
        this.from = from; this.headWait = headWait;
        for (int i = 1; i <= size; i++) queue.add("V" + node + "." + from + "." + i);
    }
}

 
class Greedy {
    enum Rule { LONGEST_QUEUE, LONGEST_WAIT }
    static Approach choose(List<Approach> list, int emergencyFrom, int emergencyTo, Rule rule) {
        Approach best = null;
        for (Approach a : list) {
            if (a.from == emergencyFrom || a.from == emergencyTo) continue;       
            if (best == null || score(a, rule) > score(best, rule)) best = a;      
        }
        return best;
    }
    static int score(Approach a, Rule r) { return r == Rule.LONGEST_QUEUE ? a.queue.size() : a.headWait; }
}

 
class SignalControl {
    final SignalSettings settings = new SignalSettings();
    final Map<Integer, IntervalTree> reservations = new HashMap<>();
    final Map<Integer, List<Approach>> approaches = new HashMap<>();

    SignalControl(Graph g) {
        for (Intersection n : g.nodes) {
            reservations.put(n.id, new IntervalTree());
            List<Approach> list = new ArrayList<>();
            for (Road r : g.adj.get(n.id)) {
                int nb = r.other(n.id);                                            
                list.add(new Approach(n.id, nb, 2 + (n.id * 7 + nb * 3) % 8, 10 + (n.id * 13 + nb * 11) % 80));
            }
            approaches.put(n.id, list);
        }
    }
     
    int crossQueue(int node, int prev, int next) {
        int total = 0;
        for (Approach a : approaches.get(node)) if (a.from != prev && a.from != next) total += a.queue.size();
        return total;
    }
     
    Interval addReservation(int node, int start, int end, String label) {
        List<Interval> clash = reservations.get(node).overlapping(start, end);
        if (!clash.isEmpty()) return clash.get(0);
        reservations.get(node).insert(start, end, label);
        return null;
    }
     
    void loadSampleReservations(Graph g) {
        addReservation(g.find("Guindy"), 175, 215, "Bus-priority phase");
        addReservation(g.find("Saidapet"), 300, 345, "Pedestrian scramble");
        addReservation(g.find("T Nagar"), 400, 440, "Another convoy");
        addReservation(g.find("Adyar"), 380, 420, "Tram crossing");
    }
}

 














class SignalScheduler {
    static final int NORMAL_WEIGHT = 3;

    static class Step {                                     
        int node, arrive, wait, greenStart, depart, release, held; boolean preempted; long cost;
        Step copy() {
            Step s = new Step(); s.node = node; s.arrive = arrive; s.wait = wait; s.greenStart = greenStart; s.depart = depart;
            s.release = release; s.held = held; s.preempted = preempted; s.cost = cost; return s;
        }
    }
    static class Plan { Step[] steps; long cost; int destArrival, states, prunes, rejected; boolean pruned; }

    private final SignalControl sc; private final SignalSettings st;
    private Graph.Route route; private int urgency; private boolean prune;
    private Step[] cur, bestSteps; private long best; private int states, prunes, rejected;

    SignalScheduler(SignalControl sc) { this.sc = sc; this.st = sc.settings; }

     
    Plan schedule(Graph.Route r, VehicleType v, boolean usePruning) {
        int m = r.nodes.size();
        if (m < 2) return null;
        route = r; urgency = v.urgency; prune = usePruning;
        cur = new Step[m]; bestSteps = null; best = Long.MAX_VALUE; states = prunes = rejected = 0;
        for (int k = 0; k < m; k++) { cur[k] = new Step(); cur[k].node = r.nodes.get(k); }
        search(1, r.roads.get(0).travelTime(), 0, 0);
        if (bestSteps == null) return null;
        Plan p = new Plan();
        p.steps = bestSteps; p.cost = best; p.destArrival = bestSteps[m - 1].arrive;
        p.states = states; p.prunes = prunes; p.rejected = rejected; p.pruned = usePruning;
        return p;
    }

    private void search(int k, int arrival, long cost, int heldSoFar) {
        states++;
        int m = route.nodes.size();
        cur[k].arrive = arrival;
        if (k == m - 1) {                                               
            if (cost < best) { best = cost; bestSteps = new Step[m]; for (int i = 0; i < m; i++) bestSteps[i] = cur[i].copy(); }
            return;
        }
        int node = route.nodes.get(k), prev = route.nodes.get(k - 1), next = route.nodes.get(k + 1);
        List<int[]> options = new ArrayList<>();                        
        for (int s : candidateStarts(node, arrival)) {
            if (!sc.reservations.get(node).overlapping(s - st.safetyGap, s + st.hold() + st.safetyGap).isEmpty()) { rejected++; continue; }
            boolean pre = !st.naturalGreenCovers(node, s);
            int held = pre ? sc.crossQueue(node, prev, next) : 0;
            if (heldSoFar + held > st.maxHeld) { rejected++; continue; }        
            options.add(new int[]{s, urgency * (s - arrival) + NORMAL_WEIGHT * held, pre ? 1 : 0, held});
        }
        options.sort(Comparator.comparingInt(o -> o[1]));               
        for (int[] o : options) {
            if (prune && cost + o[1] >= best) { prunes++; continue; }    
            Step c = cur[k];
            c.greenStart = o[0]; c.wait = o[0] - arrival; c.depart = o[0] + st.crossTime; c.release = o[0] + st.hold();
            c.preempted = o[2] == 1; c.held = o[3]; c.cost = o[1];
            search(k + 1, c.depart + route.roads.get(k).travelTime(), cost + o[1], heldSoFar + o[3]);    
        }
    }

    private TreeSet<Integer> candidateStarts(int node, int arrival) {
        TreeSet<Integer> set = new TreeSet<>();
        for (int w = 0; w <= st.maxWait; w += st.step) set.add(arrival + w);
        int p = Math.floorMod(arrival - st.offset(node), st.cycle);
        if (st.hold() <= st.naturalGreen && p > st.naturalGreen - st.hold() && arrival + st.cycle - p <= arrival + st.maxWait)
            set.add(arrival + st.cycle - p);                            
        for (Interval iv : sc.reservations.get(node).overlapping(arrival, arrival + st.maxWait + st.hold() + st.safetyGap))
            if (iv.end() + st.safetyGap <= arrival + st.maxWait) set.add(iv.end() + st.safetyGap);    
        return set;
    }

     

    int baselineArrival(Graph.Route r) {
        int t = r.roads.get(0).travelTime();
        for (int k = 1; k < r.nodes.size() - 1; k++) {
            int node = r.nodes.get(k), s = t, guard = 0;
            while ((!st.naturalGreenCovers(node, s) || !sc.reservations.get(node).overlapping(s, s + st.hold()).isEmpty()) && guard++ < 5 * st.cycle) s++;
            t = s + st.crossTime + r.roads.get(k).travelTime();
        }
        return t;
    }

     
    List<String> validate(Plan p, Graph.Route r) {
        List<String> bad = new ArrayList<>();
        int m = r.nodes.size();
        if (p.steps.length != m) { bad.add("plan length differs from route length"); return bad; }
        if (p.steps[0].node != r.nodes.get(0) || p.steps[m - 1].node != r.nodes.get(m - 1)) bad.add("plan does not start/end at the route ends");
        int expectArrive = r.roads.get(0).travelTime(), heldTotal = 0; long cost = 0;
        for (int k = 1; k < m - 1; k++) {
            Step s = p.steps[k]; String where = "signal " + k + ": ";
            if (s.arrive != expectArrive) bad.add(where + "arrival " + s.arrive + " != departure+road time " + expectArrive);
            if (s.greenStart < s.arrive) bad.add(where + "green starts before the vehicle arrives");
            if (s.wait != s.greenStart - s.arrive || s.wait > st.maxWait) bad.add(where + "wait inconsistent or above the limit");
            if (s.depart != s.greenStart + st.crossTime) bad.add(where + "departure != green start + crossing time");
            if (!sc.reservations.get(s.node).overlapping(s.greenStart - st.safetyGap, s.greenStart + st.hold() + st.safetyGap).isEmpty())
                bad.add(where + "green window overlaps an existing reservation");
            expectArrive = s.depart + r.roads.get(k).travelTime(); cost += s.cost; heldTotal += s.held;
        }
        if (p.steps[m - 1].arrive != expectArrive) bad.add("destination arrival inconsistent with road times");
        if (heldTotal > st.maxHeld) bad.add("cross-traffic held (" + heldTotal + ") exceeds the budget " + st.maxHeld);
        if (cost != p.cost) bad.add("plan cost " + p.cost + " != sum of signal costs " + cost);
        return bad;
    }
}

 
class Simulator {
     

    static int[][] run(Graph g, SignalControl sc, Graph.Route r, SignalScheduler.Plan p, Greedy.Rule rule, boolean print) {
        int m = r.nodes.size();
        int[] arrive = new int[m], start = new int[m], depart = new int[m];
        int clock = 0;                                                    
        if (print) System.out.println("Clock = " + Fmt.t(0) + "  Vehicle leaves " + g.name(r.nodes.get(0)) + ".");
        for (int k = 1; k < m; k++) {
            Road road = r.roads.get(k - 1);
            clock = depart[k - 1] + road.travelTime();                    
            arrive[k] = clock;
            String from = g.name(r.nodes.get(k - 1)), here = g.name(r.nodes.get(k));
            if (print) System.out.printf("%n>> DRIVE %s -> %s: depart %s + road time %d s = arrive %s%n", from, here, Fmt.t(depart[k - 1]), road.travelTime(), Fmt.t(clock));
            if (k == m - 1) {
                start[k] = depart[k] = clock;
                if (print) System.out.println("\n*** PREDICTED ARRIVAL at destination " + here + " at " + Fmt.t(clock) + " (" + clock + " s). ***");
                break;
            }
            SignalScheduler.Step s = p.steps[k];
            clock = Math.max(clock, s.greenStart);                        
            start[k] = clock; depart[k] = clock + sc.settings.crossTime;
            if (print) {
                int prev = r.nodes.get(k - 1), next = r.nodes.get(k + 1);
                System.out.printf("   SIGNAL %d of %d: %s%n", k, m - 2, here);
                System.out.println("     Estimated arrival ........... " + Fmt.t(arrive[k]));
                System.out.println("     Existing reservations ....... " + reservationText(sc, r.nodes.get(k)));
                System.out.println("     Emergency road signal ....... " + (s.preempted
                        ? "NOT green at the scheduled time -> preempt (" + (s.held == 0 ? "no cross-traffic to hold" : "holds " + s.held + " cross-traffic vehicles") + ")"
                        : "normal green at the scheduled time -> no preemption needed"));
                System.out.println("     Scheduled green start ....... " + Fmt.t(s.greenStart));
                System.out.println("     Estimated waiting ........... " + (start[k] - arrive[k]) + " s");
                System.out.println("     Vehicle crosses ............. " + Fmt.t(start[k]) + " -> " + Fmt.t(depart[k]) + "  (estimated departure " + Fmt.t(depart[k]) + ")");
                System.out.println("     Green clearance (released) .. " + Fmt.t(s.release) + "  (signal returns to normal)");
                System.out.println("     Next intersection ........... " + g.name(next));
                Approach a = Greedy.choose(sc.approaches.get(r.nodes.get(k)), prev, next, rule);
                if (a != null) {
                    List<String> first = new ArrayList<>(a.queue).subList(0, Math.min(3, a.queue.size()));
                    System.out.println("     Normal traffic release from " + g.name(a.from) + ": " + first + " ... (waiting vehicles " + a.queue.size() + ")");
                }
                System.out.println("     Logged: arrival, wait, green interval, clearance, departure (PREDICTED, not detected)");
            }
        }
        return new int[][]{arrive, start, depart};
    }

    static String reservationText(SignalControl sc, int node) {
        List<Interval> all = sc.reservations.get(node).all();
        if (all.isEmpty()) return "none";
        List<String> parts = new ArrayList<>();
        for (Interval iv : all) parts.add("[" + Fmt.t(iv.start()) + "-" + Fmt.t(iv.end()) + ") " + iv.label());
        return String.join("; ", parts);
    }
}
