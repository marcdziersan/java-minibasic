import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Locale;

public class MiniBasicUI extends JFrame {
    // Interpreter erwartet eine FunctionRegistry im Konstruktor
    private final Interpreter interp = new Interpreter(FunctionRegistry.createDefault());

    private final JTextArea console = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton btnRun = new JButton("RUN");
    private final JButton btnList = new JButton("LIST");
    private final JButton btnNew = new JButton("NEW");
    private final JButton btnLoad = new JButton("LOAD");
    private final JButton btnSave = new JButton("SAVE");
    private final JButton btnHelp = new JButton("HELP");
    private final JButton btnClear = new JButton("CLEAR CONSOLE");

    private volatile boolean programRunning = false;
    private LineQueueReader programInReader;        // speist INPUT()
    private PrintStream programOut;                 // schreibt in Konsole

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MiniBasicUI ui = new MiniBasicUI();
            ui.setVisible(true);
        });
    }

    public MiniBasicUI() {
        super("MiniBasic – UI-Konsole");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        console.setLineWrap(true);
        console.setWrapStyleWord(false);

        JScrollPane scroll = new JScrollPane(console);
        scroll.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        for (JButton b : new JButton[]{btnRun, btnList, btnNew, btnLoad, btnSave, btnHelp, btnClear}) {
            controls.add(b);
        }

        input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        input.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel south = new JPanel(new BorderLayout());
        south.add(controls, BorderLayout.NORTH);
        south.add(input, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // Output der Programmausführung in die Konsole leiten
        programOut = new PrintStream(new TextAreaOutputStream(console), true, StandardCharsets.UTF_8);

        // Events
        input.addActionListener(e -> onEnter());
        input.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) input.setText("");
            }
        });

        btnRun.addActionListener(e -> issueCommand("RUN"));
        btnList.addActionListener(e -> issueCommand("LIST"));
        btnNew.addActionListener(e -> issueCommand("NEW"));
        btnHelp.addActionListener(e -> issueCommand("HELP"));
        btnClear.addActionListener(e -> { console.setText(""); appendPrompt(); });

        btnLoad.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            int r = fc.showOpenDialog(this);
            if (r == JFileChooser.APPROVE_OPTION) {
                Path p = fc.getSelectedFile().toPath();
                issueCommand("LOAD " + p.toString());
            }
        });
        btnSave.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            int r = fc.showSaveDialog(this);
            if (r == JFileChooser.APPROVE_OPTION) {
                Path p = fc.getSelectedFile().toPath();
                issueCommand("SAVE " + p.toString());
            }
        });

        // Startbanner + Prompt
        println("MiniBasic UI – HELP für Hilfe, EXIT beendet.");
        appendPrompt();
    }

    /* ===== Eingabezeile verarbeitet entweder REPL oder INPUT der Programmausführung ===== */
    private void onEnter() {
        String line = input.getText();
        input.setText("");

        if (programRunning) {
            // Eingabe an das laufende Programm (INPUT) durchreichen:
            println(line);
            if (programInReader != null) programInReader.offerLine(line);
            return;
        }

        // REPL-Befehle/Zeilen
        if (line == null) return;
        line = line.strip();
        if (line.isEmpty()) { appendPrompt(); return; }

        println(line);
        handleReplLine(line);
        if (!programRunning) appendPrompt();
    }

    private void issueCommand(String cmd) {
        input.setText(cmd);
        onEnter();
    }

    /* ===== REPL-Logik (wie deine bisherige, nur ohne System.in/out) ===== */
    private void handleReplLine(String line) {
        try {
            if (!startsWithNumber(line)) {
                String u = line.toUpperCase(Locale.ROOT);
                String head = u.split("\\s+")[0];
                switch (head) {
                    case "RUN" -> startRun();
                    case "LIST" -> interp.list(programOut);
                    case "NEW" -> { interp.newProgram(); println("OK (neu)"); }
                    case "SAVE" -> {
                        String fn = (line.length() > 4) ? line.substring(5).trim() : "";
                        if (fn.isEmpty()) { println("! SAVE <datei>"); break; }
                        try { interp.save(Paths.get(fn)); println("Gespeichert in " + fn); }
                        catch (IOException ex) { println("! Konnte nicht speichern: " + ex.getMessage()); }
                    }
                    case "LOAD" -> {
                        String fn = (line.length() > 4) ? line.substring(5).trim() : "";
                        if (fn.isEmpty()) { println("! LOAD <datei>"); break; }
                        try { interp.load(Paths.get(fn)); println("Geladen von " + fn); }
                        catch (IOException ex) { println("! Konnte nicht laden: " + ex.getMessage()); }
                    }
                    case "HELP" -> printHelp();
                    case "EXIT", "BYE", "QUIT" -> System.exit(0);
                    default -> println("Unbekannter Befehl. Tippe HELP.");
                }
                return;
            }

            // Zeile mit Nummer: hinzufügen/ersetzen oder löschen
            int sp = line.indexOf(' ');
            int num; String rest = "";
            if (sp < 0) num = Integer.parseInt(line);
            else { num = Integer.parseInt(line.substring(0, sp)); rest = line.substring(sp + 1).strip(); }

            if (rest.isEmpty()) {
                interp.removeLine(num);
                println("Zeile " + num + " gelöscht.");
            } else {
                interp.addOrReplaceLine(num, rest);
                println("Zeile " + num + " gespeichert.");
            }
        } catch (NumberFormatException nfe) {
            println("! Ungültige Zeilennummer.");
        } catch (Interpreter.BasicException be) {
            println("! Fehler: " + be.getMessage());
        } catch (Exception ex) {
            println("! Unerwarteter Fehler: " + ex.getMessage());
        }
    }

    private boolean startsWithNumber(String s) {
        if (s.isEmpty()) return false;
        char c = s.charAt(0);
        return c >= '0' && c <= '9';
    }

    /* ===== Programm ausführen (in Worker-Thread), INPUT via Queue, OUTPUT in TextArea ===== */
    private void startRun() {
        if (programRunning) { println("! Läuft bereits."); return; }

        programRunning = true;
        programInReader = new LineQueueReader();

        Thread t = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(programInReader);
                interp.run(in, programOut);
            } catch (Interpreter.BasicException ex) {
                println("! Fehler: " + ex.getMessage());
            } catch (Throwable th) {
                println("! Laufzeitfehler: " + th.getMessage());
            } finally {
                programRunning = false;
                println(""); // optische Trennung
                appendPrompt();
            }
        }, "MiniBasic-Run");
        t.setDaemon(true);
        t.start();
    }

    /* ===== Hilfe-Text (synchron mit deiner FunctionRegistry) ===== */
    private void printHelp() {
        println("""
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
                  • In Ausdrücken erlaubt UND als eigenes Statement (Rückgabewert wird verworfen),
                    z. B.: CLS(), BEEP(), PAUSE(0.5).

                Eingebaute Funktionen

                  Zufall:
                    RND(), RND(n), RND(a,b)

                  Mathe:
                    INT(x), ABS(x), SGN(x), SQR(x), SIN(x), COS(x), TAN(x), ATN(x), EXP(x), LOG(x), POW(a,b), MOD(a,b)

                  Strings/Zeichen:
                    STR$(x), VAL(s), LEFT$(s,n), RIGHT$(s,n), MID$(s,start[,len]),
                    INSTR(s, sub) / INSTR(start, s, sub), CHR$(code), ASC(s)

                  Konsole/System:
                    CLS(), PAUSE(sek), BEEP()

                  Zeit/Datum:
                    TIME$(), DATE$(), TIMER()

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

    /* ===== Helpers ===== */
    private void appendPrompt() { print("OK> "); }
    private void println(String s) { print(s + "\n"); }
    private void print(String s) {
        SwingUtilities.invokeLater(() -> {
            console.append(s);
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    /** OutputStream, der in die JTextArea schreibt. */
    static class TextAreaOutputStream extends OutputStream {
        private final JTextArea area;
        TextAreaOutputStream(JTextArea area) { this.area = area; }
        @Override public void write(int b) { append(new byte[]{(byte)b}, 0, 1); }
        @Override public void write(byte[] b, int off, int len) {
            append(b, off, len);
        }
        private void append(byte[] b, int off, int len) {
            String s = new String(b, off, len, StandardCharsets.UTF_8);
            SwingUtilities.invokeLater(() -> {
                area.append(s);
                area.setCaretPosition(area.getDocument().getLength());
            });
        }
    }

    /** Reader, der zeilenweise String-Eingaben aus der UI entgegennimmt (für INPUT). */
    static class LineQueueReader extends Reader {
        private final BlockingQueue<String> q = new LinkedBlockingQueue<>();
        private String cur = null;
        private int pos = 0;

        /** Eine komplette Zeile (ohne Zeilenumbruch) anbieten. */
        public void offerLine(String line) {
            // BufferedReader.readLine() benötigt einen Trenner – wir fügen \n an.
            q.offer(line + "\n");
        }

        @Override public int read(char[] cbuf, int off, int len) {
            try {
                if (cur == null || pos >= cur.length()) {
                    cur = q.take(); // blockiert, bis eine Zeile kommt
                    pos = 0;
                }
                int n = Math.min(len, cur.length() - pos);
                cur.getChars(pos, pos + n, cbuf, off);
                pos += n;
                return n;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }

        @Override public void close() { /* nichts */ }
    }
}
