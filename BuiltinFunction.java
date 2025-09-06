import java.util.List;

public interface BuiltinFunction {
    String name(); // Groß geschrieben, z. B. "RND" oder "INT"
    // darf Fehler werfen, wenn Arity/Typen falsch sind
    Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args);
}
