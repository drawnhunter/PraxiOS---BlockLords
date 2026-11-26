# Acceptance v6 — BlockLords 0.2.7-alpha.5 Kapazität pro Ressource

## Kapazität
- `deposit` berechnet den freien Platz ausschließlich aus dem Bestand derselben Ressource, nicht aus dem Gesamtbestand.
- `applyProduction` begrenzt jede produzierte Ressource separat; Nahrungspflege verändert keine fremden Kapazitäten.
- Eine volle Ressource blockiert andere Ressourcen nicht. Überschuss verfällt nicht, Partial-Refunds und Audit bleiben erhalten.
- Die Anzeige erklärt das Limit ausdrücklich als „Limit pro Ressource“; der Gesamtbestand bleibt nur eine Information.

## Verifikation
- Voller/leerer/normaler Lagerpfad und getrennte Kapazitäten sind automatisiert geprüft.
- Vollständiger Java-21-Clean-Build mit Failures/Errors/Skipped = 0.
- Echter Paper-Start mit FAWE ohne BlockLords-Fehler und sauberer Shutdown; Protokolle unter `verifier/runs/`.
