# Acceptance v5 — BlockLords 0.2.7-alpha.4 Lager und Dorfinfos

## Lagerlogik
- Voller Vorrat meldet Fehler statt Erfolg `0`, erstattet alle Items und nennt Lösung. Leere Entnahme meldet Fehler statt Erfolg.
- Normale Ein-/Auslagerung, Kapazität, Partial-Refunds und Audit bleiben unverändert.

## Lagerbedienung
- Ressourcen: Linksklick 64 einlagern, Rechtsklick 64 entnehmen; Bestand und beide Aktionen sind sichtbar.
- Eigene Items: Linksklick Stack einlagern, Rechtsklick genau 1 Item. Nur `STORAGE` aktiviert diese Klicks; andere Menüs bleiben unverändert.
- Nach Aktionen wird die Ansicht nach DB-/Cache-Callbacks aktualisiert.

## Dorfinfos und Arbeiter
- Fehlende/veraltete Vermessung zeigt „Vermessung läuft …“ statt `0 Betten`; nach Abschluss erscheint die echte Zahl inklusive Chunk-Hinweis.
- Arbeiter-Items öffnen das Bewohnerdetail mit Beruf/Gebäude; unassign ist nur eine separate Aktion dort.
- „Schließen“ an der Baustellenkiste schließt nur das Fenster.

## Verifikation
- Vollständiger Java-21-Clean-Build mit Failures/Errors/Skipped = 0.
- Voller/leerer/normaler Lagerpfad und Links-/Rechtsklick-Mapping sind automatisiert geprüft.
- Echter Paper-Start mit FAWE ohne BlockLords-Fehler und sauberer Shutdown; Protokolle unter `verifier/runs/`.
