import java.util.List;

public class FnEXP implements BuiltinFunction {
    @Override public String name() { return "EXP"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("EXP erwartet 1 Argument");
        return Interpreter.Value.number(Math.exp(args.get(0).asNumber()));
    }
}
