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

## v4 — 2026-11-25

- Datei: `verifier/v4/acceptance.md`
- Zweck: Kistenschutz und bewusster Abbruch, Kiste innen, Komposter-/Job-Site-Position, Arbeiterlimit und Dorfinfos/XP.
- Prüfungen: Schutz und Itemrückgabe, Rotation, Arbeiterlimit, alte Config ohne Limit, vollständiger Build und Paper-/FAWE-Lauf.

## v5 — 2026-11-26

- Datei: `verifier/v5/acceptance.md`
- Zweck: Lagerlogik und direkte Lagerbedienung, Bettscan, Arbeiterdetail und Kisten-Zurück.
- Prüfungen: voller/leerer/normaler Lagerpfad, Links-/Rechtsklick-Mapping, vollständiger Build und Paper-/FAWE-Lauf.

## v6 — 2026-11-26

- Datei: `verifier/v6/acceptance.md`
- Zweck: Kapazität pro Ressource statt globalem Gesamtlimit.
- Prüfungen: getrennte Einlagerungs-/Produktionskapazitäten, voller/leerer/normaler Lagerpfad, vollständiger Build und Paper-/FAWE-Lauf.

## v7 — 2026-11-26

- Datei: `verifier/v7/acceptance.md`
- Zweck: Persönliche und gemeinsame Missionen mit echtem Fortschritt, Erst-XP, kostenlosem Ersetzen und angepinntem nächsten Schritt.
- Prüfungen: V6→V7-Upgrade, Lager-/Baustellenereignisse, Idempotenz, Mehrspieler, Wiederholungs-XP, Config-Defaults, MockBukkit-Menüfluss und Paper-/FAWE-Lauf.

## v8 — 2026-11-26

- Datei: `verifier/v8/acceptance.md`
- Zweck: Aktiv-Markierung von Missionen und normale Bearbeitung des eigenen Inventars im Lager.
- Prüfungen: Glint und Aktiv-Text, Bottom-Klicks/Drags, Shift-Einlagerung, obere Vorrat-Aktionen, Duplikatschutz und Paper-/FAWE-Lauf.

## v9 — 2026-11-26

- Datei: `verifier/v9/acceptance.md`
- Zweck: Einwanderung mit persistenter Wartezeit und atomarer Aufnahme sowie strukturierte Produktionsbilanz.
- Prüfungen: V7→V8-Upgrade, Bett-/Arbeitsplatzvoraussetzungen, Idempotenz, Tod/Retry, Identitätsvergabe, Offline-Grenze, Ruhezustand, Nahrungskapazität und Paper-/FAWE-Lauf.
- Produktivnachweis: `verifier/runs/2026-11-26-v9-production.log`; konsistente Pre-/Post-Snapshots mit Integrität `ok`, zwei Dörfern und Schema V7→V8. Visuelle Abnahme in Bus #235 offen.

## v10 — 2026-11-27

- Datei: `verifier/v10/acceptance.md`
- Zweck: Atomarer Retry nach Ablehnung/Tod, verständliche Produktionswerte inklusive Nahrungs-Netto und Konsolenspiegelung des Login-Berichts.
- Prüfungen: Snapshot-Atomarität, regulärer Prüflauf während Retry, vollständige/teilweise/fehlende Einlagerung, alte V8-Pausentexte, Spieler-/Logger-Gleichheit, vollständiger Build und Paper-/FAWE-Lauf.
