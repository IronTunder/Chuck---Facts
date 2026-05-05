# Third-party notices

Questo documento riepiloga le principali dipendenze software e le API esterne usate da Chuck & Facts.
Non modifica la licenza MIT del codice del progetto: serve come riferimento per attribution, licenze e condizioni dei servizi integrati.

## Dipendenze software

| Componente | Uso nel progetto | Licenza / riferimento |
| --- | --- | --- |
| OpenJFX / JavaFX | Interfaccia desktop, controlli UI e media playback | GPL v2 con Classpath Exception, come indicato dal progetto OpenJFX: https://openjfx.io/ |
| Gson | Serializzazione e deserializzazione JSON | Apache License 2.0: https://github.com/google/gson |
| JUnit 5 | Test automatici | Eclipse Public License 2.0: https://junit.org/junit5/ |
| Maven Compiler Plugin | Compilazione del progetto | Apache License 2.0: https://maven.apache.org/plugins/maven-compiler-plugin/ |
| Maven Surefire Plugin | Esecuzione dei test | Apache License 2.0: https://maven.apache.org/surefire/maven-surefire-plugin/ |
| JavaFX Maven Plugin | Avvio JavaFX da Maven | Apache License 2.0: https://github.com/openjfx/javafx-maven-plugin |

## API e servizi esterni

Le API elencate sotto non sono distribuite insieme al codice sorgente del progetto. L'app le interroga via HTTP durante l'esecuzione; l'uso dei dati, dei media e degli output restituiti resta soggetto ai termini dei rispettivi provider.

| Servizio | Endpoint usati | Note / riferimento |
| --- | --- | --- |
| ChuckNorris.io | `GET https://api.chucknorris.io/jokes/random`, `GET https://api.chucknorris.io/jokes/random?category={categoria}`, `GET https://api.chucknorris.io/jokes/categories` | API pubblica di battute Chuck Norris. Repository/progetto con licenza GPL-3.0: https://github.com/chucknorris-io/chuck-api |
| icanhazdadjoke | `GET https://icanhazdadjoke.com/` | API pubblica senza autenticazione. La documentazione richiede un header `Accept` appropriato e consiglia un `User-Agent` personalizzato: https://icanhazdadjoke.com/api |
| Useless Facts | `GET https://uselessfacts.jsph.pl/api/v2/facts/random?language=en` | API pubblica per fatti casuali. Documentazione: https://uselessfacts.jsph.pl/ |
| Whoa API | `GET https://whoa.onrender.com/whoas/random`, `GET https://whoa.onrender.com/whoas/movies` | API pubblica con clip e metadati sui "whoa" di Keanu Reeves. Documentazione: https://whoa.onrender.com/ |
| DeepSeek API | `POST https://api.deepseek.com/chat/completions` | API con autenticazione Bearer per traduzione e generazione di contenuti. Documentazione API: https://api-docs.deepseek.com/; termini Open Platform: https://cdn.deepseek.com/policies/en-US/deepseek-open-platform-terms-of-service.html |

## Note operative

- Le chiavi API DeepSeek non devono essere incluse nel repository.
- I contenuti generati o recuperati dai servizi esterni possono essere soggetti a limiti, variazioni, rate limit o condizioni aggiornate dai provider.
- Prima di distribuire pubblicamente una build, verificare di rispettare i termini correnti dei servizi e le eventuali attribution richieste.
