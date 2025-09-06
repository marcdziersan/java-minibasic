import java.util.List;

public class FnBEEP implements BuiltinFunction {
    @Override public String name() { return "BEEP"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 0) throw new Interpreter.BasicException("BEEP erwartet keine Argumente");
        System.out.print("\u0007"); // Terminal-Bell
        System.out.flush();
        return Interpreter.Value.number(0);
    }
}
