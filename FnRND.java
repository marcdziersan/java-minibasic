import java.util.List;

public class FnRND implements BuiltinFunction {
    @Override public String name() { return "RND"; }

    @Override
    public Interpreter.Value call(Interpreter rt, List<Interpreter.Value> args) {
        if (args.size() == 0) {
            return Interpreter.Value.number(rt.rng().nextDouble());
        } else if (args.size() == 1) {
            int n = (int)Math.floor(args.get(0).asNumber());
            if (n <= 0) throw new Interpreter.BasicException("RND(n): n>0 nötig");
            int x = 1 + rt.rng().nextInt(n);
            return Interpreter.Value.number(x);
        } else if (args.size() == 2) {
            int a = (int)Math.floor(args.get(0).asNumber());
            int b = (int)Math.floor(args.get(1).asNumber());
            int lo = Math.min(a, b), hi = Math.max(a, b);
            int x = lo + rt.rng().nextInt(hi - lo + 1);
            return Interpreter.Value.number(x);
        }
        throw new Interpreter.BasicException("RND erwartet 0, 1 oder 2 Argumente");
    }
}
