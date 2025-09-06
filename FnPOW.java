import java.util.List;

public class FnPOW implements BuiltinFunction {
    @Override public String name() { return "POW"; } // Ersatz für '^' Operator
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 2) throw new Interpreter.BasicException("POW erwartet 2 Argumente");
        return Interpreter.Value.number(Math.pow(args.get(0).asNumber(), args.get(1).asNumber()));
    }
}
