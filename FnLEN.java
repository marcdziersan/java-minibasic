import java.util.List;

public class FnLEN implements BuiltinFunction {
    @Override public String name() { return "LEN"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("LEN erwartet 1 Argument");
        return Interpreter.Value.number(args.get(0).asString().length());
    }
}
