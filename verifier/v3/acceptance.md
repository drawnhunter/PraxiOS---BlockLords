# Acceptance v3 — BlockLords 0.2.7-alpha.2 Live-Hotfix

## Material und Thread-Sicherheit
- `contribute` und `fund` lesen Block-Entities/Inventare ausschließlich im Hauptthread; DB-Executoren erhalten bereits erfasste Zahlen.
- Kein `Block entity is null, asynchronous access?` mehr beim Bestücken oder Reconcile. Vollständiges Material führt zuverlässig zu `READY` und ermöglicht den Baustart.
- Clientfehler erscheinen zusätzlich mit Akteur als Warning im Serverlog.

## Eingangskiste und Menüs
- Die Kiste entsteht einen Block vor der rotierten unteren Eingangstür, unabhängig von der Spielerposition. Position, Neustart und Gebäudeabschluss verwenden weiterhin V6-Koordinaten.
- Belegte Eingangspositionen werden vor dem DB-Insert verständlich abgelehnt.
- Das Kistenmenü schließt vor Hotbar-Snapshot und Installation. Der letzte Bauplanslot ist eine echte Zurück-Aktion, kein funktionsloser Füller.
- `Farm` ist im UI eindeutig als Bauernhaus + Feld erklärt.

## Bewohner
- Beim Laden werden `#0`, fehlende Namen und doppelte Nummern repariert; eindeutige Identitäten bleiben möglichst erhalten. Neue Batch-Vergaben bleiben eindeutig.
- Versammlung teleportiert geladene lebende Villager auf sichere freie Plätze neben der Glocke und setzt `MEETING_POINT`.
- Einzelteleport ist verfügbar. „Aus Register entfernen“ löscht Arbeits-/POI-/Registerdaten, lässt den Villager aber am Leben und verhindert sofortige Neuregistrierung.
- Verwaltung erfordert Landherr/Gefolgsmann und erzeugt Audit-Einträge.

## Verifikation
- `git diff --check` und vollständiger Java-21-Clean-Build bestehen; alle bisherigen und neuen Tests sind grün.
- Contribute, Fund, Eingang bei 0°/90°, blockierter Eingang, Identitätsreparatur, sichere Versammlung und Entfernen ohne Entity-Tod sind automatisiert geprüft.
- Echter Paper-Start mit V1–V6 und FAWE-Bau ohne BlockLords-Fehler sowie sauberer Shutdown.
- Laufprotokolle liegen unter `verifier/runs/`; v1/v2 bleiben unverändert.
