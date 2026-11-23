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
