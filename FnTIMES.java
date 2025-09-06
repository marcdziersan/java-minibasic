import java.util.List;
import java.time.LocalTime;

public class FnTIMES implements BuiltinFunction {
    @Override public String name() { return "TIME$"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 0) throw new Interpreter.BasicException("TIME$ erwartet keine Argumente");
        LocalTime t = LocalTime.now();
        String s = String.format("%02d:%02d:%02d", t.getHour(), t.getMinute(), t.getSecond());
        return Interpreter.Value.string(s);
    }
}
