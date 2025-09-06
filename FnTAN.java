import java.util.List;

public class FnTAN implements BuiltinFunction {
    @Override public String name() { return "TAN"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("TAN erwartet 1 Argument");
        return Interpreter.Value.number(Math.tan(args.get(0).asNumber()));
    }
}
