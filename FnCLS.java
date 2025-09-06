import java.util.List;

public class FnCLS implements BuiltinFunction {
    @Override public String name() { return "CLS"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 0) throw new Interpreter.BasicException("CLS erwartet keine Argumente");
        // ANSI-Clear; auf vielen Windows-Konsolen ab Win10 aktiv
        System.out.print("\u001b[2J\u001b[H");
        System.out.flush();
        return Interpreter.Value.number(0);
    }
}
