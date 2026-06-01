# SAMeal2

Aplikace pro ověřování a výdej jídel pomocí QR kódů.

## Sestavení aplikace (Build)

Pro vygenerování instalačního balíčku (APK) pro Android zařízení použijte následující příkaz v terminálu:

```bash
./gradlew assembleRelease
```

## Umístění APK

Po úspěšném dokončení příkazu naleznete výsledný soubor v:
`app/build/outputs/apk/release/SAMeal2-release-2.0.8.apk`

## Funkce
- Skenování QR kódů pomocí kamery.
- Ověřování plateb a výdeje stravy přes API `api.samorlova.cz`.
- Výběr konkrétního dne a typu jídla (snídaně/oběd/večeře).
- Statistiky vydaných porcí v reálném čase.
- Moderní Material 3 design s podporou Dynamic Color.
- Haptická a vizuální odezva při skenování.
