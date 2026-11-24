# BlockLords

Kooperatives Dorf-Aufbau-, Claim-, Wirtschafts-, Diplomatie- und Wettkampfsystem für **Paper 1.21.11 und Java 21**. Entwickelt für den PraxiOS-Familien-SMP auf einem OnePlus mit Termux.

Die verbindliche Spiel- und Architekturentscheidung steht in [KONZEPT.md](KONZEPT.md) **v0.6**. Änderungen und der tatsächliche Verifizierungsstand stehen in [DEVLOG.md](DEVLOG.md).

## Aktueller Stand: 0.2.1-alpha.1 (Phase 2)

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

- `BlockLords-0.2.1-alpha.1.jar` — **deploybares Fat-JAR**, diese Datei verwenden
- `BlockLords-0.2.1-alpha.1-thin.jar` — nur für Analyse/Entwicklung, enthält keinen SQLite-Treiber

Tests ausführen:

```bash
./gradlew test
```

## Installation auf dem Testserver

1. Release-JAR nach `plugins/BlockLords-0.2.1-alpha.1.jar` kopieren.
2. Server vollständig stoppen und wieder starten.
3. Log prüfen: `BlockLords 0.2.1-alpha.1 aktiviert: Phase 2 ...`.
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

## Befehle

| Befehl | Funktion |
|---|---|
| `/bl info` | Dorfinfo zur anvisierten Glocke oder am aktuellen Standort |
| `/bl quest` | Aktuellen Gründungsfortschritt anzeigen |
| `/bl found <Name>` | Dorf an der anvisierten Glocke gründen |
| `/bl storage` | Vorrat öffnen; `/bl storage deposit` lagert passende Inventar-Items ein |
| `/bl build` | Baupläne öffnen; `preview`, `confirm`, `contribute`, `fund`, `start`, `sites`, `select` |
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
| `/bl admin bypass` | Eigenen Admin-Bypass umschalten und auditieren |
| `/bl admin disband <ID> confirm` | Claim bestätigt auflösen; Gebäude bleiben stehen |
| `/bl admin transfer <ID> <Spieler>` | Dorf auf einen Online-Spieler übertragen |
| `/bl admin alt pos1` / `pos2` | Altparzellen-Eckpunkte setzen |
| `/bl admin alt register <Spieler\|unbekannt> <Name>` | Altparzelle registrieren |
| `/bl admin alt remove <ID>` / `list` | Altparzelle entfernen oder auflisten |
