# ARCHITECTURE état réel du projet fil rouge (séance 5)

> Ce document décrit l'architecture **telle qu'elle est dans le code** au commit `1c23aca`, et non telle qu'elle aurait dû être. Il porte sur les modules `/socket`, `/rmi` et `/rest`, demandés par l'atelier. Le module `/soap` est hors périmètre.
> Le schéma d'architecture en couches réelle (draw.io) accompagne ce document : chaque violation citée ici (repères S, R, E) y est annotée directement.

## 1. Choix de découpage

Le schéma utilise **quatre bandes** plutôt que les trois couches classiques (présentation, métier, données). Dans un système réparti, la frontière réseau est une couche à part entière : elle fixe ce que le serveur expose et sous quelle forme. Les quatre bandes sont les suivantes :

| Bande | Contenu | Pourquoi elle est séparée |
|---|---|---|
| Client / présentation | ce que voit l'utilisateur : console, Postman, scripts de test | c'est l'autre extrémité du client-serveur |
| Point d'entrée serveur / distribution | ce qui traduit le protocole en appels : boucle `accept()` et analyse du texte, interfaces `Remote` et registre, `@RestController` et DTO | c'est la « présentation » du serveur : elle seule devrait connaître le protocole |
| Métier | calcul, règles (division par zéro, types d'opération) | c'est ce qui doit rester identique d'un module à l'autre |
| Données / état | historique, stockage en mémoire | même en mémoire, c'est un état partagé, avec sa concurrence et son cycle de vie |

Les trois modules réalisent le **même besoin** (calculatrice et historique), ce qui permet de les comparer bande par bande. Un composant est placé dans la bande qui correspond à ce que **fait son code**, et non à ce que suggère son nom. Quand une classe couvre plusieurs bandes, le schéma la dessine **à cheval** sur ces bandes plutôt que de la découper artificiellement (`CalcServer`, `OperationService`).

## 2. B.1 Inventaire des couches réellement présentes

### `/socket` aucune séparation de couches

| Bande | Ce qui existe vraiment |
|---|---|
| Client | `CalcClient` : lit une ligne au clavier et l'envoie sans validation. |
| Point d'entrée | `CalcServer.main()` / `CalcServer3.ClientHandler.run()` : `accept()`, flux, un thread par connexion (version 3). |
| Métier | `calculate()`, **la même méthode** qui analyse aussi le protocole (`split(";")`, `parseDouble`) et fabrique la réponse `"RESULT;" + x`. |
| Données | Rien : aucun état n'est conservé. Le journal se limite à `System.out`. |

Constat honnête : le module est **une seule couche**. Ce n'était pas l'objectif de la séance 1, centrée sur les sockets. La logique de calcul est en outre copiée à l'identique dans deux classes.

### `/rmi` des couches identifiables, mais toutes liées à RMI

| Bande | Ce qui existe vraiment |
|---|---|
| Client | `Client` (menu console), `ConcurrentLoadTest`, `BenchmarkRMI`, `ClientNotificationListener` (objet distant de rappel). |
| Point d'entrée | Registre `:1099`, interfaces `CalculatorService` et `HistoryService` (`extends Remote`). |
| Métier | `CalculatorServiceImpl` : calcul et enregistrement de l'historique. La classe **est** un objet RMI (`extends UnicastRemoteObject`). |
| Données | `HistoryServiceImpl` : `ArrayList` protégée par `synchronized`, **plus** la diffusion des notifications aux clients. |

La séparation métier / données existe : deux classes, et la dépendance `Calculator → History` est injectée dans `Server.java`. En revanche, chaque couche dépend de la technologie de distribution, et le client accède directement à la couche basse.

### `/rest` le module le plus propre, à qui il manque une couche

| Bande | Ce qui existe vraiment |
|---|---|
| Client | Postman (collection), `benchmark-rest.ps1`. |
| Point d'entrée | `OperationController` (`@RestController`) : validation de présence, conversion des exceptions en 400 / 422 / 404, `201 Created` et `Location` ; DTO `OperationRequest` et `OperationDTO`. |
| Métier | `OperationService` (`@Service`) : normalisation, calcul, refus de `b == 0`. |
| Données | **Pas de couche dédiée** : `List<Operation>`, `AtomicLong` et `synchronized` vivent dans `OperationService`. Le modèle `Operation` existe. |

Le sens des dépendances est respecté : `OperationService` n'importe aucun type HTTP (`ResponseEntity`, `HttpStatus`…). Les erreurs métier sont signalées par des exceptions Java standard, que seul le contrôleur traduit en codes HTTP.

## 3. B.2 Violations trouvées

Les repères (S, R, E) renvoient au schéma, où chaque violation est annotée.

| Id | Catégorie (B.2) | Où | Risque concret | Cause probable |
|---|---|---|---|---|
| **S1** | Absence de couches | `CalcServer.java:49`, `calcServer3.java:108` | Le calcul fabrique la trame `RESULT;` : changer de protocole oblige à réécrire le calcul. | Séance 1 centrée sur les sockets (choix assumé) |
| **S2** | Logique dupliquée | `calculate()` en deux copies | Une correction faite dans une seule copie fait diverger les deux serveurs. | Copie pour créer la version multi-thread (délais) |
| **R1** | **Saut de couche** | `Client.java:22,29,126` ; `ConcurrentLoadTest.java:23,117` ; `record()` publié dans `HistoryService.java:11` | Le client contourne `CalculatorService`. N'importe quel client du registre peut appeler `record("ADD(1,1) = 3")` et **falsifier l'historique**, y compris le comptage sur lequel repose `ConcurrentLoadTest`. | Callbacks ajoutés au plus court (séance 3) |
| **R2** | **Couche basse qui connaît la couche haute** | `HistoryService.java:3`, `HistoryServiceImpl.java:3` → `import client.NotificationListener` | Le paquet `server` ne compile pas sans le paquet `client` : impossible de livrer un serveur seul. | Interface rangée à côté de son implémentation (oubli) |
| **R3** | **Couche basse qui connaît une technologie haute** | `CalculatorServiceImpl extends UnicastRemoteObject`, `throws RemoteException` partout | Le calcul ne peut être ni réutilisé par REST ou SOAP, ni testé sans registre RMI. | Forme du tutoriel RMI, jamais remise en question |
| **R4** | Détour par la distribution | `Server.java:22,29` injecte le **stub** obtenu par `lookup` | Chaque `add()` refait un aller-retour RMI en boucle locale. La `RemoteException` est avalée (`CalculatorServiceImpl.java:27`), et l'entrée est perdue sans erreur visible. | Copier-coller du code de `lookup` côté client |
| **R5** | Couplage à l'exécution | `HistoryServiceImpl.java:40` | `record()` appelle chaque client abonné **avant** de rendre la main : la durée d'un `add()` dépend du client le plus lent. | Simplicité du callback synchrone (choix à courte vue) |
| **E1** | Couche manquante (fusion) | `OperationService.java:16-19, 79, 88, 103` | Passer à une base de données oblige à réécrire le service, pas seulement à ajouter un repository. | Pas de persistance exigée (choix à courte vue) |
| **E2** | Responsabilité dupliquée | `OperationController.java:31-34` et `OperationService.java:26` | La même règle (type non vide) produit deux messages différents selon l'endroit où elle échoue, et ces deux versions risquent de diverger. | Propriétaire de la règle jamais choisi |
| **E3** | Symptôme « lasagne » | `Operation` ≡ `OperationDTO`, `toDTO()` l. 118 | Ajouter un champ oblige à toucher quatre endroits sans aucune transformation. | Patron DTO appliqué par réflexe |

**Méthodes de pure délégation : 0 au sens strict.** Trois candidates ont été examinées :

- `OperationController.getAllOperations()` se contente d'envelopper le résultat en `200 OK`. C'est la traduction HTTP attendue d'un contrôleur, donc une indirection justifiée.
- `CalculatorServiceImpl.recordHistory()` ajoute un contrôle de `null` et une gestion d'erreur.
- `HistoryServiceImpl.getAll()` renvoie une copie défensive.

Le projet souffre donc plutôt du défaut **inverse** de la lasagne, c'est-à-dire de couches manquantes (S1, E1). Le seul symptôme lasagne observé est la duplication de structure entre `Operation` et `OperationDTO` (E3).

## 4. Décision : corriger ou assumer ? (violation choisie : R1)

**Rappel.** Le client appelle `HistoryService` directement, ce qui est une couche détendue (*relaxed layering*). L'interface distante expose aussi `record()`, c'est-à-dire l'écriture.

Cette violation mélange deux problèmes dont le rapport coût / bénéfice est très différent. Elle est donc traitée en deux parties.

### a) Écriture exposée (`record()` publiée dans le registre) **à corriger maintenant**

- **Bénéfice.** C'est le seul cas où la violation crée un **risque réel** : n'importe quel client peut écrire de fausses entrées dans l'historique. L'intégrité de l'historique est la propriété que le test `ConcurrentLoadTest` prétend vérifier. Un historique falsifiable rend ce test non probant.
- **Coût.** Environ quinze lignes dans quatre fichiers, sans aucun changement côté client :
  - retirer `record()` de l'interface distante `HistoryService` ;
  - créer une interface locale `HistoryRecorder { void record(String) }`, implémentée par `HistoryServiceImpl` ;
  - dans `Server.java`, injecter `historyImpl` (l'objet local) au lieu du stub ;
  - changer le type du champ dans `CalculatorServiceImpl`.
- **Bonus.** La même modification supprime **R4** : plus d'aller-retour RMI local, plus de `RemoteException` avalée.

Le coût est faible et le risque est concret : la correction est justifiée tout de suite.

### b) Lecture directe (`getAll()`, `subscribe()`) — **assumée consciemment pour l'instant**

- **Coût de la correction.** Faire de `CalculatorService` la seule porte d'entrée oblige à ajouter `getHistory()` et `subscribe()` à `CalculatorService`, et à modifier `Client`, `ConcurrentLoadTest` et `Server`. Les deux nouvelles méthodes ne feraient que **déléguer** à `HistoryService` sans rien décider : c'est exactement le symptôme lasagne vu en cours.
- **Bénéfice.** Il est nul aujourd'hui. Lire l'historique ne fait intervenir **aucune règle métier** : ni droits, ni filtrage, ni calcul. Pour cette couche candidate, la question du cours (« que ferait-on de moins bien si elle n'existait pas ? ») a pour réponse honnête « rien ».
- **Condition de remise en cause.** Dès qu'une règle apparaît sur la lecture (historique par utilisateur, droits d'accès, pagination, persistance en base), la façade gagne une vraie responsabilité. On la crée alors, et on supprime l'accès direct.

**En résumé.** Corriger l'écriture tout de suite, parce qu'elle coûte peu et supprime un risque. Assumer la lecture directe comme dette technique **documentée**, avec sa condition de remboursement, parce que la supprimer ajouterait aujourd'hui de la complexité sans valeur.