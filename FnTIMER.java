import java.util.List;
import java.time.LocalTime;

public class FnTIMER implements BuiltinFunction {
    @Override public String name() { return "TIMER"; }
    @Override public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 0) throw new Interpreter.BasicException("TIMER erwartet keine Argumente");
        LocalTime t = LocalTime.now();
        double sec = t.getHour() * 3600 + t.getMinute() * 60 + t.getSecond() + t.getNano() / 1e9;
        return Interpreter.Value.number(sec);
    }
}
