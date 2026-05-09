# Tabliczka mnożenia — Android

Prosta aplikacja Android w Java + XML.

## Co robi aplikacja

- Ma przyciski: Mnożenie przez 1, 2, 3 ... 9.
- Po wejściu w kategorię pokazuje losowe pytania, np. `3 × 7 = ?`.
- Zadanie jest nauczone, gdy użytkownik odpowie dobrze 5 razy pod rząd.
- Błąd zeruje licznik dla tego konkretnego działania.
- Wyniki i daty zaliczeń zapisują się w `SharedPreferences`.
- Po ponownym otwarciu aplikacji nadal widać zaliczone testy.

## Jak uruchomić najłatwiej

1. Otwórz Android Studio.
2. Utwórz nowy projekt: **Empty Views Activity** albo zwykły pusty projekt Android.
3. Język wybierz: **Java**.
4. Nazwa projektu: `TabliczkaMnozenia`.
5. Package name: `com.example.tabliczkamnozenia`.
6. Wklej pliki z tego repozytorium w te same miejsca.
7. Uruchom aplikację zielonym przyciskiem **Run**.

## Ważne pliki

- `app/src/main/java/com/example/tabliczkamnozenia/MainActivity.java`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`

## Reklama AdMob

Ta wersja ma dodany baner AdMob na dole ekranu.

Na razie użyte są TESTOWE ID Google:
- App ID: `ca-app-pub-3940256099942544~3347511713`
- Banner ID: `ca-app-pub-3940256099942544/9214589741`

Na testowych ID aplikacja nie zarabia. Przed publikacją w Google Play trzeba założyć aplikację w AdMob i podmienić wartości w pliku:

`app/src/main/res/values/strings.xml`

Aplikacja jest edukacyjna dla dzieci, więc reklamy są ustawione jako child-directed i z maksymalną kategorią treści G.


## Wersja 1.3

Dodano:
- system gwiazdek,
- odznaki za zaliczone kategorie, perfekcyjny test i progi gwiazdek,
- dzienne wyzwanie 10 pytań,
- bonusy gwiazdkowe,
- kolorowe kafelki w menu,
- reklamy: baner + pełnoekranowa po zaliczeniu kategorii oraz po zakończeniu testu/wyzwania.
