# Verifier-Index

Dieser Index ist append-only. Jede Version erhält eigene Kriterien und reproduzierbare Prüfschritte.

## v1 — 2026-11-25

- Datei: `verifier/v1/acceptance.md`
- Zweck: Reparatur von Glocken-/Build-Flow, Bewohnernamen, Mine-Sichtbarkeit, Bauplanfeld, Türen und Betten.
- Prüfungen: statische Regeln, Unit-Tests, echter Paper-Start, FAWE-Bau und Blockstate-Nachweise.
- Laufprotokolle: `verifier/runs/`; jeder Lauf wird mit Zeit, Kommando, Exitcode und Ergebniswerten ergänzt.

## v2 — 2026-11-25

- Datei: `verifier/v2/acceptance.md`
- Zweck: P0/P1 aus Bus #230 — eindeutige Bewohnernummern, Verlustverwaltung, Kistenposition, `/bl`, Kompass, Level-/Arbeiterinfo und Hotbar ohne Offhand-Verlassen.
- Prüfungen: Batch- und Persistenztests, V5→V6-Upgrade, Unit-Tests, echter Paper-Start, Baustellenkiste und FAWE-Ausschluss.

## v3 — 2026-11-25

- Datei: `verifier/v3/acceptance.md`
- Zweck: Live-Hotfix aus Chat/Screenshots — Thread-sichere Kistenmaterialien, Eingangskiste, Menü-/Log-Korrekturen und Bewohneraktionen.
- Prüfungen: Contribute/Fund, Eingangsrotation, Identitätsreparatur, sichere Versammlung, Entfernen ohne Entity-Tod sowie Paper-/FAWE-Lauf.
