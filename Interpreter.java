import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Interpreter {
    // Programmquelle
    private final TreeMap<Integer, String> source = new TreeMap<>();

    // Variablen & Arrays
    private final Map<String, Value> vars = new HashMap<>();
    private final Map<String, ArrayVar> arrays = new HashMap<>();

    // Funktionen
    private final FunctionRegistry fn;
    private final Random rng = new Random();

    public Interpreter(FunctionRegistry fn) { this.fn = fn; }

    public FunctionRegistry functions() { return fn; }

    /* ===== Programmbearbeitung ===== */
    public void addOrReplaceLine(int line, String content) { source.put(line, content); }
    public void removeLine(int line) { source.remove(line); }
    public void list(PrintStream out) { for (var e : source.entrySet()) out.println(e.getKey() + " " + e.getValue()); }
    public void newProgram() { source.clear(); vars.clear(); arrays.clear(); }
    public void save(Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (var e : source.entrySet()) { w.write(e.getKey() + " " + e.getValue()); w.newLine(); }
        }
    }
    public void load(Path file) throws IOException {
        source.clear();
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.strip(); if (line.isEmpty()) continue;
                int sp = line.indexOf(' ');
                int num; String rest = "";
                if (sp < 0) num = Integer.parseInt(line);
                else { num = Integer.parseInt(line.substring(0, sp)); rest = line.substring(sp + 1).strip(); }
                if (!rest.isEmpty()) source.put(num, rest);
            }
        }
        vars.clear(); arrays.clear();
    }

    /* ===== Ausführen ===== */
    public void run(BufferedReader in, PrintStream out) {
        if (source.isEmpty()) throw new BasicException("Kein Programm vorhanden.");
        Map<Integer, Statement> program = new HashMap<>();
        List<Integer> order = new ArrayList<>(source.keySet());

        for (int ln : order) {
            String code = source.get(ln);
            Tokenizer tz = new Tokenizer(code);
            Parser p = new Parser(this, tz);
            Statement st = p.parseStatement();
            if (tz.hasMore()) throw new BasicException("Zeile " + ln + ": Unerwarteter Rest: " + tz.remaining());
            program.put(ln, st);
        }
        Map<Integer, Integer> lineToIndex = new HashMap<>();
        Map<Integer, Integer> nextLine = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            int ln = order.get(i);
            lineToIndex.put(ln, i);
            nextLine.put(ln, (i + 1 < order.size()) ? order.get(i + 1) : null);
        }

        Context ctx = new Context(this, in, out, nextLine);
        for (int pc = 0; pc < order.size();) {
            int ln = order.get(pc);
            ctx.currentLine = ln;
            ctx.clearControl();
            try {
                program.get(ln).execute(ctx);
            } catch (BasicException be) {
                throw new BasicException("Zeile " + ln + ": " + be.getMessage());
            } catch (RuntimeException re) {
                throw new BasicException("Zeile " + ln + ": " + re.getMessage());
            }

            if (ctx.stop) break;
            if (ctx.jumpToLine != null) {
                Integer idx = lineToIndex.get(ctx.jumpToLine);
                if (idx == null) throw new BasicException("Sprung in nicht existierende Zeile " + ctx.jumpToLine);
                pc = idx;
            } else {
                pc++;
            }
        }
    }

    /* ===== Speicher/RT ===== */
    public Value getVar(String name) { return vars.getOrDefault(name, Value.number(0)); }
    public void setVar(String name, Value v) { vars.put(name, v); }

    public void dimArray(String name, boolean stringType, int[] dims) { arrays.put(name, new ArrayVar(stringType, dims)); }
    public Value getArray(String name, int[] idxs) {
        ArrayVar a = arrays.get(name);
        if (a == null) throw new BasicException("Array " + name + " nicht dimensioniert (DIM fehlt).");
        int flat = a.indexFrom(idxs);
        if (a.stringType) {
            String s = a.strs[flat];
            if (s == null) s = "";
            return Value.string(s);
        } else {
            return Value.number(a.nums[flat]);
        }
    }
    public void setArray(String name, int[] idxs, Value v) {
        ArrayVar a = arrays.get(name);
        if (a == null) throw new BasicException("Array " + name + " nicht dimensioniert (DIM fehlt).");
        int flat = a.indexFrom(idxs);
        if (a.stringType) a.strs[flat] = v.asString();
        else a.nums[flat] = v.asNumber();
    }

    public Random rng() { return rng; }
    public void randomize(Long seed) { if (seed == null) rng.setSeed(System.nanoTime()); else rng.setSeed(seed); }

    /* ===== Hilfstypen ===== */
    public static final class BasicException extends RuntimeException { public BasicException(String msg) { super(msg); } }

    public static final class Context {
        public final Interpreter rt;
        public final BufferedReader in;
        public final PrintStream out;
        public final Map<Integer, Integer> nextLine;
        public boolean stop = false;
        public Integer jumpToLine = null;
        public Integer currentLine = null;
        public final Deque<Integer> gosubStack = new ArrayDeque<>();
        public final Deque<ForFrame> forStack = new ArrayDeque<>();

        Context(Interpreter rt, BufferedReader in, PrintStream out, Map<Integer, Integer> nextLine) {
            this.rt = rt; this.in = in; this.out = out; this.nextLine = nextLine;
        }
        public void clearControl() { stop = false; jumpToLine = null; }
        public Integer nextOf(int line) { return nextLine.get(line); }
    }

    public static final class ForFrame {
        final String var; final double end; final double step; final int lineAfterFor;
        ForFrame(String var, double end, double step, int lineAfterFor) {
            this.var = var; this.end = end; this.step = step; this.lineAfterFor = lineAfterFor;
        }
    }

    public interface Statement { void execute(Context ctx); }
    public interface Expr { Value eval(Interpreter rt); }

    /* ============================ VALUE & ARRAYS ============================ */
    public static final class Value {
        private final Double num; private final String str;
        private Value(Double num, String str) { this.num = num; this.str = str; }
        public static Value number(double d) { return new Value(d, null); }
        public static Value string(String s) { return new Value(null, s); }
        public boolean isString() { return str != null; }
        public double asNumber() { if (num != null) return num; try { return Double.parseDouble(str); } catch (Exception e) { return 0.0; } }
        public String asString() { return (str != null) ? str : (num == null ? "0" : trimDouble(num)); }
        private static String trimDouble(Double d) { String s = Double.toString(d); return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s; }
    }

    public static final class ArrayVar {
        final boolean stringType;
        final int[] dims;
        final int[] stride;
        final int total;
        final double[] nums;
        final String[] strs;

        ArrayVar(boolean stringType, int[] dims) {
            this.stringType = stringType;
            this.dims = dims.clone();
            this.stride = new int[dims.length];
            int tot = 1;
            for (int d : dims) { if (d <= 0) throw new BasicException("DIM: Dimension > 0 nötig"); tot *= d; }
            this.total = tot;
            int s = 1;
            for (int i = dims.length - 1; i >= 0; i--) { stride[i] = s; s *= dims[i]; }
            if (stringType) { this.strs = new String[total]; this.nums = null; }
            else { this.nums = new double[total]; this.strs = null; }
        }

        int indexFrom(int[] idxs) {
            if (idxs.length != dims.length) throw new BasicException("Falsche Anzahl Indizes");
            int flat = 0;
            for (int i = 0; i < idxs.length; i++) {
                int v = idxs[i]; // 1-basiert
                if (v < 1 || v > dims[i]) throw new BasicException("Index ausserhalb (1.." + dims[i] + ")");
                flat += (v - 1) * stride[i];
            }
            return flat;
        }
    }
}
