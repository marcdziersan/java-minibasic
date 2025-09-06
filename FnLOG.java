import java.util.List;

public class FnLOG implements BuiltinFunction {
    @Override public String name() { return "LOG"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("LOG erwartet 1 Argument");
        double x = args.get(0).asNumber();
        if (x <= 0) throw new Interpreter.BasicException("LOG: x>0 nötig");
        return Interpreter.Value.number(Math.log(x)); // natürlicher Logarithmus
    }
}
