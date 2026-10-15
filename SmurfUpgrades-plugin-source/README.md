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


## Poprawka 1.0.1
- `/ulepsz` przyznaje brakujące elementy darmowego zestawu, także graczom którzy weszli na serwer przed instalacją poprawki.
- Udany zakup ulepszenia aktualizuje przedmiot lub wydaje brakujący element, bez kasowania zwykłego wyposażenia.
- Otwarcie kolejnego GUI jest wykonywane w następnym ticku zgodnie z zasadami Paper.
- **Przed instalacją wykonaj kopię zapasową `plugins/SmurfUpgrades/players.yml` i ekwipunków.**
- Aby zbudować: `mvn clean package`. Plik wynikowy: `target/SmurfUpgrades-1.0.1.jar`.
- Uwaga: odblokowane poziomy pozostają zapisane w `players.yml`.


## Odświeżone menu /ulepsz (edycja GUI 2.0)
- 54 sloty, ciemnoniebieskie obramowanie, błękitne akcenty i symetryczny układ.
- Kategorie domyślnie: 19, 20, 21, 22, 23, 24, 25 oraz 31 (elytra).
- Każdy przedmiot pokazuje `⭐1`–`⭐12`, nazwę postaci i instrukcje LPM/PPM.
- Slot 40 to informacje; 49 zamyka menu.
- Menu wyboru szans pozostaje bez zmian: aktualny przedmiot 21, następny 23.
- **Ważne:** jeśli masz starszy `plugins/SmurfUpgrades/gui.yml`, zastąp go nowym
  lub usuń go po wykonaniu kopii zapasowej, aby plugin wygenerował nowy układ.
- To projekt źródłowy. Przed użyciem na produkcji należy go skompilować i przetestować.


## Nowości 2.0
- `/topulepszen` — ranking TOP graczy według sumy poziomów 8 kategorii (maks. 96).
- Konfigurowalne efekty sukcesu i porażki: dźwięki, cząsteczki i tytuł.
- Legendarna nazwa i lore dla ⭐12 oraz opcjonalny broadcast.
- Kolory każdego poziomu i format `⭐{level}` w `gui.yml`.
- Wszystkie nowe opcje w `config.yml`; po aktualizacji starszego serwera dopisz nowe sekcje z przykładowych plików, bo plugin nie nadpisuje istniejących plików.

**Ważne:** GUI to natywne menu 54 slotów. Do działania ekonomii potrzebne są Vault i plugin ekonomiczny. Główki wymagają poprawnych identyfikatorów HeadDatabase w `upgrades.yml`.


## Automatyczne główki (edycja)
- `upgrades.yml` ma wpisane HeadDatabase ID `11820` (Papa Smerf) na poziomach 1–11 i `86177` (Gargamel) na poziomie 12.
- **Uwaga:** poziomy 1–10 mają **tymczasowy zamiennik** Papy Smerfa, nie indywidualne tekstury postaci. Nie są to główki Steve'a.
- Jeśli HeadDatabase jest niedostępny, plugin używa wbudowanych tekstur Papy Smerfa i Gargamela poprzez Paper PlayerProfile.
- Zmień `levels.<poziom>.head-id` na sprawdzone ID odpowiedniej postaci, gdy będzie dostępne.
- Na istniejącym serwerze stary `plugins/SmurfUpgrades/upgrades.yml` nie jest nadpisywany; skopiuj wpisy `head-id` ręcznie lub wygeneruj konfigurację od nowa po wykonaniu kopii zapasowej.
- Źródła tekstur: https://minecraft-heads.com/custom-heads/head/11820-papa-smurf oraz https://minecraft-heads.com/custom-heads/head/86177-gargamel


## Uwaga przed produkcją
Ta paczka to kod źródłowy, nie przetestowany plik JAR. Kompilacja i testy integracyjne na Paper 1.21.4 są konieczne.
Wymagane: Java 21, Paper 1.21.4, Vault i plugin ekonomii. HeadDatabase opcjonalny.
Nie wszystkie 12 tekstur główek są zweryfikowane; domyślne tekstury dla poziomów 1–10 są zastępcze.
Wersja ta nie zawiera jeszcze integracji PlaceholderAPI; nie używaj placeholderów %smurf_...% bez rozszerzenia.
Przed aktualizacją wykonaj kopię plugins/SmurfUpgrades/players.yml.


## Siekiera: Fortune i więcej drewna (2.1)
Każdy poziom siekiery ⭐1–⭐12 ma coraz wyższe Fortune (I–XII) ustawiane w `upgrades.yml`.
W Minecraft Fortune standardowo nie zwiększa liczby zwykłych kłód, dlatego dodatkowo dodano własny bonus do drewna przy ścinaniu kłód i pni.
Domyślne mnożniki: ⭐1 = 1.00x, ⭐2 = 1.25x, ⭐3 = 1.50x, ... ⭐12 = 3.75x. Można je zmienić w `config.yml` (`axe-wood.levels`).
Bonus działa wyłącznie dla siekiery SmurfUpgrades trzymanej w głównej ręce, przy normalnym niszczeniu bloków, i nie działa w trybie kreatywnym. Przy aktywnych pluginach modyfikujących dropy zaleca się test integracyjny.

**Status:** źródła spakowane i sprawdzone pod kątem struktury; bez testu kompilacji i działania na serwerze nie można zagwarantować 100% poprawności.


## Wersja 2.2 — bonus nożyc
- Nożyce SmurfUpgrades zyskują osobny mnożnik dropu od ×1.00 (⭐1) do ×3.75 (⭐12).
- Dotyczy tylko wymienionych w `shears-drops.allowed-blocks` bloków (wełna, liście, pajęczyny).
- Mnożnik działa wyłącznie na standardowy drop bloku tego samego typu, z nożycami pluginu w głównej ręce i w survivalu.
- Fortune na nożycach może być konfigurowane w `upgrades.yml`; sam enchant nie zwiększa dropu wełny.
- Po aktualizacji starego `config.yml` dodaj sekcję `shears-drops` ręcznie.
- Projekt nie został skompilowany ani przetestowany na uruchomionym serwerze; wymaga testów integracyjnych.


## 2.3 — Netherite tools
- Siekiera jest teraz **NETHERITE_AXE**, kilof to nowa dziewiąta kategoria **NETHERITE_PICKAXE**.
- Kilof ma własne poziomy ⭐1–⭐12, Fortune, Efficiency i Unbreaking w `upgrades.yml`.
- Slot kilofa w `/ulepsz`: 32 (`gui.yml`); łącznie 9 kategorii, ranking max 108.
- Istniejący `players.yml` jest uzupełniany o nową kategorię przy wejściu gracza.
- Opcjonalny bonus rud `pickaxe-ores` jest domyślnie wyłączony, aby nie powielać Fortune.
- **Aktualizacja serwera:** nowe sekcje z `gui.yml`, `upgrades.yml`, `config.yml` trzeba przenieść do już istniejących konfiguracji.
- Kod źródłowy: kompilacja `mvn clean package` na GitHub Actions. Nie potwierdzono testu runtime.


## CustomModelData — miecz SmurfSword (2.4)
Miecz z pluginu (`DIAMOND_SWORD`) ma CustomModelData `240001` (również w podglądach GUI).
Dzięki paczce `SmurfSword-CMD-1.21.4.zip` inne diamentowe miecze zachowują wygląd vanilla.
Konfiguracja: `config.yml` → `sword-texture.enabled` i `sword-texture.custom-model-data`.
**Uwaga:** przy aktualizacji istniejącego serwera dopisz `sword-texture` do obecnego configu lub plugin użyje domyślnych wartości. Stare miecze w ekwipunku otrzymają model po ponownym wygenerowaniu przez plugin (np. po ulepszeniu); nie są automatycznie zmieniane.
