# Naprawa problemu z powrotem do aplikacji po logowaniu Steam

Użytkownik zgłasza "biały ekran" po zalogowaniu przez Steam i brak powrotu do aplikacji. Problem wynika z użycia `Chrome Custom Tabs` wraz z adresem `https://steambrowser.mobilki.pl/login`. Od systemu Android 12, linki `https` wymagają weryfikacji (App Links), aby automatycznie otwierać aplikację. Bez weryfikacji, przeglądarka próbuje załadować stronę, a ponieważ pod tym adresem prawdopodobnie nic nie ma, wyświetla się biały ekran.

## Proponowane zmiany

Rozwiązaniem jest zamiana `CustomTabsIntent` na `WebView` wewnątrz aplikacji. Pozwoli to na ręczne przechwycenie URL przekierowania (`shouldOverrideUrlLoading`), co zadziała niezależnie od weryfikacji domeny.

---

### UI Components

#### [LoginScreen.kt](file:///C:/SteamSearch/app/src/main/java/pl/mobilki/steambrowser/ui/LoginScreen.kt)

- Dodanie stanu `showWebView`, który będzie kontrolował wyświetlanie okna logowania.
- Implementacja komponentu `SteamLoginWebView` (używającego `AndroidView` i `WebView`), który przechwyci adres powrotny Steam.
- Po przechwyceniu adresu `https://steambrowser.mobilki.pl/login`, WebView zostanie zamknięte, a dane przekazane do `viewModel.verifySteamLogin(url)`.

```kotlin
@Composable
fun SteamLoginWebView(url: String, onRedirect: (String) -> Unit, onDismiss: () -> Unit) {
    // Implementacja z użyciem AndroidView i WebViewClient
}
```

---

### Logic & Configuration

#### [LoginViewModel.kt](file:///C:/SteamSearch/app/src/main/java/pl/mobilki/steambrowser/LoginViewModel.kt)

- Pozostawienie obecnych adresów `returnTo` i `realm`, ponieważ są one poprawne dla Steam i zostaną przechwycone przez `WebView`.

#### [MainActivity.kt](file:///C:/SteamSearch/app/src/main/java/pl/mobilki/steambrowser/MainActivity.kt)

- Pozostawienie obsługi intencji, co pozwoli na działanie logowania również w przyszłości, gdyby użytkownik zdecydował się na poprawną konfigurację App Links.

## Plan weryfikacji

### Weryfikacja ręczna
1. Uruchomienie aplikacji i przejście do zakładki Profil.
2. Kliknięcie "Zaloguj przez Steam".
3. Upewnienie się, że otwiera się wewnętrzne okno (WebView) ze stroną logowania Steam.
4. Zalogowanie się na konto Steam.
5. Zaobserwowanie, czy po zalogowaniu okno WebView automatycznie się zamyka.
6. Sprawdzenie, czy aplikacja wyświetla "Weryfikacja logowania..." a następnie pomyślnie loguje użytkownika.

### Polecenia do uruchomienia
- Budowanie i instalacja: `./gradlew installDebug`
- Podgląd logów: `adb logcat | grep LoginViewModel`
