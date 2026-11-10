# Comparatif polyglotte RMI, REST, Événementiel

## 1. Objectif

Cette étude compare l'implémentation du service de calcul réalisée avec **Java RMI** et celle réalisée avec **REST** dans le cadre du même besoin fonctionnel.

Les deux services permettent notamment de réaliser les opérations :

* addition ;
* soustraction ;
* multiplication ;
* division ;
* consultation de l'historique des opérations.

La comparaison porte sur le couplage, la lisibilité du contrat, les performances mesurées, la gestion des erreurs et les moyens de test.

Depuis la séance 7, une troisième implémentation événementielle (RabbitMQ, module `/evenementiel`) complète la comparaison.

---

## 2. Comparaison du couplage

### RMI

L'implémentation RMI repose directement sur les mécanismes Java.

Le client utilise l'interface distante `CalculatorService` et doit connaître le contrat Java du service.

Exemple :

```java
CalculatorService calculator =
        (CalculatorService)
                registry.lookup("CalculatorService");

double result = calculator.add(4, 7);
```

Le client est donc fortement lié à l'écosystème Java et au contrat défini par l'interface distante.

Pour utiliser le service, il faut notamment disposer des éléments nécessaires au fonctionnement du système RMI et connaître le nom du service enregistré dans le registre.

### REST

Le service REST expose une interface HTTP accessible à travers des URL et des méthodes HTTP standard.

Exemple :

```http
POST /operations
Content-Type: application/json
```

avec :

```json
{
    "type": "ADD",
    "a": 4,
    "b": 7
}
```

Un client n'a pas besoin d'utiliser Java pour communiquer avec le service. Il doit principalement connaître l'URL, la méthode HTTP, le format JSON et les règles du contrat HTTP.

### Observation

Le couplage est donc différent :

* RMI est fortement lié au modèle objet Java ;
* REST repose sur des standards HTTP et JSON, ce qui facilite l'accès depuis différents langages et outils.

---

## 3. Lisibilité du contrat

### Contrat RMI

Le contrat RMI est exprimé directement par l'interface Java :

```java
public interface CalculatorService extends Remote {

    double add(double a, double b) throws RemoteException;

    double sub(double a, double b) throws RemoteException;

    double mul(double a, double b) throws RemoteException;

    double div(double a, double b) throws RemoteException;
}
```

Un développeur Java peut rapidement comprendre les opérations disponibles et leurs paramètres.

### Contrat REST

Le contrat REST est défini par les ressources, les méthodes HTTP et les représentations JSON.

Par exemple :

```http
POST /operations
```

correspond à la création d'une nouvelle opération.

```http
GET /operations
```

permet de récupérer l'ensemble de l'historique.

```http
GET /operations/{id}
```

permet de récupérer une opération particulière.

### Observation

Dans RMI, le contrat est directement exprimé sous forme d'interface Java.

Dans REST, le contrat est réparti entre :

* les URL ;
* les méthodes HTTP ;
* les codes de statut ;
* les formats JSON ;
* les règles de validation.

REST est donc davantage indépendant du langage, tandis que RMI fournit un contrat directement exploitable par un client Java.

---

## 4. Mesure de la latence

Afin de comparer les deux approches expérimentalement, **100 appels identiques** ont été réalisés pour chaque technologie.

L'opération utilisée était :

```text
ADD(4, 7)
```

### REST

Les 100 appels ont été effectués sur :

```http
POST /operations
```

avec le corps :

```json
{
    "type": "ADD",
    "a": 4,
    "b": 7
}
```

Résultats mesurés :

| Mesure          |        REST |
| --------------- | ----------: |
| Nombre d'appels |         100 |
| Temps total     | 596,6458 ms |
| Temps moyen     | 5,966458 ms |
| Temps minimum   |   2,9976 ms |
| Temps maximum   |  15,0065 ms |

### RMI

Les 100 appels ont été effectués sur la méthode distante :

```java
calculator.add(4, 7);
```

Résultats mesurés :

| Mesure          |         RMI |
| --------------- | ----------: |
| Nombre d'appels |         100 |
| Temps total     |  73,3777 ms |
| Temps moyen     | 0,733777 ms |
| Temps minimum   |   0,4896 ms |
| Temps maximum   |    1,978 ms |

### Observation expérimentale

Dans cette exécution précise, le temps moyen mesuré pour RMI est inférieur à celui mesuré pour REST :

```text
RMI  : 0,733777 ms
REST : 5,966458 ms
```

Cette observation concerne uniquement l'environnement de test utilisé pour ce TP. Les résultats peuvent varier selon la machine, la charge du système, la JVM, le serveur HTTP, le réseau et la configuration des applications.

La mesure ne permet donc pas de conclure que l'une des technologies est systématiquement plus rapide dans tous les environnements.

---

## 5. Gestion des erreurs

Les deux approches permettent de signaler les erreurs, mais utilisent des mécanismes différents.

### RMI

Dans l'implémentation RMI, une division par zéro provoque une exception métier :

```java
throw new IllegalArgumentException(
        "Division par zéro"
);
```

Le mécanisme RMI utilise également `RemoteException` pour les problèmes liés à la communication distante.

Il faut donc distinguer :

* les erreurs métier ;
* les erreurs de communication distante.

### REST

REST utilise les codes HTTP pour représenter les différentes situations.

Dans notre implémentation :

| Situation                 |                  Code HTTP |
| ------------------------- | -------------------------: |
| Création réussie          |              `201 Created` |
| Champ obligatoire absent  |          `400 Bad Request` |
| Type d'opération invalide |          `400 Bad Request` |
| Division par zéro         | `422 Unprocessable Entity` |
| Opération inexistante     |            `404 Not Found` |

Par exemple, une division par zéro produit :

```http
422 Unprocessable Entity
```

avec un corps JSON explicatif.

### Observation

REST permet donc de représenter explicitement différentes catégories d'erreurs à travers les codes HTTP.

RMI s'appuie davantage sur les exceptions Java et sur les mécanismes de communication distante.

---

## 6. Tests avec Postman

Une collection Postman a été créée pour tester automatiquement le service REST.

Elle contient sept requêtes :

1. `POST Addition 201`
2. `POST Division par zéro 422`
3. `POST Champ manquant 400`
4. `GET Toutes les opérations 200`
5. `GET Opération existante 200`
6. `GET Opération inexistante 404`
7. `GET Même opération Idempotence`

Des scripts de test Postman vérifient automatiquement les codes HTTP et certains éléments des réponses.

La collection permet donc de rejouer les scénarios de test sans reconstruire manuellement chaque requête.

---

## 7. Collection Postman et contrat formel

La collection Postman constitue un support pratique pour tester et démontrer le comportement d'une API.

Elle permet notamment :

* d'identifier les routes ;
* de préciser les méthodes HTTP ;
* de fournir des exemples de requêtes ;
* d'observer les réponses ;
* d'automatiser certains tests.

Cependant, une collection de tests ne constitue pas nécessairement à elle seule une spécification formelle complète de l'API.

Pour un tiers, elle est particulièrement utile pour comprendre et essayer les endpoints, mais les informations concernant les modèles, les contraintes, les formats et les règles métier peuvent nécessiter une documentation complémentaire.

Dans notre projet, la collection Postman complète donc la conception REST et les tests automatisés plutôt que de remplacer toute documentation contractuelle.

---

## 8. Troisième paradigme : l'événementiel (séance 7)

Le module `/evenementiel` réimplémente le service avec RabbitMQ : le producteur publie
`OperationDemandee` sur l'échange `calcul.demandes`, `CalculConsommateur` calcule et publie
`OperationCalculee` sur `calcul.resultats`, et `AuditConsommateur` journalise chaque résultat.

### 8.1 Couplage temporel

Le producteur n'a pas besoin que le consommateur soit disponible. Test réalisé : consommateur
arrêté, publication de `ADD 1 1`. Le producteur n'a reçu aucun résultat en 5 s, mais la demande
a attendu dans la file durable `calcul.demandes.calculateur`. Au redémarrage, le consommateur l'a
traitée (`ADD(1.0, 1.0) -> 2.0`). En RMI, le même scénario donne `java.rmi.ConnectException`.

Piège observé : mes deux premières demandes, publiées avant le premier lancement du consommateur,
ont été perdues. Aucune file n'était encore liée à l'échange fanout, et le courtier a pourtant
confirmé la publication. Le consommateur doit avoir déclaré sa file au moins une fois.

### 8.2 Effort pour ajouter un traitement

Ajout du consommateur d'audit (`audit.csv`) : **0 fichier existant modifié**, 1 classe ajoutée
(`AuditConsommateur.java`, environ 80 lignes), vérifié avec `git status` après un commit de l'état
précédent. En RMI, il aurait fallu modifier le serveur (`CalculatorServiceImpl.recordHistory()`
ou `HistoryServiceImpl.record()`) puis le redémarrer.

### 8.3 Complexité du test d'intégration

En RMI et en REST, le test vérifie directement la valeur renvoyée. En événementiel, un test qui
vérifie juste après la publication a échoué (`aucun résultat reçu ... expected: not <null>`),
alors que le consommateur avait bien calculé le résultat, quelques millisecondes trop tard.
Avec une attente bornée (`await().atMost(Duration.ofSeconds(5))`, Awaitility), le test a réussi
7 fois sur 7.

### 8.4 Visibilité d'un résultat immédiat

L'équivalent du retour de `add(a, b)` n'existe plus : `Producteur.publier()` renvoie seulement
l'identifiant de l'opération. Le résultat est reconstitué après coup par `CorrelateurResultats`,
qui associe chaque `OperationCalculee` à sa demande grâce à cet identifiant, avec une attente
de 5 s au plus. Une erreur métier n'est plus une exception ni un code 422 : c'est un événement
dont le statut vaut `ERREUR`.

## 9. Synthèse

| Critère | RMI | REST | Événementiel (RabbitMQ) |
| --- | --- | --- | --- |
| Communication | Appels de méthodes distantes | HTTP | Événements via un courtier (AMQP) |
| Format principal | Objets/classes Java | JSON | JSON |
| Dépendance au langage | Forte (Java) | Indépendant | Indépendant (tout client AMQP) |
| Contrat | Interface Java | Ressources + HTTP + JSON | Schéma des événements + échanges/files |
| Gestion des erreurs | Exceptions / RemoteException | Codes HTTP + JSON | Événement `statut: ERREUR` |
| Performance mesurée | 0,733777 ms/appel | 5,966458 ms/appel | Non mesurée |
| Couplage temporel | Fort : serveur requis pendant l'appel | Fort : serveur requis pendant la requête | Faible : la demande attend dans une file durable |
| Ajout d'un traitement (audit) | Modifier le serveur et redémarrer | Modifier `OperationService` | 0 fichier modifié, 1 classe ajoutée |
| Test d'intégration | Assertion directe | Assertion directe | Attente bornée obligatoire (assertion immédiate : échec) |
| Résultat immédiat pour l'appelant | Oui : retour de `add(a, b)` | Oui : corps de la réponse 201 | Non : identifiant seulement, résultat par corrélation |

---

## 10. Conclusion

L'expérimentation montre trois styles d'architecture différents.

RMI propose une abstraction proche de l'appel de méthode Java et permet au client de manipuler directement une interface distante. Cette approche est particulièrement intégrée à l'écosystème Java.

REST expose au contraire des ressources accessibles par HTTP. L'utilisation des méthodes HTTP, des codes de statut et de JSON permet de construire une interface indépendante du langage du client.

Dans notre expérimentation, les 100 appels mesurés ont donné un temps moyen de **0,733777 ms pour RMI** et de **5,966458 ms pour REST**. Ces valeurs correspondent à l'environnement de test utilisé et doivent être interprétées comme des observations expérimentales.

L'implémentation événementielle supprime le couplage temporel et permet d'ajouter un traitement (l'audit) sans modifier le code existant. En contrepartie, l'appelant n'obtient plus de résultat immédiat : il doit le reconstituer par corrélation, et les tests doivent attendre l'événement avec un délai borné.

Le choix entre les deux approches dépend donc du contexte du système, notamment du niveau d'interopérabilité recherché, du type de clients, du contrat souhaité et des contraintes de communication.

---

## 11. Quatrième paradigme et mesures comparatives (séance 8)

Le module `/grpc` réimplémente le service avec gRPC : contrat `calcul.proto`, méthode unaire
`Additionner` et méthode bonus en streaming serveur `HistoriqueEnFlux`. Les classes Java
(`OperationRequest`, `OperationResponse`, `ServiceCalculGrpc`) sont générées par
`protobuf-maven-plugin` à chaque `mvn compile`.

### 11.1 Protocole de mesure

| Élément | Valeur |
| --- | --- |
| Machine | PC Windows (DESKTOP-P652UJ6), processeur Intel Core i5-10310U @ 1,70 GHz, RAM : 32 Go, Windows 11 64 bits (25H2) |
| JVM | Java 17 |
| Réseau | tout en `localhost` (aucun réseau externe) ; RabbitMQ dans Docker Desktop |
| Opération | `add(3, 5)` (événementiel : publication `ADD 3 5` → réception du résultat corrélé) |
| Chauffe | 10 appels ignorés |
| Mesure | 100 appels successifs, chronométrés un par un avec `System.nanoTime()` |
| Répétitions | 3 exécutions par variante (2 seulement pour RMI) ; on retient la médiane des 3 moyennes (moyenne des 2 essais pour RMI) |
| Programmes | `BenchmarkRMIProtocole`, `BenchmarkRest` (Java `HttpClient`, HTTP/1.1), `BenchmarkGrpc`, `BenchmarkEvenementiel` — même structure pour les 4 |
| Taille réseau | Wireshark, interface « Adapter for loopback traffic capture », pendant une exécution complète du benchmark (110 appels) ; Statistiques → Conversations → TCP ; total des octets ÷ 110 |
| Lignes de code | lignes non vides et hors commentaires, code métier + infrastructure minimale ; exclus : code généré (`target/`), benchmarks, tests, documentation |

Le benchmark REST est réécrit en Java : l'ancien script PowerShell (`Invoke-RestMethod`, séance 4)
ajoutait son propre surcoût et n'était pas comparable. Les valeurs de la séance 4
(RMI 0,73 ms, REST 5,97 ms, 1 seul appel de chauffe) ne sont donc pas reprises ici.

Limites : machine de développement non isolée ; le 1er essai gRPC a été fait la veille des deux
autres ; un écart inférieur à la variation entre deux essais (≈ 0,5 ms) n'est pas significatif.

### 11.2 Latence par appel (ms)

| Essai | RMI | REST | gRPC | Événementiel |
| --- | ---: | ---: | ---: | ---: |
| 1 — moyenne | 0,200 | 2,287 | 2,264 | 33,497 |
| 2 — moyenne | 0,165 | 2,297 | 2,762 | 34,506 |
| 3 — moyenne | — | 2,369 | 2,569 | 33,304 |
| Médiane des moyennes | **≈ 0,18** | **2,297** | **2,569** | **33,497** |
| Plage des médianes | 0,158 – 0,191 | 2,182 – 2,232 | 2,217 – 2,685 | 32,010 – 34,061 |

### 11.3 Taille des messages (Wireshark, benchmark de 110 appels)

| | Paquets | Octets | Paquets / appel | Octets / aller-retour | Connexions TCP |
| --- | ---: | ---: | ---: | ---: | --- |
| RMI | 498 | ≈ 32 ko | ≈ 4,5 | ≈ 290 | 2 : registre (1099) + objet distant (port aléatoire) |
| gRPC | 465 | ≈ 36 ko | ≈ 4,2 | ≈ 330 | 1 : connexion HTTP/2 réutilisée |
| REST | 891 | ≈ 85 ko | ≈ 8,1 | ≈ 770 | 2 : Tomcat ferme la connexion après 100 requêtes |
| Événementiel | 1 414 | ≈ 229 ko | ≈ 12,9 | ≈ 2 080 | 3 : producteur, corrélateur, consommateur |

Les octets comprennent les en-têtes IP/TCP et l'ouverture des connexions, répartis sur les
110 appels (même règle pour les 4). Fichiers de capture et captures d'écran : dossier `captures/`.

### 11.4 Lignes de code

| | Lignes | Détail | pom.xml |
| --- | ---: | --- | ---: |
| RMI | 283 | serveur 162 + client interactif 121 (callbacks à part : 25) | 18 |
| REST | 344 | dont 150 de DTO / modèle (getters, setters, constructeurs) | 54 |
| Événementiel | 319 | + 6 lignes de `docker-compose.yml` | 64 |
| gRPC | 105 | `.proto` 16 + Java 89 | 77 |

gRPC n'implémente que l'addition : la comparaison n'est pas à fonctionnalités égales. Le point
qui reste vrai : 16 lignes de `.proto` génèrent tout le code de sérialisation, écrit à la main en REST.

### 11.5 Complexité perçue (1 = trivial, 5 = complexe)

| | Note | Justification |
| --- | ---: | --- |
| RMI | 2 | Contrat = interface Java, aucune dépendance ; pièges : port aléatoire de l'objet, adresse `172.21.112.1` annoncée dans le stub, serveur lancé en double. |
| REST | 2 | Spring Boot démarre seul, test facile (Postman) ; mais beaucoup de code répétitif (DTO) et codes d'erreur gérés à la main. |
| gRPC | 3 | Peu de code (généré), mais configuration Maven la plus lourde (plugins protoc, 1er build de 3 min) et modèle `StreamObserver` à maîtriser. |
| Événementiel | 4 | Courtier Docker, 3 processus à lancer dans l'ordre, messages perdus tant que la file n'est pas déclarée, corrélation manuelle, tests asynchrones. |

### 11.6 Analyse

- **RMI est le plus rapide, de loin** (≈ 0,18 ms, environ 12 fois moins que REST et gRPC), alors
  que son volume réseau est proche de celui de gRPC (≈ 290 contre ≈ 330 octets). Ce n'est donc pas
  la taille des messages qui fait la latence ici, mais le traitement de chaque côté : socket direct et
  sérialisation Java pour RMI, pile HTTP (Tomcat/Spring, Netty/HTTP/2) pour REST et gRPC.
- **REST et gRPC sont à égalité en latence** sur cette machine : l'écart (≈ 0,3 ms) est plus petit que
  la variation entre deux essais de gRPC. En revanche, **REST envoie 2,3 fois plus d'octets et 2 fois
  plus de paquets** : en-têtes HTTP/1.1 en texte répétés à chaque requête, JSON verbeux. Sur un
  vrai réseau ou avec de gros volumes, cet écart compterait ; en local, il ne coûte presque rien.
- **L'événementiel est environ 15 fois plus lent que REST/gRPC** et transporte 7 fois plus d'octets
  que RMI : chaque calcul produit 2 messages qui passent chacun 2 fois par le courtier, avec
  confirmation de publication et messages persistants. Ce n'est pas une latence d'appel au même sens :
  c'est le prix du découplage temporel mesuré en séance 7.

### 11.7 Conclusion

Si ce service de calcul devait partir en production demain, nous choisirions **gRPC**. Nos mesures
montrent que RMI est de loin le plus rapide (≈ 0,18 ms), mais il impose des clients Java et nous a
déjà posé un problème d'adresse réseau en local (objet exporté sur `172.21.112.1`, port aléatoire),
ce qui deviendrait un obstacle derrière un pare-feu. Entre les deux protocoles HTTP, la latence est
identique sur notre machine (2,3 ms pour REST, 2,6 ms pour gRPC, écart inférieur à la variation entre
essais), mais gRPC transporte **2,3 fois moins d'octets et 2 fois moins de paquets** que REST
(≈ 330 contre ≈ 770 octets par appel, une seule connexion réutilisée), avec un contrat `.proto` typé de
16 lignes qui génère le client dans n'importe quel langage, et le streaming de l'historique sans
code supplémentaire. Ce choix a un coût mesuré : la configuration la plus lourde (pom de 77 lignes,
note de complexité 3/5) et des messages illisibles sans outil. L'événementiel, à 33 ms et 2 080 octets
par calcul, ne convient pas à un service où l'appelant attend le résultat immédiatement ; nous le
garderions pour les traitements annexes comme l'audit. Si les clients étaient des navigateurs ou des
partenaires externes, REST resterait le choix raisonnable, puisqu'il ne coûte rien de plus en latence.
