import java.util.List;

public class FnINT implements BuiltinFunction {
    @Override public String name() { return "INT"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("INT erwartet 1 Argument");
        return Interpreter.Value.number(Math.floor(args.get(0).asNumber()));
    }
}
