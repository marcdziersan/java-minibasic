import java.util.List;

public class FnMIDS implements BuiltinFunction {
    @Override public String name() { return "MID$"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() < 2 || args.size() > 3)
            throw new Interpreter.BasicException("MID$ erwartet 2 oder 3 Argumente");
        String s = args.get(0).asString();
        int start = (int)Math.floor(args.get(1).asNumber()); // 1-basiert
        int len = (args.size() == 3) ? (int)Math.floor(args.get(2).asNumber()) : Integer.MAX_VALUE;

        if (start < 1) start = 1;
        if (len <= 0) return Interpreter.Value.string("");
        if (start > s.length()) return Interpreter.Value.string("");

        int begin = start - 1;
        int end = Math.min(s.length(), begin + len);
        return Interpreter.Value.string(s.substring(begin, end));
    }
}
