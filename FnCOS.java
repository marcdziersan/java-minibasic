import java.util.List;

public class FnCOS implements BuiltinFunction {
    @Override public String name() { return "COS"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("COS erwartet 1 Argument");
        return Interpreter.Value.number(Math.cos(args.get(0).asNumber()));
    }
}
