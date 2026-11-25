# Acceptance v2 — BlockLords 0.2.7 P0/P1

## Bewohner
- Mehrere neue Villager in einem Scan erhalten aufsteigende, eindeutige Nummern; spätere Scans und Neustarts setzen nach dem gespeicherten Maximum fort.
- Namen und Nummern bleiben persistent; die Anzeige enthält kein doppeltes „Bewohner Bewohner“.
- Verstorbene registrierte Villager werden als verloren markiert, aus freien Bewohnern und Arbeitszuweisungen entfernt und in einem eigenen Menübereich angezeigt.
- „Verlorene entfernen“ löscht ausschließlich Verluste aus demselben Dorf und ist auf Landherr/Gefolgsmann begrenzt. Nicht geladene Villager werden nicht automatisch gelöscht.

## Baustellenkiste
- Die Kiste entsteht an der freien Fußposition des Spielers beim Preview-Confirm, nicht am festen Offset hinter dem Gebäude.
- Die Position liegt im Claim und außerhalb des Gebäudefootprints; belegte Positionen werden vor dem DB-Insert verständlich abgelehnt.
- Kistenkoordinaten bleiben bei Vorschauänderungen, Neustart und Übergang von `build_sites` zu `buildings` eindeutig erhalten.
- Reales Inventar, Vorratstransfer, Restmaterial, `siteAtChest`, `buildingAtChest` und FAWE-Ausschluss verwenden dieselbe gespeicherte Position.
- V5-Datensätze ohne Koordinaten bleiben über den bisherigen rotierten `(4, 1, 4)`-Fallback nutzbar.

## Bedienung und Information
- `/bl` öffnet bei Spielern direkt das Dorfmenü; Konsolen erhalten Hilfe.
- Das Dorfmenü enthält einen Kompass zur Ausgabe des Menü-Items und ein Level-Item mit aktuellem Stand, fehlenden XP und nächster Freischaltung.
- Das Bewohnermenü erklärt Registrierung, Arbeiterzuweisung, Nahrung und Bett in kurzen Schritten.
- Zurück/Verlassen liegt ausschließlich in der Hotbar; die Zweithand enthält kein Verlassen-Item. Snapshot, Sperren und Wiederherstellung bleiben unverändert sicher.

## Migration und Verifikation
- V1–V5 bleiben unverändert; V6 wird auf frischen und bestehenden Datenbanken idempotent angewendet.
- `git diff --check` und `./gradlew clean build` sind erfolgreich; alle Unit-/Repository-/Upgrade-Tests bestehen.
- Echter Paper-Start mit FAWE und V1–V6 ohne BlockLords-Fehler; FAWE-Bau und Kistenverhalten werden am realen Aktionspfad geprüft.
- Laufprotokolle werden unter `verifier/runs/` abgelegt; v1 bleibt unverändert.
