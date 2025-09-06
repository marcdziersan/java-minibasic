import java.util.List;

public class FnASC implements BuiltinFunction {
    @Override public String name() { return "ASC"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("ASC erwartet 1 Argument");
        String s = args.get(0).asString();
        if (s.isEmpty()) throw new Interpreter.BasicException("ASC: leerer String");
        return Interpreter.Value.number((int)s.charAt(0) & 0xFF);
    }
}
