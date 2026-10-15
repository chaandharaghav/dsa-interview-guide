import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Main {
    public static void main(String[] args) throws Exception {
        FileSystem fs = new FileSystem();

        System.out.println("== mkdir and write ==");
        fs.mkdir("/home/alice/docs", true);
        fs.mkdir("/home/bob", true);
        fs.mkdir("/tmp", false);
        fs.write("/home/alice/notes.txt", "buy milk");
        fs.write("/home/alice/docs/todo.txt", "write the design");
        fs.write("/home/bob/notes.txt", "call Alice");
        System.out.println("ls /: " + fs.ls("/"));
        System.out.println("ls /home/alice: " + fs.ls("/home/alice"));
        System.out.println("ls /home/alice/notes.txt: " + fs.ls("/home/alice/notes.txt"));

        System.out.println("== cat, append, size ==");
        fs.append("/home/alice/notes.txt", " and eggs");
        System.out.println("cat: " + fs.cat("/home/alice/notes.txt"));
        System.out.println("size /home/alice/notes.txt: " + fs.size("/home/alice/notes.txt"));
        System.out.println("size /home/alice: " + fs.size("/home/alice"));
        System.out.println("size /home: " + fs.size("/home"));
        System.out.println("find notes.txt under /: " + fs.find("/", "notes.txt"));

        System.out.println("== path resolution ==");
        System.out.println("cat via '..': " + fs.cat("/home/bob/../alice/./notes.txt"));
        System.out.println("ls /../..: " + fs.ls("/../.."));
        attempt("mkdir -p on an existing directory", () -> fs.mkdir("/home/alice", true));
        attempt("mkdir on an existing directory", () -> fs.mkdir("/home/alice", false));
        attempt("mkdir the root", () -> fs.mkdir("/", false));
        attempt("relative path", () -> fs.cat("home/alice/notes.txt"));
        attempt("missing file", () -> fs.cat("/home/alice/none.txt"));
        attempt("mkdir without -p", () -> fs.mkdir("/a/b", false));
        attempt("cat a directory", () -> fs.cat("/home"));
        attempt("write to a directory", () -> fs.write("/home/alice", "x"));
        attempt("file as parent", () -> fs.write("/home/alice/notes.txt/x", "x"));

        System.out.println("== move ==");
        fs.move("/home/alice/notes.txt", "/tmp");
        System.out.println("ls /tmp: " + fs.ls("/tmp"));
        fs.move("/tmp/notes.txt", "/tmp/old-notes.txt");
        System.out.println("ls /tmp: " + fs.ls("/tmp"));
        attempt("move into own subtree", () -> fs.move("/home/alice", "/home/alice/docs"));
        attempt("move onto itself", () -> fs.move("/home/alice", "/home/alice"));
        attempt("move onto existing file",
                () -> fs.move("/home/bob/notes.txt", "/home/alice/docs/todo.txt"));
        fs.move("/home/alice/docs", "/tmp");
        System.out.println("ls /tmp: " + fs.ls("/tmp"));
        System.out.println("path of todo.txt: " + fs.find("/", "todo.txt"));

        System.out.println("== delete ==");
        attempt("rm non-empty directory", () -> fs.remove("/home", false));
        fs.mkdir("/tmp/empty", false);
        attempt("rm empty directory", () -> fs.remove("/tmp/empty", false));
        fs.remove("/home", true);
        System.out.println("ls /: " + fs.ls("/"));
        attempt("rm root", () -> fs.remove("/", true));
        attempt("rm /tmp/.. (the root)", () -> fs.remove("/tmp/..", true));

        System.out.println("== concurrent writes ==");
        fs.mkdir("/shared", false);
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            int worker = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 25; i++) {
                    fs.write("/shared/w" + worker + "-" + i + ".txt", "data");
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("files in /shared: " + fs.ls("/shared").size());
        System.out.println("size /shared: " + fs.size("/shared"));
    }

    private static void attempt(String label, Runnable action) {
        try {
            action.run();
            System.out.println(label + ": ok");
        } catch (FsException e) {
            System.out.println(label + ": error (" + e.getMessage() + ")");
        }
    }
}

class FsException extends RuntimeException {
    FsException(String message) {
        super(message);
    }
}

abstract sealed class FsNode permits FileNode, DirNode {
    private String name;
    private DirNode parent;

    FsNode(String name) {
        this.name = name;
    }

    String name() { return name; }
    DirNode parent() { return parent; }
    void attach(DirNode newParent, String newName) {
        this.parent = newParent;
        this.name = newName;
    }
    void detach() { this.parent = null; }

    abstract long size();
    abstract void collect(Predicate<FsNode> filter, String path, List<String> out);
}

final class FileNode extends FsNode {
    private StringBuilder content = new StringBuilder();

    FileNode(String name) {
        super(name);
    }

    String read() { return content.toString(); }
    void write(String text) { content = new StringBuilder(text); }
    void append(String text) { content.append(text); }

    @Override long size() { return content.length(); }

    @Override void collect(Predicate<FsNode> filter, String path, List<String> out) {
        if (filter.test(this)) {
            out.add(path);
        }
    }
}

final class DirNode extends FsNode {
    private final Map<String, FsNode> children = new TreeMap<>();

    DirNode(String name) {
        super(name);
    }

    FsNode child(String childName) { return children.get(childName); }
    Collection<FsNode> children() { return children.values(); }
    boolean isEmpty() { return children.isEmpty(); }

    void add(FsNode node, String childName) {
        children.put(childName, node);
        node.attach(this, childName);
    }

    void remove(String childName) {
        FsNode node = children.remove(childName);
        if (node != null) {
            node.detach();
        }
    }

    @Override long size() {
        long total = 0;
        for (FsNode node : children.values()) {
            total += node.size();
        }
        return total;
    }

    @Override void collect(Predicate<FsNode> filter, String path, List<String> out) {
        if (filter.test(this)) {
            out.add(path);
        }
        for (FsNode node : children.values()) {
            String childPath = path.equals("/") ? "/" + node.name() : path + "/" + node.name();
            node.collect(filter, childPath, out);
        }
    }
}

final class FileSystem {
    private final DirNode root = new DirNode("");
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    void mkdir(String path, boolean parents) {
        inWrite(() -> {
            List<String> parts = parse(path);
            if (parts.isEmpty() && !parents) {
                throw new FsException("already exists: " + path);
            }
            DirNode current = root;
            for (int i = 0; i < parts.size(); i++) {
                String part = parts.get(i);
                boolean last = i == parts.size() - 1;
                switch (current.child(part)) {
                    case null -> {
                        if (!last && !parents) {
                            throw new FsException("no such file or directory: " + path);
                        }
                        DirNode created = new DirNode(part);
                        current.add(created, part);
                        current = created;
                    }
                    case FileNode file -> throw new FsException("not a directory: " + path);
                    case DirNode dir -> {
                        if (last && !parents) {
                            throw new FsException("already exists: " + path);
                        }
                        current = dir;
                    }
                }
            }
        });
    }

    List<String> ls(String path) {
        return inRead(() -> switch (resolve(parse(path), path)) {
            case FileNode file -> List.of(file.name());
            case DirNode dir -> {
                List<String> names = new ArrayList<>();
                for (FsNode child : dir.children()) {
                    names.add(child instanceof DirNode ? child.name() + "/" : child.name());
                }
                yield names;
            }
        });
    }

    String cat(String path) {
        return inRead(() -> file(path).read());
    }

    void write(String path, String content) {
        inWrite(() -> fileForWrite(path).write(content));
    }

    void append(String path, String content) {
        inWrite(() -> fileForWrite(path).append(content));
    }

    long size(String path) {
        return inRead(() -> resolve(parse(path), path).size());
    }

    List<String> find(String startPath, String name) {
        return inRead(() -> {
            FsNode start = resolve(parse(startPath), startPath);
            List<String> out = new ArrayList<>();
            String shown = "/" + String.join("/", parse(startPath));
            start.collect(node -> node.name().equals(name), shown, out);
            return out;
        });
    }

    void move(String from, String to) {
        inWrite(() -> {
            FsNode source = resolve(parse(from), from);
            if (source == root) {
                throw new FsException("cannot move the root");
            }
            List<String> targetParts = parse(to);
            FsNode existing = tryResolve(targetParts);
            DirNode targetDir;
            String newName;
            if (existing instanceof DirNode dir) {
                targetDir = dir;
                newName = source.name();
            } else {
                if (targetParts.isEmpty()) {
                    throw new FsException("bad target: " + to);
                }
                newName = targetParts.get(targetParts.size() - 1);
                targetDir = directory(targetParts.subList(0, targetParts.size() - 1), to);
            }
            for (FsNode up = targetDir; up != null; up = up.parent()) {
                if (up == source) {
                    throw new FsException("cannot move a directory into itself");
                }
            }
            if (targetDir.child(newName) != null) {
                throw new FsException("target exists: " + newName);
            }
            source.parent().remove(source.name());
            targetDir.add(source, newName);
        });
    }

    void remove(String path, boolean recursive) {
        inWrite(() -> {
            FsNode node = resolve(parse(path), path);
            if (node == root) {
                throw new FsException("cannot remove the root");
            }
            if (node instanceof DirNode dir && !dir.isEmpty() && !recursive) {
                throw new FsException("directory not empty: " + path);
            }
            node.parent().remove(node.name());
        });
    }

    static List<String> parse(String path) {
        if (!path.startsWith("/")) {
            throw new FsException("path must be absolute: " + path);
        }
        Deque<String> parts = new ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                parts.pollLast();
            } else {
                parts.addLast(part);
            }
        }
        return new ArrayList<>(parts);
    }

    private FsNode tryResolve(List<String> parts) {
        FsNode current = root;
        for (String part : parts) {
            if (!(current instanceof DirNode dir)) {
                return null;
            }
            current = dir.child(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private FsNode resolve(List<String> parts, String path) {
        FsNode node = tryResolve(parts);
        if (node == null) {
            throw new FsException("no such file or directory: " + path);
        }
        return node;
    }

    private DirNode directory(List<String> parts, String path) {
        if (resolve(parts, path) instanceof DirNode dir) {
            return dir;
        }
        throw new FsException("not a directory: " + path);
    }

    private FileNode file(String path) {
        if (resolve(parse(path), path) instanceof FileNode file) {
            return file;
        }
        throw new FsException("is a directory: " + path);
    }

    private FileNode fileForWrite(String path) {
        List<String> parts = parse(path);
        if (parts.isEmpty()) {
            throw new FsException("is a directory: " + path);
        }
        DirNode parent = directory(parts.subList(0, parts.size() - 1), path);
        String name = parts.get(parts.size() - 1);
        return switch (parent.child(name)) {
            case null -> {
                FileNode created = new FileNode(name);
                parent.add(created, name);
                yield created;
            }
            case FileNode file -> file;
            case DirNode dir -> throw new FsException("is a directory: " + path);
        };
    }

    private <T> T inRead(Supplier<T> action) {
        lock.readLock().lock();
        try {
            return action.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void inWrite(Runnable action) {
        lock.writeLock().lock();
        try {
            action.run();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
