import java.util.List;

public class FnSIN implements BuiltinFunction {
    @Override public String name() { return "SIN"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("SIN erwartet 1 Argument");
        return Interpreter.Value.number(Math.sin(args.get(0).asNumber()));
    }
}
