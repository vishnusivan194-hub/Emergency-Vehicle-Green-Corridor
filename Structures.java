
import java.util.*;

 
class DSU {
    private final int[] parent, size;
    int components;

    DSU(int n) {
        parent = new int[n]; size = new int[n]; components = n;
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
    }
    int find(int x) { if (parent[x] != x) parent[x] = find(parent[x]); return parent[x]; }    
    boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }                            
        parent[rb] = ra; size[ra] += size[rb]; components--;
        return true;
    }
    boolean connected(int a, int b) { return find(a) == find(b); }
}

 
class KDTree {
    private static class Node { Intersection p; Node left, right; }
    private final Node root;
    private Intersection best; private double bestDist;

    KDTree(List<Intersection> points) { root = build(new ArrayList<>(points), 0); }

    private Node build(List<Intersection> pts, int depth) {
        if (pts.isEmpty()) return null;
        final int axis = depth % 2;
        pts.sort(Comparator.comparingDouble(p -> axis == 0 ? p.x : p.y));
        int mid = pts.size() / 2;                                    
        Node n = new Node(); n.p = pts.get(mid);
        n.left = build(new ArrayList<>(pts.subList(0, mid)), depth + 1);
        n.right = build(new ArrayList<>(pts.subList(mid + 1, pts.size())), depth + 1);
        return n;
    }
    Intersection nearest(double x, double y) {
        best = null; bestDist = Double.MAX_VALUE;
        search(root, x, y, 0);
        return best;
    }
    private void search(Node n, double x, double y, int depth) {
        if (n == null) return;
        double d = (n.p.x - x) * (n.p.x - x) + (n.p.y - y) * (n.p.y - y);
        if (d < bestDist) { bestDist = d; best = n.p; }
        double diff = depth % 2 == 0 ? x - n.p.x : y - n.p.y;
        Node near = diff < 0 ? n.left : n.right, far = diff < 0 ? n.right : n.left;
        search(near, x, y, depth + 1);
        if (diff * diff < bestDist) search(far, x, y, depth + 1);     
    }
}

 
record Interval(int start, int end, String label) {}

 

class IntervalTree {
    private static class Node { final Interval iv; int maxEnd; Node left, right; Node(Interval iv) { this.iv = iv; maxEnd = iv.end(); } }
    private Node root;

    void insert(int start, int end, String label) { root = insert(root, new Interval(start, end, label)); }
    private Node insert(Node n, Interval iv) {
        if (n == null) return new Node(iv);
        if (iv.start() < n.iv.start()) n.left = insert(n.left, iv); else n.right = insert(n.right, iv);
        n.maxEnd = Math.max(n.maxEnd, iv.end());
        return n;
    }
     
    List<Interval> overlapping(int start, int end) { List<Interval> out = new ArrayList<>(); query(root, start, end, out); return out; }
    private void query(Node n, int s, int e, List<Interval> out) {
        if (n == null || n.maxEnd <= s) return;                    
        query(n.left, s, e, out);
        if (n.iv.start() < e && s < n.iv.end()) out.add(n.iv);
        if (n.iv.start() < e) query(n.right, s, e, out);           
    }
    List<Interval> all() { List<Interval> out = new ArrayList<>(); inOrder(root, out); return out; }
    private void inOrder(Node n, List<Interval> out) { if (n == null) return; inOrder(n.left, out); out.add(n.iv); inOrder(n.right, out); }
}

 
class Directory {
    private final Graph g;

    Directory(Graph g) { this.g = g; }

     
    List<Integer> find(String text) {
        List<Integer> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        String t = text.trim();
        if (t.matches("\\d{1,4}")) { int n = Integer.parseInt(t); if (n >= 1 && n <= g.size()) out.add(n - 1); return out; }
        Integer exact = g.find(t);
        if (exact != null) { out.add(exact); return out; }
        for (String name : prefix(t)) out.add(g.find(name));
        return out;
    }
    List<String> prefix(String p) {
        List<String> matches = new ArrayList<>();
        if (p == null) return matches;
        String prefix = p.trim().toLowerCase(Locale.ROOT);
        for (Intersection node : g.nodes) {
            if (node.name.toLowerCase(Locale.ROOT).startsWith(prefix)) matches.add(node.name);
        }
        matches.sort(String.CASE_INSENSITIVE_ORDER);
        return matches;
    }
}
