import java.util.List;

public class FnVAL implements BuiltinFunction {
    @Override public String name() { return "VAL"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("VAL erwartet 1 Argument");
        String s = args.get(0).asString().trim();
        double d;
        try { d = Double.parseDouble(s); }
        catch (Exception e) { d = 0.0; } // BASIC-typisch: nicht parsebar -> 0
        return Interpreter.Value.number(d);
    }
}
