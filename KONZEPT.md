# BlockLords — Konzept v0.6

**Status:** Von Alexander am 23.11.2026 final bestätigte Entwicklungsgrundlage. Basistechnik und frühere Fassungen stammen aus `dd520df`, `6ff16ff` und `e3b8db1`; v0.6 ersetzt die dort noch offenen beziehungsweise widersprüchlichen Regeln.

**Produktvision:** BlockLords ist ein kooperatives Dorf-Aufbau- und Verwaltungsspiel für den PraxiOS-Familien-SMP. Spieler gründen oder besuchen Dörfer, übernehmen sichtbare Aufgaben, entwickeln Wirtschaft und Identität ihres Dorfes und treten regelmäßig in freiwilligen Dorfspielen gegeneinander an. Kriege mit verhandelbaren Einsätzen existieren zusätzlich, gefährden aber standardmäßig weder Gebäude noch persönliche Hinterlassenschaften.

## 1. Erfahrungsziele und Leitregeln

| Ziel | Verbindliche Auswirkung |
|---|---|
| **Gemeinsam statt allein verwalten** | Landherr, Gefolgsmann und Bürger besitzen unterschiedliche, jeweils sinnvolle Aufgaben. Wichtige Dorfziele zeigen die Beiträge aller Mitglieder. |
| **Sichtbarer Fortschritt** | Jedes Bauvorhaben besitzt Vorschau, Materialliste, Bauphasen und einen eindeutigen Abschluss. Dorf-Level zeigen konkret, was als Nächstes freigeschaltet wird. |
| **Wettkampf ohne sozialen Zwang** | Niemand wird allein durch Dorfmitgliedschaft Kriegsteilnehmer. Teilnahme, Modus, Dauer, Verlustregeln und Einsatz werden vorher angezeigt und bestätigt. |
| **Rückschläge ohne Vernichtung** | Reguläre Konflikte löschen keine Gebäude, öffnen keine privaten Kisten und enteignen keine Spieler. Verlierer erhalten Teilnahme- und Lernfortschritt. |
| **Kurze Sitzungen bleiben wertvoll** | Bereits 15–30 Minuten ermöglichen einen sichtbaren Beitrag. Rückkehrende erhalten eine knappe Zusammenfassung statt einer Suchaufgabe. |
| **Java und Bedrock gleichwertig** | Jede Kernfunktion ist auf beiden Clients erreichbar. Kein Pflichtschritt setzt Java-only-Eingaben oder die Bedienung eines Chest-GUI voraus. |
| **Konfigurierbar, aber nicht widersprüchlich** | Kosten, Erträge, Timer und Limits sind konfigurierbar. Schutzversprechen, Zustimmungslogik und Datenintegrität sind feste Systemregeln. |

## 2. Spielschleifen

### 2.1 Momentane Schleife: handeln und Rückmeldung erhalten

**Aufgabe wählen → selbst sammeln, bauen, handeln oder verteidigen → Beitrag wird sofort angezeigt → Dorf oder eigener Bereich verändert sich sichtbar.**

Passive Erträge ergänzen dieses Handeln, ersetzen es aber nicht. Ein Arbeiter darf deshalb auch dann produzieren, wenn sein NPC-Pathfinding scheitert; der sichtbare Arbeitsweg ist Ambiente, nicht die Voraussetzung für die wirtschaftliche Berechnung. Pathfinding-Probleme erzeugen einen Hinweis und eine Reparaturaktion, aber keine unsichtbare Nullproduktion.

### 2.2 Sitzungsschleife: 15–45 Minuten

1. Beim Betreten erscheinen höchstens drei Karten: **Dorfzustand**, **empfohlener nächster Schritt**, **offene Einladung oder Mission**.
2. Spieler wählt eine persönliche oder gemeinsame Aufgabe: Materialbeitrag, Baustellenhilfe, Handel, Bewohnerversorgung, Verteidigung oder Dorfspiel.
3. Die Aufgabe endet mit sichtbarer Veränderung, Dorf-XP und gegebenenfalls einer kleinen Ressourcenbelohnung.
4. Vor dem Ausloggen kann der Spieler eine Folgeaufgabe anpinnen; beim nächsten Besuch erscheint genau diese wieder.

### 2.3 Langzeitschleife: Wochen

**Dorf gründen → Grundversorgung sichern → Bewohner und Mitspieler aufnehmen → Gebäude und Forschung wählen → Dorf spezialisieren → an Dorfspielen teilnehmen → Bündnisse und Handel ausbauen → weitere Dörfer gründen.**

Weitere Dörfer vergrößern Gestaltungs- und Handlungsmöglichkeiten, nicht nur die passive Ertragsmenge. Der Fortschritt endet nicht in einer reinen Zahlensteigerung: Trophäen, Dorfbanner, öffentliche Aufträge, gemeinsame Bauwerke und freiwillige Wettkämpfe bilden den dauerhaften Endgame-Rahmen.

## 3. Die erste Spielsitzung

### 3.1 Entdecken ohne Vorwissen

Ein Klick auf eine Dorfglocke öffnet eine verständliche Übersicht: Dorfname oder „Unbeanspruchtes Dorf“, Landherr, eigene Claim-Regel, sichtbare Bewohner- und Bettzahl, öffentliche Angebote und mögliche nächste Aktionen. Die Übersicht unterscheidet ausdrücklich zwischen **Vanilla-Dorfdaten** und der **BlockLords-Claim-Region**.

Bei einem unbeanspruchten Dorf startet die optionale **Gründungsquest**:

1. Glocke läuten und Dorfinfo öffnen.
2. Drei vorhandene Dorfpunkte besichtigen: Wohnraum, Nahrungsquelle und Arbeitsplatz.
3. Ein kleines Gründungspaket einsammeln.
4. Dorfname wählen oder Vorschlag übernehmen.
5. Gründung bestätigen; die erste Dorfgründung kostet keine Smaragde.

Die Quest ist jederzeit abbrechbar. Erfahrene Spieler können sie mit „Direkt gründen“ überspringen, sofern sie die Bedingungen bereits erfüllen. Folgedörfer kosten standardmäßig 64 Smaragde und benötigen die unter 11.2 beschriebenen Fortschrittsbedingungen.

### 3.2 Erster sichtbarer Erfolg

Das Gründungspaket deckt das erste Rathaus ab. Dadurch entsteht nach dem Claim kein Softlock durch fehlende Baustoffe. Die Rathaus-Vorschau wird zunächst als Geisterbild platziert; Rotation, Kollisionen und fehlende Materialien werden vor der Bestätigung angezeigt. Bis zur Fertigstellung des Rathauses übernimmt die Glocke die Basisverwaltung.

**Zielwert für den ersten Test:** vom Finden eines geeigneten Dorfes bis zum fertigen Rathaus höchstens 15 Minuten, ohne vorheriges Smaragd-Farming. Der Wert ist ein UX-Testziel, kein hartkodierter Timer.

## 4. Spielerrollen und soziale Rechte

| Aktion | Landherr | Gefolgsmann | Bürger | Asyl | Fremder |
|---|---|---|---|---|---|
| Öffentliches Dorf betreten | ✓ | ✓ | ✓ | ✓ | ✓ |
| Rathaus-Dorfinfo und eigene Beiträge sehen | ✓ | ✓ | ✓ | ✓ | ✓ |
| Zugewiesenes Bett nutzen | ✓ | ✓ | ✓ | ✓ | — |
| Öffentliche Türen und Einrichtungen nutzen | ✓ | ✓ | ✓ | ✓ | nur wenn geöffnet |
| Dorf- und persönliche Missionen annehmen | ✓ | ✓ | ✓ | eingeschränkt | — |
| Dorffelder nutzen und Anteil erhalten | ✓ | ✓ | ✓ | — | — |
| Öffentlichen Markt nutzen | ✓ | ✓ | ✓ | ✓ | ✓, wenn geöffnet |
| Eigene Parzelle gestalten | ✓ | ✓ | ✓, falls zugewiesen | — | — |
| Gemeinschaftslager einzahlen | ✓ | ✓ | ✓ | optional | — |
| Gemeinschaftslager abheben | ✓ | nach Delegation | nur eigener Ertragsanteil | — | — |
| Blaupausen vorbereiten | ✓ | ✓ | Vorschlag einreichen | — | — |
| Blaupausen verbindlich starten | ✓ | ✓ im delegierten Bereich | — | — | — |
| Arbeiter und Gebäude verwalten | ✓ | ✓ im delegierten Bereich | eigener Arbeitsplatz | — | — |
| Private Kisten anderer Spieler öffnen | — | — | — | — | — |
| Ränge vergeben oder Mitglieder entfernen | ✓ | — | — | — | — |
| Diplomatie und Kriegsvertrag abschließen | ✓ | nur mit gesonderter Vollmacht | — | — | — |
| An Wettkampf teilnehmen | ✓ | ✓ | ✓ nach eigener Zustimmung | nicht automatisch | nicht automatisch |

**Rollenverständnis:**

- **Asyl** ist ein vorübergehender Gaststatus mit zugewiesenem Bett und Zugang zu ausdrücklich öffentlichen Bereichen, nicht ein pauschales Recht an sämtlichen Türen, Knöpfen oder Betten.
- **Bürger** ist dauerhaftes Dorfmitglied. Bürger wählen Aufgaben, erhalten Ertragsanteile und können eine persönliche Parzelle besitzen. Der Standard für Parzellen ist **an**, die Größe bleibt konfigurierbar.
- **Gefolgsmann** erhält delegierte Verwaltungsrechte. Die Ernennung gibt nicht automatisch Zugriff auf private Kisten oder die Macht, Kriege abzuschließen.
- **Landherr** trägt strategische Verantwortung, wird aber nicht zum Eigentümer sämtlicher persönlicher Gegenstände seiner Mitglieder.

**Austritt und Entfernung:** Persönliche Items bleiben in der Parzelle oder werden in einen persönlichen Übergabe-Container überführt. Eine Entfernung sperrt den Zugriff nicht sofort auf die eigenen Hinterlassenschaften; eine Abholfrist und ein Audit-Eintrag sind verbindlich. Ein Krieg oder Dorftransfer verändert diese persönlichen Rechte nicht.

## 5. Claim, Region und Altbesitz

### 5.1 Claim-Region

Die BlockLords-Region ist ein eigener Zylinder um die registrierte Glocke, Start-Radius 48 Blöcke und volle Höhe. Sie ist keine Behauptung über eine natürliche Vanilla-Dorfgrenze. Beim Betreten wird der Dorfname kurz angezeigt; auf Wunsch zeigen Partikel die Grenze. Ein späterer Bibliotheksausbau kann den Radius auf 64 erhöhen, sofern keine fremden Regionen oder geschützten Altbesitze kollidieren.

Die Glocke ist für Nichtmitglieder unzerstörbar. Der Landherr kann sie über einen eigenen Verlegungsassistenten verschieben; dabei werden Zielregion, Kollisionen und aktive Ereignisse geprüft. Eine durch Administratoreingriff oder Weltfehler verlorene Glocke darf den Claim nicht löschen: Der Claim besitzt einen gespeicherten Ersatzpunkt und eine Reparaturaktion.

### 5.2 Altbesitz ohne falsche Automatik

Ein nachträglich installiertes Plugin kann nicht verlässlich rekonstruieren, welcher Spieler einen vorhandenen Block gesetzt hat. Deshalb wird die v0.4-Regel zur automatischen Altbesitz-Erkennung ersetzt:

1. Vor der ersten produktiven Nutzung registriert ein Admin bestehende Spielerbauten einmalig manuell oder markiert betroffene Bereiche als persönliche Altparzellen.
2. Neue Blockplatzierungen werden ab Plugin-Installation mit Besitzer und Zeitpunkt protokolliert.
3. Vor einer neuen Gründung prüft BlockLords auf Hinweise aktiver Nutzung, etwa persönliche Betten, registrierte Parzellen, vorhandene Schutzregionen und bereits protokollierte Blöcke.
4. Bei möglichem Altbesitz wird die Gründung nicht automatisch über das fremde Eigentum gelegt. Erforderlich ist die Zustimmung des betroffenen Spielers, eine ausgeschlossene Altparzelle oder eine Admin-Entscheidung mit Audit-Eintrag.
5. Eine Material- oder Smaragd-Abfindung ist ein optionaler Handel, niemals eine automatische Pflicht.

Damit bleibt der Fairnessgedanke erhalten, ohne eine technisch unmögliche historische Besitzer-Map vorzutäuschen.

## 6. Gebäude, Bauen und Forschung

| Gebäude | Spielerfunktion | Wirtschaftliche oder systemische Funktion |
|---|---|---|
| **Rathaus** | Dorfübersicht, Missionen, Mitglieder, Vorrat, Diplomatie | Zentraler Speicher und Verwaltungspunkt; Basiszugang bereits über Glocke |
| **Wohnhaus I** | Sichtbares Wachstum und neue Bewohner | Zwei erreichbare NPC-Betten; Voraussetzung für Einwanderung |
| **Wohnhaus II** | Bibliotheks-Upgrade mit verändertem Erscheinungsbild | Drei NPC-Betten und verbesserter Wohnkomfort |
| **Bauernhaus + Feld** | Sammelaufträge, Feldwahl und sichtbare Ernte | Weizen und Nahrung; Arbeiterplatz für Bauern |
| **Mine** | Schacht, Arbeitsbereich und Reparaturbedarf | Stein, Kohle und seltene Smaragde; direkte Minenarbeiter-Zuordnung |
| **Schmiede** | Werkzeug- und Ausrüstungsaufträge | Eisen- und Kohlesenke; stellt Miliz- und Werkzeug-Upgrades her, nicht mehr selbst die Minenarbeiter |
| **Lagerhaus** | Ein-/Auszahlung und Bestandsübersicht | Erhöht Kapazität; zusätzlicher physischer Einzahlungspunkt |
| **Bibliothek** | Forschungsbaum und Dorf-Spezialisierung | Schaltet Bauvarianten, Gebäude-Upgrades und Dorfoptionen frei |
| **Kaserne** | Milizverwaltung und freiwillige Verteidigungsereignisse | Nahkampfmiliz, Respawn und Ausrüstungskosten |
| **Schießturm** | Fernkampf und sichtbare Verteidigungszone | Fernkampfmiliz; verbraucht Pfeile bei Einsätzen |
| **Marktstand** | Öffentliche Angebote und Handelsaufträge | Spieler-/Dorfhandel; öffentlicher Zugriff ist separat schaltbar |

### 6.1 Bauablauf

1. **Auswählen:** Das Menü zeigt Zweck, Voraussetzungen, Arbeitsplätze, Unterhalt und spätere Upgrades.
2. **Vorschau:** Geisterbild platzieren, rotieren und mit bereits vorhandenen Gebäuden abgleichen. Die Vorschau berücksichtigt die spätere Maximalgröße, damit ein Upgrade nicht unerwartet angrenzende Bauten überdeckt.
3. **Prüfen:** Kollisionen, Altbesitz, Schutzgrenzen, erreichbare Eingänge und benötigte Materialien werden angezeigt. Nicht erlaubte Platzierungen nennen den konkreten Grund.
4. **Beitrag leisten:** Materialien können aus Rathaus, Lagerhaus oder Inventar stammen. Mehrere Spieler dürfen eine Baustelle gemeinsam bestücken; jeder Beitrag wird registriert.
5. **Bauen:** FAWE-Staging zeigt Fundament, Wände und Dach. NPC-Bauambiente, Geräusche und Partikel ergänzen den Ablauf. Die eigentliche Bauzeit darf nicht durch asynchrone Weltzugriffe erzwungen werden.
6. **Abschließen:** Fertigstellung erzeugt Dorf-XP, eine kurze Rückmeldung und eine sichtbare Namensliste der wesentlichen Beitragsleister.

Vor Baubeginn ist eine vollständige Rücknahme möglich. Nach Baubeginn werden nur noch nicht verbaute Materialien erstattet; ein Abbruch löscht keine bereits bestehenden Spielerbauten. Fehlgeschlagene Schematic-Vorgänge müssen transaktional abgebrochen oder sicher fortgesetzt werden.

### 6.2 Forschung und Spezialisierung

Die Bibliothek besitzt drei übersichtliche Zweige: **Gemeinwesen**, **Wirtschaft** und **Verteidigung**. Forschung kostet Materialien, etwas Zeit und gegebenenfalls Smaragde; vor Start werden Dauer, Kosten und Ergebnis angezeigt. Abbruchregeln stehen vor der Bestätigung fest.

Ab Dorf-Level 5 wählt das Dorf eine aktive Spezialisierung:

| Spezialisierung | Bevorzugter Spielstil | Beispielwirkung |
|---|---|---|
| **Kornkammer** | Versorgung, Einwanderung, Erntespiele | Bessere Feldaufträge und zusätzliche Nahrungsoptionen |
| **Zunftdorf** | Bauen, Handel, Schmiede und Bibliothek | Effizientere Upgrades und zusätzliche Marktaufträge |
| **Wachtburg** | Verteidigung, Raidlauf und Glockensturm | Schnellere Milizvorbereitung und zusätzliche Verteidigungsoptionen |

Eine Spezialisierung ist keine dauerhafte Sackgasse. Ein Wechsel benötigt eine kurze Abklingzeit und einen moderaten Forschungsbetrag; bereits gebaute Häuser bleiben erhalten. Boni sollen Spielstile ergänzen, nicht andere Dörfer dauerhaft entwerten.

## 7. Wirtschaft und Vorrat

### 7.1 Ressourcenmodell

BlockLords verwendet zunächst acht klar definierte abstrakte Ressourcen:

| Ressource | Zweck | Beispielhafte Quellen und Senken |
|---|---|---|
| **Nahrung** | Versorgung von Arbeitern und Einwanderung | Bauernhof, Missionen; Verbrauch durch Arbeit und Bevölkerungswachstum |
| **Weizen** | Handel, Feldaufträge und ausgewählte Forschung | Felder; Verarbeitung, Handel und Missionen |
| **Holz** | Gebäude und Werkzeuge | Spielerbeitrag, spätere Forstoption; Bau und Reparatur |
| **Stein** | Gebäude und Befestigung | Spielerbeitrag, Mine; Bau und Reparatur |
| **Kohle** | Schmiede, Forschung und Ausrüstung | Mine und Spielerbeitrag; Schmiede und Forschung |
| **Eisen** | Werkzeuge und Milizausrüstung | Spielerbeitrag und seltene Minenfunde; Schmiede und Militär |
| **Smaragd** | Folgeclaims, Forschung, Verwaltung und Einsätze | Vanilla-Handel, Missionen, seltene Minenfunde; Gebäudegebühren, Forschung und Verträge |
| **Pfeil** | Fernkampf und Verteidigungsereignisse | Schmiede/Fletcherei über Spielerbeitrag; Schießturm und Raidverbrauch |

Nur ausdrücklich zugeordnete Vanilla-Items können in abstrakte Ressourcen umgewandelt werden. Die Umwandlungstabelle zeigt Kurs und Rücktausch vor jeder Transaktion. Wertvolle oder nicht eindeutig austauschbare Gegenstände werden nicht stillschweigend in „Nahrung“, „Holz“ oder andere Kategorien verwandelt.

### 7.2 Lager und Überlauf

Der Rathaus-Vorrat startet mit einer gemeinsamen Kapazität von 1.728 Einheiten; Lagerhäuser erhöhen sie. Die Kapazitätsanzeige zeigt Gesamtfüllung und Bestand pro Ressource. Bei 80 % und 95 % erscheinen dezente Warnungen.

**Kein Überschuss verfällt.** Ist eine Ressource am Limit, pausiert nur deren weitere Produktion. Die Baustelle oder Produktionsanzeige nennt drei Auswege: abheben, ausgeben oder Kapazität erweitern. Bereits erzeugte Einheiten verschwinden nicht, und ein voller Lagerbestand blockiert nicht unbeteiligte Systeme.

### 7.3 Arbeitszyklus und Ertrags-Startwerte

Ein Arbeitszyklus dauert testweise 5 Minuten. Produktion setzt einen zugewiesenen erwachsenen NPC, ein funktionsfähiges Gebäude und ausreichend Nahrung voraus. Der reale NPC-Aufenthaltsort beeinflusst die Berechnung nicht.

| Beruf | Bruttoertrag je Arbeiter und Zyklus | Zusätzlicher Verbrauch |
|---|---|---|
| **Bauer** | 6 Weizen + 4 Nahrung | 1 Nahrung je Arbeitszyklus |
| **Minenarbeiter** | 6 Stein; 50 % Chance auf 1 Kohle; 25 % Chance auf 1 Smaragd | 1 Nahrung je Arbeitszyklus |
| **Weitere Berufe** | Werden mit Schmiede, Bibliothek und Militär ergänzt | Kosten werden vor Freischaltung angezeigt |

Die Formel lautet: `Ertrag = Basis × zugewiesene Arbeiter × Gebäude-/Forschungsmodifikator × Aktivitätsmodifikator`. Zufriedenheit ist kein versteckter Multiplikator. Versorgungs- oder Gebäudeeffekte werden als einzelne nachvollziehbare Zeilen in der Produktionsbilanz angezeigt.

Diese Werte sind **Test-Defaults, kein Balance-Versprechen**. Der erste Spieltest prüft insbesondere Lagerfüllung, Zeit bis zum zweiten Dorf, Smaragdquellen und ob manuelles Spielen gegenüber passivem Warten relevant bleibt.

### 7.4 Online und Offline

Während mindestens ein Dorfmitglied online und das Dorf aktiv bespielt wird, produziert es mit 100 %. Danach wird zeitbasiert eine Offline-Produktion von höchstens vier Stunden mit 50 % berechnet. Vorrat, Nahrung, Gebäudezustand und Kapazität begrenzen auch diese Nachberechnung. Beim nächsten Besuch erscheint eine einzige Zusammenfassung mit Ertrag, Verbrauch und gestoppten Produktionslinien.

Es gibt keine täglichen Login-Serien und keinen Verfall von Missionen allein zum Erzeugen von Termindruck. Wer mehrere Tage nicht spielt, verliert keine bereits verdienten Ressourcen.

## 8. Bewohner, Missionen und Dorf-Level

### 8.1 Bewohnerwachstum

Ein freies Bett ist eine Wohnraum-Kapazität, aber keine Garantie für sofortige Vermehrung. BlockLords unterstützt zwei nachvollziehbare Wege:

- **Vanilla-Vermehrung** bleibt möglich und respektiert erreichbare Betten, Nahrung und vorhandene Erwachsene.
- **Einwanderung** ist die verlässliche BlockLords-Ergänzung: Sind freier Wohnraum, Nahrung und ein geeigneter Arbeitsplatz vorhanden, kann nach einem sichtbaren Wartezyklus ein Wanderer an der Glocke eintreffen. Der Landherr oder ein Gefolgsmann nimmt ihn über eine kurze Interaktion auf.

Jeder Bewohner besitzt Namen, Wohnhaus, Beruf und Arbeitsplatz. Fehlende Erreichbarkeit wird als Reparaturhinweis angezeigt. Bevölkerungswachstum stoppt kontrolliert bei Bett- oder Nahrungsmangel; es startet keine unbemerkte Hunger- oder Todesspirale.

### 8.2 Missionen ohne Termindruck

Das Dorf besitzt drei Missionstypen:

| Typ | Zweck | Beispiele |
|---|---|---|
| **Persönlich** | Eigene Ziele und Einstieg | 32 Holz beisteuern, ein Bett zuweisen, einen Arbeitsplatz reparieren |
| **Dorfgemeinschaft** | Mehrere Mitglieder tragen bei | Baustelle gemeinsam bestücken, 120 Nahrung sammeln, einen Raid vorbereiten |
| **Diplomatisch** | Andere Dörfer einbeziehen | Handelsauftrag erfüllen, Gast aufnehmen, an einem Dorfspiel teilnehmen |

Drei aktive Vorschläge genügen. Eine Mission kann kostenlos ersetzt werden; eine neue erscheint nach Abschluss oder nach einer langen, konfigurierbaren Aktualisierungsfrist. Wiederholbare Missionen geben reduzierte oder keine Dorf-XP, damit nicht dieselbe Trivialschleife unendlich das Dorf levelt. Einmalige Entwicklungsschritte bleiben die Hauptquelle für Dorf-XP.

### 8.3 Dorf-Level 1–10

| Level | Sichtbarer Entwicklungsschritt und Freischaltung |
|---|---|
| **1 — Gründung** | Glocke, Rathaus, Wohnhaus I, Bauernhaus/Feld, persönliche und Dorfmissionen |
| **2 — Versorgtes Dorf** | Mine, Lagerhaus, Einwanderung, Produktionsbilanz |
| **3 — Dorf mit Zukunft** | Bibliothek, erste Forschung; zweites Dorf nach zusätzlichen Kernbedingungen |
| **4 — Handwerksdorf** | Schmiede-Upgrades, Wohnhaus II, Marktstand |
| **5 — Eigener Weg** | Eine Dorf-Spezialisierung; Kaserne und erste Verteidigungsereignisse |
| **6 — Bündnisfähig** | Erweiterte Handelsverträge; drittes Dorf nach Kernbedingungen |
| **7 — Befestigtes Dorf** | Schießturm, höhere Milizstufen und schwierigere Raidlauf-Stufen |
| **8 — Herrschaftsverband** | Viertes Dorf nach Kernbedingungen und erweiterte Diplomatieoptionen |
| **9 — Berühmtes Dorf** | Kosmetische Rathaus-, Banner- und Trophäenvarianten; keine entscheidende Machtsteigerung |
| **10 — Leuchtendes Erbe** | Prestige-Abschluss, besondere Dorfspiel-Trophäe und dauerhafte Chronik; keine Pflicht zum Neustart |

Dorf-XP entsteht durch erstmals fertiggestellte Gebäude und Upgrades, erfolgreiche Einwanderung, gemeinsame Missionen, erstmalige Ereignisse und Wettkampfteilnahme. Reine Lagerfüllung oder das Horten von Smaragden erzeugt keine XP.

## 9. Militär und Verteidigung

### 9.1 Milizen

Die Kaserne stellt ein bis drei Nahkampfmilizen; der Schießturm ermöglicht Fernkampf. Milizen sind eigenen BlockLords-Teams zugeordnet, greifen nur zulässige Ziele an und besitzen keine Friendly-Fire-Schäden gegen Dorfmitglieder. Anzahl, Stärke, Respawn und Reichweite sind konfiguriert und durch Gebäudestufen begrenzt.

Respawn ist nicht kostenlos: Er benötigt eine kurze Vorbereitungszeit sowie Nahrung und ausgewählte Ausrüstung. Dadurch bleibt Verteidigung eine Wirtschaftsentscheidung, ohne dass ein einzelner Verlust dauerhaft schmerzt. Milizen sind schwächer als gut vorbereitete Spieler, übernehmen aber Grundschutz und sichtbare Dorfbelebung.

### 9.2 Vanilla-Raids und BlockLords-Raidlauf

Vanilla-Raids bleiben möglich; BlockLords verändert ihre Auslösebedingungen nicht heimlich. Zusätzlich bietet die Kaserne einen **freiwilligen Raidlauf** mit klarer Vorbereitung, ausgewählter Schwierigkeit und definierter Teilnehmerliste.

Die Schwierigkeit berücksichtigt Dorf-Level, ausgewählte Stufe und aktiv angemeldete Verteidiger. Ein höheres Dorf-Level darf nicht automatisch zu einer unlösbaren Strafskalierung führen. Vor Beginn werden Gegnerwellen, besondere Regeln und mögliche Belohnungen angezeigt.

- **Sieg:** kleine Smaragdbelohnung, Dorf-XP beim ersten Abschluss, Trophäenfortschritt und optionales „Held des Dorfes“-Zeichen.
- **Niederlage:** moderate Reparatur- oder Nachschubkosten, kein dauerhafter Gebäudeverlust und keine gelöschten Spielerinventare durch Plugin-Regeln.
- **Teilnahme:** Auch Verteidiger ohne finale Tötung erhalten Beitragsanerkennung für Heilung, Schutz, Reparatur oder Schadensanteil.
- **Wiederholung:** Weitere Versuche sind nach einem kurzen Cooldown möglich; farmbare Erstabschluss-XP werden nicht wiederholt vergeben.

## 10. Dorfspiele, Krieg und Diplomatie

### 10.1 Dorfspiele als regelmäßige Wettkampfebene

Dorfspiele sind freiwillige, kurze Wettbewerbe zwischen Dörfern. Sie können jederzeit durch Herausforderung oder als moderiertes Serverereignis stattfinden. Es gibt keine Pflichtteilnahme und keine verfallende wöchentliche Serie.

| Disziplin | Ablauf | Schutz- und Belohnungsregel |
|---|---|---|
| **Glockensturm** | Zwei Teams, etwa 10 Minuten; Kanalisierung an der gegnerischen Glocke sammelt Fortschritt, Verteidiger unterbrechen oder drängen zurück | Nur angemeldete Spieler sind PvP-fähig; Ereignis-Ausrüstung oder garantierte Rückgabe; Dorfblöcke und Kisten bleiben geschützt |
| **Raidlauf-Duell** | Gleiche ausgewählte PvE-Schwierigkeit; Wertung nach Wellen, verhindertem Schaden und Nebenzielen | Kooperation innerhalb des eigenen Dorfes; kein direkter Eingriff in das andere Dorf |
| **Lieferwettlauf** | Mehrere eindeutig beschriebene Aufträge in etwa 15 Minuten; Sammeln, Verarbeiten und geschickte Arbeitsteilung | Ereignisaufträge verwenden markierte Güter, damit keine Lagerbestände unbeabsichtigt verloren gehen oder Dupes entstehen |
| **Baustellen-Duell** | Beide Dörfer erhalten dieselbe fehlende Struktur und ergänzen sie nach einem festen Regelblatt | Bau nur in temporärer Wettkampfzone; automatische Bewertung nach nachprüfbaren Kriterien |

**Startumfang:** Phase 4 implementiert zuerst Glockensturm und Raidlauf-Duell. Lieferwettlauf und Baustellen-Duell folgen erst nach stabiler Vertrags- und Ereignislogik.

Alle angemeldeten Teilnehmer erhalten eine Grundbelohnung und Fortschritt in der Dorf-Chronik. Sieger erhalten einen sichtbaren Bonus, Trophäenpunkte und gegebenenfalls einen kleinen Ressourcengewinn. Niemand verliert durch ein gewöhnliches Dorfspiel eingezahlte Vorräte.

### 10.2 Kriegsvertrag

Ein Krieg ist ein separater Vertrag mit Einsatz, kein Synonym für jedes Dorfspiel. Der Vertrag zeigt vor Bestätigung vollständig:

- beteiligte Dörfer und ausdrücklich angemeldete Spieler,
- Modus und Siegbedingung,
- Start, Vorbereitungszeit und maximale Dauer,
- erlaubte Interaktionen und ausdrücklich verbotene Zerstörung,
- Einsatz je Seite und Art der Hinterlegung,
- Regeln für Aufgabe, Unentschieden, Verbindungsabbruch und Serverneustart,
- Friedensfrist nach Abschluss.

Beide Landherren bestätigen den Vertrag. Jeder Spieler entscheidet zusätzlich selbst über seine Teilnahme. Ein Landherr kann Bürger nicht allein durch den Vertrag in PvP zwingen.

**Standardregeln:**

1. Fünf Minuten Vorbereitung; vor Kampfbeginn kann eine Seite ohne Verlust zurücktreten.
2. Ressourcen-Einsätze werden vorab aus dem Rathaus-Vorrat in einen Treuhandtopf übertragen. Nicht vorhandene Einsätze können nicht versprochen werden.
3. Gebäude-, Container-, Parzellen- und NPC-Schutz bleibt aktiv. Bau- und Containerrechte werden nicht im Dorfradius ausgesetzt.
4. PvP gilt nur zwischen angemeldeten Teilnehmern und nur in der markierten Ereigniszone.
5. Die Glocken-Kanalisierung sammelt über die Kampfzeit Fortschritt; sie muss nicht 60 Sekunden fehlerfrei am Stück gehalten werden. Unterbrechung, Fortschrittsverlust und Endphase sind für beide Seiten sichtbar.
6. Aufgabe ist jederzeit möglich und löst den vereinbarten Einsatzverlust aus. Ein Unentschieden gibt beide Treuhandtöpfe zurück.
7. Serverfehler pausieren oder brechen das Ereignis ohne Einsatzverlust ab. Bei vollständigem Verbindungsverlust einer Seite gilt zunächst eine Wiederkehrfrist; kein sofortiger Verlust bereits beim ersten Disconnect.
8. Nach Abschluss wird der Treuhandtopf automatisch ausgeführt; ein 24-stündiger Nichtangriffspakt zwischen beiden Dörfern beginnt.

**Einsatzarten:** Ressourcen und definierte NPC-Bewohnergruppen sind zulässig. Menschliche Mitspieler können niemals als Einsatz übertragen werden. Das gesamte Dorf ist im regulären System **nicht** verfügbar; dafür existiert ausschließlich der folgende gesonderte Herrschaftskampf.

### 10.3 Herrschaftskampf um ein gesamtes Dorf

Der Herrschaftskampf ist ein bewusst extremer Spezialvertrag: Der Sieger wird Landherr des vereinbarten Dorfes. Dieses Gewicht ist ausdrücklich gewollt, deshalb gelten verschärfte Zustimmungs- und Schutzregeln:

1. **Einstimmige Zustimmung:** Beide Landherren sowie alle registrierten Bürger und Gefolgsleute beider Dörfer müssen dem konkreten Vertrag zustimmen. Asylbewohner werden informiert, besitzen aber keine Vertragsstimme. Fehlt eine erforderliche Zustimmung, startet der Vertrag nicht.
2. **Keine Spieler als Einsatz:** Übertragen werden ausschließlich Dorfverwaltung, gemeinsamer Rathaus-Vorrat, registrierte Gebäude und NPC-Zuordnungen. Menschliche Mitglieder, deren Inventare, persönliche Parzellen und private Container werden niemals übertragen.
3. **Kein Gebäudeschaden:** Auch dieser Modus hebt Bau-, Container- und Parzellenschutz nicht auf. Der Einsatz ist die Herrschaft, nicht die physische Zerstörung des Dorfes.
4. **Verlängerte Vorbereitung:** Mindestens 24 Stunden Ankündigung, eine zweite Bestätigung unmittelbar vor Beginn und ein freiwilliges Kampf-Opt-in jedes Teilnehmers. Während Ankündigung und Kampf sind Dorfauflösung, Eigentumsübertragung und Änderungen der stimmberechtigten Mitgliedschaft gesperrt.
5. **Durchführbarkeit vor Vertrag:** Das anvisierte Sieger-Dorf muss den neuen Claim rechtlich und technisch übernehmen können; der Gewinner benötigt einen freien Claim-Slot. Das Einsatz-Dorf wird bis Vertragsende für andere Verträge gesperrt.
6. **Atomarer Transfer:** Erst nach eindeutigem Sieg erfolgt eine einzelne Datenbanktransaktion mit Rollenwechsel, Claim-Slot-Aktualisierung und Audit-Einträgen auf beiden Seiten. Serverfehler, Neustart, unentschiedener Ausgang oder abgelaufene Wiederkehrfrist lösen keinen Transfer aus.
7. **Nachfolge und Mitglieder:** Der bisherige Landherr verliert nur das Amt im verlorenen Dorf, nicht seinen Privatbesitz. Bürger und Gefolgsleute bleiben Mitglieder und können anschließend geordnet austreten. Eine siebentägige Schutzfrist verhindert unmittelbare Revanche- oder Weiterverkaufsverträge.

Der Herrschaftskampf verwendet die erweiterte Glockensturm-Logik mit längerer Vorbereitung und ausdrücklich bestätigten Teilnehmern. Die genauen Kampfzeiten und die Schutzfrist sind konfigurierbar; Einstimmigkeit, Privatbesitzschutz und atomare Übertragung sind unveränderliche Invarianten.

### 10.4 Diplomatie

Diplomatie bleibt lesbar und zweckgebunden:

- **Handelsauftrag:** einmaliger, eindeutiger Austausch definierter Ressourcen; beide Seiten hinterlegen ihren Anteil.
- **Handelsabkommen:** wiederholbare Aufträge mit wählbaren Obergrenzen; keine automatische Enteignung bei leerem Vorrat.
- **Nichtangriffspakt:** verhindert neue Kriegsverträge für die vereinbarte Dauer; Dorfspiele bleiben ausdrücklich erlaubt oder ausgeschlossen.
- **Bündnis:** ermöglicht gemeinsame Raidziele, Hilfsmissionen und optionale Verteidigungsanfragen; es erzeugt keine automatische Kriegsbeteiligung.
- **Chronik:** Verträge, Ereignisse und Transfers bleiben für die beteiligten Dörfer nachvollziehbar.

## 11. Inaktivität, weitere Dörfer und Nachfolge

### 11.1 Ruhezustand statt schutzloser Zerfall

Als Dorfaktivität zählt ein sinnvoller Beitrag eines Landherrn, Gefolgsmanns oder Bürgers, beispielsweise Baustellenbeitrag, Mission, Lagertransaktion oder Ereignis. Das bloße kurze Login-Event allein soll kein dauerhaftes AFK-Verbleiben ermöglichen; die genaue Mindestaktion bleibt konfigurierbar.

| Zeit ohne Dorfaktivität | Zustand und Wirkung |
|---|---|
| **Tag 10** | Warnung beim nächsten Login und dauerhaft in Dorfübersicht; kein Schutz- oder Ertragsverlust bereits verdienter Bestände |
| **Tag 14** | Dorf wird **ruhend**: passive Produktion und Einwanderung pausieren; Schutz, Gebäude und Mitgliederrechte bleiben unverändert |
| **Tag 21** | Landherr kann kein weiteres Dorf gründen, bevor er das ruhende Dorf reaktiviert oder geordnet abgibt |
| **Tag 30** | Dorf wird für Admin-Prüfung als verlassen vorgeschlagen; sieben Tage letzte Kontaktfrist |
| **Frühestens Tag 37** | Nur ein Admin kann den Dorf-Claim nach abgeschlossener Prüfung aufheben; keine automatische Löschung von Gebäuden oder persönlichen Parzellen |

Die Rückkehr eines Mitglieds reaktiviert das Dorf ohne Strafgebühr. Standardmäßig ist die automatische Claim-Freigabe **aus**. Zerfall hebt niemals allein den Bau- oder Containerschutz auf. Mit `decay.enabled: false` lässt sich der gesamte Zerfall einschließlich Warnungen und Ruhezustand abschalten; Schutz und bereits gespeicherte Dörfer bleiben in beiden Modi unverändert.

### 11.2 Weitere Dörfer und Bestandsschutz

- **Zweites Dorf:** Hauptdorf mindestens Level 3, Rathaus fertig, stabile Nahrungsversorgung, ein Wohnhaus und eine funktionierende Produktionsstätte.
- **Drittes Dorf:** mindestens ein eigenes Dorf Level 6, zwei aktive Kernbedingungen erfüllt und keine ungeklärte verlassene Gründung.
- **Viertes Dorf:** mindestens ein eigenes Dorf Level 8 und entsprechende Verwaltungsreife.
- Startlimits bleiben vier Dörfer je Landherr und 20 BlockLords-Dörfer je Welt, sind aber Konfigurationswerte. Die reale Leistungsgrenze wird durch Entity-, Bau- und Tick-Messungen bestimmt, nicht durch die Dorfanzahl allein.

Sinkt die spätere Berechtigung durch Inaktivität oder Regeländerung, bleiben bereits geführte Dörfer bestehen. Erst eine tatsächlich freigegebene oder übertragene Gründung verändert den belegten Claim-Slot. Es gibt keine automatische Aufgabe eines bestehenden Dorfes nur wegen eines gesunkenen Counters.

### 11.3 Nachfolge und Übertragung

Der Landherr kann einen Nachfolger bestimmen oder das Amt geordnet übertragen. Bei langer Inaktivität kann ein Admin nach der Warnkette einen aktiven Gefolgsmann als Verwalter einsetzen. Eine Übertragung umfasst Dorfverwaltung und gemeinsames Rathausvermögen, aber keine privaten Parzellen oder Spielerinventare. Alle Schritte sind auditierbar.

## 12. Bedienung, Feedback und Crossplay

### 12.1 Gemeinsame Informationsarchitektur

Jede Kernfunktion folgt derselben Menülogik: **Übersicht → Bereich → Aktion → Bestätigung → Ergebnis**. Java erhält Chest-GUIs; Bedrock erhält über Floodgate native Formulare. Befehle bleiben als Fallback und für erfahrene Spieler vorhanden.

Texteingaben wie Dorfnamen funktionieren über Bedrock-Formular, Java-Eingabe oder Befehl. Eine Vorschlagsliste verhindert Eingabeaufwand auf Mobilgeräten. Namen werden auf Länge, unzulässige Zeichen und Duplikate geprüft; Freitext bleibt möglich.

### 12.2 Verständliche Rückmeldung

- Jede verweigerte Aktion nennt Grund und mögliche Lösung: „Dir fehlen 12 Stein“ statt nur „nicht erlaubt“.
- Produktionsbilanz zeigt Ertrag, Verbrauch, pausierte Berufe und Lagergrund in einer Ansicht.
- Bossbars werden nur für zeitlich aktive Ereignisse verwendet; dauerhafte Informationen stehen im Menü oder als dezente Actionbar.
- Chatmeldungen werden gebündelt. Kein Ressourcen-Tick spammt jede Minute den Dorfchat.
- Sounds und Partikel markieren Gründung, Bauabschluss, Levelaufstieg, Wettkampf und Warnung, lassen sich aber individuell reduzieren.
- Farben sind nie der einzige Informationskanal; Icons und Klartext ergänzen Ampelfarben.

### 12.3 Rückkehr- und Fehlerzustände

Beim Betreten nach Abwesenheit erscheinen maximal vier Zeilen: Zeitraum, Ertrag minus Verbrauch, gestoppte Systeme und ein empfohlener nächster Schritt. Nach Serverneustart werden Claim, Baustellen, Treuhandtöpfe und Ereignisse aus persistierten Zuständen fortgesetzt oder sicher abgebrochen. Kein Spieler muss einen Admin wegen eines normalen Verbindungsabbruchs um manuelle Rückerstattung bitten.

## 13. Technische Architektur

### 13.1 Zielplattform und Abhängigkeiten

- **Paper 1.21.11, Java 21.** Produktiv- und Build-Bytecode bleiben Java-21-kompatibel.
- **FAWE 2.16.3** für Schematics; keine WorldGuard-Abhängigkeit. FAWE wird als Laufzeitabhängigkeit deklariert, sobald Blaupausen implementiert sind.
- **Eigener Schutz** für die BlockLords-spezifische Semantik. Die Entscheidung gegen WorldGuard beruht auf Kontrolle und Integrationsaufwand, nicht auf der pauschalen Behauptung, alle WorldEdit-7.4.x-Versionen benötigten Java 25.
- **SQLite** unter `plugins/BlockLords/blocklords.db`, mit versionierten Migrationen, Transaktionen, einem kontrollierten Schreibweg und regelmäßigen Backups. Der JDBC-Treiber und seine Auslieferung werden vor Implementierung ausdrücklich festgelegt.
- **Floodgate optional** für native Bedrock-Formulare; ohne Floodgate bleiben Geyser-Inventare und Befehle nutzbar.
- **LuckPerms optional** zur Spiegelung technischer Außenrechte. Die interne Dorfmitgliedschaft bleibt in BlockLords führend.

### 13.2 Scheduler- und Datenregeln

Welt-, Block-, Inventory- und Entity-Zugriffe erfolgen im von Paper vorgesehenen synchronen beziehungsweise passenden Scheduler-Kontext. Datenbanken, Dateien, Webzugriffe und längere reine Berechnungen laufen asynchron. „Möglichst alles async“ ist ausdrücklich keine Architekturregel.

Bett-, Bewohner- und Arbeitsplatzdaten werden ereignisgesteuert aktualisiert und gecacht. Vollständige Zylinder- oder Chunk-Scans laufen nicht periodisch im Tick. Ein initialer Dorfscan wird gestaffelt, mit Abbruch- und Fortsetzungspunkt. FAWE-Aktionen verwenden dessen vorgesehene API und Thread-Grenzen; vor dem Einfügen erfolgen Kollisions- und Berechtigungsprüfungen.

### 13.3 Persistente Kernobjekte

Mindestens vorgesehen: `villages`, `claims`, `members`, `roles`, `plots`, `block_ownership`, `buildings`, `build_sites`, `villagers`, `jobs`, `storage`, `storage_transactions`, `research`, `missions`, `events`, `event_participants`, `contracts`, `escrow`, `diplomacy`, `activity`, `audit`, `schema_migrations`.

Ressourcenbewegungen, Gründungen, Rollenänderungen, Admin-Eingriffe, Verträge und Transfers erhalten Audit-Einträge. Datenbankmigrationen werden vor dem Start auf eine Sicherung angewendet; bei ungültiger Konfiguration startet das Plugin nicht mit halb aktiviertem Schutz.

### 13.4 Schutzumfang

Der eigene Schutz muss neben Blockabbau und Interaktion mindestens Container, Türen, Betten, Redstone, Item Frames, Armor Stands, Dorfbewohner, Explosionen, Feuer, Kolben über Regionsgrenzen, Flüssigkeiten, Mob-Griefing, Projektile und Ereignis-Umgehungen berücksichtigen. Admin-Bypass ist ausdrücklich, protokolliert und standardmäßig nicht aktiv.

### 13.5 Build- und Testbasis

Vor Phase 1 werden die fehlenden Gradle-Wrapper-Startskripte ergänzt. Danach sind mindestens vorgesehen: reproduzierbarer Java-21-Build, Unit-Tests für Kosten, Produktion, Claim-Bedingungen und Vertragszustände sowie Integrationstests auf einem echten Paper-Testserver. Für jeden Release werden JAR, Config-Defaults und Datenbankmigration gemeinsam geprüft.

## 14. Umsetzungsphasen mit Abnahmekriterien

| Phase | Konkreter Lieferumfang | Spielerisches Abnahmekriterium |
|---|---|---|
| **1 — Fundament und sichere Gründung** | Wrapper-Reparatur; Config-Validierung; SQLite-Migrationen; Glocke/Dorfinfo; Gründungsquest; erster Claim; Zylinder; Basisrollen Landherr/Fremder; Schutzpaket; Altbestand-Registrierung; Rathaus-Basis; Audit; Adminbefehle; Java/Bedrock-Zugang | Zwei Testspieler gründen beziehungsweise besuchen ein Dorf, erleben einen geführten ersten Erfolg und können nach Neustart sicher weiterspielen. Kein fremder Block- oder Containerzugriff; Bedrock-Gründung ohne Java-Hilfe |
| **2 — Gemeinsames Wachstum und Wirtschaft** | Blaupausen-Vorschau und FAWE-Staging; Materialbeiträge mehrerer Spieler; Rathaus/Lager; Wohnhaus, Farm, Mine, Schmiede, Bibliothek; Arbeiterzuordnung; Einwanderung; Produktionsbilanz; Missionen; Dorf-Level; Bürger/Gefolgsmann und Parzellen; Offline-Zusammenfassung; Telemetrie | Eine 30-Minuten-Testsitzung ermöglicht mindestens einen sichtbaren gemeinsamen Fortschritt. Ein zweiter Spieler hat ohne Adminhilfe eine sinnvolle Aufgabe. Lager voller Bestand führt zu keiner stillen Vernichtung; Produktion überlebt NPC-Pathfindingfehler |
| **3 — Verteidigung als Ereignis** | Kaserne, Schießturm, Milizteams, Respawnkosten, freiwilliger Raidlauf, aktive Teilnehmerwahl, Schwierigkeitsstufen, Reparatur/Nachschub, Trophäen und Held-des-Dorfes-Rückmeldung | Ein vorbereitetes Dorf kann einen Raidlauf gemeinsam bestehen; Niederlage ist erklärbar und reparierbar. Zuschauer und nicht angemeldete Spieler werden nicht in PvP oder Plugin-Verluste hineingezogen |
| **4 — Wettkämpfe, Diplomatie und Herrschaft** | Multi-Claim-Bedingungen; Nachfolge; Markt-/Handelsverträge; NAP/Bündnis; Dorfspiele Glockensturm und Raidlauf-Duell; Teilnehmer-Opt-in; Treuhandkonto; Kriegsvertrag ohne Gebäudeschaden; Herrschaftskampf mit einstimmiger Zustimmung und atomarem Dorftransfer; Disconnect-/Restart-Regeln; Friedensfrist; LuckPerms-Bridge | Zwei Dörfer können ein kurzes Spiel und einen Ressourcen-Krieg vollständig ohne Admin abschließen. Beim Herrschaftskampf sind alle Stimmen und Risiken vorher nachweisbar; der Transfer erfolgt genau einmal und nur nach eindeutigem Sieg; Privatbesitz bleibt geschützt; Neustart oder Disconnect erzeugt keinen Doppelgewinn oder unbeabsichtigten Verlust |

**Nicht im Startumfang:** Lieferwettlauf und Baustellen-Duell als weitere Disziplinen, destruktive Kriege, beliebig konfigurierbare Rechte-Matrix-Zellen, komplexe Steuer-/Schuldenwirtschaft und automatische Löschung von Bauten. Der Herrschaftskampf gehört dagegen als bestätigter Spezialmodus zu Phase 4.

## 15. Balancing- und Spieltest-Regeln

Vor größerem Ausbau werden lokale, datensparsame Messwerte erhoben: Zeit bis zur Gründung und zum ersten Gebäude, Zeit bis zum zweiten Dorf, Ertrag und Verbrauch je Ressource, Dauer mit vollem Lager, Anteil manueller gegenüber passiver Erträge, abgebrochene Bauvorschauen, Missionsteilnahme je Rolle, Raid-Erfolg nach Schwierigkeit sowie Wettkampfteilnahme und Ergebnisverteilung. Es gibt keine externe Übertragung dieser Daten durch BlockLords.

**Erste Zielgrößen für den Familien-SMP:**

- erste Gründung und Rathaus in einer kurzen Einstiegssitzung,
- erster gemeinsamer Ausbau innerhalb von 30 Minuten nach Gründung,
- zweites Dorf nach mehreren bedeutungsvollen Entwicklungsschritten statt bloßem Warten,
- kein einzelner Beruf als dominante Smaragdquelle,
- mindestens eine sinnvolle Aufgabe für Bürger und Gefolgsmann in jeder Testsitzung,
- keine dauerhafte Vernichtung von Spielerarbeit durch reguläre Konflikte oder Inaktivität.

Nach jedem Test werden zuerst Erklärbarkeit, Langeweile, Frust und Rollenungleichgewicht bewertet, danach Zahlen angepasst. Balancing erfolgt in kleinen nachvollziehbaren Schritten und wird im `DEVLOG.md` dokumentiert.

## 16. Final bestätigte Entscheidungen

Alexander hat am 23.11.2026 alle zunächst empfohlenen A-Varianten und die Restvorgaben bestätigt: erste Gründung nach kurzer Quest kostenlos; Gebäude mit Materialien plus moderaten Smaragdkosten; virtueller Vorrat mit physischer Ein-/Auszahlung; Offline-Produktion mit 50 % für höchstens vier Stunden; Ruhezustand mit Schutz und Admin-Freigabe; Bürger-Parzellen und eine Hauptmitgliedschaft; einmalige Altbestand-Registrierung; vollständiges Crossplay ab Phase 1 sowie Phase 1 als sicherer vertikaler Schnitt. `decay.enabled: false` schaltet Zerfall vollständig ab.

Der Herrschaftskampf um ein gesamtes Dorf ist als extremer Spezialmodus bestätigt. Erforderlich sind die Zustimmung beider Landherren sowie aller registrierten Bürger und Gefolgsleute beider Dörfer. Der bisherige Projektserver wird bereits als Testserver verwendet; Entwicklung und GitHub-Bereitstellung erfolgen getrennt vom Deployment durch Alexander beziehungsweise Axon.

Dorfnamen sind Freitext mit Vorschlagsliste. Die Rollennamen **Landherr, Gefolgsmann, Bürger und Asyl** bleiben bestehen. Bürger-Parzellen sind standardmäßig aktiviert. Raid-Belohnung umfasst kleine Smaragdboni und sichtbaren Dorfruhm. Die bisherigen Zahlen für Radien, Limits, Kosten, Timer und Erträge sind Startwerte in der Konfiguration und keine unveränderlichen Designentscheidungen.
