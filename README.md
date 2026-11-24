# BlockLords

Kooperatives Dorf-Aufbau-, Claim-, Wirtschafts-, Diplomatie- und Wettkampfsystem für **Paper 1.21.11 und Java 21**. Entwickelt für den PraxiOS-Familien-SMP auf einem OnePlus mit Termux.

Die verbindliche Spiel- und Architekturentscheidung steht in [KONZEPT.md](KONZEPT.md) **v0.6**. Änderungen und der tatsächliche Verifizierungsstand stehen in [DEVLOG.md](DEVLOG.md).

## Aktueller Stand: 0.2.5-alpha.1 (Phase 2)

Enthalten:

- reproduzierbarer Java-21-Build mit Gradle-Wrapper und GitHub-CI,
- validierte `config.yml` und versionierte SQLite-Migrationen,
- SQLite-Backups inklusive Android-/Termux-Natives,
- Dorfglocke und Rathaus-Basis-GUI,
- Gründungsquest mit Wohnraum, Nahrung und Arbeitsplatz,
- einmaliges Gründungspaket und kostenlose erste Dorfgründung,
- Dorfnamensprüfung, Zylinder-Claims und Überlappungsschutz,
- Schutz für Blöcke, Container, Entities, Explosionen, Feuer, Kolben, Flüssigkeiten, Hoppers, Eimer und Mob-Griefing,
- Altparzellen-Registrierung und ereignisbasierte Besitzerzuordnung für neue Blöcke,
- Audit-Log, Admin-Bypass, Dorftransfer und bestätigte Dorfauflösung,
- Ruhezustand bei Inaktivität ohne Schutzverlust; `decay.enabled: false` deaktiviert Zerfall vollständig,
- Java-Chest-GUI, native Floodgate-Formulare und Befehle als Fallback,
- gestaffelte Dorfvermessung für Bewohner und Betten in geladenen Chunks,
- clientseitige Blaupausen-Vorschau, Materialbeiträge mehrerer Spieler und echter FAWE-Bau,
- Rathaus, Wohnhaus, Farm, Mine, Lagerhaus, Bibliothek, Schmiede und Wohnhaus II,
- virtueller Vorrat mit physischer Ein-/Auszahlung, Kapazitätsgrenzen ohne Ressourcenverfall,
- Arbeiterzuweisung für Farm und Mine mit zeitbasierter Online-/Offline-Produktion,
- Dorf-XP, Level 1–10, Bibliotheksforschung und Wohnhaus-II-Freischaltung,
- gemeinsame Aktionsmenüs für Lager, Baupläne, Baustellen, Arbeiter und Forschung.

**Bewusste Grenzen:** Mehr-Dorf-Claims bleiben vorerst gesperrt. Missionen und persönliche Parzellen sind der nächste Phase-2-Folgeschritt. Militär, Diplomatie, reguläre Kriege und der bestätigte Herrschaftskampf folgen in Phasen 3–4.

### Neuerungen in 0.2.5-alpha.1 (P2/P3)

- **Echte Job-Sites:** Komposter und Schmiedetisch werden zugewiesen und mit `MemoryKey.JOB_SITE` verknüpft; die Mine enthält jetzt eine echte Schmiedetafel.
- **Bewohnerdetail:** alle registrierten Bewohner mit/ohne Bett, Anforderungen, Bettzuweisung und Auswahl des Arbeitsgebäudes.
- **Bettbelegung über Y-Ebenen korrigiert:** Mehrere Betten an derselben X/Z-Position werden jetzt korrekt unterschieden.
- **P3-Kurztexte:** einheitliches Muster Aktion → kurze Bestätigung → nächster Schritt statt langer Chat-Anweisungen.
- Recherche und Zahlenprüfung in `docs/VILLAGER_MECHANICS_RESEARCH.md` dokumentiert.

### Neuerungen in 0.2.4-alpha.1 (Build-Flow)

- Kisten-GUI mit klaren Menüpunkten und einheitlichen Glas-Füllern; Bauoptionen werden **8 pro Seite** mit Weiter/Zurück-Pfeilen paginiert.
- Platzierung standardmäßig **2 Blöcke vor dem Spieler**; Türen und andere directional Blockdaten rotieren korrekt mit der Spielerorientierung.
- Vollständige Edit-Hotbar mit `x±`, `z±`, links/rechts, hoch/runter und Bestätigen/Abbrechen; Sneak steuert Anker und Abbruch.
- Bewohnerdetail mit Anforderungen, Bettzuweisung und Auswahl des Arbeitsgebäudes.
- Eisen-Golems und andere Kampfentitäten werden nicht mehr pauschal durch den Dorfschutz blockiert; geschützt bleiben Villager und dekorative Objekte.
- Gras, hohes Gras, Farn und großer Farn werden beim Platzieren ignoriert/überschrieben.
- Selftest akzeptiert optional `/bl admin selftest <Typ> <x> <y> <z> <Rotation>`.

### Neuerungen in 0.2.3-alpha.1 (Hotbar-Baumodus)

- **Verankerte Preview:** Der Bauplan bleibt beim Umhergehen an seiner Position; Verschieben und Rotieren verwenden dieselbe Session und Kollisionsprüfung.
- **Hotbar-Werkzeugmodus:** Nach Preview-Auswahl über `/bl build tools` oder das Baummenü. Werkzeuge für Position, links/rechts, Anker, Bestätigen und Abbrechen.
- **Sichere Inventar-Session:** SQLite-Snapshot von Inventar, Rüstung, Offhand/Extras und Slot vor der ersten Ersetzung; PDC-Werkzeuge und Sperren gegen Drop, Drag, Klick, Offhand-Swap und fremde Container.
- **Wiederherstellung:** idempotent nach Bestätigen, Abbrechen, Logout, Tod, Weltwechsel, Reload und Serverstopp; Logout während Snapshot-Speicherung wird abgefangen.
- **Formular-Fallback:** Alle Bewegungs-/Rotationsaktionen bleiben über das Platzierungsmenü verfügbar; Hotbar ist optional.
- `/bl admin itemtest` prüft den ItemStack-NBT-Roundtrip auf dem laufenden Server.

### Neuerungen in 0.2.2-alpha.1

- **Baustellenkiste:** echte Kiste am Gebäudeanker mit Materialbestand, Lieferstand, Auftragsstatus und Restmaterial/Info nach Abschluss.
- **Rechtsklick auf die Kiste:** Auftragsmenü; Sneak + Rechtsklick öffnet das physische Inventar.
- Materialbeiträge kommen direkt aus Spielerinventar oder Rathausvorrat und werden aus dem realen Kistenbestand abgeleitet.
- FAWE überspringt die Kiste; Materialcommit bei Baustart, Rückgabe bei normalem Fehler, Reservierung bei Timeout.
- **Automatisches Bewohnerregister:** Meeting-Point an der Dorfglocke bestätigt, Bett im Claim registriert unbestätigt; Wanderer bleiben außen vor.
- **Versammlungsritual:** Sneak + Rechtsklick auf die Dorfglocke oder Aktion im Arbeiter-Menü; live Zählung von Anwesenden, neu Bestätigten und Bettlosen.
- Arbeiterzuweisung nutzt das dorffweite Register ohne 12-Block-Limit.
- **Dorfmenü-Kompass:** `/bl menu` oder automatisch bei Gründung; Rechtsklick öffnet Dorf-/Baumenüs.
- `/bl admin tp <Dorf-ID>` teleportiert mit Prüfung auf sicheren Stand.
- `/bl admin selftest <Typ>` funktioniert auch ohne Koordinaten am anvisierten Block.
- Timeout nach bereits erfolgreichem FAWE-Bau wird unterdrückt; kein `LATE_FAILURE_IGNORED` mehr nach `SUCCESS`.

### Änderungen in 0.2.1-alpha.1

- Preview wird vor dem Bestätigen geschlossen; eigene Ghost-Positionen blockieren die Validierung nicht mehr.
- FAWE-Aktionen besitzen Aktions-ID, Chunk-Prüfung, Fortschrittslogs, Timeout und genau einen Ergebnis-Callback.
- `/bl admin auditselftest` schreibt und liest eine eindeutige Audit-Probe.
- `logging.level: INFO|DEBUG` und `logging.action-timeout-seconds` sind konfigurierbar.
- Der Aktivierungslog zeigt die tatsächliche JVM-Version.
- Questtexte nennen konkrete Klickziele; Arbeitergebäude verweisen nach Fertigstellung auf `/bl workers`.
- Das Bau-Revamp mit Baustellenkiste ist in [docs/BAU_REVAMP_RESEARCH.md](docs/BAU_REVAMP_RESEARCH.md) beschrieben.

## Voraussetzungen

- Paper 1.21.11
- Java 21 auf Build- und Zielsystem
- **Erforderlich:** FAWE 2.16.3 für den echten Gebäudebau
- Optional: Floodgate für native Bedrock-Formulare
- Optional später: LuckPerms für externe Rang-Spiegelung

SQLite wird als Fat-JAR mitgeliefert. Neben den üblichen Desktop-/Linux-Bibliotheken enthält das JAR ausdrücklich die seit SQLite-JDBC 3.53 separaten **Android-Natives**.

## Build

```bash
# Linux/macOS, JAVA_HOME auf JDK 21 setzen
./gradlew build

# Windows
gradlew.bat build
```

Ergebnisse in `build/libs/`:

- `BlockLords-0.2.5-alpha.1.jar` — **deploybares Fat-JAR**, diese Datei verwenden
- `BlockLords-0.2.5-alpha.1-thin.jar` — nur für Analyse/Entwicklung, enthält keinen SQLite-Treiber

Tests ausführen:

```bash
./gradlew test
```

## Installation auf dem Testserver

1. Release-JAR nach `plugins/BlockLords-0.2.5-alpha.1.jar` kopieren.
2. Server vollständig stoppen und wieder starten.
3. Log prüfen: `BlockLords 0.2.5-alpha.1 aktiviert: Phase 2 ...`.
4. Daten und Migrationen liegen unter `plugins/BlockLords/`.
5. Vor manuellen Eingriffen `/bl admin backup` ausführen. Backups liegen unter `plugins/BlockLords/backups/`.

Deployment und Test auf dem OnePlus übernehmen Alexander beziehungsweise Axon. Entwicklung und Veröffentlichung erfolgen getrennt davon über dieses Repo.

## Erster manueller Abnahmetest

1. **Start:** `/bl admin list` zeigt noch keine Dörfer; `/bl admin backup` erzeugt eine Datenbankdatei.
2. **Dorf vorbereiten:** Eine Glocke, ein Bett, Ackerland/Getreide/Komposter und einen Arbeitsblock wie ein Lesepult platzieren.
3. **Quest starten:** Mit einem Nicht-OP-Spieler die Glocke rechtsklicken und „Gründungsquest starten“ wählen.
4. **Quest abschließen:** Bett, Nahrungsquelle und Arbeitsblock anklicken. Das Gründungspaket darf nur einmal ausgegeben werden.
5. **Gründen:** Über Menü/Formular einen Namen wählen oder mit Blick auf die Glocke `/bl found Eichhain` ausführen.
6. **Persistenz prüfen:** `/bl admin list` zeigt das Dorf. Nach einem Neustart müssen Claim und Landherr erhalten bleiben.
7. **Schutz prüfen:** Ein zweiter Spieler darf weder Blöcke abbauen noch Container/Dorfbewohner benutzen. Der Landherr darf es. `/bl admin bypass` muss ausdrücklich aktiviert werden und wird auditiert.
8. **Bedrock prüfen:** Gründung und Dorfinfo vollständig über native Formulare ausführen. Ohne Floodgate müssen Chest-GUI und Befehle weiterhin funktionieren.
9. **Altbesitz prüfen:** Mit `/bl admin alt pos1` und `pos2` einen Bereich markieren, registrieren und anschließend einen kollidierenden Claim versuchen. Dieser muss mit konkreter Fehlerursache abgelehnt werden.
10. **Zerfall prüfen:** `/bl admin decay` darf Statuswerte ändern, niemals jedoch den Schutz öffnen. Mit `decay.enabled: false` bleibt Zerfall vollständig deaktiviert.
11. **FAWE-Diagnose:** In einer freien, vollständig geladenen Testfläche `/bl admin selftest RATHAUS <x> <y> <z>` ausführen. Der direkte Baupfad muss ohne Fehler 99 Blöcke erzeugen. Ungeladene Chunks müssen sofort eine verständliche Fehlermeldung mit Aktions-ID erzeugen; `/bl admin auditselftest` muss anschließend sichtbar sein.
12. **Bauplan testen:** Im Dorf `/bl build preview RATHAUS`, Vorschau mit `/bl build confirm` bestätigen, Material mit `/bl build contribute` oder `/bl build fund` vollständig bestücken und danach `/bl build start` ausführen.
13. **Lager testen:** `/bl storage deposit` und anschließend `/bl storage` verwenden. Volle Lager dürfen keine Ressourcen vernichten; Entnahmen und Baustellenfinanzierung müssen im Audit erscheinen.
14. **Arbeiter testen:** Farm und Mine bauen, Bewohner in der Nähe platzieren und über `/bl workers` zuweisen. Nahrung einlagern und Produktionsbericht prüfen.
15. **Forschung testen:** Bibliothek bauen, `/bl research start` ausführen und anschließend Wohnhaus II als Bauplan bestätigen.
16. **Persistenz:** Nach Bau, Produktion und Forschung den Server neu starten. Gebäude, Lager, Arbeiterzuordnung, XP und Forschungsstatus müssen erhalten bleiben.
17. **Baustellenkiste:** Nach Preview-Confirm muss eine echte Kiste am Gebäude stehen. Rechtsklick öffnet das Auftragsmenü; Inventar- und Vorratstransfers müssen im realen Kistenbestand erscheinen. FAWE darf die Kiste nicht ersetzen.
18. **Bewohnerregister:** Bewohner mit Dorfglocken-Meeting-Point werden als bestätigt, Bettbewohner als unbestätigt erfasst. Sneak + Rechtsklick auf die Glocke muss Anwesende, neue Bestätigungen und bettlose Bewohner melden.
19. **Bedienung:** `/bl menu` liefert den Kompass; Rechtsklick öffnet Menüs. `/bl admin selftest RATHAUS` funktioniert ohne Koordinaten. `/bl admin tp <ID>` findet einen sicheren Zielpunkt oder meldet einen Fehler.
20. **Timeout-Race:** Nach erfolgreichem FAWE-Selftest mindestens 25 Sekunden warten. Es darf kein `LATE_FAILURE_IGNORED` nach `SUCCESS` erscheinen.
21. **Hotbar-Session:** Preview wählen, `/bl build tools`, rotieren/verschieben/Anker lösen und bestätigen. Originalinventar muss exakt zurückkehren. Tests zusätzlich mit Abbrechen, Logout/Join, Tod, Weltwechsel und Serverstopp wiederholen; keine Items dürfen dupliziert werden. `/bl admin itemtest` muss erfolgreich sein.
22. **Formular-Fallback:** Dieselben Platzierungsaktionen auf Bedrock über das Platzierungsmenü ausführen; keine Hotbar-Übernahme ist erforderlich.
23. **Build-Flow:** Bauoptionen mit Pfeilen paginieren, Bauplan zwei Blöcke vor Spieler platzieren und Edit-Hotbar für X/Z, Rotation und Höhe nutzen. Sneak steuert Anker und Abbrechen.
24. **Front und Gras:** Türseite muss zum Spieler zeigen. Gras/hohes Gras/Farn wird beim Platzieren überschrieben, andere Blöcke bleiben Kollisionen.
25. **Bewohnerdetail:** Alle registrierten Bewohner mit/ohne Bett öffnen, Anforderungen lesen, freies Bett zuweisen und Arbeitsgebäude auswählen.
26. **Golems:** Eigene Eisen-Golems müssen Zombies im Dorf angreifen können; Villager und dekorative Objekte bleiben geschützt.
27. **Job-Sites:** Bauer erhält Komposter, Minenarbeiter erhält Schmiedetisch. `/bl admin selftest MINE <x> <y> <z> <Rotation>` muss die echte Schmiedetafel erzeugen.
28. **Bewohnerdetail:** Alle registrierten Bewohner mit/ohne Bett öffnen, Anforderungen lesen, freies Bett zuweisen und Arbeitsgebäude auswählen.
29. **P3-Texte:** Kurze Hinweise lesen und befolgen; Detailinformationen bleiben per Klick verfügbar, nicht als lange Chat-Anweisung.

## Befehle

| Befehl | Funktion |
|---|---|
| `/bl menu` | Dorfmenü-Kompass erhalten; Rechtsklick öffnet Dorf-/Baumenüs |
| `/bl info` | Dorfinfo zur anvisierten Glocke oder am aktuellen Standort |
| `/bl quest` | Aktuellen Gründungsfortschritt anzeigen |
| `/bl found <Name>` | Dorf an der anvisierten Glocke gründen |
| `/bl storage` | Vorrat öffnen; `/bl storage deposit` lagert passende Inventar-Items ein |
| `/bl build` | Baupläne öffnen; `preview`, `tools`, `move`, `left`, `right`, `confirm`, `cancel`, `contribute`, `fund`, `start`, `sites`, `select` |
| `/bl workers` | Arbeiterübersicht; `assign <Gebäude-ID>`, `unassign <Arbeiter-ID>` |
| `/bl research` | Forschung öffnen beziehungsweise mit `start` beginnen |
| `/bl level` | Dorf-Level, XP und Status anzeigen |
| `/bl admin selftest <Typ> <x> <y> <z>` | Direkten FAWE-Baupfad ohne Dorfveränderung diagnostizieren |
| `/bl admin list` | Dorf-IDs, Level und Status anzeigen |
| `/bl admin reload` | Konfiguration validieren und sicher neu laden |
| `/bl admin backup` | SQLite-Backup erzeugen und alte Backups rotieren |
| `/bl admin decay` | Zerfallsprüfung ohne Schutzänderung ausführen |
| `/bl admin audit [n]` | Letzte Audit-Einträge anzeigen |
| `/bl admin auditselftest` | Audit-Schreib-/Leseprobe mit eindeutiger ID ausführen |
| `/bl admin itemtest` | ItemStack-NBT-Serialisierung und Rücklesung prüfen |
| `/bl admin bypass` | Eigenen Admin-Bypass umschalten und auditieren |
| `/bl admin tp <Dorf-ID>` | Sicher zur Dorfglocken-Umgebung teleportieren |
| `/bl admin disband <ID> confirm` | Claim bestätigt auflösen; Gebäude bleiben stehen |
| `/bl admin transfer <ID> <Spieler>` | Dorf auf einen Online-Spieler übertragen |
| `/bl admin alt pos1` / `pos2` | Altparzellen-Eckpunkte setzen |
| `/bl admin alt register <Spieler\|unbekannt> <Name>` | Altparzelle registrieren |
| `/bl admin alt remove <ID>` / `list` | Altparzelle entfernen oder auflisten |
