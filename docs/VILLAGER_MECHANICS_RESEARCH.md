# BlockLords — Villager- und Wirtschaftsmechanik: Recherche und Zahlenentscheid

**Datum:** 25.11.2026  
**Zweck:** P2/P3 aus #210 mit belastbaren Vanilla- und Systemvergleichen statt frei erfundenen Regeln abschließen.

## 1. Vanilla-POIs und Register

Villager besitzen getrennte Erinnerungen für `home`, `job_site` und `meeting_point` mit Dimension und Koordinaten ([Minecraft Wiki](https://minecraft.wiki/w/Entity_format/Villager_memories)). Unsere Registrierung folgt daraus: Glocken-Meeting-Point bestätigt, Bett im Claim registriert unbestätigt, alle anderen bleiben Wanderer. Eine fertige dauerhafte Vanilla-Mitgliedschaftsliste existiert nicht und wird durch unser POI-Register ergänzt.

Arbeitsplätze sind eindeutig: **Bauer = Komposter**, **Schmied = Schmiedetisch**. Villager arbeiten morgens und nachmittags an ihrem Job-Site und nutzen diesen zur Auffüllung ihrer Angebote ([Minecraft Wiki](https://minecraft.wiki/w/Village_mechanics)). Die explizite `JOB_SITE`-Zuweisung wird daher mit einem echten Schmiedetisch in der Mine ergänzt. Unsere Datenbankzuweisung bleibt die verlässliche Produktionsgrundlage; Pfadfehler sind kein stiller Produktionsstopp.

Betten benötigen Erreichbarkeit und freien Kopfraum. Für Züchtung werden freie Betten benötigt, aber keine Arbeitsplätze ([Minecraft Wiki](https://minecraft.wiki/w/Tutorial:Villager_farming)). Wohnhaus I/II bleiben klein: Zwei beziehungsweise drei Betten als kontrollierte Wachstumskapazität, nicht als automatische Bewohnergenerierung. Bettzuweisung prüft echte Betten mit Teil `FOOT` und jetzt auch Y-Koordinate.

## 2. Golems, Raids und Schutz

Eisen-Golems greifen Zombies und andere feindliche Mobs aktiv an. Unser alter pauschaler Schutz aller Nicht-Spieler-Entitäten im Claim hat deshalb echte Vanilla-Verteidigung behindert. Die neue Regel begrenzt Schadensschutz auf Villager und dekorative Objekte und lässt Golems kämpfen ([Minecraft Wiki](https://minecraft.wiki/w/Iron_Golem)). Raids hängen ebenfalls von beanspruchten POIs ab und skalieren mit Schwierigkeit und Raid-Omen-Level ([Minecraft Wiki](https://minecraft.wiki/w/Raid)). Spätere Raidlauf-Regeln müssen deshalb aktive Teilnehmer und POI-Zustand anzeigen, nicht nur Dorf-Level.

## 3. Etablierte Baustellen- und Wirtschaftsmodelle

MineColonies trennt `Supplied %` und `Used %`: gelieferte Materialien und tatsächlich verbaute Blöcke. Fehlende Ressourcen werden farbig markiert und können direkt aus dem Inventar übernommen werden ([MineColonies](https://minecolonies.com/wiki/buildings/builder/)). Unsere Baustellenkiste bildet diese Trennung korrekt ab. Eine Materialanforderung ist nicht dasselbe wie Baufortschritt.

TekTopia und Millénaire zeigen lebendige Dorfwirtschaft durch Produktionsabhängigkeiten, aber auch Blockaden durch Pathfinding und verkettete Abhängigkeiten ([TekTopia](https://tektopia.fandom.com/wiki/A_Guide_For_Newbies), [Millénaire](https://millenaire.wiki.gg/)). Deshalb bleibt unsere Wirtschaft lesbar und modular: Berufe sind hilfreich, aber keine harten Verkettungen, die ohne NPC-Reparatur nicht mehr funktionieren.

Die Wirtschaft wird als Quellen-Senken-Modell geprüft: Gebäude, Forschung, Ausbau, Miliz und Reparaturen sind echte Senken. Spielerhandel, Missionen und passive Erträge sind Quellen. Quellen und Senken müssen mit Aktivität und Lagerkapazität geprüft werden, nicht mit pauschaler Maximierung ([Yu-kai Chou](https://yukaichou.com/gamification-analysis/economy-design-sources-sinks-confidence/)).

## 4. Zahlenentscheid

Der aktuelle 5-Minuten-Zyklus ergibt 12 Zyklen/Stunde. Dabei gelten folgende Startwerte:

| Pro Arbeiter und Stunde | Weizen | Nahrung brutto | Stein | Kohle, Erwartung | Smaragde, Erwartung |
|---|---:|---:|---:|---:|---:|
| 1 | 72 | 48 | 72 | 6 | 3 |
| 2 | 144 | 96 | 144 | 12 | 6 |
| 4 | 288 | 192 | 288 | 24 | 12 |
| 8 | 576 | 384 | 576 | 48 | 24 |

Für 64 Smaragde als zweiter Dorf-Claim ergibt sich je nach Minenbelegschaft:

- **1 Minenarbeiter:** etwa 21,3 Stunden
- **2 Minenarbeiter:** etwa 10,7 Stunden
- **4 Minenarbeiter:** etwa 5,3 Stunden
- **8 Minenarbeiter:** etwa 2,7 Stunden

Das ist ein plausibler Rahmen für mehrere Spielsitzungen. Ein einzelner Bauer füllt die Startkapazität von 1.728 Einheiten rechnerisch erst nach 14,4 Stunden; der Offline-Deckel von vier Stunden mit 50 % begrenzt zusätzlich passive Anhäufung. Diese Werte bleiben vorerst bestehen, werden aber durch tatsächliche Produktions- und Lagerdaten weiter getestet, nicht als endgültiges Balance-Versprechen.

## 5. UX und P3-Texte

Progressive Offenlegung ist sinnvoll: ein sichtbarer nächster Schritt, kontextuelle Hilfe und Detailinformationen auf Abruf statt einer langen Fronttour ([Kompassify](https://kompassify.com/blog/video-game-onboarding-guide), [LogRocket](https://blog.logrocket.com/ux-design/cognitive-principles-for-ux-designers/)). Deshalb lautet unser Muster: **Aktion → kurze Bestätigung → nächster Schritt**. Beispiele:

- `Bauplan wählen → /bl build tools`
- `Material liefern → /bl build contribute oder /bl build fund`
- `Arbeiter zuweisen → Nahrung einlagern`
- `Bau starten → Fundament → Wände → Dach`

Die P2/P3-Änderungen sind damit abgeschlossen: echte Job-Sites, Bewohnerdetails, Bettzuweisung, Grasfreimachung, kurze Hinweise und überprüfte Zahlen statt neuer Systeme ohne Kontext.
