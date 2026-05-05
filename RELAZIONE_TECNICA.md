# Relazione Tecnica - Chuck & Facts

## Descrizione del Progetto

Chuck & Facts e' un'applicazione desktop sviluppata in Java 17 con interfaccia JavaFX. Lo scopo del progetto e' recuperare contenuti leggeri da Web Service esterni, mostrarli in modo interattivo e arricchirli con funzioni di Intelligenza Artificiale. L'applicazione include sezioni dedicate a battute, fatti curiosi, cronologia, preferiti e due modalita' di gioco.

L'architettura e' organizzata in modo semplice:

- `service`: contiene i client dei servizi esterni.
- `model`: contiene i record usati per deserializzare le risposte JSON.
- `util`: contiene configurazione e invio centralizzato delle richieste HTTP.
- `gui`: gestisce la finestra JavaFX, la navigazione, la cronologia, i preferiti e i giochi.

## Web Service Scelti

Il progetto non si basa su un solo Web Service, ma su una combinazione di API REST pubbliche. La scelta e' coerente con l'obiettivo dell'applicazione: offrire contenuti casuali, brevi e facilmente visualizzabili in una GUI desktop.

### ChuckNorris.io

ChuckNorris.io fornisce battute casuali su Chuck Norris e un elenco di categorie disponibili. E' usato per la sezione "Chuck Norris" dell'applicazione.

Endpoint utilizzati:

- `GET https://api.chucknorris.io/jokes/random`
- `GET https://api.chucknorris.io/jokes/random?category={categoria}`
- `GET https://api.chucknorris.io/jokes/categories`

Elaborazione:

- Il JSON ricevuto viene deserializzato nel modello `ChuckNorrisResponse`.
- Il campo principale usato dall'app e' `value`.
- Le categorie vengono caricate in una `ComboBox` e usate per filtrare la richiesta.

### icanhazdadjoke

Il servizio icanhazdadjoke restituisce battute brevi in stile "dad joke". Viene usato nella schermata dedicata alle dad joke.

Endpoint utilizzato:

- `GET https://icanhazdadjoke.com/`

Header utilizzati:

- `Accept: application/json`
- `User-Agent: Chuck & Facts`

Elaborazione:

- L'header `Accept` forza la risposta in JSON.
- Il campo `joke` viene estratto dal modello `DadJokeResponse`.
- Il testo viene poi tradotto in italiano tramite il servizio IA.

### Useless Facts

Useless Facts restituisce fatti casuali in inglese. Il progetto lo usa sia come contenuto informativo sia come base per il gioco "Vero o falso".

Endpoint utilizzato:

- `GET https://uselessfacts.jsph.pl/api/v2/facts/random?language=en`

Elaborazione:

- Il campo `text` viene estratto dal modello `UselessFactResponse`.
- Nella visualizzazione normale, il testo viene tradotto in italiano.
- Nel gioco, il fatto originale puo' rimanere vero oppure essere trasformato dall'IA in una frase falsa ma plausibile.

### Whoa API

Whoa API fornisce clip, audio e metadati delle scene in cui Keanu Reeves pronuncia "Whoa". Il progetto la usa per creare un quiz multimediale.

Endpoint utilizzati:

- `GET https://whoa.onrender.com/whoas/random`
- `GET https://whoa.onrender.com/whoas/movies`

Elaborazione:

- La risposta casuale viene deserializzata in una lista di `WhoaResponse`.
- L'app seleziona il primo elemento valido.
- Vengono controllati film, anno e media riproducibili.
- La lista dei film viene usata per generare risposte multiple: una corretta e tre errate.

## Endpoint IA Utilizzato

Per le funzioni di Intelligenza Artificiale viene usato DeepSeek tramite endpoint compatibile con chat completions.

Endpoint utilizzato:

- `POST https://api.deepseek.com/chat/completions`

Autenticazione:

- Header `Authorization: Bearer {DEEPSEEK_API_KEY}`

Body principale:

```json
{
  "model": "deepseek-v4-flash",
  "temperature": 0.1,
  "stream": false,
  "messages": [
    {
      "role": "system",
      "content": "Istruzioni dell'applicazione"
    },
    {
      "role": "user",
      "content": "Testo da elaborare"
    }
  ]
}
```

Il modello e la chiave API sono configurabili tramite `config.properties` o variabili d'ambiente.

## Elaborazione Affidata all'Intelligenza Artificiale

L'IA non viene usata per recuperare dati dai Web Service: questa parte e' gestita tramite chiamate HTTP REST tradizionali. L'IA interviene dopo il recupero del contenuto, con due compiti principali.

1. Traduzione in italiano

I contenuti ricevuti in inglese da ChuckNorris.io, icanhazdadjoke e Useless Facts vengono inviati a DeepSeek con un prompt vincolato. Il modello deve rispondere solo con la traduzione, senza note o virgolette. La temperatura usata e' bassa (`0.1`) per ottenere risposte stabili e aderenti al testo originale.

2. Generazione di un fatto falso plausibile

Nel gioco "Vero o falso", l'app parte da un fatto vero ottenuto da Useless Facts. Quando deve generare una domanda falsa, invia il fatto a DeepSeek chiedendo di modificare uno o due dettagli concreti, mantenendo la frase credibile. In questo caso la temperatura e' piu' alta (`0.8`) per favorire variazioni piu' creative.

## Flusso di Elaborazione

1. L'utente sceglie una sezione dell'app.
2. La GUI invoca il servizio Java corrispondente.
3. `HttpUtil` costruisce la richiesta HTTP, applica timeout e controlla lo status code.
4. Gson deserializza il JSON nel modello corretto.
5. Se necessario, il testo viene inviato a DeepSeek per traduzione o trasformazione.
6. Il risultato viene mostrato nella GUI e puo' essere copiato, salvato nei preferiti o inserito nella cronologia.

## Gestione Errori e Dati Locali

Le chiamate HTTP sono centralizzate in `HttpUtil`, che gestisce:

- errori di rete;
- JSON non valido;
- status code non riusciti;
- errori di autenticazione;
- troppe richieste.

I preferiti e i record dei giochi vengono salvati nella cartella utente, sotto `.chuck-and-facts`, in modo da mantenere i dati anche dopo la chiusura dell'applicazione.

## Conclusione

Chuck & Facts integra piu' Web Service REST con una GUI JavaFX e usa l'Intelligenza Artificiale come livello di arricchimento del contenuto. Le API pubbliche forniscono dati e media, mentre DeepSeek si occupa di operazioni linguistiche e creative: traduzione e generazione controllata di affermazioni false ma plausibili. Questa separazione rende chiaro il ruolo dei servizi tradizionali e quello dell'IA all'interno dell'applicazione.
