import java.util.List;

public class FnMOD implements BuiltinFunction {
    @Override public String name() { return "MOD"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 2) throw new Interpreter.BasicException("MOD erwartet 2 Argumente");
        double a = args.get(0).asNumber();
        double b = args.get(1).asNumber();
        if (b == 0.0) throw new Interpreter.BasicException("MOD: Division durch 0");
        double r = a - Math.floor(a / b) * b; // mathematisches Modulo
        return Interpreter.Value.number(r);
    }
}
