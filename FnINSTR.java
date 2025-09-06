import java.util.List;

public class FnINSTR implements BuiltinFunction {
    @Override public String name() { return "INSTR"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() < 2 || args.size() > 3)
            throw new Interpreter.BasicException("INSTR erwartet 2 oder 3 Argumente");
        int start = 1;
        String s, sub;
        if (args.size() == 2) {
            s = args.get(0).asString();
            sub = args.get(1).asString();
        } else {
            start = (int)Math.floor(args.get(0).asNumber());
            s = args.get(1).asString();
            sub = args.get(2).asString();
        }
        if (start < 1) start = 1;
        if (sub.isEmpty()) return Interpreter.Value.number(start <= s.length()+1 ? start : 0);
        int pos = s.indexOf(sub, Math.min(start - 1, Math.max(0, s.length())));
        return Interpreter.Value.number(pos >= 0 ? (pos + 1) : 0);
    }
}
