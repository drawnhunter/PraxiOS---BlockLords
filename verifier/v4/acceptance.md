# Acceptance v4 — BlockLords 0.2.7-alpha.3

## Kistenschutz und Abbruch
- Aktive und fertige Baustellenkisten können durch Spieler, Explosionen, Feuer oder Kolben nicht zerstört/verschoben werden. Abbauversuche nennen Menü/Befehl für den bewussten Abbruch.
- `/bl build cancel <ID>` und der Menübutton funktionieren nur mit Landherr/Gefolgsmann und nur vor Baubeginn. Alle echten Kistenitems gehen an den Spieler, Überlauf droppt neben ihm, kein Itemverlust. DB, Kiste und Cache werden konsistent entfernt und auditiert.

## Platzierung und Arbeitsplätze
- Neue Kiste liegt innen bei `(2, 1, 3)` mit korrekter Rotation und V6-Persistenz. Alte gespeicherte Positionen bleiben erhalten.
- Komposter liegt bei `(3, 1, 2)`; Tür, Betten, Lagerkisten und Arbeitsblöcke kollidieren nicht. Job-Site und Anzeige verwenden dieselbe rotierte Position.
- `workers.per-building` fehlt in alten Configs nicht mehr: Default 2, ungültige Werte werden abgelehnt. Dritte Zuweisung wird verweigert, Bestand über dem Limit bleibt erhalten. UI zeigt Belegung und freie Plätze `x/2`.

## Dorfinfos und XP
- „Dorf Infos“ zeigt Name, Landherr, Level/XP/Freischaltung, Bewohner, Arbeiter/freie Plätze, Betten/Obdachlose und Vorrat in einem mehrzeiligen Menü.
- XP-Quellen entsprechen exakt `BlueprintType.xp()` und sind sichtbar. Bauabschluss meldet XP-Zuwachs, Gesamt-XP und Level; Level-up zeigt Titel und Sound.

## Verifikation
- `git diff --check` und vollständiger Java-21-Clean-Build bestehen; Failures/Errors/Skipped = 0.
- Kistenschutz, Abbruch mit Rückgabe, Innenposition/Rotation, Job-Site, Arbeiterlimit und Config-Kompatibilität sind automatisiert geprüft.
- Echter Paper-Start mit FAWE ohne BlockLords-Fehler und sauberer Shutdown; Protokolle unter `verifier/runs/`.
