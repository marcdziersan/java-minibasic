import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class MiniBasic {

    public static void main(String[] args) throws Exception {
        // Interpreter mit Default-Funktions-Registry
        Interpreter interpreter = new Interpreter(FunctionRegistry.createDefault());
        new REPL(interpreter).start();
    }

    /* ============================ REPL / SHELL ============================ */

    static final class REPL {
        private final Interpreter interp;
        private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        private final PrintStream out = System.out;

        REPL(Interpreter interp) { this.interp = interp; }

        void start() throws IOException {
            out.println("MiniBasic (modular) – HELP für Hilfe, EXIT beendet.");
            while (true) {
                out.print("OK> ");
                String line = in.readLine();
                if (line == null) break;
                line = line.strip();
                if (line.isEmpty()) continue;

                if (!startsWithNumber(line)) {
                    String u = line.toUpperCase(Locale.ROOT);
                    switch (u.split("\\s+")[0]) {
                        case "RUN" -> {
                            try { interp.run(in, out); }
                            catch (Interpreter.BasicException ex) { out.println("! Fehler: " + ex.getMessage()); }
                        }
                        case "LIST" -> interp.list(out);
                        case "NEW"  -> { interp.newProgram(); out.println("OK (neu)"); }
                        case "SAVE" -> {
                            String fn = safeArg(line);
                            try { interp.save(Paths.get(fn)); out.println("Gespeichert in " + fn); }
                            catch (IOException ex) { out.println("! Konnte nicht speichern: " + ex.getMessage()); }
                        }
                        case "LOAD" -> {
                            String fn = safeArg(line);
                            try { interp.load(Paths.get(fn)); out.println("Geladen von " + fn); }
                            catch (IOException ex) { out.println("! Konnte nicht laden: " + ex.getMessage()); }
                        }
                        case "HELP" -> printHelp();
                        case "EXIT", "BYE", "QUIT" -> { out.println("Tschüss!"); return; }
                        default -> out.println("Unbekannter Befehl. Tippe HELP.");
                    }
                    continue;
                }

                // Programmlinie
                int sp = line.indexOf(' ');
                int num; String rest = "";
                try {
                    if (sp < 0) num = Integer.parseInt(line);
                    else { num = Integer.parseInt(line.substring(0, sp)); rest = line.substring(sp + 1).strip(); }
                } catch (NumberFormatException nfe) { out.println("! Ungültige Zeilennummer."); continue; }

                if (rest.isEmpty()) {
                    interp.removeLine(num);
                    out.println("Zeile " + num + " gelöscht.");
                } else {
                    interp.addOrReplaceLine(num, rest);
                    out.println("Zeile " + num + " gespeichert.");
                }
            }
        }

        private String safeArg(String line) {
            int sp = line.indexOf(' ');
            return (sp < 0) ? "" : line.substring(sp + 1).trim();
        }

        private boolean startsWithNumber(String s) {
            if (s.isEmpty()) return false;
            char c = s.charAt(0);
            return c >= '0' && c <= '9';
        }

private void printHelp() {
    out.println("""
            Befehle (ohne Zeilennummer):
              RUN, LIST, NEW, SAVE <file>, LOAD <file>, HELP, EXIT

            BASIC-Sprache:
              REM <text>
              PRINT <expr> [;|, <expr> ...]       (',' fügt ein Leerzeichen ein)
              INPUT X | INPUT NAME$
              LET X = <expr>                      (LET optional)
              X = <expr>                          (Zuweisung ohne LET)
              IF <cond> THEN <zeile|statement>
              GOTO <zeile>
              GOSUB <zeile> / RETURN
              FOR I = <start> TO <end> [STEP <s>] / NEXT I
              DIM A(10), B$(3,3)
              RANDOMIZE [<seed>]
              END / STOP
              (Mehrere Statements pro Zeile mit ':'.
               Hinweis: FOR/NEXT sowie GOSUB/RETURN zur Sicherheit nicht in der gleichen Zeile mit ':' kombinieren.)

            Ausdrücke:
              Zahlen, Strings "so", Variablen: A, X1, NAME$, Arrays: A(1), B$(2,3)
              Operatoren: +  -  *  /, Klammern ( )
              Unäres Vorzeichen: -X, +X
              Vergleiche in IF: =  <>  <  <=  >  >=
              Wahrheitswerte: Zahl ≠ 0 bzw. String ≠ "" gilt als wahr
              String-Verkettung: +

            Funktionsaufrufe:
              • Funktionsaufrufe sind in Ausdrücken erlaubt UND können als eigenes Statement stehen
                (Rückgabewert wird dabei verworfen), z. B.: CLS(), BEEP(), PAUSE(0.5).

            Eingebaute Funktionen

              Zufall:
                RND()              → reelle Zahl [0..1)
                RND(n)             → ganze Zahl 1..n        (n>0)
                RND(a,b)           → ganze Zahl min..max     (a,b)

              Mathe:
                INT(x)             → abrunden (floor)
                ABS(x)             → Betrag
                SGN(x)             → Vorzeichen (-1,0,1)
                SQR(x)             → Wurzel (x>=0)
                SIN(x), COS(x), TAN(x)    (x in Bogenmaß)
                ATN(x)             → Arctan (Bogenmaß)
                EXP(x)             → e^x
                LOG(x)             → natürlicher Logarithmus (x>0)
                POW(a,b)           → a^b
                MOD(a,b)           → mathematisches Modulo (b≠0)

              Strings/Zeichen:
                STR$(x)            → Zahl als String
                VAL(s)             → String zu Zahl (nicht parsebar → 0)
                LEFT$(s,n)         → n Zeichen von links
                RIGHT$(s,n)        → n Zeichen von rechts
                MID$(s,start[,len])→ Teilstring ab 1-basiertem start, optional Länge
                INSTR(s, sub)      → 1-basierte Position von sub in s, 0 wenn nicht gefunden
                INSTR(start, s, sub)→ Suche ab 1-basiertem start
                CHR$(code)         → Zeichen (0..255)
                ASC(s)             → Code des 1. Zeichens (Fehler bei leerem String)

              Konsole/System:
                CLS()              → Bildschirm löschen (ANSI; ggf. nur sichtbares Scrollen)
                PAUSE(sek)         → warten in Sekunden (z. B. 0.2)
                BEEP()             → Terminal-Bell

              Zeit/Datum:
                TIME$()            → "HH:MM:SS"
                DATE$()            → "YYYY-MM-DD"
                TIMER()            → Sekunden seit Mitternacht (reell)

            Arrays:
              • Vorher mit DIM anlegen, Indizes sind 1-basiert: 1..N
              • Mehrdimensional möglich: DIM A(3,3), Zugriff: A(2,3)

            Beispiele:
              10 A=POW(2,10) : PRINT "A=";A
              20 PRINT LEFT$("HELLO",2), MID$("HELLO",2,3), RIGHT$("HELLO",3)
              30 IF INSTR("ABCDEF","CD") THEN PRINT "gefunden"
              40 CLS() : BEEP() : PAUSE(0.3) : PRINT TIME$(), DATE$()
            """);
}

    }
}
