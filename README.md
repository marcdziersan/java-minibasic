# MiniBasic (Java) & MiniBasicUI

Ein kleiner, lehrreicher BASIC-Interpreter in **Java 17+** – mit
*CLI/REPL* (**MiniBasic.java**) und optionaler *Swing-Oberfläche* (**MiniBasicUI.java**).
Ideal für das Schreiben und Ausführen kleiner, zeilennummerierter BASIC-Programme
wie `10 PRINT "HELLO"`.

> **Status:** stabiler, bewusst kleiner Kern. Perfekt zum Lernen/Erweitern  
> **Lizenz:** MIT

---

## Inhalt

- [Features](#features)
- [Schnellstart](#schnellstart)
- [Kompilieren & Ausführen](#kompilieren--ausführen)
  - [CLI (MiniBasic)](#cli-minibasic)
  - [GUI (MiniBasicUI)](#gui-minibasicui)
- [REPL/Editor-Befehle](#repleditor-befehle)
- [BASIC-Sprachreferenz](#basic-sprachreferenz)
- [Beispiele](#beispiele)
- [Dateiformat & Speichern/Laden](#dateiformat--speichernladen)
- [Architektur & Erweiterbarkeit](#architektur--erweiterbarkeit)
- [Roadmap](#roadmap)
- [Troubleshooting](#troubleshooting)
- [FAQ](#faq)
- [Mitwirken](#mitwirken)
- [Lizenz](#lizenz)

---

## Features

**Interpreter (CLI & UI)**
- Zeilennummerierte Programme à la 8-Bit-BASIC
- Befehle: `PRINT`, `LET` (optional), `INPUT`, `IF … THEN <Zeile>`, `GOTO`, `REM`, `END`/`STOP`
- Variablen:
  - numerisch: `A`, `X1`, `COUNT` …
  - Strings: `NAME$` (Suffix `$`)
- Ausdrücke: `+ - * /`, Klammern, Vergleiche `= <> < <= > >=`
- Strings in `"` mit `""` als Escaping für Anführungszeichen
- REPL/Editor: `RUN`, `LIST`, `NEW`, `SAVE <file>`, `LOAD <file>`, `HELP`, `EXIT`

**MiniBasicUI (Swing)**
- Editor mit Monospace-Eingabe, RUN/STOP, LIST, NEW, LOAD/SAVE Buttons
- Konsolen-Panel (Ausgabe) + Eingabe-Prompt für `INPUT`
- Dark-Mode-Look & klare Typo (reines Swing/Nimbus, keine Fremdlibs)
- Einzellayout – leicht zu verstehen/erweitern

---

## Schnellstart

### Hello World (im laufenden Programm)

```
10 PRINT "HELLO WORLD"
20 END
RUN
```

### Mini-Dialog

```
10 PRINT "Wie heißt du?"
20 INPUT NAME$
30 PRINT "Hallo, "; NAME$
40 END
RUN
```

---

## Kompilieren & Ausführen

> Voraussetzungen: **Java 17+** (JDK). Keine externen Abhängigkeiten.

### CLI (MiniBasic)

```bash
javac MiniBasic.java
java MiniBasic
```

Prompt: `OK>` – hier BASIC-Zeilen oder REPL-Befehle eingeben.

### GUI (MiniBasicUI)

```bash
javac MiniBasicUI.java
java MiniBasicUI
```

- Linkes Feld: Programmtext (eine BASIC-Zeile pro Zeile, inkl. Zeilennummer).
- Unten: Ausgabekonsole; bei `INPUT` erscheint eine Eingabezeile.
- Buttons: **Run**, **Stop**, **List**, **New**, **Load**, **Save**.

> Hinweis: Wenn du gerade nur **MiniBasic.java** im Repo hast, kannst du die UI später ergänzen – diese README ist bereits dafür vorbereitet.

---

## REPL/Editor-Befehle

Geben Sie diese **ohne** Zeilennummer ein:

- `RUN` – Programm ausführen  
- `LIST` – Programm anzeigen  
- `NEW` – Programm löschen (Variablen werden ebenfalls gelöscht)  
- `SAVE <datei>` – Programm in Textdatei speichern  
- `LOAD <datei>` – Programm aus Datei laden (überschreibt geladenen Quelltext)  
- `HELP` – Kurzhilfe  
- `EXIT` – Interpreter beenden

**Programmzeilen verwalten**

- `10 PRINT "HI"` – legt/ersetzt Zeile 10  
- `10` – löscht Zeile 10 (leere Zeile nur mit Nummer)

---

## BASIC-Sprachreferenz

**Kommentare**
- `REM <Text>` – Kommentar bis Zeilenende

**Ausgabe**
- `PRINT <expr> [; <expr> | , <expr>]...`
  - `;` hängt direkt an, `,` fügt eine Leerstelle ein

**Zuweisung**
- `LET X = <expr>` oder schlicht `X = <expr>`
- String-Variablen enden mit `$`, z. B. `NAME$`

**Eingabe**
- `INPUT X` (Zahl) oder `INPUT NAME$` (Text)

**Verzweigung & Sprünge**
- `IF <expr_relational> THEN <zeile>`  
  Wahr = ungleich 0 bzw. nicht-leerer String
- `GOTO <zeile>`
- `END` / `STOP` – Programm beenden

**Ausdrücke**
- Arithmetik: `+ - * /` mit üblicher Priorität (`*`/`/` vor `+`/`-`)
- Klammern `(` `)`  
- Vergleich: `=  <>  <  <=  >  >=`
- String-Verkettung mit `+` **oder** per `PRINT`-Separatoren (`;`/`,`)  

**Strings**
- In `"` – Anführungszeichen im String: `""` → `"`

---

## Beispiele

**1) Zähler/Schleife (ohne FOR/NEXT)**
```
10 I = 5
20 PRINT "I="; I
30 I = I - 1
40 IF I >= 0 THEN 20
50 END
```

**2) Einfache Eingabeprüfung**
```
10 INPUT N
20 IF N = 0 THEN 60
30 PRINT "Nicht Null"
40 GOTO 10
60 PRINT "Fertig."
70 END
```

**3) String-Verarbeitung**
```
10 INPUT NAME$
20 PRINT "Hallo, " + NAME$ + "!"
30 END
```

---

## Dateiformat & Speichern/Laden

- Reiner Text, **eine BASIC-Zeile pro Zeile**, beginnend mit der Zeilennummer.  
  Beispiel:
  ```
  10 PRINT "HELLO"
  20 END
  ```
- Empfohlene Endung: `.bas` (optional).
- Befehle: `SAVE datei.bas`, `LOAD datei.bas`.

---

## Architektur & Erweiterbarkeit

**Datei `MiniBasic.java`**
- **Tokenizer** → **Parser** → **AST (Statements/Exprs)** → **Interpreter/Context**
- Variablenspeicher: `Map<String, Value>` (`Value` ist Zahl *oder* String)
- Ausführung in Quellzeilen-Reihenfolge; Sprungtabelle für `GOTO`/`IF … THEN`

**Datei `MiniBasicUI.java` (Frontend)**
- Schlanker Swing-Editor (JTextArea), Konsolenpanel (JTextArea), Buttons
- UI ruft denselben Interpreter an, leitet `INPUT` an ein Eingabefeld weiter
- Keine externen Bibliotheken (Nimbus LAF, dunkles Farbschema)

**Typische Erweiterungspunkte**
- **Neue Befehle**: Im Parser Schlüsselwort verzweigen (`CASE "FOR"` …)
- **Funktionen**: Ausdrucksknoten (z. B. `RND()`, `ABS()`, `LEN()`), Tokenizer um Klammern/Kommas erweitern
- **Steuerstrukturen**: `FOR/NEXT`, `GOSUB/RETURN`, `ON … GOTO`
- **Datensektion**: `DATA`/`READ`/`RESTORE`
- **Arrays**: `DIM A(10)` – erfordert neuen `Value`-Typ (Array) und Indizierung
- **Datei-I/O** (vorsichtig): `OPEN`, `INPUT#`, `PRINT#`, `CLOSE`

---

## Roadmap

- [ ] `FOR/NEXT`
- [ ] `GOSUB/RETURN`
- [ ] `RND()`, `ABS()`, `INT()`, `LEN()`, `LEFT$/RIGHT$/MID$`
- [ ] Arrays (`DIM`) & einfache Matrizen
- [ ] `DATA/READ/RESTORE`
- [ ] Optionale `CLEAR`-Funktion (Variablen zurücksetzen ohne `NEW`)
- [ ] Breakpoints/Step-Modus im UI

---

## Troubleshooting

- **„Unerwartetes Zeichen/Token …“**  
  Tippfehler? Nicht geschlossene Anführungszeichen?  
  Strings mit `"` beginnen und enden, `""` dient als Escaping.

- **„Division durch 0“**  
  Nenner prüfen (z. B. vor Division testen).

- **„Sprung in nicht existierende Zeile …“**  
  Zeilennummer in `IF … THEN`/`GOTO` existiert nicht oder wurde gelöscht.

- **Keine Ausgabe/BLOCKIERUNG bei `INPUT`**  
  In der CLI erscheint `? VAR = ` – Eingabe ohne Anführungszeichen für Zahlen,
  mit beliebigem Text für `$`-Variablen.

- **Variablen behalten alte Werte nach RUN**  
  Gewollt – sie bleiben erhalten, bis `NEW`/`LOAD` ausgeführt wird.
  (Eine künftige `CLEAR`-Funktion kann das ändern.)

---

## FAQ

**Warum zeilennummerierte Programme?**  
Weil es das klassische Tiny-BASIC-Gefühl vermittelt und Sprünge trivial macht.

**Gibt es Logik-Operatoren (AND/OR/NOT)?**  
Noch nicht – aktuell nur Vergleiche. In der Praxis reicht oft Arithmetik + Vergleich.

**Wie konkateniere ich Strings?**  
Entweder mit `+` oder im `PRINT` per `;`/`,`:
```
PRINT "Hallo, "; NAME$
```

**Kann ich mehrere Statements in einer Zeile haben?**  
Nein, bewusst **ein** Statement pro Zeile – einfach, lehrreich, robust.

---

## Mitwirken

- **Issues** eröffnen für Bugs/Feature-Wünsche
- **PRs** willkommen – bitte klein schneiden (Parser/Funktionen getrennt)
- Code-Stil: klar, wenige Abkürzungen, deutsche Fehlermeldungen, englische Keywords

---

## Lizenz

**MIT License**

---

### Anhang: Mini-Cheatsheet

```
REM Kommentar
PRINT "Text", X, NAME$      ' , fügt Leerstelle ein; ; ohne Abstand
LET X = 42                  ' LET optional
INPUT N                     ' Zahl
INPUT NAME$                 ' String
IF X >= 10 THEN 100         ' Sprung zu Zeile 100 wenn wahr
GOTO 10
END
```

Viel Spaß mit **MiniBasic** & **MiniBasicUI**!
