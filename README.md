# Steam Browser

Prosta przeglądarka popularnych gier Steam napisana w Kotlinie i Jetpack Compose.

## Konfiguracja

Wymagane lokalnie:

- Android Studio z zainstalowanym Android SDK,
- klucz Steam Web API,
- Java 17 lub nowsza.

Gradle jest dodany jako wrapper, więc nie trzeba instalować go osobno.

1. Otwórz projekt w Android Studio. Jeśli IDE zapyta o SDK, wskaż lub pobierz Android SDK.
2. Utwórz plik `local.properties` w katalogu głównym projektu.
3. Dodaj do niego klucz Steam Web API:

```properties
STEAM_API_KEY=twoj_klucz
```

4. Jeśli Android Studio nie dopisze ścieżki SDK automatycznie, dodaj ją ręcznie:

```properties
sdk.dir=C\:\\Users\\TwojUzytkownik\\AppData\\Local\\Android\\Sdk
```

5. Uruchom moduł `app` w Android Studio albo zbuduj projekt:

```powershell
.\gradlew.bat assembleDebug
```

## Funkcje

- lista popularnych gier Steam,
- liczba aktualnych graczy,
- minimalny ekran szczegółów gry,
- ulubione przechowywane w pamięci aplikacji,
- polski interfejs oraz komunikaty błędów.
