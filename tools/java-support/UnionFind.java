/**
 * Union Find with union by size and path compression.
 * It has the same members as the class on kb/dsa/13-union-find/README.md, so that
 * a block on a KB page can call UnionFind without a copy of the class in the block.
 */
public class UnionFind {
    private final int[] parent;
    private final int[] size;
    private int components;

    public UnionFind(int n) {
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
        components = n;
    }

    public int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);
        return parent[x];
    }

    public boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        size[ra] += size[rb];
        components--;
        return true;
    }

    public boolean connected(int a, int b) { return find(a) == find(b); }

    public int componentSize(int x) { return size[find(x)]; }

    public int components() { return components; }
}
