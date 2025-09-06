import java.util.List;

public class FnATN implements BuiltinFunction {
    @Override public String name() { return "ATN"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("ATN erwartet 1 Argument");
        return Interpreter.Value.number(Math.atan(args.get(0).asNumber()));
    }
}
