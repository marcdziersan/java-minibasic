import java.util.List;

public class FnABS implements BuiltinFunction {
    @Override public String name() { return "ABS"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("ABS erwartet 1 Argument");
        return Interpreter.Value.number(Math.abs(args.get(0).asNumber()));
    }
}
