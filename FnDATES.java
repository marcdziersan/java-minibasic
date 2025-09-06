import java.util.List;
import java.time.LocalDate;

public class FnDATES implements BuiltinFunction {
    @Override public String name() { return "DATE$"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 0) throw new Interpreter.BasicException("DATE$ erwartet keine Argumente");
        LocalDate d = LocalDate.now();
        String s = String.format("%04d-%02d-%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
        return Interpreter.Value.string(s);
    }
}
