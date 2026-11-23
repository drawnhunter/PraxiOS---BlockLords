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
