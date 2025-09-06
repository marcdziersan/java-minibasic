import java.util.List;

public class FnCHRS implements BuiltinFunction {
    @Override public String name() { return "CHR$"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() != 1) throw new Interpreter.BasicException("CHR$ erwartet 1 Argument");
        int code = (int)Math.floor(args.get(0).asNumber());
        if (code < 0) code = 0;
        if (code > 255) code = 255; // QBASIC: 0..255
        char ch = (char) code;
        return Interpreter.Value.string(String.valueOf(ch));
    }
}
