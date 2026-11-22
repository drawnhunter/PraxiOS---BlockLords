# BlockLords — Konzept (v0.2, 2026-11-22)

Dorf-Claim-, Wirtschafts-, Diplomatie- und Kriegssystem für PaperMC.
Ein Spieler wird **Landherr** eines Dorfes, baut es aus, wirtschaftet, diplomiert und führt (nur mit gegenseitiger Zustimmung) Kriege um Güter, Bewohner oder ganze Dörfer.

## 1. Grundidee & Kernschleife

1. **Entdecken:** Spieler findet ein Dorf (Vanilla-Dörfer: Glocke, Betten, Bewohner). Klick auf die **Dorfglocke** öffnet Info: echte Bett-/Bewohnerzahl, Dorf-Level, **Amt-Kosten** (Vorschlag: 64 Smaragde).
2. **Erwirtschaften:** Amt kaufen → Spieler wird **Landherr**; Glocke + Region + Speicher gehören ihm, Grundschutz aktiv.
3. **Ausbauen:** Blueprints platzieren → Dorfbewohner **bauen gestaffelt** (FAWE-Staging, Geräusche/Partikel). Gebäude erzeugen Ertrag/Vorräte ins Rathaus.
4. **Verwalten:** Rathaus-GUI: Vorräte, Bewohner-Übersicht, Ränge für Mitspieler, Asyl-Anfragen.
5. **Verteidigen & expandieren:** Milizen + Golems vs. Raids; Diplomatie; mit Fortschritt weitere Dörfer übernehmen.

## 2. Design-Entscheidungen (mit Alexander, 2026-11-22)

| Thema | Entscheidung |
|---|---|
| Name | **BlockLords** (Rang im Spiel: *Landherr*) |
| Mehrere Dörfer | Erstes Dorf muss Fortschritt zeigen (Vorschlag: Dorf-Level ≥ 3 **oder** ≥ 15 Bewohner) → 2. Dorf claimbar. **Zerfall** (Inaktivität) senkt den Claim-Counter wieder — **Bestandsschutz** für bestehende Dörfer |
| Kriege | **Nur bei Zustimmung beider Seiten**. Verhandelbare Einsätze: Güter/Ressourcen, Bewohner, oder das **gesamte Dorf**. Diplomatie-System (Bündnisse, Handel) wird einfließen |
| Smaragd-Quellen | Vanilla-Handel + **Minenarbeiter finden ab und zu Smaragde** |
| Fremde im Dorf | Über Rathaus-GUI: Dorfinfo → Landherr anklicken → **Asyl-Anfrage**. Asyl = Betten nutzen, sich bewegen, **Holztüren** öffnen. Höherer Rang für Vertraute des Landherrn (Arbeitstitel: **Gefolgsmann**/*Lehensmann*) = zusätzlich bauen/interagieren |
| Blueprints | **Festes Set** am Start + 1–2 via **Bibliotheks-Upgrades**. Bibliothek = Forschungsgebäude (z. B. Wohnhaus-Upgrade auf 2 Stockwerke → **3 statt 2 Betten**) |

## 3. Gebäude

| Gebäude | Funktion |
|---|---|
| **Rathaus** | Kern: GUI, Vorrats-Speicher, Verwaltung, Diplomatie, Asyl/Ränge. Erstes Pflicht-Gebäude nach Claim |
| **Wohnhaus** | 2 Betten (2 Bewohner-Slots) |
| **Wohnhaus II** | Bibliotheks-Upgrade: 3 Betten |
| **Bauernhaus + Felder** | Getreide-Ertrag; Felder als Schematic (versch. Größen/Typen später) |
| **Schmiede** | Wohngebäude + stellt Bewohner als **Minenarbeiter** ein |
| **Mine** | Schematic: Eingang + kleiner Schacht; Arbeitszone der Minenarbeiter; ab und zu Smaragde |
| **Kaserne** | Ermöglicht Milizen (1–3, schwächer als richtige Kämpfer, respawnen schnell) |
| **Schießturm** | Fernkämpfer-Miliz, erhöht Verteidigung |
| **Bibliothek** | Forschung: neue Blueprints, Gebäude-Upgrades |

## 4. Arbeiter & Wirtschaft

- Bewohner haben **Arbeitsplätze** (Bauernhaus/Schmiede→Mine/Kaserne).
- Tagsüber **laufen Arbeiter zu ihren Arbeitszonen** (Villager-Pathfinding), verbringen Zeit dort, kehren abends ins Wohnhaus zurück (Ambiente + nachvollziehbare Wirtschaft).
- Erzeugte Ressourcen landen **periodisch im Rathaus-Vorrat** (virtueller Speicher + Anzeige im Rathaus; physische Kisten optional später).
- Ertrag skaliert mit Bewohnerzahl, Gebäudestufe und Zufriedenheit (Zufriedenheit erst ab Phase 2/3).

## 5. Krieg & Diplomatie (Phase 4)

- **Kriegserklärung nur mit Einverständnis beider Landherren** (Dialog im Rathaus, Einsatz-Verhandlung).
- Kampfmodus-Vorschlag: **Glocken-Eroberung** — Angreifer muss die gegnerische Dorfglocke X Minuten (z. B. 10) „kanalisieren", Verteidiger kann abbrechen. Sieg → Einsatz-Transfer.
- **Diplomatie**: Bündnisse, Nichtangriffspakte, Handelsabkommen (Ressourcen gegen Ressourcen, z. B. Weizen gegen Smaragde).

## 6. Technische Architektur

- **Region:** eigener Zylinder-Claim (Glocke = Zentrum, Radius 48, volle Höhe), persistent in SQLite. Kein Überschreiben fremder Claims; Vanilla-Dorf-Daten (Betten/Bewohner) werden aus der Region gezählt (Entity-Query + Block-Scan, gecacht).
- **Schematics:** FAWE-API, Async-Paste, gestaffelte Bau-Phasen (Fundament → Wände → Dach), Blitz-Build dauert z. B. 60–120 s mit Sound/Partikeln.
- **Milizen:** freundliche Vindikatoren/Pillager, eigener Team-Eintrag (kein Friendly-Fire), Tick-Task setzt nahe Hostile als Target; Respawn-Timer über Kaserne; schwächer als späte Krieger-Einheiten, dafür schnell ersetzbar.
- **GUI:** Chest-Inventory-Menüs (Bukkit Inventory API), Text-Buttons via ItemMeta.
- **Persistenz:** SQLite (`plugins/BlockLords/blocklords.db`), Tabellen: claims, buildings, villagers_jobs, storage, ranks, wars, diplomacy.
- **Java-21-Zwang:** Zielserver läuft auf Termux-openjdk-21 — keine Java-25-Features, Dependencies vor Download auf Kompatibilität prüfen (Lessons Learned: WorldEdit 7.4.x = Java 25 → deshalb FAWE 2.16.3).
- **LuckPerms:** optionale Spiegelung der Ränge (Landherr/Gefolgsmann/Asyl) in LP-Gruppen.

## 7. Phasenplan (MVP-zuerst)

| Phase | Lieferumfang | Ziel |
|---|---|---|
| **1 Fundament** | Glocke-Claim + Amt-Kauf, Region/Grundschutz, Rathaus (Block + Basis-GUI), SQLite-Schema, Config | Erstes Dorf spielbar claimen |
| **2 Wirtschaft** | Blueprint-System (FAWE-Staging), Wohnhaus/Bauernhaus/Felder/Schmiede/Mine, Arbeiter-Lauf-Ambiente, Rathaus-Vorrat, Bibliothek + 1. Upgrades | Dorf wächst und wirtschaftet |
| **3 Militär** | Kaserne/Schießturm, Milizen-Spawn/Respawn, Raid-Skalierung nach Dorf-Level | Verteidigung spielbar |
| **4 Multiplayer & Krieg** | Asyl/Gefolgsmann-Ränge, Multi-Claim-Regel, Diplomatie-UI, Kriegs-Modus (Glocken-Eroberung), LuckPerms-Spiegelung | Das volle BlockLords |

## 8. Offene Design-Fragen (vor/nährend Phase 1–2 klären)

1. Zerfall-Timer: nach wie viel Tagen Inaktivität zerfällt ein Dorf? (Vorschlag: 14 Tage, Warnung ab 10.)
2. Kampfmodus-Details: reine Glocken-Kanalisation oder auch PvP? (PvP ggf. serverweit via Paper-Config einschränkbar.)
3. Vorrat physisch (Kisten im Rathaus) oder virtuell mit Abhebe-GUI? (Vorschlag: virtuell + optional Auszahlung.)
4. Smaragd-Balancing: wie viele Minenarbeiter → wie viele Smaragde/Tag? (Erstwerte in Config, live tunen.)
