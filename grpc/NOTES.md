# NOTES Module gRPC et comparatif des quatre paradigmes (séance 8)

Synthèse officielle : voir rest/COMPARATIF.md

## 1. Le module gRPC

### Contrat

Le contrat est écrit **avant** le code, dans `src/main/proto/calcul.proto` :

```proto
service ServiceCalcul {
  rpc Additionner (OperationRequest) returns (OperationResponse);
  rpc HistoriqueEnFlux (HistoriqueRequest) returns (stream OperationResponse);
}

message OperationRequest  { double a = 1; double b = 2; }
message OperationResponse { double resultat = 1; string id_operation = 2; }
message HistoriqueRequest {}
```

- Les numéros (`= 1`, `= 2`) sont les **identifiants des champs** dans le format binaire, pas des valeurs.
- `stream` indique que le serveur renvoie **plusieurs réponses** pour une seule requête.
- Le contrat est **indépendant du langage** : un client Python ou Go peut être généré à partir du même fichier.

### Génération de code

`mvn compile` lance `protoc` (plugin `protobuf-maven-plugin`) et génère dans `target/generated-sources/protobuf/` :

| Dossier | Classes | Rôle |
|---|---|---|
| `java/` | `OperationRequest`, `OperationResponse`, `HistoriqueRequest` (+ `...OrBuilder`), `Calcul` | les messages Protobuf |
| `grpc-java/` | `ServiceCalculGrpc` | la classe de base du serveur (`ServiceCalculImplBase`) et les stubs client |

Ces classes ne sont jamais écrites ni modifiées à la main.

### Classes écrites

| Fichier | Rôle |
|---|---|
| `ServiceCalculImpl.java` | implémente `ServiceCalculImplBase` : `additionner` et `historiqueEnFlux` |
| `ServeurGrpc.java` | démarre le serveur sur le port 50051 |
| `ClientGrpc.java` | 3 appels unaires (stub bloquant) puis lecture de l'historique en flux (stub asynchrone) |
| `BenchmarkGrpc.java` | mesure de latence (section 3) |

### Points importants observés

- Le serveur ne fait pas `return` : il appelle `onNext(réponse)` puis **`onCompleted()`**. Sans `onCompleted()`, le client reste bloqué en attente.
- Une exception non gérée laisserait l'appel ouvert : on la transforme en `onError(Status.INTERNAL...)`.
- L'historique est une `CopyOnWriteArrayList`, car plusieurs clients peuvent appeler le serveur en même temps.
- **Stub bloquant** (`newBlockingStub`) : s'utilise comme un appel de méthode classique.
- **Stub asynchrone** (`newStub`) : les réponses arrivent dans `onNext` ; un `CountDownLatch` empêche le `main` de se terminer avant la fin du flux.

### Lancer le module

```powershell
cd grpc
mvn -q compile exec:java "-Dexec.mainClass=com.nelly.grpc.ServeurGrpc"   # terminal 1
mvn -q exec:java "-Dexec.mainClass=com.nelly.grpc.ClientGrpc"           # terminal 2
mvn -q exec:java "-Dexec.mainClass=com.nelly.grpc.BenchmarkGrpc"        # terminal 2
```

Sortie obtenue avec `ClientGrpc` :

```text
additionner(3.0, 5.0) = 8.0  id=c5e13a2e-...
additionner(4.0, 7.0) = 11.0  id=2459093a-...
additionner(10.0, 2.5) = 12.5  id=49b25dd9-...
  [flux] 8.0  id=c5e13a2e-...
  [flux] 11.0  id=2459093a-...
  [flux] 12.5  id=49b25dd9-...
Fin de l'historique
```

Le flux renvoie bien l'historique dans l'ordre, avec les mêmes identifiants.

---

## 2. Protocole de mesure commun

Pour comparer les quatre paradigmes **dans les mêmes conditions**, un benchmark identique a été écrit pour chacun :

| Paradigme | Classe |
|---|---|
| RMI | `rmi/src/main/java/client/BenchmarkRMIProtocole.java` |
| REST | `rest/src/main/java/com/rest/nelly/rest/benchmark/BenchmarkRest.java` |
| gRPC | `grpc/src/main/java/com/nelly/grpc/BenchmarkGrpc.java` |
| Événementiel | `evenementiel/src/main/java/com/nelly/evenementiel/BenchmarkEvenementiel.java` |

Règles communes :

- même opération : addition 3 + 5, résultat vérifié (= 8.0) à chaque appel ;
- **10 appels de chauffe** non mesurés, puis **100 appels mesurés** avec `System.nanoTime()` ;
- une seule connexion / un seul stub ouvert pour tous les appels (on ne mesure pas l'établissement de la connexion) ;
- appels **séquentiels** (on attend la réponse avant l'appel suivant) ;
- statistiques : moyenne, **médiane**, minimum, maximum ;
- plusieurs exécutions, client et serveur sur la même machine (localhost).

Pour l'événementiel, on mesure l'aller-retour complet : publication de la demande → réception du résultat corrélé.

---

## 3. Latence

### Résultats bruts

| Paradigme | Essai | Moyenne | Médiane | Min | Max |
|---|---|---|---|---|---|
| RMI | 1 | 0,200 ms | 0,191 ms | 0,148 ms | 0,301 ms |
| RMI | 2 | 0,165 ms | 0,158 ms | 0,133 ms | 0,261 ms |
| REST | 1 | 2,287 ms | 2,223 ms | 1,863 ms | 3,445 ms |
| REST | 2 | 2,297 ms | 2,182 ms | 1,709 ms | 5,536 ms |
| REST | 3 | 2,369 ms | 2,232 ms | 1,703 ms | 8,414 ms |
| gRPC | 1 | 2,264 ms | 2,217 ms | 1,707 ms | 3,435 ms |
| gRPC | 2 | 2,762 ms | 2,685 ms | 2,145 ms | 7,103 ms |
| gRPC | 3 | 2,569 ms | 2,470 ms | 2,010 ms | 3,790 ms |
| Événementiel | 1 | 33,497 ms | 32,530 ms | 5,488 ms | 56,808 ms |
| Événementiel | 2 | 34,506 ms | 34,061 ms | 14,011 ms | 59,343 ms |
| Événementiel | 3 | 33,304 ms | 32,010 ms | 7,430 ms | 58,845 ms |

### Synthèse (médiane des moyennes ; moyenne des 2 essais pour RMI)

| Paradigme | Latence retenue | Rapport à RMI |
|---|---|---|
| RMI | **≈ 0,18 ms** | × 1 |
| REST | **2,297 ms** | × 13 |
| gRPC | **2,569 ms** | × 14 |
| Événementiel | **33,497 ms** | × 186 |

### Interprétation

1. **On compare les médianes, pas les maximums.** Le maximum varie beaucoup d'un essai à l'autre (REST : de 3,4 à 8,4 ms) à cause de pics ponctuels (ramasse-miettes, ordonnancement des threads). La médiane est stable.
2. **REST et gRPC sont à égalité.** L'écart entre eux (≈ 0,3 ms) est plus petit que la variation entre deux essais de gRPC (≈ 0,5 ms) : on ne peut pas dire que l'un est plus rapide sur ce test.
3. **RMI est environ 13 fois plus rapide** : un socket TCP direct et la sérialisation Java, sans couche HTTP ni framework (Spring, Netty).
4. **L'événementiel est le plus lent**, et c'est une conséquence de la conception, pas un défaut :
   - 2 messages (demande + résultat) passent chacun par le courtier ;
   - les messages sont **persistants** et le producteur attend la **confirmation** du courtier (`waitForConfirmsOrDie`) : RabbitMQ ne confirme qu'après écriture sur disque ;
   - le minimum (5 ms) montre que le chemin réseau seul est bien plus rapide que la médiane.
5. **La chauffe compte beaucoup.** L'ancien `BenchmarkRMI` (1 seul appel de chauffe) mesurait 0,73 ms ; avec 10 appels de chauffe on descend à ≈ 0,18 ms. Un protocole identique est indispensable pour comparer.

### Limites

- Mesures en local : le coût réseau réel (latence, débit) n'apparaît pas.
- Appels séquentiels uniquement : les avantages de gRPC (multiplexage HTTP/2) et de l'événementiel (débit, découplage) ne sont pas mesurés.
- Toutes les mesures n'ont pas été faites au même moment (REST le soir, deux essais gRPC le lendemain matin).
- Pour l'événementiel, un test de **débit** (publier 1 000 demandes sans attendre, compter les résultats par seconde) serait plus représentatif.

---

## 4. Analyse du trafic avec Wireshark

### Méthode

- Interface **« Adapter for loopback traffic capture »** (Npcap), car tout tourne sur localhost.
- Filtre de capture par port : `tcp port 8080` (REST), `tcp port 50051` (gRPC), `tcp port 5672` (RabbitMQ). Pour RMI, **pas de filtre**, car le port de l'objet distant est choisi au hasard.
- Capture pendant le benchmark (110 appels), puis **Statistiques > Conversations > TCP** ; les totaux sont divisés par 110.

### Connexions observées

| Paradigme | Connexions TCP | Détail |
|---|---|---|
| RMI | 2 | registre (port 1099, 20 paquets, 1 ko, une seule fois) + objet distant (port aléatoire 64672, 478 paquets, 31 ko) |
| REST | 2 | vers 8080 : 807 paquets / 77 ko (≈ 100 appels) et 84 paquets / 8 ko (≈ 10 appels) |
| gRPC | 1 | vers 50051 : 465 paquets, 36 ko ; tous les appels dans une seule connexion HTTP/2 |
| Événementiel | 3 | producteur (481 paquets, 57 ko), corrélateur (269 paquets, 54 ko), consommateur de calcul (664 paquets, 118 ko) |

### Volume par appel

| Paradigme | Paquets / appel | Octets / appel (aller-retour) | Format sur le réseau |
|---|---|---|---|
| RMI | ≈ 4,5 | ≈ 290 | binaire, sérialisation Java (JRMP) |
| gRPC | ≈ 4,2 | ≈ 330 | binaire, HTTP/2 + Protobuf |
| REST | ≈ 8,1 | ≈ 770 (84 475 octets / 110 = 768) | texte, HTTP/1.1 + JSON |
| Événementiel | ≈ 12,9 | ≈ 2 080 | AMQP + JSON, 2 messages, 4 transferts |

Les totaux incluent toutes les connexions (pour RMI : registre + objet distant, 498 paquets), les en-têtes IP/TCP et l'ouverture des connexions, répartis sur les 110 appels.

### Observations

- **REST** est le seul format **lisible en clair** (« Suivre le flux HTTP ») : en-têtes texte et JSON. Les en-têtes HTTP/1.1 sont renvoyés en entier à chaque requête et le JSON répète les noms des champs : environ 2,3 fois plus d'octets que gRPC.
- **gRPC** : Wireshark décode le trafic en `GRPC/PROTOBUF`. La réponse (HEADERS + DATA + HEADERS) tient dans **un seul paquet de 128 octets**. HTTP/2 compresse les en-têtes (HPACK) et Protobuf n'envoie qu'un numéro de champ et la valeur binaire.
- **RMI** : deux phases bien visibles, d'abord le `lookup` au registre (une fois), puis les appels sur un port aléatoire. Le flux contient du binaire avec des noms de classes Java lisibles (`server.CalculatorService`, `java.rmi...`).
  - **Piège observé** : l'objet distant a été annoncé à l'adresse **172.21.112.1** (carte virtuelle Hyper-V/WSL) et non 127.0.0.1. RMI écrit dans le stub l'adresse que la JVM du serveur juge principale ; sur une machine à plusieurs cartes réseau, il faut souvent fixer `-Djava.rmi.server.hostname=...`.
- **Événementiel** : chaque calcul produit `Basic.Publish` → `Basic.Ack` (confirmation) → `Basic.Deliver` vers le consommateur → son `Basic.Ack` → `Basic.Publish` du résultat → `Basic.Deliver` vers le corrélateur. Chaque message AMQP fait 3 trames (méthode, en-tête de propriétés, corps JSON). Le consommateur porte le plus de trafic, car il reçoit **et** publie.

### Lien avec la latence

RMI et gRPC échangent presque le même volume, pourtant RMI est environ 13 fois plus rapide : **ce n'est pas la taille des messages qui fait la différence en local**, mais le traitement de chaque côté (pile HTTP/2 + Netty contre simple socket). De même, l'événementiel transporte environ 6 fois plus d'octets que gRPC mais est environ 13 fois plus lent : l'essentiel vient de la persistance sur disque et des confirmations, pas du réseau.

---

## 5. Taille du code

Lignes de code hors lignes vides et commentaires ; code généré, benchmarks, tests et documentation exclus.

| Paradigme | Lignes | Détail | `pom.xml` |
|---|---|---|---|
| RMI | 283 | interfaces + implémentations + `Server` + `Client` (121) ; callbacks en bonus : +25 | 18 |
| REST | 344 | contrôleur, service, modèle, 2 DTO, application, `application.properties` | 54 |
| Événementiel | 319 | producteur, 2 consommateurs, corrélateur, topologie, 2 événements ; + 6 lignes de `docker-compose.yml` | 64 |
| gRPC | 105 | `calcul.proto` (16) + service, serveur, client (89) | 77 |

Attention à l'interprétation :

- gRPC ne fait que l'**addition** et garde l'historique en mémoire ; les autres variantes font ADD, SUB, MUL, DIV et gèrent la division par zéro. La comparaison n'est donc pas à fonctionnalités égales.
- En REST, environ 150 lignes sont du code répétitif (getters, setters, constructeurs dans `Operation`, `OperationDTO`, `OperationRequest`) ; des `record` Java les réduiraient fortement.
- En gRPC, ce code est **généré** à partir des 16 lignes du `.proto`. En contrepartie, le `pom.xml` est le plus long (plugins `protoc`).

---

## 6. Synthèse

| Critère | RMI | REST | gRPC | Événementiel |
|---|---|---|---|---|
| Latence (médiane des moyennes, local) | ≈ 0,18 ms | 2,297 ms | 2,569 ms | 33,497 ms |
| Octets par appel | ≈ 290 | ≈ 770 | ≈ 330 | ≈ 2 080 |
| Contrat | interface Java | URL + verbes HTTP + JSON (implicite) | fichier `.proto` (explicite, généré) | format des événements JSON |
| Indépendant du langage | non (Java uniquement) | oui | oui | oui |
| Lisible sur le réseau | non | **oui** | non | oui (corps JSON) |
| Streaming | non (callbacks à la main) | non | **oui, natif** | oui (par nature) |
| Couplage temporel | fort | fort | fort | **faible** (le courtier garde les messages) |
| Tolérance à l'arrêt du serveur | non | non | non | **oui** |

### Conclusion

- **RMI** est le plus rapide et le plus léger, mais il lie client et serveur à Java et pose des problèmes de déploiement (port aléatoire, adresse annoncée dans le stub).
- **REST** est le plus simple à tester et à lire (navigateur, Postman, Wireshark), au prix de messages plus gros ; sa latence reste équivalente à gRPC en local.
- **gRPC** offre un contrat strict et multi-langage, des messages compacts et le streaming natif ; son intérêt apparaîtrait surtout sur un vrai réseau, avec de gros volumes ou beaucoup d'appels en parallèle, ce que ce test séquentiel ne mesure pas.
- **L'événementiel** est le plus lent pour un échange requête/réponse, mais c'est le seul qui découple producteur et consommateur dans le temps et qui ne perd pas de demande si le consommateur est arrêté. Il se juge sur le débit et la fiabilité, pas sur la latence d'un appel.

Le bon choix dépend donc du besoin : performance entre services Java (RMI), ouverture et simplicité (REST), contrat fort et efficacité entre services hétérogènes (gRPC), ou découplage et fiabilité (événementiel).
