import java.util.List;

public class FnRIGHTS implements BuiltinFunction {
    @Override public String name() { return "RIGHT$"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 2) throw new Interpreter.BasicException("RIGHT$ erwartet 2 Argumente");
        String s = args.get(0).asString();
        int n = (int)Math.floor(args.get(1).asNumber());
        if (n <= 0) return Interpreter.Value.string("");
        if (n >= s.length()) return Interpreter.Value.string(s);
        return Interpreter.Value.string(s.substring(s.length() - n));
    }
}
