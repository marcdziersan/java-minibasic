import java.util.Locale;

public class Tokenizer {
    private final String s;
    private int i = 0;
    private String cached = null;

    public Tokenizer(String s) { this.s = s; }

    public boolean hasMore() { skipWs(); return i < s.length(); }
    public String remaining() { if (!hasMore()) return ""; return s.substring(i); }

    public String peek() { if (cached == null) cached = nextToken(); return cached; }
    public String peekUpper() { return peek().toUpperCase(Locale.ROOT); }
    public String next() { if (cached != null) { String t = cached; cached = null; return t; } return nextToken(); }

    public void expect(String sym) {
        String t = next();
        if (!sym.equals(t)) throw new Interpreter.BasicException("Erwartet '" + sym + "', erhielt '" + t + "'");
    }
    public int expectInteger() {
        String t = next();
        try { return Integer.parseInt(t); }
        catch (NumberFormatException nfe) { throw new Interpreter.BasicException("Erwartete Zahl, erhielt '" + t + "'"); }
    }
    public String expectIdentifier() {
        String t = next().toUpperCase(Locale.ROOT);
        if (!(Character.isLetter(t.charAt(0)))) throw new Interpreter.BasicException("Erwartete Variable, erhielt '" + t + "'");
        return t;
    }

    private void skipWs() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private String nextToken() {
        skipWs();
        if (i >= s.length()) return "";
        char c = s.charAt(i);

        if (c == '"') {
            int start = i++;
            StringBuilder sb = new StringBuilder(); sb.append('"');
            boolean closed = false;
            while (i < s.length()) {
                char ch = s.charAt(i++); sb.append(ch);
                if (ch == '"') {
                    if (i < s.length() && s.charAt(i) == '"') { sb.append('"'); i++; continue; }
                    else { closed = true; break; }
                }
            }
            if (!closed) throw new Interpreter.BasicException("Unbeendetes Stringliteral");
            return sb.toString();
        }

        if (Character.isDigit(c) || (c == '.' && i + 1 < s.length() && Character.isDigit(s.charAt(i + 1)))
                || ((c == '+' || c == '-') && i + 1 < s.length() && Character.isDigit(s.charAt(i + 1)))) {
            int start = i++; boolean dot = (c == '.');
            while (i < s.length()) {
                char ch = s.charAt(i);
                if (Character.isDigit(ch)) { i++; continue; }
                if (ch == '.' && !dot) { dot = true; i++; continue; }
                break;
            }
            return s.substring(start, i);
        }

        if (Character.isLetter(c)) {
            int start = i++; while (i < s.length()) {
                char ch = s.charAt(i);
                if (Character.isLetterOrDigit(ch) || ch == '$' || ch == '_') { i++; continue; }
                break;
            }
            return s.substring(start, i);
        }

        if (i + 1 < s.length()) {
            String two = s.substring(i, i + 2);
            if (two.equals("<>") || two.equals("<=") || two.equals(">=")) { i += 2; return two; }
        }

        if ("+-*/()=,<>&".indexOf(c) >= 0 || c == '>' || c == ';' || c == ':') { i++; return String.valueOf(c); }

        throw new Interpreter.BasicException("Unerwartetes Zeichen: '" + c + "'");
    }
}
