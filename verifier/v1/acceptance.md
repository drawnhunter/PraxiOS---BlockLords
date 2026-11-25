# Acceptance v1 — BlockLords-Reparatur

## Flow
- Aus der Glocken-Bauauswahl startet die Preview **2 Blöcke vor dem Spieler**, niemals automatisch über der Glocke.
- Nach Auswahl wird der Hotbar-Baumodus sofort aktiviert und kurze Befehlsinfo angezeigt.
- Bewegung, Rotation, Höhenänderung, Bestätigen und Abbrechen bleiben ohne Inventarverlust möglich.

## UI und Bewohner
- Menüeintrag heißt **Bewohner**, nicht Arbeiter.
- Alle registrierten Bewohner haben Namen aus der Namensliste und fortlaufende `#Nummer` statt UUID-Anzeige.
- Namen und Nummern bleiben nach Neustart erhalten.
- Die Mine erscheint im Baumenü auch bei gesperrtem Level mit klarer Anforderung.

## Baupläne
- Das Feld liegt neben dem Bauernhaus und nicht mehr im Inneren.
- Das Bauernhaus enthält mindestens ein vollständiges Bett.
- Jede Tür hat genau eine untere und eine obere Hälfte.
- Jedes Bett hat genau einen Fuß- und einen Kopfteil.
- Directional Blockdaten werden weiterhin korrekt rotiert.
- Gras/Farn-Regeln und Kollisionsschutz bleiben erhalten.

## Verifikation
- `git diff --check` ohne Fehler.
- Alle Unit-Tests bestanden.
- Echter Paper-Start mit Migrationen ohne BlockLords-Fehler.
- FAWE-Bau ohne Silent-Fail; kein verspäteter Timeout nach Erfolg.
- Tür-/Bett-Blockstates werden direkt am Server nachgewiesen.
- Verifier-Läufe werden in `verifier/runs/` protokolliert.
