import java.util.*;

public class Parser {
    private final Interpreter rt;
    private final Tokenizer tz;

    public Parser(Interpreter rt, Tokenizer tz) { this.rt = rt; this.tz = tz; }

    /* ===== Mehrere Statements pro Zeile via ':' ===== */
    public Interpreter.Statement parseStatement() {
        Interpreter.Statement st = parseSingleStatement();
        while (tz.hasMore() && ":".equals(tz.peek())) {
            tz.next(); // ':'
            Interpreter.Statement next = parseSingleStatement();
            Interpreter.Statement prev = st;
            st = c -> {
                prev.execute(c);
                if (c.stop || c.jumpToLine != null) return; // Sprünge/END stoppen Rest der Zeile
                next.execute(c);
            };
        }
        return st;
    }

    /* ===== Ein einzelnes Statement ===== */
    private Interpreter.Statement parseSingleStatement() {
        if (!tz.hasMore()) return c -> {};
        String t = tz.peekUpper();
        switch (t) {
            case "REM" -> { tz.next(); while (tz.hasMore()) tz.next(); return c -> {}; }
            case "PRINT" -> { tz.next(); return parsePrint(); }
            case "LET" -> { tz.next(); return parseAssignment(); }
            case "INPUT" -> { tz.next(); return parseInput(); }
            case "IF" -> { tz.next(); return parseIfThen(); }
            case "GOTO" -> { tz.next(); return parseGoto(); }
            case "GOSUB" -> { tz.next(); return parseGosub(); }
            case "RETURN" -> { tz.next(); return parseReturn(); }
            case "FOR" -> { tz.next(); return parseFor(); }
            case "NEXT" -> { tz.next(); return parseNext(); }
            case "DIM" -> { tz.next(); return parseDim(); }
            case "RANDOMIZE" -> { tz.next(); return parseRandomize(); }
            case "END", "STOP" -> { tz.next(); return c -> c.stop = true; }
            default -> {
                if (isIdentifier(t)) {
                    // Funktionsaufruf ALS STATEMENT? -> Name gefolgt von '(' und in Registry vorhanden
                    String nameUpper = tz.next().toUpperCase(Locale.ROOT); // Name konsumieren
                    if (rt.functions().has(nameUpper) && tz.hasMore() && "(".equals(tz.peek())) {
                        tz.expect("(");
                        List<Interpreter.Expr> args = new ArrayList<>();
                        if (!tz.peek().equals(")")) {
                            args.add(parseExpr());
                            while (tz.hasMore() && tz.peek().equals(",")) { tz.next(); args.add(parseExpr()); }
                        }
                        tz.expect(")");
                        final String fname = nameUpper;
                        final List<Interpreter.Expr> fargs = args;
                        return c -> {
                            List<Interpreter.Value> values = new ArrayList<>(fargs.size());
                            for (Interpreter.Expr e : fargs) values.add(e.eval(c.rt));
                            // Rückgabewert verwerfen (Statement)
                            c.rt.functions().call(fname, c.rt, values);
                        };
                    }
                    // sonst: Zuweisung (wir haben den Namen bereits konsumiert)
                    return parseAssignmentFromConsumedName(nameUpper);
                }
                throw new Interpreter.BasicException("Unbekanntes Schlüsselwort: " + t);
            }
        }
    }

    private Interpreter.Statement parsePrint() {
        List<Interpreter.Expr> parts = new ArrayList<>();
        List<Character> seps = new ArrayList<>();
        parts.add(parseExpr());
        while (tz.hasMore() && (tz.peek().equals(";") || tz.peek().equals(","))) {
            char sep = tz.next().charAt(0);
            seps.add(sep);
            parts.add(parseExpr());
        }
        return c -> {
            for (int i = 0; i < parts.size(); i++) {
                Interpreter.Value v = parts.get(i).eval(c.rt);
                c.out.print(v.asString());
                if (i < seps.size()) if (seps.get(i) == ',') c.out.print(" ");
            }
            c.out.println();
        };
    }

    /* ----- Assignment: Var oder Array-LValue ----- */
    private Interpreter.Statement parseAssignment() {
        LValue lv = parseLValue();
        tz.expect("=");
        Interpreter.Expr rhs = parseExpr();
        return c -> lv.assign(c.rt, rhs.eval(c.rt));
    }

    // wie parseAssignment(), aber der erste Identifier wurde bereits konsumiert
    private Interpreter.Statement parseAssignmentFromConsumedName(String firstNameUpper) {
        LValue lv;
        if (tz.hasMore() && tz.peek().equals("(")) {
            tz.next(); // (
            List<Interpreter.Expr> idxExprs = new ArrayList<>();
            idxExprs.add(parseExpr());
            while (tz.hasMore() && tz.peek().equals(",")) { tz.next(); idxExprs.add(parseExpr()); }
            tz.expect(")");
            lv = new ArrayLValue(firstNameUpper, idxExprs);
        } else {
            lv = new VarLValue(firstNameUpper);
        }
        tz.expect("=");
        Interpreter.Expr rhs = parseExpr();
        return c -> lv.assign(c.rt, rhs.eval(c.rt));
    }

    private LValue parseLValue() {
        String name = tz.expectIdentifier();
        String uname = name.toUpperCase(Locale.ROOT);
        if (tz.hasMore() && tz.peek().equals("(")) {
            tz.next(); // (
            List<Interpreter.Expr> idxExprs = new ArrayList<>();
            idxExprs.add(parseExpr());
            while (tz.hasMore() && tz.peek().equals(",")) { tz.next(); idxExprs.add(parseExpr()); }
            tz.expect(")");
            return new ArrayLValue(uname, idxExprs);
        } else {
            return new VarLValue(uname);
        }
    }

    private Interpreter.Statement parseInput() {
        String var = tz.expectIdentifier();
        boolean stringVar = var.endsWith("$");
        String uname = var.toUpperCase(Locale.ROOT);
        return c -> {
            try {
                c.out.print("? " + uname + " = ");
                String line = c.in.readLine(); if (line == null) line = "";
                if (stringVar) c.rt.setVar(uname, Interpreter.Value.string(line));
                else {
                    double d;
                    try { d = Double.parseDouble(line.trim()); }
                    catch (NumberFormatException nfe) { throw new Interpreter.BasicException("Eingabe ist keine Zahl."); }
                    c.rt.setVar(uname, Interpreter.Value.number(d));
                }
            } catch (Exception e) { throw new Interpreter.BasicException("E/A-Fehler bei INPUT"); }
        };
    }

    private Interpreter.Statement parseIfThen() {
        Interpreter.Expr cond = parseBoolExpr();
        String kw = tz.peekUpper();
        if (!"THEN".equals(kw)) throw new Interpreter.BasicException("Erwartet THEN");
        tz.next(); // THEN

        if (!tz.hasMore()) throw new Interpreter.BasicException("IF ... THEN ohne Ziel");

        String nextTok = tz.peek();
        if (isNumber(nextTok)) {
            int line = tz.expectInteger();
            return c -> {
                Interpreter.Value v = cond.eval(c.rt);
                boolean truth = v.isString() ? !v.asString().isEmpty() : (v.asNumber() != 0.0);
                if (truth) c.jumpToLine = line;
            };
        }

        // Nur EIN Statement nach THEN (Rest der Zeile bleibt für ':'-Kette übrig)
        Interpreter.Statement inner = parseSingleStatement();
        return c -> {
            Interpreter.Value v = cond.eval(c.rt);
            boolean truth = v.isString() ? !v.asString().isEmpty() : (v.asNumber() != 0.0);
            if (truth) inner.execute(c);
        };
    }

    private Interpreter.Statement parseGoto() { int line = tz.expectInteger(); return c -> c.jumpToLine = line; }

    private Interpreter.Statement parseGosub() {
        int line = tz.expectInteger();
        return c -> {
            Integer ret = c.nextOf(c.currentLine);
            if (ret == null) throw new Interpreter.BasicException("GOSUB von letzter Zeile nicht möglich");
            c.gosubStack.push(ret);
            c.jumpToLine = line;
        };
    }

    private Interpreter.Statement parseReturn() {
        return c -> {
            if (c.gosubStack.isEmpty()) throw new Interpreter.BasicException("RETURN ohne GOSUB");
            c.jumpToLine = c.gosubStack.pop();
        };
    }

    private Interpreter.Statement parseFor() {
        final String var = tz.expectIdentifier().toUpperCase(Locale.ROOT);
        tz.expect("=");
        final Interpreter.Expr start = parseExpr();
        String to = tz.peekUpper(); if (!"TO".equals(to)) throw new Interpreter.BasicException("Erwartet TO");
        tz.next();
        final Interpreter.Expr end = parseExpr();

        Interpreter.Expr stepTmp = null;
        if (tz.hasMore() && tz.peekUpper().equals("STEP")) { tz.next(); stepTmp = parseExpr(); }
        final Interpreter.Expr stepExpr = stepTmp; // effektiv final

        return c -> {
            double s  = start.eval(c.rt).asNumber();
            double e  = end.eval(c.rt).asNumber();
            double st = (stepExpr == null) ? 1.0 : stepExpr.eval(c.rt).asNumber();
            c.rt.setVar(var, Interpreter.Value.number(s));
            Integer after = c.nextOf(c.currentLine);
            if (after == null) throw new Interpreter.BasicException("FOR am Programmenende");
            c.forStack.push(new Interpreter.ForFrame(var, e, st, after));
        };
    }

    private Interpreter.Statement parseNext() {
        String var = tz.expectIdentifier().toUpperCase(Locale.ROOT);
        return c -> {
            if (c.forStack.isEmpty()) throw new Interpreter.BasicException("NEXT ohne FOR");
            Interpreter.ForFrame f = c.forStack.peek();
            if (!f.var.equals(var)) throw new Interpreter.BasicException("NEXT für falsche Variable (erwartet " + f.var + ")");
            double cur = c.rt.getVar(f.var).asNumber();
            cur += f.step;
            c.rt.setVar(f.var, Interpreter.Value.number(cur));
            boolean cont = f.step >= 0 ? (cur <= f.end) : (cur >= f.end);
            if (cont) c.jumpToLine = f.lineAfterFor;
            else c.forStack.pop();
        };
    }

    private Interpreter.Statement parseDim() {
        List<DimDef> defs = new ArrayList<>();
        while (true) {
            String name = tz.expectIdentifier();
            boolean stringType = name.endsWith("$");
            String uname = name.toUpperCase(Locale.ROOT);
            tz.expect("(");
            List<Interpreter.Expr> dims = new ArrayList<>();
            dims.add(parseExpr());
            while (tz.hasMore() && tz.peek().equals(",")) { tz.next(); dims.add(parseExpr()); }
            tz.expect(")");
            defs.add(new DimDef(uname, stringType, dims));
            if (tz.hasMore() && tz.peek().equals(",")) { tz.next(); continue; }
            break;
        }
        return c -> {
            for (DimDef d : defs) {
                int[] di = new int[d.dimExprs.size()];
                for (int i = 0; i < di.length; i++) {
                    int v = (int)Math.floor(d.dimExprs.get(i).eval(c.rt).asNumber());
                    if (v <= 0) throw new Interpreter.BasicException("DIM: Dimension > 0 nötig");
                    di[i] = v;
                }
                c.rt.dimArray(d.name, d.stringType, di);
            }
        };
    }

    private Interpreter.Statement parseRandomize() {
        if (!tz.hasMore()) return c -> c.rt.randomize(null);
        Interpreter.Expr seed = parseExpr();
        return c -> c.rt.randomize((long)seed.eval(c.rt).asNumber());
    }

    /* --- boolischer Ausdruck: Vergleich --- */
    private Interpreter.Expr parseBoolExpr() {
        Interpreter.Expr left = parseExpr();
        if (!tz.hasMore()) return left;
        String op = tz.peek();
        if (isRelop(op)) {
            tz.next();
            Interpreter.Expr right = parseExpr();
            return rt -> {
                Interpreter.Value a = left.eval(rt);
                Interpreter.Value b = right.eval(rt);
                int cmp;
                if (a.isString() || b.isString()) cmp = a.asString().compareTo(b.asString());
                else cmp = Double.compare(a.asNumber(), b.asNumber());
                boolean res = switch (op) {
                    case "=" -> cmp == 0;
                    case "<>" -> cmp != 0;
                    case "<" -> cmp < 0;
                    case "<=" -> cmp <= 0;
                    case ">" -> cmp > 0;
                    case ">=" -> cmp >= 0;
                    default -> throw new Interpreter.BasicException("Unbekannter Vergleich: " + op);
                };
                return Interpreter.Value.number(res ? 1 : 0);
            };
        }
        return left;
    }
    private boolean isRelop(String s) {
        return s.equals("=") || s.equals("<>") || s.equals("<") || s.equals("<=") || s.equals(">") || s.equals(">=");
    }

    /* --- Arithmetischer Ausdruck --- */
    private Interpreter.Expr parseExpr() {
        Interpreter.Expr e = parseTerm();
        while (tz.hasMore() && (tz.peek().equals("+") || tz.peek().equals("-"))) {
            String op = tz.next();
            Interpreter.Expr r = parseTerm();
            e = combine(e, op, r);
        }
        return e;
    }
    private Interpreter.Expr parseTerm() {
        Interpreter.Expr e = parseFactor();
        while (tz.hasMore() && (tz.peek().equals("*") || tz.peek().equals("/"))) {
            String op = tz.next();
            Interpreter.Expr r = parseFactor();
            e = combine(e, op, r);
        }
        return e;
    }
    private Interpreter.Expr parseFactor() {
        if (!tz.hasMore()) throw new Interpreter.BasicException("Unerwartetes Ende im Ausdruck");
        String t = tz.peek();
        if (t.equals("(")) {
            tz.next();
            Interpreter.Expr e = parseBoolExpr();
            tz.expect(")");
            return e;
        }
        if (t.equals("+")) { tz.next(); return parseFactor(); }
        if (t.equals("-")) {
            tz.next();
            Interpreter.Expr f = parseFactor();
            return rt -> {
                Interpreter.Value v = f.eval(rt);
                if (v.isString()) throw new Interpreter.BasicException("Negation von String nicht möglich");
                return Interpreter.Value.number(-v.asNumber());
            };
        }
        if (isNumber(t)) {
            double d = Double.parseDouble(t);
            tz.next();
            return rt -> Interpreter.Value.number(d);
        }
        if (t.startsWith("\"")) {
            String s = tz.next();
            return rt -> Interpreter.Value.string(unquote(s));
        }
        if (isIdentifier(t)) {
            String name = tz.next().toUpperCase(Locale.ROOT);
            // Funktionsaufruf in Ausdrücken
            if (tz.hasMore() && tz.peek().equals("(") && rt.functions().has(name)) {
                tz.expect("(");
                List<Interpreter.Expr> args = new ArrayList<>();
                if (!tz.peek().equals(")")) {
                    args.add(parseExpr());
                    while (tz.hasMore() && tz.peek().equals(",")) { tz.next(); args.add(parseExpr()); }
                }
                tz.expect(")");
                final String fname = name;
                final List<Interpreter.Expr> fargs = args;
                return rti -> {
                    List<Interpreter.Value> values = new ArrayList<>(fargs.size());
                    for (Interpreter.Expr e : fargs) values.add(e.eval(rti));
                    return rt.functions().call(fname, rti, values);
                };
            }
            // Array-Referenz in Ausdrücken
            if (tz.hasMore() && tz.peek().equals("(")) {
                tz.next(); // (
                List<Interpreter.Expr> idx = new ArrayList<>();
                idx.add(parseExpr());
                while (tz.hasMore() && tz.peek().equals(",")) { tz.next(); idx.add(parseExpr()); }
                tz.expect(")");
                final String arr = name;
                final List<Interpreter.Expr> idxList = idx;
                return rti -> {
                    int[] ii = new int[idxList.size()];
                    for (int i = 0; i < ii.length; i++)
                        ii[i] = (int)Math.floor(idxList.get(i).eval(rti).asNumber());
                    return rti.getArray(arr, ii);
                };
            }
            // einfache Variable
            final String uname = name;
            return rti -> rti.getVar(uname);
        }
        throw new Interpreter.BasicException("Unerwartetes Token: " + t);
    }

    private Interpreter.Expr combine(Interpreter.Expr a, String op, Interpreter.Expr b) {
        return rt -> {
            Interpreter.Value va = a.eval(rt);
            Interpreter.Value vb = b.eval(rt);
            switch (op) {
                case "+" -> {
                    if (va.isString() || vb.isString()) return Interpreter.Value.string(va.asString() + vb.asString());
                    return Interpreter.Value.number(va.asNumber() + vb.asNumber());
                }
                case "-" -> {
                    if (va.isString() || vb.isString()) throw new Interpreter.BasicException("String mit '-' nicht erlaubt");
                    return Interpreter.Value.number(va.asNumber() - vb.asNumber());
                }
                case "*" -> {
                    if (va.isString() || vb.isString()) throw new Interpreter.BasicException("String mit '*' nicht erlaubt");
                    return Interpreter.Value.number(va.asNumber() * vb.asNumber());
                }
                case "/" -> {
                    if (va.isString() || vb.isString()) throw new Interpreter.BasicException("String mit '/' nicht erlaubt");
                    double denom = vb.asNumber();
                    if (denom == 0.0) throw new Interpreter.BasicException("Division durch 0");
                    return Interpreter.Value.number(va.asNumber() / denom);
                }
                default -> throw new Interpreter.BasicException("Unbekannter Operator: " + op);
            }
        };
    }

    private boolean isIdentifier(String s) {
        if (s == null || s.isEmpty()) return false;
        char c = s.charAt(0);
        if (!Character.isLetter(c)) return false;
        for (int i = 1; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || ch == '$' || ch == '_')) return false;
        }
        return true;
    }
    private boolean isNumber(String s) {
        int dot = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == 0 && (c == '+' || c == '-')) continue;
            if (c == '.') { dot++; if (dot > 1) return false; continue; }
            if (!Character.isDigit(c)) return false;
        }
        return s.length() > 0;
    }
    private String unquote(String token) {
        if (token.length() >= 2 && token.startsWith("\"") && token.endsWith("\"")) {
            String inner = token.substring(1, token.length() - 1);
            return inner.replace("\"\"", "\"");
        }
        return token;
    }

    /* --- LValues --- */
    interface LValue { void assign(Interpreter rt, Interpreter.Value v); }
    static final class VarLValue implements LValue {
        final String name;
        VarLValue(String name) { this.name = name; }
        public void assign(Interpreter rt, Interpreter.Value v) { rt.setVar(name, v); }
    }
    static final class ArrayLValue implements LValue {
        final String name;
        final List<Interpreter.Expr> idx;
        ArrayLValue(String name, List<Interpreter.Expr> idx) { this.name = name; this.idx = idx; }
        public void assign(Interpreter rt, Interpreter.Value v) {
            int[] ii = new int[idx.size()];
            for (int i = 0; i < ii.length; i++) ii[i] = (int)Math.floor(idx.get(i).eval(rt).asNumber());
            rt.setArray(name, ii, v);
        }
    }

    static final class DimDef {
        final String name; final boolean stringType; final List<Interpreter.Expr> dimExprs;
        DimDef(String name, boolean stringType, List<Interpreter.Expr> dimExprs) {
            this.name = name; this.stringType = stringType; this.dimExprs = dimExprs;
        }
    }
}
