import java.util.List;

public class FnSTRS implements BuiltinFunction {
    @Override public String name() { return "STR$"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("STR$ erwartet 1 Argument");
        return Interpreter.Value.string(args.get(0).asString());
    }
}
