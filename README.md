# Chuck & Facts

Chuck & Facts e' un'applicazione desktop JavaFX che raccoglie battute, curiosita' e clip da diversi Web Service pubblici. L'app permette di generare contenuti casuali, tradurli in italiano tramite Intelligenza Artificiale, salvarli nei preferiti e giocare con quiz basati su fatti veri/falsi e clip "Whoa" di Keanu Reeves.

## Funzionalita'

- Generazione di battute Chuck Norris casuali o filtrate per categoria.
- Generazione di dad joke da icanhazdadjoke.com.
- Generazione di fatti curiosi da Useless Facts.
- Traduzione automatica in italiano dei contenuti in inglese.
- Gioco "Vero o falso" basato su fatti reali e versioni false ma plausibili generate dall'IA.
- Gioco "Whoa Game" con clip video/audio e scelta del film corretto.
- Cronologia dei contenuti generati.
- Preferiti salvati localmente.
- Copia rapida del testo visualizzato.

## Tecnologie Utilizzate

- Java 17
- JavaFX 21
- Maven
- Gson per serializzazione e parsing JSON
- JUnit 5 per i test
- API HTTP tramite `java.net.http.HttpClient`
- DeepSeek Chat Completions per le elaborazioni IA

## Web Service Utilizzati

| Servizio | Endpoint | Uso |
| --- | --- | --- |
| ChuckNorris.io | `GET https://api.chucknorris.io/jokes/random` | Battuta Chuck Norris casuale |
| ChuckNorris.io | `GET https://api.chucknorris.io/jokes/random?category={categoria}` | Battuta Chuck Norris per categoria |
| ChuckNorris.io | `GET https://api.chucknorris.io/jokes/categories` | Elenco categorie disponibili |
| icanhazdadjoke | `GET https://icanhazdadjoke.com/` | Dad joke casuale in formato JSON |
| Useless Facts | `GET https://uselessfacts.jsph.pl/api/v2/facts/random?language=en` | Fatto casuale in inglese |
| Whoa API | `GET https://whoa.onrender.com/whoas/random` | Clip casuale "Whoa" |
| Whoa API | `GET https://whoa.onrender.com/whoas/movies` | Lista film per il quiz |
| DeepSeek | `POST https://api.deepseek.com/chat/completions` | Traduzione e generazione di fatti falsi plausibili |

## Requisiti

- JDK 17 o superiore
- Connessione Internet
- Chiave API DeepSeek per usare traduzioni e gioco vero/falso

## Configurazione

Il progetto legge la configurazione da variabili d'ambiente oppure da un file locale `config.properties`.

1. Copiare il file di esempio:

```bash
copy config.example.properties config.properties
```

2. Inserire la propria chiave DeepSeek:

```properties
DEEPSEEK_API_KEY=your-deepseek-api-key
DEEPSEEK_MODEL=deepseek-v4-flash
```

Le variabili d'ambiente hanno priorita' sui valori presenti in `config.properties`.

## Avvio

Su Windows e' disponibile lo script:

```bash
start.bat
```

In alternativa, usare Maven Wrapper:

```bash
.\mvnw.cmd javafx:run
```

## Test

Per eseguire la suite di test:

```bash
.\mvnw.cmd test
```

I test verificano il parsing delle risposte JSON, la gestione della configurazione e parte della logica dei servizi.

## Struttura del Progetto

```text
src/main/java/app       Avvio dell'app JavaFX
src/main/java/gui       Interfaccia utente e flussi applicativi
src/main/java/model     Record e DTO delle risposte API
src/main/java/service   Client dei Web Service esterni
src/main/java/util      Configurazione e utilita' HTTP
src/test/java           Test JUnit
```

## Licenza

Questo progetto e' distribuito con licenza MIT. Vedere il file `LICENSE` per i dettagli.

Le dipendenze software e le API esterne usate dall'app sono riepilogate in `THIRD_PARTY_NOTICES.md`.
