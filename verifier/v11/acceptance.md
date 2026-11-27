# Verifier v11 — Scan-sicherer Retry und Netto-Bilanz überall

Zielversion: **0.2.9-alpha.3**. v10 bleibt unverändert erhalten.

## Automatisierte Kriterien

- Alle v10-Regeln bleiben gültig; Java-21-Clean-Build und `git diff --check` bestehen ohne Failure, Error oder Skip.
- Eine fehlende/abgelaufene Dorfvermessung ist kein fehlendes Spielkriterium. `runVillage` startet einen Scan, ohne den gespeicherten `nextArrivalAt`-Wert auf 0 oder 300 Sekunden zu verändern.
- Der Scanner verwendet eine kontrollierbare Zeitquelle. Der Test beweist mit derselben Uhr: Scan zunächst frisch, nach Ablauf nicht frisch, Retry bleibt exakt erhalten, nach Abschluss des Scans kommt genau ein Wanderer ohne Nahrungkosten.
- Registrierung, Aufnahme, Ablehnung und Tod bleiben atomar; die erzwungene Ablehnung nach Kandidaten-Commit und vor einem möglichen alten Ankunfts-Callback bleibt abgesichert.
- Jede Ressource zeigt Netto und Aufschlüsselung im Titel: `Nahrung · +116 netto (+308/-192)`, Weizen/Stein entsprechend mit Verbrauch 0. Geplant, eingelagert, Verbrauch, nicht eingelagert und echter Bestand/Limit bleiben getrennt lesbar.
- Kein zusätzlicher Nahrungsbilanz-Doppeleintrag. Die Status vollständig/teilweise/nichts eingelagert und alte V8-Kapazitätstexte bleiben korrekt.
- `cycleInfo()` erklärt einen Arbeitszyklus als 300 Sekunden und Offline-Ertrag 50 %. Derselbe Text wird in der gemeinsamen Java-/Bedrock-Ansicht verwendet.
- Login-Bericht verwendet dieselbe Netto-Aufschlüsselung für alle Ressourcen und bleibt zwischen Spieler und Konsole identisch; kein Bericht/Logout erzeugt keinen Log.
- Echter Paper-1.21.11-/FAWE-2.16.3-Lauf mit alpha.3, V8, Befehlsregistrierung, Selftest, keinem späten Fehler und sauberem Shutdown.

## Live-Abnahme in Bus #235

1. Nächsten Silberbrück-Wanderer ablehnen: auch mit abgelaufener Vermessung kommt der folgende nach etwa 60–65 Sekunden, nicht nach 300 Sekunden.
2. Danach aufnehmen: +5 XP, einmalig 16 Nahrung, kein Doppelklick-Duplikat, Arbeitsplatz zuweisbar.
3. `/bl production`: Netto-Bilanz für alle Ressourcen, Arbeitszyklus-Erklärung und Pausengründe auf Java und Bedrock prüfen.
4. #235 bleibt bis zu Lex’ ausdrücklicher visueller Bestätigung offen.
