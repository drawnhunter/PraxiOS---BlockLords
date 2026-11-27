# Verifier v10 — Retry-Atomarität und lesbare Produktionsbilanz

Zielversion: **0.2.9-alpha.2**, Phase 2 gemäß `KONZEPT.md` v0.6. v9 bleibt unverändert erhalten.

## Automatisierte Kriterien

- Java-21-Clean-Build und `git diff --check` bestehen; alle bisherigen und neuen Tests bestehen ohne Failure, Error oder Skip.
- Registrierung und „Wanderer wartet“ werden gemeinsam geschrieben; kein später Ankunfts-Callback darf eine bereits erfolgte Entscheidung wieder auf `nextArrivalAt=0` zurücksetzen. Der Service-Test erzwingt eine Ablehnung nach dem Kandidaten-Commit und vor einem möglichen alten Ankunfts-Callback.
- Ablehnung/Tod schreiben Kandidatenstatus und 60-Sekunden-Retry in derselben SQLite-Transaktion. Aufnahme schreibt Kandidat, Bewohner, Nahrung, XP und den 300-Sekunden-Folgezyklus gemeinsam.
- Ein Snapshot von vor der Entscheidung enthält noch den Wartenden und verhindert Zustands schreibungen; ein Snapshot nach Commit enthält gleichzeitig keinen Wartenden und den korrekten Retry. Kein sichtbarer Zwischenzustand `WAITING=0` mit altem `nextArrivalAt=0`.
- Ein regulärer `runVillage`-Lauf während der Retry-Frist verlängert diese nicht. Weltoperationen bleiben im Hauptthread.
- Doppelte Entscheidungen, Tod/Retry, fehlende Nahrung, Identitätsvergabe und Ruhezustand bleiben durch die bestehenden Tests abgesichert.
- Produktionsressourcen unterscheiden geplant, eingelagert, nicht eingelagert und echten Bestand/Limit. Vollständig, teilweise und nichts eingelagert werden korrekt bezeichnet.
- Kapazitätsgründe zeigen die nicht eingelagerte Menge und vermischen nicht eingelagert/geplant mit Bestand/Limit. Alte V8-Pausentexte werden bei der Anzeige aus den strukturierten Werten ersetzt, ohne Datenbankmigration.
- Die Nahrungsbilanz zeigt eingelagerte Nahrung, Verbrauch und Netto mit Vorzeichen; `308 - 192 = +116` wird am tatsächlich geöffneten MockBukkit-Inventar geprüft.
- `sendJoinReport` sendet und loggt exakt denselben Bericht mit Spieler-/Dorfbezug. Kein vorhandener Bericht oder ein vor dem Callback offline gegangener Spieler erzeugt keinen Log.
- Echter Paper-1.21.11-/FAWE-2.16.3-Lauf mit 0.2.9-alpha.2, vorhandenem V8-Schema, beiden Befehlen, stabilem Selftest und sauberem Shutdown ohne BlockLords-Fehler.

## Live-Abnahme durch Alexander in Bus #235

1. Nach dem Deployment in Silberbrück den nächsten Wanderer ablehnen: NPC verschwindet ohne Tod, Nahrung/XP bleiben unverändert und der nächste Versuch erfolgt nach etwa 60–65 Sekunden statt 300 Sekunden.
2. Den folgenden Wanderer aufnehmen: +5 statt +30 XP, genau einmal 16 Nahrung, eindeutiger Name/#Nummer und keine Doppelbuchung bei Doppelklick.
3. Neuen Bewohner einem freien Arbeitsplatz zuweisen; die durch Zombies verstorbenen Bauern dürfen keine Plätze mehr blockieren.
4. `/bl production`: geplante/eingelagerte/nicht eingelagerte Mengen, echter Bestand, verständliche Gründe und Nahrungs-Netto in Java und Bedrock prüfen.
5. Nach erneutem Login erscheint derselbe Dorfbericht bei Spieler und Konsole.

Technische Nachweise ersetzen die visuelle Abnahme nicht. #235 bleibt bis zu Lex’ ausdrücklicher Bestätigung offen.
