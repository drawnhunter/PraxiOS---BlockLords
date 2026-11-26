# Verifier v8 — Missions- und Lager-UX

Zielversion: **0.2.8-alpha.2**, kleiner UX-Hotfix aus der abgenommenen 0.2.8-alpha.1 und Bus #233/#234.

## Automatisierte Kriterien

- Alle 70 bisherigen und neuen Tests bestehen ohne Fehler oder Skip; Java-21-Clean-Build und `git diff --check` sind grün.
- Angenommene persönliche Missionen und Dorfmissionen erhalten am Item `enchantment_glint_override=true` und den Klartext „Aktiv“. Angebote bleiben ohne Glint; abgeschlossene Missionen verschwinden weiterhin aus den aktiven Angeboten.
- Ein Rechtsklick im eigenen Inventar des Lagers ist erlaubt und kann einen Stack normal teilen. Gewöhnliche untere Pickup-/Cursor-Aktionen und reine Bottom-Drags bleiben normal.
- Shift+Links auf ein eigenes Item lagert genau dessen Stack ein; Shift+Rechts lagert genau ein Item ein.
- Klicks auf obere Vorrat-Items behalten die bisherigen Aktionen: Links 64 einlagern, Rechts 64 entnehmen.
- Doppelklicks, Hotbar-/Cursor-Manipulation am oberen Menü und Drags mit mindestens einem oberen Slot bleiben abgebrochen; keine Geisteritems oder Duplikate.
- Die übrigen Missions-, Lager-, Migrations- und Baustellenregeln bleiben unverändert und alle Bestandstests bestehen.
- Echter Paper-Start mit alpha.2: vorhandene V7-Datenbank bleibt lesbar, Missionsbefehl und FAWE-Selftest funktionieren, kein BlockLords-Fehler und sauberer Shutdown.

## Spielerische Abnahme durch Alexander

1. Missionsangebot annehmen: Papier/Glocke erhält Glint und „Aktiv“ im Namen; der Zustand ist auch auf Bedrock verständlich.
2. Eigenen Stack im Lager mit Rechtsklick teilen; keine ungewollte Einlagerung oder Entnahme.
3. Mit Shift+Links einen gewählten Stack einlagern und mit Shift+Rechts genau ein Item; obere Vorrat-Items weiterhin mit Links/Rechts bedienen.
4. Doppelklick und Drag oben erzeugen keine Menüitems im eigenen Inventar; unteres normales Sortieren bleibt möglich.
5. Bestehende Dörfer, Vorräte, Missionen und XP bleiben nach dem Update erhalten.

Nachweisdateien liegen unter `verifier/runs/`. Ein fehlender visueller Test wird nicht durch Konsolenausgabe ersetzt.
