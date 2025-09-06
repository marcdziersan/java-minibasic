import java.util.List;

public class FnPAUSE implements BuiltinFunction {
    @Override public String name() { return "PAUSE"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("PAUSE erwartet 1 Argument (Sekunden)");
        double seconds = Math.max(0.0, args.get(0).asNumber());
        long ms = (long)Math.round(seconds * 1000.0);
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        return Interpreter.Value.number(0);
    }
}
