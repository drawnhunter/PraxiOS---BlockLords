# BlockLords

Dorf-Claim-, Wirtschafts-, Diplomatie- und Kriegssystem für PaperMC (Java 21).
Entstanden auf dem Praxios-Familien-SMP (mc.praxios.eu, Paper 1.21.11 auf einem OnePlus 6T in Termux).

## Konzept
Siehe [KONZEPT.md](KONZEPT.md) — Spielidee, Phasen, Design-Entscheidungen.

## Tech-Stack
- Java 21 (Paper-API 1.21.11) — **Achtung: Termux-host läuft JDK 21, Plugins MÜSSEN Java-21-kompatibel bleiben**
- FAWE-API (FastAsyncWorldEdit) für Schematic-Platzierung — bereits auf dem Zielserver installiert
- SQLite (eingebettet) für Claims/Gebäude/Wirtschaft
- LuckPerms optional zur Rank-Spiegelung

## Build
```
gradlew build
# Jar: build/libs/BlockLords-<version>.jar
```
Erster Build lädt Gradle herunter (~130 MB).

## Deploy-Loop (Zielserver = OnePlus 6T / Termux)
Jar liegt nach Build in `build/libs/`. Aufs Handy bringen (adb push → `~/mcserver/plugins/`), Server-Neustart (~50 s Boot mit Plugin-Set), Log-Check. Hilfsskripte liegen im Workspace unter `temp/oneplus/` (`plug-restart.sh`, `restart.sh`), ausführbar per SSH (`ssh -p 8022 u0_a130@localhost` via `adb forward tcp:8022 tcp:8022`, Key `~/.ssh/axon_oneplus6t`). Server-Konsole: RCON 127.0.0.1:25575 (Client `temp/oneplus/rcon.ps1`, Passwort in `enable-rcon.sh` auf dem Handy).
