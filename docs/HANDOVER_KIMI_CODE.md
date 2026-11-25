# BlockLords — Übergabeprotokoll an Kimi Code

**Datum:** 26.11.2026  
**Von:** BLWB / Kimi  
**Für:** Kimi Code, Alexander, Axon  
**Repo:** https://github.com/drawnhunter/PraxiOS---BlockLords  
**Aktueller Produktstand:** **0.2.6-alpha.1**  
**Release-Commit:** `4c2e7879ca48c9193d799e33cb6819228b3dc045`  
**JAR-SHA256:** `8b0e74f574f2041078cb16ec673c5bacaf1237337c94f12afc1f7ed9f6db754d`  
**Direkter Download:** https://raw.githubusercontent.com/drawnhunter/PraxiOS---BlockLords/master/dist/BlockLords-0.2.6-alpha.1.jar

## 1. Zweck und Prioritäten

BlockLords ist ein kooperatives Dorf-Aufbau-, Claim-, Wirtschafts-, Diplomatie- und Wettkampfplugin für den PraxiOS-Familien-SMP. Das Ziel ist kein generisches Claim-Plugin, sondern ein nachvollziehbares Dorfleben: Spieler gründen Dörfer, bauen gemeinsam, registrieren Bewohner, weisen Berufe zu und entwickeln später freiwillige Wettkämpfe und Diplomatie. Die wichtigste aktuelle Arbeitsregel lautet: **Bestehende Live-Fehler zuerst, neue Systeme erst nach stabiler Abnahme.** Alexander entscheidet über Spielregeln und Go/No-Go, Axon deployed und testet auf dem OnePlus/Mobian-Server, BLWB beziehungsweise künftig Kimi Code entwickelt und veröffentlicht.

Die aktuelle Fassung 0.2.6 ist ein reparierter Zwischenstand. Sie enthält die neue Hotbar-Menüführung, Bewohnernamen mit Nummern, eine Baustellenkiste und korrigierte Bauplan-Mehrblocklogik. Diese Version ist auf einem eigenen Paper-Server verifiziert, benötigt aber noch Axons interaktive Live-Abnahme auf Paper 26.3. Keine noch nicht implementierte Funktion darf als vorhanden angenommen werden. Die verbindliche Spielvision bleibt `KONZEPT.md` v0.6; der tatsächliche Änderungs- und Testverlauf steht in `DEVLOG.md`.

## 2. Verbindliche Designentscheidungen

| Thema | Bestätigte Regel |
|---|---|
| **Einstieg** | Erste Dorfgründung nach kurzer, überspringbarer Gründungsquest kostenlos; Gründungspaket für den Start |
| **Folgedörfer** | Fortschrittsbedingungen und Smaragdkosten; in der aktuellen Phase weiterhin deaktiviert |
| **Gebäudekosten** | Echte Materialien plus moderate Smaragdkosten |
| **Baustellen** | Physische Baustellenkiste mit realem Inventar, Beiträgen, Status und Restmaterial |
| **Bewohner** | POI-basiertes Register, Namen plus `#Nummer`, dorffweite Arbeiterzuweisung |
| **Wettkämpfe** | Freiwillig, persönliches Opt-in, reguläre Konflikte ohne Gebäude-/Kisten-/Parzellenverlust |
| **Herrschaftskampf** | Extremer Spezialmodus um ein ganzes Dorf; beide Landherren plus alle Bürger/Gefolgsleute beider Dörfer müssen zustimmen |
| **Inventar** | Hotbar-Menüs nur mit persistierter Inventar-Session und idempotenter Wiederherstellung |
| **Zerfall** | Standardmäßig Ruhezustand ohne Schutzverlust; `decay.enabled: false` schaltet ihn vollständig ab |
| **Crossplay** | Java-GUI, Bedrock-Formulare und Befehle als Fallback; keine Pflicht-Clientmods |
| **Konfiguration** | Zahlen, Timer, Kosten und Limits konfigurierbar; Schutz- und Zustimmungsinvarianten bleiben fest |

Die Herrschaftskampfregel ist bewusst nicht implementiert und darf nicht versehentlich aus einem regulären Krieg entstehen. Ebenso sind destruktive Kriege, automatische Bautenlöschung, Steuer-/Schuldenwirtschaft und beliebig konfigurierbare Rechte-Matrix-Zellen aus dem Startumfang ausgeschlossen.

## 3. Veröffentlichungshistorie

| Version | Hauptinhalt | Commit |
|---|---|---|
| **0.1.0-alpha.1** | Phase 1: Gründung, Claims, Schutz, Altbestand, Audit, SQLite, Crossplay-Basis | `027b7d9cad03b3419568334f518d4b63a4d7ee95` |
| **0.2.0-alpha.1** | Blaupausen, FAWE-Bau, Baustellen, Lager, Arbeiter, Produktion, Level, Forschung | `bb35bb63995046ff34a1c9fefaaae16c4f15f07c` |
| **0.2.1-alpha.1** | Preview-Fix, Aktionslogs, Audit-Probe, echte JVM-Anzeige, Quest-/Onboarding-Hinweise | `b6d5d6734a0d7e2060f47f9e737241a6746173b0` |
| **0.2.2-alpha.1** | Baustellenkiste, POI-Bewohnerregister, Versammlung, Dorfmenü-Kompass, Admin-Teleport | `659fd6dad0da646f3e4e40852051e711e708baa6` |
| **0.2.3-alpha.1** | Verankerte Preview, Hotbar-Werkzeugmodus, persistierte Inventar-Session, Event-Sperren | `e9d4aaae02d65140a319e8b143fdf86de926569d` |
| **0.2.4-alpha.1** | Build-Flow, Pagination, Distanz 2, Directionals, Bewohnerdetails, Golem-/Gras-Fix | `c171ac6772a2525376cd5c524c64a0faf187ac87` |
| **0.2.5-alpha.1** | Echte Job-Sites, Schmiedetisch, Bett-Y-Fix, kurze Schritt-Hinweise, Zahlenprüfung | `b0d910c27351a57b94b7ce429247f9b030cfda16` |
| **0.2.6-alpha.1** | Hotbar-Root-Menü, Zurück/Verlassen, gesperrte Bauplaninfos, Namen/#Nummern, Tür-/Bett-Reparatur | `4c2e7879ca48c9193d799e33cb6819228b3dc045` |

Zusätzliche Artefakt-Commits stellen JARs bei blockiertem Release-Asset-Upload direkt unter `dist/` bereit. Für die aktuelle Version ist `dist/BlockLords-0.2.6-alpha.1.jar` maßgeblich. Ältere Release-Assets können vorhanden sein, aber der direkte Repo-Download ist der zuverlässigere Testweg, wenn der Upload-Endpunkt wieder Netzwerkprobleme zeigt.

## 4. Aktuelle Architektur

### 4.1 Paketkarte

- `BlockLords.java`: Plugin-Lebenszyklus, Start, Migration, Verkabelung, Backup, Reload, Diagnose.
- `config/BlockLordsConfig.java`: vollständige Validierung der `config.yml`.
- `database/Database.java`: kontrollierte SQLite-Verbindung, Transaktionen, WAL, Backups.
- `database/MigrationRunner.java`: explizit versionierte Migrationen V1–V5.
- `model/`: Dorf, ClaimRegion, Rollen, Altparzellen, Baupläne, Baustellen, Bewohner, Ressourcen, Snapshots.
- `repository/`: SQLite-Zugriffe und Transaktionen; keine versteckte zweite Wahrheit für Kistenmaterial.
- `service/`: Gründung, Schutz, Bau, FAWE, Produktion, Forschung, Bewohner, Hotbar-Session, Audit und Logging.
- `ui/`: Chest-GUI, Aktionsmenüs, Floodgate-Formulare und Menü-Listener.
- `listener/`: Glocke, Schutz, Kiste, Hotbar, Bewohner-Lebenszyklus und Besitzer-Tracking.

### 4.2 Threading- und Persistenzregeln

Welt-, Block-, Inventory- und Entity-Zugriffe erfolgen im passenden Spielkontext. Datenbanken, Dateien, Berechnungen und FAWE-Queues laufen auf getrennten Executors. `TaskRunner` besitzt einen DB-Executor und einen Scan-/FAWE-Executor. Schutzentscheidungen verwenden einen Main-Thread-Cache und nicht bei jedem Event eine Datenbankabfrage. FAWE-Aktionen besitzen Aktions-ID, Chunk-Prüfung, Queue-/Flush-Logs, Timeout und genau einen Ergebnis-Callback. Der Timeout darf nach `SUCCESS` nicht mehr als `LATE_FAILURE_IGNORED` erscheinen.

SQLite verwendet WAL, `foreign_keys=ON`, `busy_timeout=5000` und kontrollierte Transaktionen. Das Fat-JAR enthält den JDBC-Treiber, Linux-Natives einschließlich `aarch64` und Android-Natives für die alte Termux-Umgebung. Backups liegen unter `plugins/BlockLords/backups/`; `/bl admin backup` erzeugt ein Backup mit `VACUUM INTO`. `DECAY` verändert niemals den Schutz, sondern nur Status und zukünftige Produktionsregeln.

### 4.3 Datenbankmigrationen

| Version | Inhalt |
|---|---|
| **V1** | Dörfer, Mitglieder, Gründungsquests, Altparzellen, Blockbesitz, Features, Audit |
| **V2** | Gebäude, Baustellen, Materialbeiträge, Lager, Arbeiter, Produktion, Forschung |
| **V3** | Bewohnerregister mit POI-Links und Status |
| **V4** | Hotbar-/Platzierungs-Sessions mit serialisiertem Inventar |
| **V5** | Persistente Bewohnernamen und fortlaufende Nummern |

Bei neuen Migrationen niemals bereits veröffentlichte SQL-Versionen nachträglich verändern. Neue Regeln kommen in V6, V7 usw. Die bisherigen Dateien bleiben reproduzierbar. Besondere Schutzregeln, Claims, Zustimmungen und Audit brauchen Transaktionen und Tests, keine nachträglichen Datenbank-Hacks.

## 5. Aktuelle Spielmechaniken

### 5.1 Gründung und Schutz

Die Gründungsquest verlangt Bett, Nahrungsquelle und Arbeitsblock und gibt genau einmal das Starterpaket aus. Die erste Gründung ist kostenlos, Folgedörfer sind derzeit gesperrt. Claim ist ein eigener Zylinder um die Glocke, Start-Radius 48, volle Höhe. Altbesitz wird nicht rückwirkend erraten: Admins registrieren Altparzellen; neue Blockplatzierungen werden danach ereignisbasiert zugeordnet. Schutz umfasst Blöcke, Container, Entities, Explosionen, Feuer, Kolben, Flüssigkeiten, Hoppers, Eimer und Mob-Griefing. Der Kampfschutz gilt gezielt für Villager und dekorative Objekte, nicht pauschal für Zombies oder Golems.

`/bl admin tp <Dorf-ID>` sucht in der Nähe der Glocke einen sicheren Stand mit festem Boden, passierbaren Fuß-/Kopfblöcken und scheitert verständlich, wenn keiner gefunden wird. `/bl admin auditselftest` schreibt und liest eine eindeutige Probe; `/bl admin itemtest` prüft ItemStack-NBT auf dem laufenden Server.

### 5.2 Baustellenkiste und Baupläne

Nach Preview-Confirm entsteht eine echte Kiste im hinteren Innenraum des Bauplans `(2, 1, 3)`, außerhalb von Türweg, Betten und Arbeitsblöcken. Sie kann nicht versehentlich abgebaut, explodiert, verbrannt oder per Kolben verschoben werden; der bewusste Abbruch erfolgt über `/bl build cancel <ID>` oder das Baustellenmenü und gibt alle Items zurück. Rechtsklick öffnet das Auftragsmenü, Sneak + Rechtsklick das reale Inventar. Inventar- und Rathausvorratstransfers landen als echte Items in der Kiste; der reale Bestand wird ausschließlich im Hauptthread gelesen. `reconcileFromChest` leitet den Status aus diesen erfassten Zahlen ab. Beim Bau wird Material committet; normale Fehler geben es zurück, Timeouts lassen es sicher reserviert. FAWE überspringt die gespeicherte Kistenposition. Nach Abschluss bleibt die Kiste als Informations- und Restmaterialpunkt erhalten; ihre Koordinaten werden auch in das Gebäude übernommen.

Die Startbaupläne umfassen Rathaus, Wohnhaus, Farm, Mine, Lagerhaus, Bibliothek, Schmiede und Wohnhaus II. Die Mine besitzt eine echte Schmiedetafel. Das Bauernhaus besitzt ein Bett und ein Feld außerhalb der Hütte. Türen besitzen genau eine `half=bottom` und eine `half=top`, Betten genau einen `part=foot` und einen `part=head`. Directional Blockdaten rotieren mit. Diese Regeln wurden nicht nur kompiliert, sondern per echtem Server-`execute if block` nachgewiesen.

### 5.3 Bewohner und Arbeit

Das Register nutzt `MEETING_POINT` und `HOME`: Dorfglocke = bestätigt, Bett im Claim = unbestätigt, andere = Wanderer. Die Versammlung per Sneak + Rechtsklick auf die Dorfglocke oder Arbeiter-Menü setzt den Vanilla-Meeting-Point, teleportiert geladene lebende Villager auf sichere Plätze neben die Glocke und meldet versammelte, neue, bettlose und nicht platzierbare Bewohner. Einzelteleport und Entfernen aus dem Register sind verfügbar; letzteres löscht POI-/Arbeitsdaten, aber tötet keinen Villager. Die Zuweisung zu Farm/Mine nutzt das dorffweite Register ohne altes 12-Block-Limit. Bauer erhält Komposter, Minenarbeiter Schmiedetisch und beide werden mit `MemoryKey.JOB_SITE` verknüpft. Pro Gebäude sind standardmäßig zwei Arbeitsplätze frei (`workers.per-building`); die Anzeige zeigt Belegung und freie Plätze `x/2`. Namen werden aus `ResidentNames` zufällig gewählt und als `Name #Nummer` angezeigt und persistiert. Nummern werden bereits bei der Vergabe reserviert, damit auch Batch-Scans keine Duplikate erzeugen; `#0`, fehlende Namen und alte Duplikate werden beim Laden repariert. Verstorbene Bewohner erscheinen getrennt als Verlorene, verlieren ihre Arbeitszuweisung und können gemeinsam aus dem Register entfernt werden.

### 5.4 Hotbar-Menü und Edit-Modus

Die Glockenaktion „Bauen & Baustellen“ öffnet ein Hotbar-Root-Menü: Bauen, Baustellen, Bewohner, Vorschau und Zurück/Verlassen. „Bauen“ zeigt Baupläne in der Hotbar, acht pro Seite mit Weiter-Pfeil. Zurück und Verlassen liegen in der Hotbar; die Zweithand enthält kein Verlassen-Werkzeug. Gesperrte Baupläne bleiben sichtbar und nennen die Freischaltbedingung. Nach Auswahl entsteht die Preview zwei Blöcke vor dem Spieler und wechselt direkt in den Edit-Modus mit X/Z ±, Rotation, Höhe und Bestätigen/Abbrechen.

Die Inventar-Session speichert Inventar, Rüstung, Offhand/Extras und Slot in SQLite. PDC-Werkzeuge und Event-Sperren verhindern Drops, Klicks, Drags, Offhand-Swaps und fremde Container. Wiederherstellung erfolgt durch Ersetzen statt Addieren und ist idempotent nach Bestätigen, Abbrechen, Logout, Tod, Weltwechsel, Reload und Serverstopp. Logout während Snapshot-Speicherung schließt die Session, ohne offline Werkzeuge zu installieren. Alle Platzierungsaktionen bleiben über Menü/Formular erreichbar.

## 6. Research-Wissen und Quellen

### 6.1 Pflichtdokumente

| Datei | Inhalt |
|---|---|
| `KONZEPT.md` | Verbindliche Spielvision und bestätigte Entscheidungen, v0.6 |
| `DEVLOG.md` | Vollständiger Änderungs- und Verifizierungsverlauf |
| `docs/BAU_REVAMP_RESEARCH.md` | Ghost-Preview, Baustellenkiste, Schematics, Fortschritt, NPC-Bau, Diagnose |
| `docs/UPDATE_RESEARCH_HOTBAR_SCHEMATICS_NPC.md` | Hotbar-Sicherheit, Schematic-Quellen, Mobian, NPC-/Quest-Roadmap |
| `docs/VILLAGER_MECHANICS_RESEARCH.md` | Vanilla-POIs, Golems, Baustellen-UX, Produktionszahlen und P3-Texte |
| `docs/images/BlockLords_Baustellen_Flow.png` | Zielbild der Baustellen-Warteschlange |
| `docs/images/BlockLords_NPC_Roadmap.png` | Funktion zuerst, Dialoge, optionale Optik, Rigs später |
| `verifier/` | Abnahmekriterien, Checkskript und Laufprotokolle |

### 6.2 Zentrale Research-Schlussfolgerungen

- **Preview:** Paper-`sendBlockChange`/`sendMultiBlockChange` fälschen Clientblöcke, ohne die Welt zu ändern. Preview-Sessions brauchen eigene Positionen, Sichtbarkeitsgrenzen und explizites Zurücksetzen. WorldEditCUI ist ein optionales Clientmod-Protokoll und keine Pflichtlösung für Crossplay.
- **Schematics:** Sponge `.schem` v3 unterstützt Blockstates, Block-Entities, Entities und Offset. FAWE/WorldEdit rotiert über den ClipboardHolder vor dem Paste. Jeder Bauplan braucht Anker, Rotationszentrum, Eingang, Kistenposition, Job-Sites und Kosten.
- **Baustellen:** MineColonies trennt gelieferte von tatsächlich verwendeten Ressourcen. Genau diese Trennung ist auch für BlockLords verbindlich: `geliefert/gesamt` und `verbaut/gesamt` dürfen nicht verwechselt werden.
- **NPC-Wirtschaft:** TekTopia und Millénaire zeigen den Wert sichtbarer Produktionsabhängigkeiten, aber auch die Risiken von Pathfinding und harten Verkettungen. Unsere Zuweisung ist robust; Laufwege sind Ambiente, keine unsichtbare Produktionsbedingung.
- **Hotbar:** Inventar-Session, PDC-Werkzeuge, Sperren und Wiederherstellung nach jedem Abbruchweg. Keine Item-Adds bei der Wiederherstellung, damit keine Duplikate entstehen.
- **Mobian:** Normales Linux/ARM64; Java 21 bis 1.21.11, Java 25 ab Paper 26.1. Unsere Linux-aarch64-Natives reichen; keine DB-Konvertierung nötig.
- **NPCs:** Texturen und Skins sind möglich; neue Modelle benötigen auf Java meist Mods. Geyser-Rigs brauchen eigene Pakete und Java-Simulation und sind ein separates Crossplay-Projekt. Dialoge und Quests sind davon unabhängig.
- **UX:** Ein sichtbarer nächster Schritt und Detailinformationen auf Abruf statt langer Fronttour. Muster: Aktion → kurze Bestätigung → nächster Schritt.

### 6.3 Zahlen, die bereits geprüft wurden

Der aktuelle Produktionszyklus beträgt 300 Sekunden, also 12 Zyklen/Stunde. Pro Minenarbeiter werden im Erwartungswert 3 Smaragde/Stunde erzeugt. Für 64 Smaragde als zweiter Claim ergeben sich ungefähr 21,3 Stunden bei einem Minenarbeiter, 10,7 bei zwei, 5,3 bei vier und 2,7 bei acht. Ein einzelner Bauer füllt die Startkapazität von 1.728 Einheiten rechnerisch erst nach 14,4 Stunden; der Offline-Deckel von vier Stunden mit 50 % begrenzt zusätzlich passive Anhäufung. Diese Werte sind Startwerte und keine endgültige Balance. Kimi Code soll mit tatsächlichen Live-Daten arbeiten und sie nicht ohne Testgrund verschärfen.

## 7. Offene Grenzen und bekannte Risiken

- **0.2.6 braucht noch Axons interaktive Live-Abnahme** für Hotbar-Root, Pagination, Zurück/Verlassen, Namen/#Nummern, Feld, Türen und Betten auf Paper 26.3/Java 25.
- Mehr-Dorf-Claims sind weiterhin gesperrt; die Fortschrittsprüfung ist lediglich vorbereitet.
- Missionen und persönliche Parzellen sind noch nicht implementiert.
- Physische Schematic-Dateien und Herkunftsakte sind geplant, aber der allgemeine Loader ist noch nicht fertig.
- Arbeiter-Laufwege bleiben Vanilla-/NPC-Ambiente; es gibt noch keine eigene Navigationssimulation.
- Bauhütte und Builder-NPC sind noch nicht implementiert.
- Militär, Diplomatie, reguläre Kriege und der bestätigte Herrschaftskampf folgen erst in den späteren Phasen.
- Eigene NPC-Rigs sind kein aktueller Auftrag und dürfen nicht zum Pflichtbestandteil werden.
- Release-Asset-Uploads waren zeitweise instabil; deshalb werden Test-JARs zusätzlich unter `dist/` versioniert.
- `/tmp` und andere temporäre Bereiche werden zwischen Chats bereinigt. Arbeitsstände gehören immer in GitHub; keine lokale Datei darf die einzige Wahrheit sein.

## 8. Konkreter Updateplan für Kimi Code

### Sofortige Reihenfolge

1. `KONZEPT.md`, `DEVLOG.md`, die drei Research-Dokumente und `verifier/` lesen.
2. Repo auschecken, Java 21 einrichten und `./gradlew clean build` ausführen.
3. Axons neuesten Live-Test zu 0.2.6 lesen; blockierende Fehler zuerst beheben.
4. Erst danach die nächste Version implementieren.

### Geplante Versionen

| Version | Inhalt |
|---|---|
| **0.2.7-alpha.1** | `.schem`-Loader, Metadatenvalidator, Anker/Rotationszentrum, Herkunftsakte, erste eigene Baupläne und Kolonie-Gründungs-/UI-Basis |
| **0.2.8-alpha.1** | Bauhammer, manuelle Blockerkennung, atomare Reservierung und sichtbarer Fortschritt |
| **0.2.9-alpha.1** | Bauhütte und Builder-Villager mit derselben Kiste/Warteschlange |
| **0.3.x** | Dialog-/Quest-Grundlagen, optionale Textur-/Skin-Pakete und Bewertung von Citizens/BetonQuest |
| **Spätere Phase 3/4** | Militär, Diplomatie, reguläre Kriege und Herrschaftskampf nach stabilem Phase-2-Kern |

Kolonien bleiben Verwaltungssektionen mit gemeinsamer Dorf-XP, Lager und Konto. Keine zweite Wirtschaft. Gründung ist explizit und nie automatisch bei Deko-Glocken. Eine Kolonie-Glocke liegt außerhalb des Dorf-Claims und höchstens 256 Blöcke von der Hauptglocke entfernt; darüber soll das Plugin eine neue Dorfgründung vorschlagen. Fremde Claims dürfen nicht geschnitten werden. Haupt- und Kolonie-Glocken brauchen getrennte Auflösungsregeln.

### Verbotene Abkürzungen

- Keine neuen Abhängigkeiten wie Citizens, Denizen, BetonQuest oder ModelEngine nur wegen Optikwünschen.
- Keine Pflicht-Clientmods oder Rigs für Kernmechaniken.
- Keine nachträgliche Änderung bereits veröffentlichter Migrationen.
- Keine `System.out`-Diagnose als Ersatz für Aktionslogs.
- Keine pauschale Blockierung aller Entity-Schäden im Claim.
- Keine zweite Materialwahrheit neben dem realen Kisteninhalt.
- Keine automatische Gründung oder Kolonie durch bloßes Anklicken einer Deko-Glocke.

## 9. Build-, Test- und Deploymentvertrag

### Build

```bash
./gradlew clean build
```

Ergebnisse:

- `build/libs/BlockLords-<version>.jar` = deploybares Fat-JAR
- `build/libs/BlockLords-<version>-thin.jar` = nur Analyse/Entwicklung, kein SQLite-Treiber

### Erforderliche Laufzeit

- Paper 1.21.11 oder kompatible 26.x
- Java 21; für Paper 26.x wurde Java 25 bereits getestet
- FAWE 2.16.3 ist Pflicht
- Floodgate optional für native Bedrock-Formulare
- LuckPerms optional und derzeit nicht führend für interne Rollen

### Eigener Verifier

- Aktuelle Kriterien: `verifier/v4/acceptance.md`; `verifier/v1/`, `verifier/v2/` und `verifier/v3/` bleiben als historische Abnahmen erhalten.
- Build-/Testskript: `verifier/v4/check.sh`
- Protokolle: `verifier/runs/`
- Bei geänderten Kriterien eine neue Verifier-Version anlegen, alte Kriterien nicht überschreiben.

### Live-Abnahme auf dem OnePlus

Der aktive Coding-/Deploy-Agent führt Deployment, Konsolenprüfung und technische Live-Tests auf dem OnePlus/Mobian-Server mit Java 25 und Paper 26.3 selbst durch. Alexander ergänzt die spielerische Abnahme:

1. Glocke öffnen und Hotbar-Root prüfen.
2. Bauplan auswählen, Preview 2 Blöcke vor Spieler und sofortige Edit-Hotbar prüfen.
3. Pagination, Zurück bis Root und Verlassen prüfen.
4. Gesperrte Baupläne und deren Freischaltinfo prüfen.
5. Bewohnernamen/#Nummern und Neustart-Persistenz prüfen.
6. Baustellenkiste, Inventar-/Vorratstransfer und FAWE-Ausnahme prüfen.
7. Feld, Türhälften und Bettkopf/-fuß prüfen.
8. Bettzuweisung, Job-Site-Verknüpfung und kurze Hinweise prüfen.
9. Golem-Kampf und Grasfreimachung prüfen.
10. Logout/Tod/Serverstopp auf Inventarverlust oder Duplikate prüfen.

Aktuelles Feedback kommt in #230; #210 bleibt als älterer Testverlauf erhalten, ebenso #201/#202 für Onboarding und Research. Neues Material bitte mit Schritten, Log-Aktions-ID, Screenshot und Serverversion melden.

## 10. Rollen- und Sicherheitsregeln

- **Alexander:** Go/No-Go, Designentscheidungen, Prioritäten, Tokens und spielerische Abnahme.
- **Kimi Code:** Code, Commits, Push, Releases, eigener Testserver, Verifier, Deployment, OnePlus/Mobian, MC-Panel, SSH, Konsolen- und Log-Auswertung.
- **Bus:** Nachrichten, Status, Abstimmung und Testfeedback. GitHub bleibt Ort für Code, JARs, Releases und Research-Dokumente.
- **Keine Tokens** in Ticket-Texten, Git-Dateien, Logs oder diesem Handover. PAT/Bus-Token werden ausschließlich über die dafür vorgesehenen Laufzeitparameter verwendet und regelmäßig rotiert.

## 11. Startauftrag für Kimi Code

Die korrekte erste Aktion ist nicht „neue Features bauen“, sondern:

1. Aktuellen Stand 0.2.6 aus GitHub ziehen.
2. Build und 28 Tests bestätigen.
3. Axons Live-Testergebnis in #210 prüfen.
4. Falls kein Blocker: 0.2.7 mit Schematic-Loader und Kolonie-Gründungsbasis beginnen.
5. Falls Blocker: Bugfix-Release mit nachvollziehbaren Tests zuerst.

Damit bleibt die Projektgeschichte, die Designabsicht, die technische Architektur, die Testbarkeit und die tatsächliche Verantwortungsverteilung erhalten.
