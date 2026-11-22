# BlockLords — Konzept (v0.3, 2026-11-22)

Dorf-Claim-, Wirtschafts-, Diplomatie- und Kriegssystem für PaperMC.
Ein Spieler wird **Landherr** eines Dorfes, baut es aus, wirtschaftet, diplomiert und führt (nur mit gegenseitiger Zustimmung) Kriege um Güter, Bewohner oder ganze Dörfer.

## 1. Grundidee & Kernschleife

1. **Entdecken:** Dorf-Glocke anklicken → Info: echte Bett-/Bewohnerzahl, Dorf-Level, Amt-Kosten (Start: 64 Smaragde).
2. **Erwirtschaften:** Amt kaufen → **Landherr**; Glocke + Region + Speicher gehören ihm, Grundschutz aktiv.
3. **Ausbauen:** Blueprints platzieren → Bewohner bauen gestaffelt (FAWE-Staging). Gebäude erzeugen Ertrag ins Rathaus.
4. **Verwalten:** Rathaus-GUI: Vorräte, Bewohner, Ränge, Asyl, Diplomatie.
5. **Verteidigen & expandieren:** Milizen + Golems vs. Raids; mit Fortschritt weitere Dörfer.

## 2. Design-Entscheidungen (mit Alexander, 2026-11-22)

| Thema | Entscheidung |
|---|---|
| Name | **BlockLords** (Rang: *Landherr*) |
| Mehrere Dörfer | 1. Dorf braucht Fortschritt (Dorf-Level ≥ 3 od. ≥ 15 Bewohner) → 2. Dorf. Zerfall senkt Claim-Counter, **Bestandsschutz** für bestehende Dörfer |
| Kriege | **Nur bei Zustimmung beider Seiten**; Einsätze verhandelbar: Güter, Bewohner, ganzes Dorf. Diplomatie (Bündnis/NAP/Handel) fließt ein |
| Smaragd-Quellen | Vanilla-Handel + Minenarbeiter (selten) + Missionen |
| Fremde im Dorf | Rathaus-GUI → Landherr → **Asyl-Anfrage** (Betten, Bewegen, Holztüren). Vertrauten-Rang: **Gefolgsmann** (+bauen/interagieren) |
| Blueprints | Festes Set + 1–2 via **Bibliotheks-Upgrades** (z. B. Wohnhaus II = 3 Betten) |

### 2.1 Rechte-Matrix (NEU v0.3 — Kern der Fairness)

| Aktion | Landherr | Gefolgsmann | Bürger | Asyl | Fremder | Kriegsgegner¹ |
|---|---|---|---|---|---|---|
| Dorf betreten | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Blöcke setzen/abbauen | ✓ | ✓ | ✗² | ✗ | ✗ | siehe Fußnote |
| Kisten öffnen | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Holztüren/Knöpfe/Hebel | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| Betten nutzen | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| Villager handeln | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| Ernten (Dorffelder) | ✓ | ✓ | ✓ (Anteil) | ✗ | ✗ | ✗ |
| Blaupause platzieren | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Milizen/Bewohner verwalten | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Asyl gewähren/Ränge | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| Krieg erklären/Diplomatie | ✓ | ✗ | ✗ | ✗ | ✗ | — |

¹ während eines laufenden Kriegs: Schutz-Aussetzung nur im vereinbarten Kampfbereich (siehe 5.1), außerhalb greifen Schutzregeln weiter.
² Bürger dürfen in ihrer **zugewiesenen Parzelle** bauen (Option, Standard: aus).

Rang-Vorschlag ergänzt: **Bürger** = wohnhaft im Dorf (Bett zugeteilt), nutzt Felder/Farm, kein Bau-Recht. Abfolge: Asyl → Bürger → Gefolgsmann.

## 3. Gebäude

| Gebäude | Funktion |
|---|---|
| **Rathaus** | Kern: GUI, Vorrat, Verwaltung, Diplomatie, Asyl/Ränge. Pflicht nach Claim |
| **Wohnhaus** | 2 Betten |
| **Wohnhaus II** | Bibliotheks-Upgrade: 3 Betten |
| **Bauernhaus + Felder** | Getreide-Ertrag; Felder als Schematic |
| **Schmiede** | Wohngebäude + stellt **Minenarbeiter** ein |
| **Mine** | Eingang + kleiner Schacht; Arbeitszone; selten Smaragde |
| **Kaserne** | Milizen (1–3, schwach, schneller Respawn) |
| **Schießturm** | Fernkämpfer-Miliz |
| **Bibliothek** | Forschung: Blueprints + Gebäude-Upgrades |
| **Lagerhaus** *(NEU)* | Erhöht Vorrats-Kapazität (siehe 4.1) |

### 3.1 Dorf-Level (NEU v0.3)
XP durch: Gebäude fertiggestellt (je nach Typ), Bewohner beheimatet, Missionen erfüllt.
**Level 1–10.** Steuert: 2.-Dorf-Freischaltung, Raid-Stärke, max. Milizen, sichtbarer Prestige-Status.

## 4. Arbeiter & Wirtschaft

- Bewohner = Arbeitsplätze (Bauernhaus / Schmiede→Mine / Kaserne). Tagsüber laufen Arbeiter zu Zonen (Pathfinding), abends heim — Ambiente + Wirtschafts-Takt.
- Erträge landen periodisch im **Rathaus-Vorrat**.

### 4.1 Vorrat & Ressourcen (NEU v0.3)
- Virtueller Lager-Bestand, **8 Basistypen**: Nahrung, Weizen, Holz, Stein, Kohle, Eisen, Smaragd, Pfeil.
- Kapazität startet bei 1.728 Einheiten, wächst mit **Rathaus-Level** und **Lagerhaus**.
- Ein-/Auszahlung über Rathaus-GUI; Überschuss verfällt (kein Härtefall, klar kommuniziert).

### 4.2 Baumaterial-Regel (NEU v0.3 — wichtigste Spielwirtschafts-Entscheidung)
Blueprints kosten **zweifach**: Smaragde (Kauf) **+ echte Materialien** aus dem Vorrat/dem Inventar des Platzierers (Holz/Stein/etc. je nach Gebäude). So entsteht echte Wirtschaft: farmen, lagern, bauen — statt „Geld aus dem Nichts".

### 4.3 Ertragsformel (NEU v0.3)
`Ertrag/Zyklus = Basis( Gebäude ) × Arbeiter( 0 wenn unbesetzt ) × Stufe × Zufriedenheit( später )`
Startwerte pro Zyklus (600 Ticks ≈ 30 s): Bauernhaus+ Feld je Arbeiter 2 Weizen/1 Nahrung; Mine je Minenarbeiter 1 Stein, 25 % 1 Kohle, 5 % 1 Smaragd. Alles Config.

## 5. Krieg, Diplomatie & Zerfall

### 5.1 Kriegs-Detail (NEU v0.3)
- **Voraussetzung:** beide Landherren **online**; beide akzeptieren Einsatz-Vertrag im Rathaus-Diplomatie-UI.
- **Vorbereitung:** 5 Min Countdown (beide Seiten werden im Chat gewarnt; Abbrechen nur gegenseitig).
- **Modus: Glocken-Kanalisation** — Angreifer muss 60 s ununterbrochen an der gegnerischen Glocke kanalisieren (Abbrechen bei Schaden/Abstand). Verteidiger-Verbund (Mitstreiter im Dorf) zählt als Verteidigung.
- **Schutz während Krieg:** nur im Kampf-Radius um beide Dörfer (Radius = Dorfradius) hebt sich der Bau-/Container-Schutz auf; Flucht = Verlust nach Timeout-Regel (siehe Frage 6).
- **Nach Krieg:** 24 h Friedens-Cooldown, Vertragserfüllung automatisch (Transfer Güter/Bewohner/Dorf).

### 5.2 Zerfall-Detail (NEU v0.3)
Inaktivität = Landherr X Tage nicht online **und** kein Gefolgsmann online.
- Tag 10: Warnung (Glocke/Chat/Rathaus-GUI).
- Tag 14: **Zerfall-Stufe 1** — Bau-Schutz fremd, Erträge halbiert.
- Tag 21: **Zerfall-Stufe 2** — Milizen despawnen, Blaupausen gesperrt.
- Tag 30: **Kollaps** — Claim fällt, Gebäude bleiben stehen (werden „verlassen"), Dorf ist neu claimbar.
- **Bestandsschutz:** Kollaps reduziert die persönlichen Claim-Slots erst nach Freigabe eines neuen Claims; bereits geführte Dörfer werden nie automatisch aufgegeben.

## 6. Technik & Regelwerk

### 6.1 Altbesitz-Regel (NEU v0.3 — Fairness bei Claim)
Hat ein Fremder **vor** dem Claim Bauten im Gebiet, gelten diese als **Altbesitz**: Er behält volle Rechte an seinen eigenen Blöcken (Plugin führt eine Besitzer-Map), kann aber die Region nicht erweitern. Beim Claim werden gefundene Altbesitze dem Landherrn angezeigt; Abfindung (Smaragde) optional verhandelbar. Verhindert Claim-Griefing.

### 6.2 Region & Limits (NEU v0.3)
- Claim-Region: Zylinder, Glocke = Zentrum, **Radius 48**, volle Höhe. (Optional später: „Stadt-Upgrade" Radius 64 via Bibliothek.)
- **Limits pro Welt:** max. 20 Dörfer, pro Landherr max. 4 (Performance auf dem Phone-Server! Scans gecacht, FAWE async, alle Scheduler-Tasks asynchron wo möglich).
- Glocke **unzerstörbar** für Nicht-Mitglieder; im Krieg wird sie nicht zerstört, sondern kanalisiert.

### 6.3 Technische Architektur
- Paper-API 1.21.11, **Java 21** (Zielserver Termux — keine Java-25-Dependencies, alle Libs vor Verwendung prüfen!), FAWE-API für Schematics, SQLite (`plugins/BlockLords/blocklords.db`), Chest-GUI, LuckPerms-Spiegelung optional.
- Tabellen: `claims, buildings, villagers, storage, ranks, wars, diplomacy, audit`.
- Admin-Kommandos: Claim auflösen/übertragen, Refund, Audit-Log.

## 7. Phasenplan

| Phase | Lieferumfang |
|---|---|
| **1 Fundament** | Glocke-Claim + Amt-Kauf, Region/Grundschutz (Matrix!), Rathaus (Block + Basis-GUI), SQLite-Schema, Altbesitz-Erkennung, Config |
| **2 Wirtschaft** | Blueprint-System (Material-Regel!), Wohnhaus/Bauernhaus/Felder/Schmiede/Mine, Arbeiter-Lauf-Ambiente, Vorrat + Lagerhaus, Bibliothek + 1. Upgrades, Dorf-Level |
| **3 Militär** | Kaserne/Schießturm, Milizen (Spawn/Respawn/Targeting), Raid-Skalierung |
| **4 Multiplayer & Krieg** | Ränge (Asyl/Bürger/Gefolgsmann), Multi-Claim-Regel, Diplomatie-UI, Glocken-Kanalisation, Zerfall, LP-Spiegelung |

## 8. Offene Fragen an Alexander (vor Start Phase 1–2)

1. **Baumaterial-Regel (4.2):** Blaupausen kosten zusätzlich echte Materialien aus dem Lager — so gewollt? (Empfehlung: ja, sonst inflations-würdig.)
2. **Rechte-Matrix:** Passt die Tabelle so? Soll Bürger in einer eigenen Parzelle bauen dürfen (2²)? Welche Rechte fehlen dir?
3. **Rang-Namen:** „Bürger" + „Gefolgsmann" — oder deutscher/mittelalterlicher (z. B. „Lehnsmann", „Dorfbewohner")?
4. **Krieg:** Glocken-Kanalisation (60 s am Gegner-Glocke) als einziger Modus ok? Soll Flucht möglich sein (Timeout = Niederlage) oder harter Kampf bis Kanalisierung?
5. **Zerfall:** Timer 10/14/21/30 Tage sinnvoll? Soll Zerfall komplett abschaltbar sein (Config) für pure Kooperation?
6. **Raid-Belohnung:** Dorf erhält bei gewonnenem Raid kleinen Smaragd-Bonus + „Held des Dorfes" — ok?
7. **Limits:** Radius 48 / max. 4 Dörfer je Landherr / 20 pro Welt — passt für deine Gruppe?
8. **Namen für Dörfer:** Landherr vergibt Namen beim Claim (Weltkarte/GUI) — Freitext oder aus Vorschlagsliste?
