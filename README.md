# SmurfUpgrades 1.0.0 — Paper 1.21.4 / Java 21

Projekt źródłowy pluginu. **Nie jest to gotowy skompilowany JAR**.

## Budowanie
1. Zainstaluj JDK 21 i Maven 3.9+.
2. W katalogu projektu uruchom `mvn clean package` (wymaga Internetu do pobrania zależności).
3. Skopiuj `target/SmurfUpgrades-1.0.0.jar` do `plugins/`.
4. Zainstaluj Vault, kompatybilny plugin ekonomii (np. EssentialsX Economy) i opcjonalnie HeadDatabase.
5. Uruchom serwer, uzupełnij ID główek w `plugins/SmurfUpgrades/upgrades.yml`, wykonaj `/smurfadmin reload`.

## Komendy
- `/ulepsz` lub `/ulepszenie` — GUI 54 sloty; LPM ulepszanie, PPM podgląd 12 poziomów.
- `/sety` — lista setów; `/sety 12` — pełny podgląd poziomu 12.
- `/cenyulepszen` — koszt pojedynczego poziomu całego setu i koszt sumaryczny.
- `/smurfadmin reload` — permission `smurf.admin`.

## Mechanika
- 8 osobnych kategorii, 12 poziomów; pierwszy darmowy.
- Slot 21 = obecny przedmiot, slot 23 = następny; w drugim rzędzie 11/13/15 = 25/50/100%.
- 25% kosztuje 25% ceny pełnej, 50% połowę, 100% pełną cenę.
- Porażka zabiera koszt próby, ale nie cofa odblokowanego poziomu.
- Poziomy są przypisane do gracza, zapis w `players.yml`.
- Na pierwszym wejściu i po odrodzeniu plugin wydaje aktualny set. Przedmioty pluginu nie wypadają przy śmierci, aby zapobiec ich duplikowaniu.
- Główki: HeadDatabase, po wpisaniu właściwych ID. Bez ID pokazuje się domyślna główka gracza — **wymaga konfiguracji**, aby wyglądała jak postać Smerfów.
- Kolory skórzanej zbroi i enchanty edytowalne w `upgrades.yml`; możliwe enchanty ponad vanilla.

## Ważne
- Ochrona enchantem Protection jest ograniczona mechaniką vanilla; Protection 60 nie daje 6x większej ochrony niż Protection 10.
- Główki HeadDatabase wymagają posiadania odpowiednich tekstur w bazie; nie są dołączone do tego archiwum.
- Kod nie był jeszcze kompilowany ani testowany na rzeczywistym serwerze. Przetestuj na serwerze testowym przed wdrożeniem.
- Sloty kategorii, szans, obecnego i następnego przedmiotu oraz powrotu można zmieniać w `gui.yml`.

**Uwaga:** Bez skonfigurowanych ID HeadDatabase plugin wyświetla zwykłe główki. Żeby mieć faktycznie Smerfy, musisz wpisać poprawne ID lub przygotować własne tekstury.
