# Verifier v7 — Missionen und nächste Schritte

Zielversion: **0.2.8-alpha.1**, Phase 2 gemäß `KONZEPT.md` v0.6, Abschnitte 2.2, 8.2 und 12.

## Automatisierte Kriterien

- Java-21-Clean-Build und `git diff --check` bestehen; alle 52 bisherigen Tests bleiben grün.
- V1–V7 migrieren ohne Fehler. Eine echte V6-Datenbank behält Dorf, XP, Lager, Baustellen und Bewohner; sie erhält keine rückwirkenden Missionsabschlüsse oder XP.
- Alte Config ohne `missions:` nutzt Defaults. Ungültige Angebotsanzahl, Zielwerte und XP werden abgelehnt.
- Pro Spieler erscheinen zwei persönliche Angebote und eine gemeinsame Dorfmission; Missionen ohne Mitgliederrolle werden abgelehnt.
- Nur angenommene Missionen zählen neue, tatsächlich akzeptierte Beiträge. Teilbeträge, falsche Ressourcen und Überziele werden korrekt behandelt.
- Lager-Entnahmen, Produktion und Rückerstattungen erzeugen keinen Missionsfortschritt.
- Baustellenbeiträge aus Inventar und Dorfvorrat zählen nur die reale Kistenannahme genau einmal.
- Dieselbe Ereignis-ID und Doppelklicks erzeugen keinen doppelten Fortschritt.
- Persönliche Missionen sind getrennt; eine Dorfmission summiert nur Beiträge registrierter Mitglieder und zeigt den eigenen Anteil.
- Erst-XP ist atomar und einmalig. Wiederholungsmissionen erhalten 0 XP und melden keine falsche XP-Belohnung.
- Annehmen, Fortschritt, kostenloses Ersetzen, Pinnen und Login-Hinweis überleben Reload/Neustart.
- Missionsmenü und echte Spieleraktionen werden mit MockBukkit über denselben Servicepfad geprüft.
- Echter Paper-1.21.11-Start mit FAWE 2.16.3: Plugin aktiv, V7 sichtbar, Missionsbefehl registriert, bestehender RATHAUS-Selftest erfolgreich und sauberer Shutdown ohne `LATE_FAILURE`/`LATE_SUCCESS`.

## Spielerische Abnahme durch Alexander

1. Dorfmenü → **Missionen**: zwei persönliche und eine Dorfmission; `/bl missions` und Bedrock-Formular bieten dieselben Aktionen.
2. Holzmission annehmen: Start `0/32`; 16 Holz einlagern ergibt `16/32`; weitere 16 schließen mit sichtbarer Meldung und einmalig +20 Dorf-XP ab.
3. Dieselbe Mission erneut annehmen und abschließen: Fortschritt funktioniert, aber keine weitere XP; Meldung nennt den bereits verbrauchten Bonus.
4. Baustellenhilfe: Material aus Inventar und Dorfvorrat zählt nur real angenommene Mengen; Abbruch/Rückgabe erhöht nichts nachträglich.
5. Dorfmission Nahrung: Fortschritt und eigener Beitrag sind verständlich; ein zweites Mitglied erhöht denselben Dorfauftrag, nicht fremde persönliche Missionen.
6. Mission kostenlos ersetzen: Warnung bei Fortschritt, Ressourcen bleiben erhalten, neue Auswahl entsteht ohne Kosten.
7. Persönliche Mission anpinnen, neu einloggen und Server neu starten: nur ein kurzer Hinweis, Fortschritt bleibt erhalten, keine Doppelbelohnung.
8. Bestehende Dörfer, Gebäude, Lagerbestände und Arbeiter bleiben nach Migration unverändert; keine BlockLords-Fehler im Produktivlog.

## Nachweis

- Testanzahl und Ergebnisse werden aus `build/test-results/test/` gezählt und in `verifier/runs/` protokolliert.
- Release-JAR, öffentlicher Download und Produktiv-JAR werden per SHA256 verglichen.
- Ohne tatsächlichen Spielertest bleiben visuelle Crossplay- und Live-Kriterien ausdrücklich offen; kein automatisches „bestanden“ durch Konsolenausgabe allein.
