# BlockLords — Entwicklungsprotokoll

## 2026-11-23 — Konzept v0.6 final

- Deep Research und technische Gegenprobe auf Basis von `dd520df`, `6ff16ff` und `e3b8db1` abgeschlossen.
- Alle unkommentierten A-Empfehlungen und Restvorgaben durch Alexander bestätigt.
- Erste Gründung nach kurzer Quest kostenlos; Gründungspaket für den Einstieg.
- Reguläre Konflikte ohne Gebäude-, Container- oder Parzellenverlust.
- Zerfall standardmäßig als Ruhezustand mit unverändertem Schutz; `decay.enabled: false` als bestätigte Config-Alternative ohne Zerfall.
- **Herrschaftskampf als Extrem-Spezialmodus bestätigt:** Einsatz ist das gesamte Dorf. Erforderlich sind beide Landherren sowie alle registrierten Bürger und Gefolgsleute beider Dörfer. Privatbesitz und menschliche Mitglieder werden niemals übertragen. Der Dorftransfer muss atomar, auditierbar und nur nach eindeutigem Sieg erfolgen.
- Der vorhandene Projektserver wird Testserver. Deployment erfolgt durch Alexander/Axon; Entwicklung und GitHub-Bereitstellung getrennt davon.

## 2026-11-23 — 0.1.0-alpha.1, Phase 1 Fundament

### Implementiert

- Offizielle Gradle-Wrapper-Skripte ergänzt; Shadow/Fat-JAR und CI-Build eingerichtet.
- Java-21-Toolchain, Paper-API 1.21.11, FAWE-API 2.16.3 compileOnly, Floodgate-API 2.2.5 optional.
- SQLite-JDBC 3.54.1.0 einschließlich separatem `natives-android`-Classifier für Termux.
- Migration `V1__phase1_foundation.sql` für Dörfer, Mitglieder, Quests, Altparzellen, Blockbesitz, Features und Audit.
- Transaktionen, WAL, Busy-Timeout, kontrollierte DB-Ausführung, Backups mit `VACUUM INTO` und Aufbewahrungsrotation.
- Gründungsquest, Starterpaket, Namensprüfung, Claim-Limits, Zylinder- und Überlappungsregeln.
- Altbestand-Registrierung und neue Blockbesitz-Ereignisse; keine erfundene historische Besitzererkennung.
- Main-Thread-Cache für Schutzentscheidungen; DB und Scans auf getrennten Executors.
- Schutz für Block-/Container-/Entity-Interaktionen, Explosionen, Feuer, Kolben, Flüssigkeiten, Hoppers, Eimer und Mob-Griefing.
- Java-GUI, optionale native Floodgate-Formulare und Befehls-Fallback.
- Ruhezustand ohne Schutzänderung; Audit, Adminbefehle, Transfer und bestätigte Auflösung.

### Verifiziert

- `./gradlew clean build` erfolgreich mit Java 21.
- **10 Unit-Tests ohne Fehler:** Migration, Backup, Gründung, Claim-Überlappung, Mehr-Dorf-Sperre, Altparzellen, Blockbesitz, Region und Dorfnamen.
- Fat-JAR enthält `plugin.yml`, Config, Migration, JDBC-Service sowie Linux-aarch64- und Android-Natives.
- Echter Start auf **Paper 1.21.11 Build 132** ohne Floodgate: Migration, Aktivierung, Adminliste, Backup, Zerfallsprüfung und sauberes Beenden erfolgreich.
- Zweiter echter Start mit **Floodgate 2.2.5 Build 141**: Floodgate und BlockLords aktiv, keine fehlenden Klassen, Befehle und sauberes Beenden erfolgreich.
- Während der Laufzeitprüfung wurde eine optionale Floodgate-Klassenbindung beim Start ohne Floodgate gefunden und durch lazy Initialisierung korrigiert.

### Bekannte Grenzen / nächster Test durch Alexander und Axon

- Kein automatisierter echter Spieler-Client: Gründungsklicks, Chest-GUI und native Formulare müssen im manuellen Test auf dem OnePlus bestätigt werden.
- Mehr-Dorf-Claims bleiben in Phase 1 deaktiviert; die Regelprüfung für spätere Kernbedingungen ist lediglich vorbereitet.
- Rathaus ist in Phase 1 die Verwaltungsoberfläche; physische Blaupausen und Wirtschaft gehören zu Phase 2.
- Dorfvermessung erfasst nur geladene Chunks und kennzeichnet unvollständige Ergebnisse ausdrücklich.
- Offline-Produktion ist noch nicht implementiert; Ruhezustand betrifft derzeit Status und künftige Produktion.


## 2026-11-23 — 0.2.0-alpha.1, Phase 2 Wachstum und Wirtschaft

### Implementiert

- Migration `V2__phase2_economy.sql` für Gebäude, Baustellen, Materialbeiträge, Lager, Arbeiter, Produktion und Forschung.
- Acht Ressourcen mit kontrollierter Item-Zuordnung und kanonischer Rückgabe.
- Clientseitige Bauvorschau mit Kollisions- und Claim-Prüfung; keine Weltänderung vor Bestätigung.
- Baustellen mit Inventar- und Lagerfinanzierung, transaktionalen Beiträgen und Beitragsprotokoll mehrerer Spieler.
- FAWE-Bau mit sortierten Blocklagen und Undo-Versuch bei Fehlern; `/bl admin selftest` als direkte Diagnose.
- Rathaus, Wohnhaus, Farm, Mine, Lagerhaus, Bibliothek, Schmiede und Wohnhaus II als Startbaupläne.
- Virtueller Vorrat mit physischer Ein-/Auszahlung, Lagerkapazität und keiner Vernichtung von Überschuss.
- Sichere SQLite-Abzüge durch UPDATE-first statt negativem UPSERT; kein negativer Bestand und keine Teilbelastung bei fehlenden Kosten.
- Villager-Zuordnung für Bauer und Minenarbeiter; Berechnung unabhängig vom NPC-Pathfinding.
- Online-/Offline-Produktion mit Aktivitätsfenster, Vier-Stunden-Deckel, Nahrungsverbrauch und Kapazitätsgrenzen.
- Dorf-XP und Level 1–10; Bibliotheksforschung für Wohnhaus II.
- Gemeinsame Aktionsmenüs für Lager, Bauen, Baustellen, Arbeiter und Forschung auf Java und Bedrock; Befehle als Fallback.

### Verifiziert

- `./gradlew test shadowJar` und `./gradlew clean build` mit Java 21 erfolgreich.
- **20 Unit-Tests ohne Fehler**, davon neue Tests für Produktion, Lagerkapazität, Kostenatomarität, Baustellen und Levelgrenzen.
- Echter Start auf Paper 1.21.11 Build 132 mit FAWE 2.16.3 und Floodgate 2.2.5 Build 141.
- Migrationen V1 und V2, Aktivierung, Adminliste, Backup, Zerfallsprüfung und sauberer Shutdown erfolgreich.
- `/bl admin selftest RATHAUS 20 80 20` erzeugte über denselben FAWE-Baupfad wie im Spiel **99 Blöcke** und endete ohne Fehler.

### Bekannte Grenzen / nächster Folgeschritt

- Missionen und persönliche Parzellen sind noch nicht implementiert und bilden den nächsten Phase-2-Folgeschritt.
- Mehr-Dorf-Claims bleiben vorerst gesperrt; die Regelprüfung ist vorbereitet.
- Arbeiter-Laufwege sind weiterhin Vanilla-/NPC-Ambiente, keine eigene Navigationssimulation.
- Interaktive Klicks, Menüs, Arbeiterzuweisung und Produktion müssen von Axon auf dem OnePlus getestet werden.
- Militär, Diplomatie, reguläre Kriege und der bestätigte Herrschaftskampf folgen in Phasen 3–4.


## 2026-11-24 — 0.2.1-alpha.1, Diagnose und Bugfixpaket aus #202

### Implementiert

- Preview wird vor dem Bestätigen geschlossen; ausschließlich eigene, exakt übereinstimmende Ghost-Positionen werden ausgenommen. Fremde/neue Blöcke bleiben Kollisionen.
- FAWE-Bau erhält Aktions-ID, Welt-/Chunk-Prüfung, EditSession-/Queue-/Flush-Logs, Timeout und genau einen Ergebnis-Callback.
- Ungeladene Ziel-Chunks werden sofort und verständlich gemeldet, statt stumm zu warten.
- `logging.level: INFO|DEBUG` und `logging.action-timeout-seconds` sind konfigurierbar.
- Aktivierungslog verwendet `System.getProperty("java.version")` statt festem Text.
- `/bl admin auditselftest` schreibt eine eindeutige Probe und verlangt deren Nachweis beim Lesen; Audit-Fehler landen im Serverlog.
- Questtexte nennen konkrete Klickziele; geöffnete Questmenüs werden aktualisiert; fertige Arbeitergebäude verweisen auf `/bl workers`.

### Verifiziert

- Java-21-Clean-Build und **20 Unit-Tests** erfolgreich.
- Echter Start auf Paper 1.21.11 Build 132 mit FAWE 2.16.3 und Floodgate 2.2.5 Build 141.
- Audit-Probe wurde geschrieben, wiedergefunden und über `/bl admin audit 5` angezeigt.
- Ungeladene Chunks erzeugten sofortige Fehlermeldungen mit Aktions-ID und Stacktrace.
- Nach `forceload add 0 0` lief `/bl admin selftest RATHAUS 0 80 0` mit vollständiger Logfolge erfolgreich: **99 Blöcke in 496 ms**.
- Kompatibilitätsfeedback von Axon übernommen: Phase 2 läuft auch auf Paper 26.2-135 und 26.3-259 mit Java 25.

### Nächster Schritt

- Nach Axons Live-Gegenprobe folgt 0.2.2 mit der Baustellenkiste gemäß `docs/BAU_REVAMP_RESEARCH.md`.


## 2026-11-24 — 0.2.2-alpha.1, Baustellenkiste und Bewohnerregister

### Implementiert

- **Baustellenkiste als physischer Auftragsanker:** Nach Preview-Confirm entsteht eine echte Kiste am Gebäude; Rechtsklick öffnet das Auftragsmenü, Sneak + Rechtsklick das reale Inventar.
- Inventar- und Vorratstransfers landen als echte Items in der Kiste; `reconcileFromChest` leitet Bereitschaft aus diesem Bestand ab.
- FAWE schließt die Kistenposition aus dem Bau aus. Material wird beim Start committet, bei normalem Fehler mit Undo zurückgegeben und bei Timeout sicher reserviert.
- Nach Abschluss bleibt die Kiste als Informations-/Restmaterialpunkt erhalten.
- **Bewohnerregister mit Vanilla-POI-Links:** `MEETING_POINT` an der Dorfglocke = bestätigt; `HOME`/Bett im Claim = unbestätigt; sonst Wanderer ohne Registrierung.
- Diff-basierte Aktualisierung statt blindem Überschreiben; persistente Speicherung in Migration V3.
- **Versammlungsritual:** Sneak + Rechtsklick auf die Dorfglocke oder Arbeiter-Menü. Setzt den Vanilla-Meeting-Point, bestätigt Anwesende und meldet versammelte, neue und bettlose Bewohner.
- Arbeiterzuweisung nutzt das dorffweite Register; das alte 12-Block-Limit ist entfernt.
- Dorfmenü-Kompass mit PDC-Erkennung, `/bl menu` und automatischer Ausgabe bei Gründung.
- `/bl admin tp <Dorf-ID>` mit Suche nach passierbarem Stand, Kopffreiheit und festem Boden.
- `/bl admin selftest <Typ>` ohne Koordinaten am anvisierten Block; Koordinatenform bleibt erhalten.
- Timeout-Timer prüft den abgeschlossenen Aktionszustand; kein falscher `LATE_FAILURE_IGNORED` nach erfolgreichem Bau.

### Verifiziert

- Java-21-Clean-Build und **22 Unit-Tests** erfolgreich.
- Echter Start auf Paper 1.21.11 Build 132 mit FAWE 2.16.3 und Floodgate 2.2.5 Build 141.
- Migrationen V1, V2 und V3 erfolgreich; Audit-Probe und Audit-Lesung erfolgreich.
- FAWE-Selftest mit **99 Blöcken in 553 ms** und vollständiger Aktionslogfolge.
- Nach Erfolg 27 Sekunden weiterlaufen lassen: **kein verspäteter Timeout-/LATE_FAILURE-Eintrag**.
- Sauberer Shutdown ohne BlockLords-Fehler.

### Nächste Schritte

- Axon testet Kiste, Kistenmaterial, Versammlung, dorffweite Arbeiterzuweisung, Kompass und Admin-Teleport auf Paper 26.3/Java 25.
- Danach folgen 0.2.3 Schematic-/Rotationsausbau und die Kolonie-Gründungsbasis; 0.2.4 aktive Bauhilfe und 0.2.5 Bauhütte/Builder-NPC gemäß Research-Dokument.


## 2026-11-24 — 0.2.3-alpha.1, verankerte Preview und Hotbar-Werkzeugmodus

### Implementiert

- Preview-Sessions besitzen Verschieben und Rotation ohne neuen Konflikt mit der bestehenden Position; dieselbe Kollisionsprüfung gilt für Erstplatzierung und Anpassung.
- Anker standardmäßig fixiert; Hotbar-Werkzeuge für Position, Rotation links/rechts, Anker, Bestätigen und Abbrechen.
- **Migration V4** persistiert Originalinventar, Rüstung, Offhand/Extras, ausgewählten Slot, Dorf und Bauplan.
- ItemStack-NBT-Serialisierung statt Eigenformat; Snapshot wird vor der ersten Werkzeug-Ersetzung gespeichert.
- Werkzeuge besitzen PDC-Erkennung; Event-Sperren für Drop, Klick, Drag, Offhand-Swap, Container und Entity-Interaktionen.
- Idempotente Wiederherstellung durch Ersetzen statt Addieren; Abbruchwege für Bestätigen, Abbrechen, Logout, Join, Tod, Weltwechsel, Reload und Serverstopp.
- Logout-Race während Snapshot-Speicherung schließt die Session, ohne offline Werkzeuge zu installieren.
- Alle Platzierungsaktionen bleiben über das Formular-/Menü-Fallback erreichbar.
- `/bl admin itemtest` prüft den NBT-Roundtrip auf dem laufenden Server.

### Verifiziert

- Java-21-Clean-Build und **23 Unit-Tests** erfolgreich.
- Echter Start auf Paper 1.21.11 Build 132 mit FAWE 2.16.3 und Floodgate 2.2.5 Build 141.
- Migrationen V1–V4 erfolgreich.
- ItemStack-NBT-Roundtrip erfolgreich (169 Bytes).
- FAWE-Selftest mit **99 Blöcken in 598 ms**; 27 Sekunden danach kein `LATE_FAILURE_IGNORED`.
- Koordinatenloser Konsolen-Selftest wird verständlich als Spielerfunktion abgelehnt.
- Sauberer Shutdown ohne BlockLords-Fehler.

### Nächster Test durch Axon

- Hotbar-Werkzeuge, Anker, Rotation und Verschieben mit echtem Spieler.
- Wiederherstellung nach Bestätigen, Abbrechen, Logout/Join, Tod, Weltwechsel und Serverstopp.
- Bedrock-Formular-Fallback und Multiplayer-Verhalten.
- Danach folgen 0.2.3-alpha.2 Schematic-Loader/Herkunftsakte und die Kolonie-Gründungsbasis.


## 2026-11-25 — 0.2.4-alpha.1, Build-Flow aus #210

### Implementiert

- Kisten-GUI mit einheitlichen Glas-Füllern und **8 Bauoptionen pro Seite**; Pagination für beliebig viele Baupläne.
- Standardplatzierung **2 Blöcke vor dem Spieler** statt fester Distanz 4.
- Directional Blockdaten werden mitrotiert: Türen, Betten, Leitern und andere Directionals zeigen relativ zum Spieler korrekt.
- Vollständige Edit-Hotbar mit `x±`, `z±`, links/rechts, hoch/runter und Bestätigen/Abbrechen; Sneak steuert Anker und Abbruch.
- Bewohnerdetail mit Anforderungen, freier Bettzuweisung und Auswahl des Arbeitsgebäudes.
- Golem-/Kampfkorrektur: Schutz nur noch für Villager und dekorative Objekte statt pauschalem Stopp aller Nicht-Spieler-Kämpfe.
- Gras, hohes Gras, Farn und großer Farn werden beim Platzieren überschrieben.
- Optionaler Rotationsparameter für `/bl admin selftest`.

### Verifiziert

- Java-21-Clean-Build und **23 Unit-Tests** erfolgreich.
- Echter Start auf Paper 1.21.11 Build 132 mit FAWE 2.16.3 und Floodgate 2.2.5 Build 141.
- Migrationen V1–V4 und NBT-Roundtrip erfolgreich.
- Zwei FAWE-Selftests mit 99 Blöcken sowie zusätzlicher 90°-Rotationstest erfolgreich.
- Direkter Blockstate-Nachweis: Tür nach 90°-Rotation **`facing=east`** (`execute if block` = Test passed).
- Nach Erfolg 27 Sekunden kein `LATE_FAILURE_IGNORED`.
- Sauberer Shutdown ohne BlockLords-Fehler.

### Toolchain

- Java, Gradle, Paper, FAWE und Floodgate liegen jetzt ausführbar unter `/home/kimi/.cache/blocklords`; wiederholte Downloads werden vermieden.

### Nächster Test durch Axon

- Build-Flow, Pagination, Distanz 2, Edit-Hotbar, Bewohnerdetail, Bettzuweisung, Grasfreimachung und Golem-Kampf auf Paper 26.3/Java 25.
- Danach folgen Schematic-Loader/Herkunftsakte und die Kolonie-Gründungsbasis.


## 2026-11-25 — 0.2.5-alpha.1, P2/P3 mit Vanilla-Mechanik und Zahlenprüfung

### Recherche

- Vanilla-POIs `HOME`, `JOB_SITE`, `MEETING_POINT` und Arbeitsplatztypen bestätigt: Bauer = Komposter, Schmied = Schmiedetisch.
- Golems greifen Zombies aktiv an; pauschaler Schutz aller Nicht-Spieler-Entitäten wurde verworfen.
- MineColonies bestätigt Trennung von `Supplied %` und `Used %`.
- TekTopia/Millénaire bestätigen sichtbare Abhängigkeiten, aber keine harten NPC-Verkettungen als Zielbild.
- Zahlen geprüft: 12 Zyklen/Stunde; 64 Smaragde dauern je nach Minenbelegschaft etwa 21,3 h (1), 10,7 h (2), 5,3 h (4) oder 2,7 h (8). Ein Bauer füllt das Startlager rechnerisch erst nach 14,4 h; Offline-Deckel 4 h/50 % begrenzt passive Anhäufung. Startwerte bleiben bestehen und werden live weiter getestet.
- Vollständiger Bericht: `docs/VILLAGER_MECHANICS_RESEARCH.md`.

### Implementiert

- **Echte Job-Sites:** `MemoryKey.JOB_SITE` für Bauer und Minenarbeiter; die Mine enthält eine echte `smithing_table`.
- Bewohnerdetail mit Anforderungen, Bettzuweisung und Auswahl des Arbeitsgebäudes; Job-Site wird in der Detailansicht genannt.
- Bettbelegung über alle Y-Ebenen korrigiert: X/Y/Z-Schlüssel statt nur X/Z.
- P3-Kurztexte: Aktion → kurze Bestätigung → nächster Schritt.
- P2-Grasregel bleibt auf `SHORT_GRASS`, `TALL_GRASS`, `FERN`, `LARGE_FERN` begrenzt.

### Verifiziert

- Java-21-Clean-Build und **23 Unit-Tests** erfolgreich.
- Echter Start auf Paper 1.21.11 Build 132 mit FAWE 2.16.3 und Floodgate 2.2.5 Build 141.
- FAWE-Selftest `MINE` mit 90°-Rotation: **102 Blöcke**, Schmiedetisch-Nachweis `execute if block ... minecraft:smithing_table` = Test passed.
- NBT-Roundtrip und sauberer Shutdown ohne BlockLords-Fehler.

### Nächster Test durch Axon

- Bewohnerdetail, Bettzuweisung, Arbeitsgebäudewahl, echte Job-Site-Verknüpfung und P3-Kurztexte auf Paper 26.3/Java 25.
- Danach folgen Schematic-Loader/Herkunftsakte und die Kolonie-Gründungsbasis.


## 2026-11-25 — 0.2.6-alpha.1, Hotbar-Menüführung und Bauplanreparatur

### Implementiert

- Glockenaktion „Bauen & Baustellen“ öffnet jetzt ein **Hotbar-Root-Menü**: Bauen, Baustellen, Bewohner, Vorschau sowie Zurück/Verlassen.
- „Bauen“ zeigt Baupläne in der Hotbar, **8 pro Seite** mit Weiter-Pfeil. Offhand-Aktion geht stufenweise zurück und beendet am Root.
- Gesperrte Baupläne bleiben sichtbar und nennen die erforderliche Dorf-Level-Freischaltung im Itemtext.
- Auswahl startet die Preview **2 Blöcke vor dem Spieler** und wechselt direkt in den Edit-Modus; keine automatische Platzierung über der Glocke.
- Menübegriff **Bewohner** statt Arbeiter; Bewohner erhalten Namen aus `ResidentNames` plus fortlaufende `#Nummer`, persistent in Migration V5.
- Bauernhaus: Feld liegt außerhalb der Hütte, ein vollständiges Bett im Inneren. Türen besitzen echte `half=bottom/top`; Betten echte `part=foot/head`.
- Directional Blockdaten bleiben bei Rotation korrekt.
- Verifier-Ordner mit Abnahmekriterien, Build-/Serverläufen und reproduzierbarem Checkskript.

### Verifiziert

- Java-21-Clean-Build und **28 Unit-Tests** erfolgreich.
- Echter Start auf Paper 1.21.11 Build 132 mit FAWE 2.16.3 und Floodgate 2.2.5 Build 141.
- Migrationen V1–V5 erfolgreich.
- FARM: **119 Blöcke**, MINE mit 90°-Rotation: **102 Blöcke**.
- Direkte Blockstate-Nachweise bestanden: Tür `half=bottom/top`, Bett `part=foot/head`, Feld außerhalb, Schmiedetisch vorhanden.
- 27 Sekunden nach Erfolg kein `LATE_FAILURE_IGNORED`.
- Sauberer Shutdown ohne BlockLords-Fehler.

### Nächster Test durch Axon

- Hotbar-Root, Bauplan-Pagination, Zurück/Verlassen, sofortiger Edit-Modus, Namen/#Nummern, Feld, Türen und Betten auf Paper 26.3/Java 25.
- Danach folgen Schematic-Loader/Herkunftsakte und die Kolonie-Gründungsbasis.


## 2026-11-25 — 0.2.7-alpha.1, Live-Feedback P0/P1 aus #230

### Implementiert

- **P0 Bewohnernummern:** Ein dorfspezifischer Identity-Allocator reserviert Nummern und Namen bereits bei der Vergabe. Mehrere neue Villager im selben Scan erhalten dadurch aufsteigende Nummern; spätere Scans und Neustarts setzen nach dem gespeicherten Maximum fort.
- **P0 Anzeige und Verlustverwaltung:** Kein doppeltes „Bewohner Bewohner“ mehr. Verstorbene Villager werden persistent als verloren markiert, aus freien Bewohnern und Arbeitszuweisungen entfernt, in einem eigenen Menübereich angezeigt und können gemeinsam gelöscht werden.
- **Migration V6:** `residents.dead` sowie nullable Kistenkoordinaten an `build_sites` und `buildings`. Alte Datensätze behalten den bisherigen rotierten Kisten-Fallback; V1–V5 bleiben unverändert.
- **Baustellenkiste:** Die freie Fußposition beim Preview-Confirm wird gegen Claim, Belegung und Gebäudefootprint geprüft, gespeichert und beim Gebäudeabschluss übernommen. Kistenmenüs, Restmaterial und FAWE-Ausschluss verwenden dieselbe Position.
- **Menüzugang:** `/bl` öffnet für Spieler direkt das Dorfmenü; ein Kompass-Eintrag stellt das Menü-Item ins Inventar.
- **Quick-Wins:** Zurück/Verlassen nur noch in der Hotbar, Level-Item mit aktuellem Stand/fehlenden XP/nächster Freischaltung und kurze Erklärung der Arbeiterzuweisung.
- **Tests:** FAWE 2.16.3 steht auch im Testklassenpfad; das offizielle EngineHub-Repository ergänzt dessen eigene Laufzeitabhängigkeiten.

### Verifiziert

- Java-21-Clean-Build und **35 Tests ohne Fehler, Fehlerfälle oder Überspringungen** erfolgreich.
- `ResidentServiceTest`: Drei gleichzeitig versammelte Villager erhalten exakt **#1/#2/#3**.
- `ConstructionServiceTest`: Preview-Confirm erzeugt die Kiste an der Spielerposition und persistiert sie; Position im Gebäude wird vor dem DB-Insert abgelehnt.
- `MigrationUpgradeTest`: Echte V1–V5-Datenbank wird auf V6 angehoben; Bewohner und alte Baustellen/Gebäude mit Legacy-Fallback bleiben lesbar.
- Echter Start auf Paper 1.21.11 Build 132 mit FAWE 2.16.3; Migrationen V1–V6 und Aktivierung erfolgreich.
- FAWE-Selftest RATHAUS: **99 Blöcke**, Aktions-ID `PASTE-ac2903ae`, `SUCCESS` nach 7.891 ms.
- `/bl admin list`, `/bl admin backup`, RCON und TPS geprüft. Nach Erfolg deutlich mehr als 27 Sekunden kein `LATE_FAILURE_IGNORED` oder `LATE_SUCCESS_IGNORED`.
- Sauberer Shutdown ohne BlockLords-Fehler. Abnahmekriterien und Laufprotokolle liegen in `verifier/v2/` und `verifier/runs/`.

### Nächster Schritt

- Produktivdeployment durch den Coding-/Deploy-Agenten mit Datenbank-/JAR-Backup und Rollback-Pfad; anschließend Konsolen- und spielerische Live-Abnahme von #230.


## 2026-11-25 — 0.2.7-alpha.2, Live-Hotfix aus Chat und Screenshots

### Implementiert

- **Materialtransfer repariert:** `contribute` und `fund` lesen die echte Baustellenkiste ausschließlich im Hauptthread. DB-Executors erhalten bereits erfasste Kistenzahlen; eine Guard verhindert künftige asynchrone Kistenzugriffe.
- **Eingangskiste:** Die Kiste entsteht einen Block vor der rotierten unteren Eingangstür des Bauplans, nicht mehr an der Spielerposition. V6-Koordinaten, Legacy-Fallback, Kistenmenüs und FAWE-Ausschluss bleiben erhalten.
- **Menüführung:** Kistenmenü schließt vor dem Hotbar-Snapshot; letzter Bauplanslot ist echte Zurück-Aktion statt funktionslosem Glasfüller. `Farm` erklärt Bauernhaus + Feld eindeutig.
- **Diagnose:** Clientfehler werden zusätzlich mit Akteur als Warning im Serverlog geschrieben.
- **Bewohner:** Alte `#0`-Werte, fehlende Namen und doppelte Nummern werden beim Laden repariert. Versammlung und Einzelaktion teleportieren auf sichere Plätze neben der Glocke. Entfernen aus dem Register löscht POI-/Arbeitsdaten ohne Villager-Tod; Berechtigungen und Audit sind abgesichert.

### Verifiziert

- Java-21-Clean-Build und **40 Tests ohne Fehler, Fehlerfälle oder Überspringungen** erfolgreich.
- `ConstructionServiceTest`: Inventar- und Dorfvorratstransfer erzeugen reale Kistenitems und `READY`; Eingangsposition wird bei 0° und 90° geprüft, blockierte Eingänge werden vor dem DB-Insert abgelehnt.
- `ResidentServiceTest`: Drei Villager erhalten #1/#2/#3 und sichere Glockenplätze; Entfernen lässt den Villager gültig und lebendig und löscht HOME/MEETING_POINT.
- `ResidentIdentityAllocatorTest`: `#0`, fehlende Namen und Duplikate werden eindeutig repariert.
- JAR-SHA256: `fd5eb93bc21fee8542c2af29e65f53e96ec8f2fd502d893ad5ea1ad757f2b059`.
- Echter Paper-1.21.11-Start mit Java 21 und FAWE 2.16.3: BlockLords 0.2.7-alpha.2 aktiviert; Selftest RATHAUS mit `PASTE-30bb5331` und **99 Blöcken** erfolgreich.
- Abnahmekriterien und Laufprotokolle liegen in `verifier/v3/` und `verifier/runs/`.


## 2026-11-25 — 0.2.7-alpha.3, Schutz, Dorfinfos und Arbeiterlimit

### Implementiert

- **Unzerstörbare Baustellenkisten:** Abbau, Explosionen, Feuer und Kolben werden verhindert. Bewusster Abbruch über `/bl build cancel <ID>` oder das Menü mit vollständiger Itemrückgabe und Audit statt versehentlichem Blockverlust.
- **Kiste und Komposter:** Kiste innen bei `(2, 1, 3)`, Komposter seitlich bei `(3, 1, 2)`. Job-Site und Anzeige verwenden dieselbe rotierte Bauplanposition; V6-Koordinaten und alte Positionen bleiben gültig.
- **Arbeiterlimit:** `workers.per-building` mit Default 2, auch wenn alte Configs den Schlüssel noch nicht enthalten. Dritte Zuweisung wird verweigert, bestehende Bestände bleiben erhalten. UI zeigt Belegung und freie Plätze `x/2`.
- **Dorf Infos:** Mehrzeiliges Menü für Dorfname, Landherr, Level/XP/Freischaltung, Bewohner, Arbeiter/freie Plätze, Betten/Obdachlose, Vorrat und XP-Quellen.
- **XP-Feedback:** Bauabschluss meldet Zuwachs, Gesamt-XP und Level; Level-up zeigt Titel und Sound.

### Verifiziert

- Java-21-Clean-Build und **47 Tests ohne Fehler, Fehlerfälle oder Überspringungen** erfolgreich.
- `ConstructionServiceTest`: Kiste innen bei 0°/90°, Kistenschutz, Abbruch mit kompletter Itemrückgabe und rotierte Job-Site.
- `WorkerServiceTest`: zwei Zuweisungen erlaubt, dritte abgelehnt; bestehende Zuweisungen über dem Limit bleiben erhalten.
- `BlockLordsConfigTest`: fehlender Limit-Schlüssel verwendet 2, ungültige Werte werden abgelehnt.
- JAR-SHA256: `4b80c3c96d95a2e5b9acb629e32d81d8ccc18092d31fe33f57f64d941a9dd4dc`.
- Echter Paper-1.21.11-Start mit Java 21 und FAWE 2.16.3: BlockLords 0.2.7-alpha.3 aktiviert; Selftest RATHAUS mit `PASTE-024a7dfa` und **99 Blöcken** erfolgreich.
- Abnahmekriterien und Laufprotokolle liegen in `verifier/v4/` und `verifier/runs/`.


## 2026-11-26 — 0.2.7-alpha.4, Lager und Dorfinfos

### Implementiert

- **Lagermeldungen:** Voller Vorrat und leere Entnahme melden Fehler mit Lösung statt Erfolg `0`; betroffene Items werden vollständig erstattet.
- **Direkte Lagerbedienung:** Ressourcen mit Links 64 einlagern und Rechts 64 entnehmen. Eigene Items mit Links als Stack oder Rechts einzeln einlagern. Bestand und beide Aktionen stehen im Itemtext; nur die geöffnete Vorratsansicht wird nach der Transaktion aktualisiert.
- **Bettscan:** Fehlende oder alte Vermessung zeigt „Vermessung läuft …“ und öffnet nach Abschluss automatisch die aktualisierte Dorfinfo mit echter Zahl statt `0`.
- **Arbeiter und Baustelle:** Arbeiter-Items öffnen wieder das Bewohnerdetail statt sofort die Zuweisung zu lösen. „Schließen“ an der Baustellenkiste schließt nur das Fenster.

### Verifiziert

- Java-21-Clean-Build und **52 Tests ohne Fehler, Fehlerfälle oder Überspringungen** erfolgreich.
- `StorageServiceTest`: voller Vorrat erstattet und meldet Fehler, leere Entnahme meldet Fehler, normale Ein-/Auslagerung funktioniert.
- `UiActionTest`: Standardaktion und getrennte Rechtsklick-Aktion funktionieren.
- JAR-SHA256: `ab76d0b181bca16bd1199314f28687a214ed24c1550d3246815d542da59502a8`.
- Echter Paper-1.21.11-Start mit Java 21 und FAWE 2.16.3: BlockLords 0.2.7-alpha.4 aktiviert; Selftest RATHAUS mit `PASTE-8f27b841` und **99 Blöcken** erfolgreich.
- Abnahmekriterien und Laufprotokolle liegen in `verifier/v5/` und `verifier/runs/`.


## 2026-11-26 — 0.2.7-alpha.5, Kapazität pro Ressource

### Implementiert

- **Limit pro Ressource:** `deposit` berechnet freien Platz aus dem Bestand derselben Ressource statt aus dem Gesamtbestand aller Ressourcen.
- **Produktion:** `applyProduction` begrenzt jede produzierte Ressource separat; Nahrungspflege beeinflusst keine fremden Kapazitäten.
- **Anzeige:** Der Vorrat erklärt das Limit ausdrücklich als „Limit pro Ressource“, während der Gesamtbestand nur Information bleibt.
- Volle Ressourcen blockieren andere Ressourcen nicht; Partial-Refunds, Audit und bestehende Bestände bleiben erhalten.

### Verifiziert

- Java-21-Clean-Build und **52 Tests ohne Fehler, Fehlerfälle oder Überspringungen** erfolgreich.
- `StorageRepositoryTest`: getrennte Einlagerungs- und Produktionskapazitäten, volle Ressource A blockiert Ressource B nicht.
- Voller/leerer/normaler Lagerpfad und Links-/Rechtsklick-Mapping bleiben bestehen.
- JAR-SHA256: `c53af908faaeb6e1e8ca7ba5aa0ea195654572362914419f86e7682cb28986d0`.
- Echter Paper-1.21.11-Start mit Java 21 und FAWE 2.16.3: BlockLords 0.2.7-alpha.5 aktiviert; Selftest RATHAUS mit `PASTE-6f5e5311` und **99 Blöcken** erfolgreich.
- Abnahmekriterien und Laufprotokolle liegen in `verifier/v6/` und `verifier/runs/`.


## 2026-11-26 — 0.2.8-alpha.1, Missionen und nächste Schritte

### Implementiert

- **Drei klare Angebote:** Zwei persönliche Missionen und eine gemeinsame Dorfmission über Dorfmenü, `/bl missions`, Chest-GUI und die vorhandenen Bedrock-Formulare. Annehmen, Details, kostenloses Ersetzen mit Fortschrittswarnung und persönliches Anpinnen.
- **Vier Startmissionen:** 32 Holz oder Stein einlagern, 32 benötigte Ressourcen an Baustellen liefern und gemeinsam 120 Nahrung einlagern. Zielwerte und XP sind validiert konfigurierbar.
- **Echte Fortschritte:** Nur tatsächlich akzeptierte Einlagerungen nach Missionsannahme und reale Baustellenbeiträge aus Inventar oder Dorfvorrat zählen. Entnahmen, Produktion, Erstattungen und nicht angenommene Items erzeugen keinen Fortschritt.
- **Einmalige Dorf-XP:** Persönliche Erstabschlüsse 20 XP, erste Dorfmission 40 XP. Eindeutige Ereignis- und XP-Ledger verhindern Doppelzählung und Doppelbelohnung; Wiederholungen geben 0 XP und melden dies korrekt. Keine zusätzlichen Smaragd-/Itembelohnungen.
- **Ohne Termindruck:** Angenommene oder angepinnte Missionen verfallen nicht. Unangenommene Angebote können nach konfigurierbarer Frist beim Öffnen wechseln. Ein angepinnter nächster Schritt erscheint als einzelne kurze Zeile beim Login.
- **Migration V7:** Missionen, Ereignisse, Fortschrittsverknüpfungen und Erst-XP-Zuordnungen sind persistent. Missionsabschluss und XP erfolgen atomar in SQLite; alte Dörfer erhalten keine rückwirkenden Abschlüsse oder XP.
- Mitgliedschafts- und Dorfprüfung, Audit-Einträge und gemeinsame `UiAction`-Menüs für Java/Bedrock; keine neue Abhängigkeit oder Java-only-Bedienung.

### Verifiziert

- Java-21-Clean-Build und **65 Tests ohne Fehler, Fehlerfälle oder Überspringungen** erfolgreich; `git diff --check` bestanden.
- `MigrationUpgradeTest`: echte V6-Datenbank mit Lager und vorhandenen XP auf V7; Bestände bleiben erhalten, keine Altbelohnung. `MissionRepositoryTest`: Angebotslebenszyklus, Teilbeträge, Rechte, Mehrspieler, Lager-/Baustellenfortschritt, Idempotenz und Erst-/Wiederholungs-XP.
- `MissionServiceTest`: Spieler öffnet Missionsmenü, nimmt Holzmission an, lagert in Teilbeträgen ein, erhält einmalige XP-Meldung und bei Wiederholung korrekt 0 XP; Ersetzen verlangt Bestätigung und erhält Ressourcen; Pin-/Login-Hinweis und Fremdenablehnung geprüft.
- Finales JAR-SHA256: `043f1328f61fb9c77aaae31b97709099c329670cf843dd4d75f74df343562220`. Alle 356 Laufzeit-Einträge sind per CRC mit dem zuvor auf Paper getesteten JAR identisch.
- Echter Paper-1.21.11-Start mit Java 21 und FAWE 2.16.3: V7 aktiv, Missionsbefehl registriert, Selftest RATHAUS mit `PASTE-00a04a1b` und **99 Blöcken** erfolgreich. Nach mehr als 27 Sekunden kein `LATE_FAILURE`/`LATE_SUCCESS`; BlockLords und FAWE sauber beendet.
- Abnahmekriterien und Laufprotokolle liegen in `verifier/v7/` und `verifier/runs/`.

### Produktivdeployment und offene Live-Abnahme

- GitHub-CI `45821784009` erfolgreich; Release `v0.2.8-alpha.1` und öffentlicher Download hashgeprüft.
- alpha.5-JAR, Config und SQLite gesichert; konsistenter Pre-Migration-Snapshot mit Integrität `ok`, zwei Dörfern und Schema V6. Nach dem Deployment bestätigt ein weiterer Snapshot Integrität `ok`, zwei Dörfer und Schema V7.
- Auf Paper 26.3/Java 25 ist ausschließlich BlockLords 0.2.8-alpha.1 aktiv; die zunächst doppelt erkannte alpha.5-JAR wurde hashgeprüft in den Backup-Namen verschoben und der Server erneut sauber gestartet. Beide Dörfer bleiben Level 2 und aktiv, TPS 20,0/20,0/20,0, kein BlockLords-Fehler im finalen Startlog.
- Alexander hat die Abnahme anschließend mit **„Top“** bestätigt: Missionsannahme, STEIN_32 und HOLZ_32 mit je +20 Dorf-XP, gemeinsame NAHRUNG_120 mit +40 Dorf-XP, Banner, Sound und Wiederholung ohne weitere XP funktionieren. Kisten funktionieren durchgehend. Ein zweiter menschlicher Spieler war bei der Dorfmission nicht beteiligt; Mehrspielerbeiträge sind durch automatisierte Tests abgesichert. **Bus #233 ist abgenommen und geschlossen.**


## 2026-11-26 — 0.2.8-alpha.2, Missions- und Lager-UX

### Implementiert

- **Aktiv-Markierung:** Angenommene Missionen erhalten Glint über `setEnchantmentGlintOverride(true)` und den Klartext „Aktiv“. Angebote bleiben unverändert; der Zustand ist auf Java und Bedrock verständlich.
- **Normale Inventarbedienung:** Im Lager sind gewöhnliche untere Pickup-/Cursor-Aktionen und reine Bottom-Drags wieder erlaubt. Rechtsklick kann Stacks normal teilen, ohne ungewollt einzulagern.
- **Gezielte Einlagerung:** Shift+Links auf ein eigenes Item lagert dessen Stack ein, Shift+Rechts genau ein Item. Obere Vorrat-Items behalten Links 64 einlagern und Rechts 64 entnehmen.
- **Schutz:** Doppelklicks, Top-Manipulation und Drags mit oberen Slots bleiben blockiert. Keine Geisteritems oder Duplikate; keine Änderung an XP, Missionen, Wirtschaft oder Schema.

### Verifiziert

- Java-21-Clean-Build und **70 Tests ohne Fehler, Fehlerfälle oder Überspringungen** erfolgreich; `git diff --check` bestanden.
- `MenuListenerTest`: Bottom-Rechtsklick/Stack teilen, Shift+Links Stack und Shift+Rechts Einzelitem, obere Vorrat-Aktionen, Bottom-Drag sowie Doppelklick-/Top-Drag-Schutz.
- `MissionServiceTest`: Aktiv-Text und `enchantment_glint_override=true` am tatsächlichen Missions-Item; `UiActionTest`: Glint ist nur opt-in.
- JAR-SHA256: `f598404752077afdb211516c06ad9b644e999ca17da3863b8f367770e815f04a`.
- Echter Paper-1.21.11-/FAWE-2.16.3-Start mit bestehender V7-Datenbank: alpha.2 aktiv, Missionsbefehl registriert, `PASTE-3a945541` mit **99 Blöcken** erfolgreich, keine späten Fehler, sauberer Shutdown.
- Abnahmekriterien und Laufprotokolle liegen in `verifier/v8/` und `verifier/runs/`.

### Produktivdeployment und offene Abnahme

- GitHub-CI `45832855800` erfolgreich; Release `v0.2.8-alpha.2`, Download und Produktiv-JAR hashgeprüft.
- alpha.1-JAR, Config und konsistenter Pre-Migration-Snapshot mit Integrität `ok`, zwei Dörfern und Schema V7 gesichert. Post-Migration-Snapshot bestätigt ebenfalls Integrität `ok`, zwei Dörfer und Schema V7.
- Auf Paper 26.3/Java 25 ist ausschließlich BlockLords 0.2.8-alpha.2 aktiv; beide Dörfer bleiben erhalten, Silberbrück ist nach den Missions-XP Level 3, Nebelbrück Level 2. TPS etwa 20, kein BlockLords-Fehler im finalen Startlog.
- Alexander hat den UX-Hotfix anschließend mit **„Stark! Anpassungen sind da!“** bestätigt: Aktiv-Glint/-Text und die normale Inventarbedienung im Lager sind live abgenommen. **Bus #234 ist geschlossen.** Der nächste Phase-2-Schritt ist 0.2.9 mit Einwanderung und Produktionsbilanz.


## 2026-11-26 — 0.2.9-alpha.1, Einwanderung und Produktionsbilanz

### Implementiert

- **Einwanderung ab Level 2:** Freies physisches Bett, 16 Nahrung und freier Arbeitsplatz werden vor Ankunft und erneut vor Aufnahme geprüft. Der sichtbare Wartezyklus bleibt persistent. Erwachsener Wanderer an sicherem Glockenplatz, ohne Änderung von Spielerblöcken oder Abhängigkeit vom NPC-Pathfinding.
- **Bewusste Entscheidung:** `/bl immigration` und gemeinsame Java-/Bedrock-Menüs zeigen Wartezeit, fehlende Gründe und Kandidaten. Landherr/Gefolgsmann kann aufnehmen oder ablehnen; Ablehnung entfernt ohne Töten und ohne Kosten.
- **Atomare Aufnahme:** Nahrungkosten, Kandidatenstatus, bestätigter Bewohner mit Bett-POI und XP werden in einer SQLite-Transaktion gebucht. Doppelklick, Retry und Reload erzeugen keine Duplikate oder Zusatzkosten. Erste Aufnahme +30, weitere +5 Dorf-XP.
- **Persistente Lebenszyklen:** Wartezeit, Kandidaten-UUID, Entscheidung und Retry bleiben nach Neustart erhalten. Nachweislicher Tod wird von ungeladenen Chunks unterschieden und erzeugt weder Kosten noch einen zweiten gleichzeitigen Kandidaten.
- **Bewohneridentitäten:** Eindeutiger Name/#Nummer, Bett und Glocken-Meeting-Point. Bewohnernummern werden in derselben Transaktion aus dem Dorfmaximum +1 vergeben; Register und Identity-Allocator werden nach Aufnahme aktualisiert, sodass normale spätere Villager die nächste freie Nummer erhalten.
- **Produktionsbilanz:** `/bl production` zeigt echte geplante und akzeptierte Erträge je Ressource, Nahrungsverbrauch, Arbeiterzahlen, Bestand/Limit und Pausengründe. Alte Textberichte bleiben als Fallback erhalten.
- **Zeitgrenzen:** Offline-Gutschrift nur innerhalb von vier Stunden mit 50 %, danach keine weiteren Erträge. `RESTING`/`REVIEW` stoppt Produktion und Einwanderung ohne Ruhe-Nachberechnung.
- **Kapazität nach Verbrauch:** Nahrungsverbrauch wird vor der Prüfung neuer Nahrung auf das Lagerlimit berücksichtigt; volle Ressourcen pausieren nur ihre eigene Produktionslinie.
- **Migration V8:** Einwanderungszustände, Kandidaten und strukturierte Produktionsberichte. Keine Alt-Kandidaten, Alt-XP oder erfundenen historischen Bilanzen.

### Verifikation

- **86 bestandene Tests ohne Fehler oder Überspringungen**, darunter V7→V8-Migration, atomare Aufnahme, Tod/Retry, Identitätsvergabe #1→#2, Offline-Grenze, Ruhezustand und volle Ressourcen. Nach einem intermittierenden CI-Fehler wartet der Tod/Retry-Test nach Ablehnung sowohl auf den Datenbankstatus als auch auf die Entity-Entfernung im Hauptthread; die Produktivlogik blieb unverändert.
- Erneuter Java-21-Clean-Build erfolgreich in 1m 44s: **86 Tests, 0 Failures, 0 Errors, 0 Skipped**; `git diff --check` bestanden. Finales JAR-SHA256: `cb6382a1dbd659376f3c650e3e24902448d48a43e694dc243d9c362a7096734d`. Alle 372 Archiveinträge sind per CRC und Größe identisch mit dem zuvor auf Paper geprüften JAR; lediglich ZIP-Zeitstempel unterscheiden sich.
- Echter Paper-1.21.11-/FAWE-2.16.3-Lauf: V8 migriert, `/bl immigration` und `/bl production` registriert, alle vier V8-Tabellen per SQLite nachgewiesen. Stabiler Selftest `PASTE-40a3e510` mit **99 Blöcken in 523 ms** und sauberem Shutdown. Ein erster Lauf unter Starthauptlast überschritt das 20-Sekunden-Timeout und wurde nachfolgend im Log erfasst; im stabilen Betrieb gab es keinen späten Fehler mehr. Details liegen in `verifier/runs/2026-11-26-v9-server.log`.

### Produktivdeployment und offene Live-Abnahme

- GitHub-CI `45905799169` für Commit `12902d0` erfolgreich; Prerelease `v0.2.9-alpha.1`, öffentlicher Download und Produktiv-JAR hashgeprüft (`cb6382a1…734d`).
- alpha.2-JAR, Config und konsistente Pre-Migration-Snapshots gesichert; unmittelbar vor dem Wechsel Integrität `ok`, zwei Dörfer und Schema V7 bestätigt. Post-Migration-Snapshot `postdeploy-v029-verified.db` bestätigt Integrität `ok`, zwei Dörfer und Schema V8.
- Auf Paper 26.3/Java 25 wird ausschließlich BlockLords 0.2.9-alpha.1 geladen; V8 migriert, beide Dörfer aktiv, Silberbrück Level 3 und Nebelbrück Level 2. `/bl immigration` und `/bl production` registriert, TPS 20,0/20,0/20,0 und keine BlockLords-Warnungen oder -Fehler im Startlog. Details liegen in `verifier/runs/2026-11-26-v9-production.log`.
- Die visuelle Einwanderungs- und Produktionsabnahme durch Alexander steht noch aus und wird in **Bus #235** geführt.


## 2026-11-27 — 0.2.9-alpha.2, Retry- und Produktions-UX

### Live-Befund aus alpha.1

- Der kontrollierte Neustart mit zwei wartenden Wanderern ist bestanden: Silberbrück #3 und Nebelbrück #2 behielten Kandidaten-ID und Entity-UUID; beide reale NPCs wurden nach dem Neustart per `data get entity` nachgewiesen. Pro Dorf wartet weiterhin höchstens ein Kandidat.
- Die Ablehnung von Silberbrück #3 entfernte den NPC ohne Tod und ohne Kosten. Nahrung blieb 1728, Silberbrück blieb Level 4 mit 500 XP. Audit `b1a58090` bestätigt `IMMIGRATION_REJECTED`.
- Der Livetest fand einen echten Retry-Fehler: Ein regulärer asynchroner `runVillage`-Zyklus überschrieb den 60-Sekunden-Retry rund acht Sekunden später mit `Wanderer erwartet` und rund 300 Sekunden. Ursache war der getrennte Schreibzugriff auf Kandidatenentscheidung und Folgezustand.
- Screenshots aus `/bl production` zeigten außerdem eine echte UX-Schwäche: `308/768` bedeutet eingelagert/geplant, während `1728/1728` der tatsächliche Bestand ist. Der Pausentext `Lager voll (308/768)` vermischte diese Bedeutungen. Produktions- und Lagerlogik selbst bleiben unverändert.

### Implementiert

- **Atomare Lebenszyklen:** Registrierung, Ablehnung, Tod und Aufnahme schreiben Kandidatenstatus und Folgezustand nun jeweils in derselben SQLite-Transaktion. Kein später Ankunfts-Callback setzt eine bereits erfolgte Entscheidung mehr auf `nextArrivalAt = 0` zurück; die separaten nachträglichen `setState`-Aufrufe entfallen. Der Regressionstest erzwingt eine Ablehnung nach dem Kandidaten-Commit und vor einem möglichen alten Ankunfts-Callback.
- **Verständliche Ressourcenwerte:** `/bl production` unterscheidet geplant, eingelagert, nicht eingelagert und echten Bestand/Limit. Vollständige, teilweise und fehlende Einlagerung werden korrekt bezeichnet.
- **Eindeutige Kapazitätsgründe:** Neue Texte nennen die nicht eingelagerte Menge, beispielsweise `460 von 768 nicht eingelagert – Lager voll`. Auch alte strukturierte V8-Pausentexte werden bei der Anzeige aus den gespeicherten Zahlen korrekt abgeleitet; keine Migration und keine Änderung historischer Daten.
- **Nahrungs-Netto:** Ein eigener Bilanzeintrag zeigt eingelagerte Nahrung, Arbeiterverbrauch und Netto-Veränderung mit Vorzeichen. Das Referenzbeispiel `308 - 192` ergibt `+116`.
- **Konsolenspiegelung:** `sendJoinReport` sendet denselben Bericht an Spieler und Plugin-Log mit Spieler-/Dorfbezug. Keine zweite Produktionsberechnung; kein Log ohne Bericht oder nach einem Logout vor dem Callback.
- Version **0.2.9-alpha.2**, weiterhin Schema V8 und dieselben Java-/Bedrock-Aktionsmenüs. Keine neuen Abhängigkeiten oder Spielregeländerungen.

### Verifikation

- Gezielte Einwanderungs-Repository-/Service-Tests erfolgreich, einschließlich atomarem Snapshot, Retry-Erhalt während `runVillage`, Kosten-, Tod-, Identitäts- und Rollback-Fällen.
- Gezielte Produktionstests erfolgreich: vollständige/teilweise/fehlende Einlagerung und alter V8-Pausentext am tatsächlich geöffneten MockBukkit-Inventar; Nahrungsbilanz `+116`; identischer Spieler-/Konsolentext und kein Log ohne Bericht.
- Vollständiger Java-21-Clean-Build erfolgreich in 6m 58s: **89 Tests, 0 Failures, 0 Errors, 0 Skipped**; `git diff --check` bestanden. JAR-SHA256: `31969bdfc1c61d8efea89281aff2b9b3d30f2efd70023bc57f00d058d05b9d65`.
- Echter Paper-1.21.11-/FAWE-2.16.3-Lauf mit bestehendem V8-Stand: alpha.2 aktiviert, `/bl immigration` und `/bl production` separat per RCON bestätigt. Selftest `PASTE-f036c769` mit **99 Blöcken in 5830 ms**, mehr als 27 Sekunden ohne späten Fehler und sauberer Shutdown mit Exitcode 0. Details in `verifier/runs/2026-11-27-v10-server.log`.
### Release und Produktivdeployment

- GitHub-CI `46067863420` für Commit `ca1ba60` erfolgreich; Prerelease `v0.2.9-alpha.2`, öffentlicher Download und Produktiv-JAR hashgeprüft (`31969bdf…9d65`).
- alpha.1-JAR, Config und konsistenter Pre-Deploy-Snapshot gesichert (`ok`, zwei Dörfer, V8). `postdeploy-v0292-verified.db` bestätigt ebenfalls Integrität `ok`, zwei Dörfer und Schema V8.
- Auf Paper 26.3/Java 25 wird ausschließlich 0.2.9-alpha.2 geladen; Silberbrück Level 4 und Nebelbrück Level 2 bleiben aktiv. Beide wartenden Wanderer #4/#2 behalten ihre UUIDs und werden als reale NPCs gefunden. TPS nach dem Start 18,6/19,0/19,0; keine unerwarteten BlockLords-Warnungen oder Fehler. Details in `verifier/runs/2026-11-27-v10-production.log`.
- Der echte Reconnect erzeugt `DORFBERICHT | HazeHunter | Dorfbericht 'Silberbrück' …` und verwendet dabei bereits die klaren Kapazitätstexte `6 von 8` beziehungsweise `12 von 12` nicht eingelagert. Spieler- und Konsolentext sind damit auch live nachgewiesen.
- Weiterhin offen in **#235**: Retry nach Ablehnung von #4, anschließende +5-XP-Aufnahme, Doppelklick/Arbeitsplatz sowie die visuelle Java-/Bedrock-Bewertung der Produktionsbilanz.
