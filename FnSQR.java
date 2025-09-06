import java.util.List;

public class FnSQR implements BuiltinFunction {
    @Override public String name() { return "SQR"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("SQR erwartet 1 Argument");
        double x = args.get(0).asNumber();
        if (x < 0) throw new Interpreter.BasicException("SQR: x>=0 nötig");
        return Interpreter.Value.number(Math.sqrt(x));
    }
}
