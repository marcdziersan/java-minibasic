import java.util.List;

public class FnSGN implements BuiltinFunction {
    @Override public String name() { return "SGN"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("SGN erwartet 1 Argument");
        double x = args.get(0).asNumber();
        return Interpreter.Value.number(x > 0 ? 1 : (x < 0 ? -1 : 0));
    }
}
