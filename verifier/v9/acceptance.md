# Verifier v9 — Einwanderung und Produktionsbilanz

Zielversion: **0.2.9-alpha.1**, Phase 2 gemäß `KONZEPT.md` v0.6.

## Automatisierte Kriterien

- Java-21-Clean-Build und `git diff --check` bestehen; alle bisherigen und neuen Tests bestehen ohne Fehler oder Skip.
- Echte V7-Datenbank mit Dorf, XP, Lager, Missionen, Bewohnern und altem Textbericht migriert verlustfrei auf V8. Keine Alt-Kandidaten, Alt-XP oder erfundenen strukturierten Berichte.
- Alte Config ohne `immigration:` nutzt Defaults; ungültige Wartezeiten und Nahrungkosten werden abgelehnt.
- Nur Level ≥2 und `ACTIVE` ermöglichen Einwanderung. Freies physisches Bett, 16 Nahrung und freier Arbeitsplatz sind vor Ankunft und Aufnahme erforderlich.
- Wartezeit bleibt nach Neustart erhalten; keine Sofort-Spawns oder Altbelohnungen durch Migration.
- Pro Dorf wartet nur ein Kandidat. Spawn, Ablehnung, nachweislicher Tod, ungeladene Chunks und Retry erzeugen keine Duplikate oder Kosten.
- Aufnahme, Nahrungkosten, Kandidatenstatus, Bewohner, Bett-POI und XP sind atomar. Doppelklick/Retry kostet nichts zusätzlich.
- Erste Einwanderung +30, weitere +5; erneutes Laden verändert die Vergabe nicht.
- Neue Bewohner erhalten eindeutigen Namen/#Nummer, Bett und Meeting-Point; normale spätere Villager erhalten die nächste freie Nummer statt erneut #1.
- Produktionsberichte enthalten geplante/akzeptierte Erträge, Nahrungsverbrauch, Arbeiterzahlen und Pausengründe; alter Textbericht bleibt Fallback.
- Nahrungsverbrauch wird vor der Kapazitätsprüfung für neu produzierte Nahrung berücksichtigt. Volle Ressource A pausiert nur A, Ressource B läuft weiter.
- Offline-Gutschrift endet nach vier Stunden mit 50 %; danach keine neuen Erträge. `RESTING`/`REVIEW` erzeugt weder Produktion noch Einwanderung und keine Ruhe-Nachberechnung.
- `/bl immigration` und `/bl production` verwenden dieselben Services wie Java-Chest-GUI und Bedrock-Formulare; keine neuen Abhängigkeiten.
- Echter Paper-Start mit V8, bestehendem FAWE-Selftest und sauberem Shutdown ohne BlockLords-Fehler.

## Spielerische Abnahme durch Alexander

1. `/bl immigration`: Voraussetzungen, Wartezeit und konkrete fehlende Gründe sind verständlich.
2. Nach sichtbarem Zyklus erscheint genau ein Wanderer sicher an der Glocke; nach Neustart bleibt es derselbe.
3. Aufnahme kostet einmalig 16 Nahrung, erzeugt eindeutigen bestätigten Bewohner mit Bett und Name/#Nummer, erst +30 dann +5 XP. Doppelklick kostet nichts erneut.
4. Ablehnung/Tod/fehlende Voraussetzungen erzeugen keine Kosten oder Duplikate; neuer Versuch nach Frist.
5. Neuer Bewohner kann anschließend einem freien Arbeitsplatz zugewiesen werden; nachfolgende normale Villager erhalten keine doppelte Nummer.
6. `/bl production` zeigt realen Ertrag, Nahrungsverbrauch, Bestand/Limit und Pausengründe in Java und Bedrock.
7. Nach vier Stunden Offline-Fenster oder während `RESTING` gibt es weder Produktion noch Einwanderung; Reaktivierung erzeugt keine Ruhe-Nachzahlung.

Nachweisdateien liegen unter `verifier/runs/`. Visuelle Abnahme wird nicht durch Konsolenausgabe ersetzt.
