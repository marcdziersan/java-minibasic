import java.util.*;

public class FunctionRegistry {
    private final Map<String, BuiltinFunction> fns = new HashMap<>();

    public FunctionRegistry register(BuiltinFunction fn) {
        fns.put(fn.name().toUpperCase(Locale.ROOT), fn);
        return this;
    }

    public boolean has(String name) { return fns.containsKey(name.toUpperCase(Locale.ROOT)); }

    public Interpreter.Value call(String name, Interpreter rt, List<Interpreter.Value> args) {
        BuiltinFunction fn = fns.get(name.toUpperCase(Locale.ROOT));
        if (fn == null) throw new Interpreter.BasicException("Unbekannte Funktion: " + name);
        return fn.call(rt, args);
    }

public static FunctionRegistry createDefault() {
    return new FunctionRegistry()
            .register(new FnRND())
            .register(new FnINT())
            .register(new FnABS())
            .register(new FnMOD())
            .register(new FnSTRS())
            .register(new FnVAL())
            .register(new FnLEFTS())
            .register(new FnRIGHTS())
            .register(new FnMIDS())
            .register(new FnCLS())
            .register(new FnPAUSE())
            .register(new FnBEEP())
            .register(new FnSGN())
            .register(new FnSQR())
            .register(new FnSIN())
            .register(new FnCOS())
            .register(new FnTAN())
            .register(new FnATN())
            .register(new FnEXP())
            .register(new FnLOG())
            .register(new FnPOW())
            .register(new FnINSTR())
            .register(new FnCHRS())
            .register(new FnASC())
            .register(new FnTIMES())
            .register(new FnDATES())
            .register(new FnTIMER());
}
}
