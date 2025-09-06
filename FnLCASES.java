import java.util.List;
import java.util.Locale;

public class FnLCASES implements BuiltinFunction {
    @Override public String name() { return "LCASE$"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("LCASE$ erwartet 1 Argument");
        return Interpreter.Value.string(args.get(0).asString().toLowerCase(Locale.ROOT));
    }
}
