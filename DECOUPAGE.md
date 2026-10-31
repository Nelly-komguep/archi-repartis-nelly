# DECOUPAGE projet fil rouge en microservices (séance 6)

> Cette note justifie le découpage cible du projet fil rouge en microservices, et surtout les découpages **écartés**.
> Les diagrammes C4 (niveaux 1 à 3) sont dans le fichier draw.io `seance6-c4.drawio`, une page par niveau. Les services du niveau 2 correspondent exactement au tableau de la section 2.

## 1. Inventaire des capacités métier (A.1)

| Capacité | Ce qu'elle fait aujourd'hui, et où | Possède-t-elle ses propres données ? |
|---|---|---|
| **Calcul** | ADD, SUB, MUL, DIV ; refus de la division par zéro ; normalisation du type. Présente quatre fois : `calculate()` (socket), `CalculatorServiceImpl` (RMI), `OperationService.compute()` (REST), `CalculatorEndpoint` (SOAP). | **Non.** Elle produit une opération mais ne la conserve pas : c'est une capacité sans état. |
| **Historique** | Conserver chaque opération et la restituer : `HistoryServiceImpl` (RMI), la liste de `OperationService` (REST), la liste de `CalculatorEndpoint` (SOAP). | **Oui** : les opérations horodatées (id, type, a, b, résultat, date). |
| **Notification** | Prévenir les clients abonnés de chaque nouvelle opération : `subscribe()` et `onNewOperation()` (callbacks RMI, séance 3). | **Oui** : le registre des abonnés (qui veut être prévenu, et par quel canal). |
| Authentification | **N'existe pas dans le projet.** Elle n'est pas ajoutée : cette note découpe le système réel, pas un système imaginé. | — |

Deux capacités possèdent des données (historique, notification) : ce sont les frontières les plus solides. Le calcul ne possède rien, mais il a ses **propres règles** (types d'opération, division par zéro), qui évoluent indépendamment du stockage. La section 3 justifie pourquoi il reste quand même un service distinct.

## 2. Découpage retenu (A.2)

| Service | Responsabilité | Données possédées exclusivement | Dépend de |
|---|---|---|---|
| **calc-service** | Valider et exécuter les quatre opérations, avec leurs règles (type connu, division par zéro). | Aucune : service sans état. | **history-service**, pour enregistrer chaque opération (REST, synchrone). |
| **history-service** | Conserver l'historique horodaté, en ajout seul, et l'exposer en lecture. | Table des opérations dans **history-db**. Aucun autre service n'y accède. | **notification-service**, pour signaler chaque opération enregistrée (REST, **non bloquant**). Il fonctionne même si ce service est indisponible. |
| **notification-service** | Gérer les abonnements et pousser chaque nouvelle opération aux abonnés. | Registre des abonnés (sessions WebSocket actives, en mémoire). | Aucun. |

**Règles de frontière.**
- **Base de données.** Seul history-service lit et écrit dans history-db. Les autres passent par son API, ce qui évite le dépôt partagé vu en séance 5.
- **Écriture dans l'historique.** `POST /operations` est un point d'entrée **interne** : seul calc-service peut écrire dans l'historique, le client n'a que la lecture. C'est la correction de la violation R1 de la séance 5, où n'importe quel client pouvait appeler `record()`.
- **Si history-service est indisponible**, calc-service **refuse** le calcul (503) plutôt que de rendre un résultat non historisé. L'historique fait partie du contrat : `ConcurrentLoadTest` vérifie justement qu'aucune opération n'est perdue. Ce couplage synchrone est assumé ; la séance 7 (messagerie asynchrone) permettra de le réexaminer.
- **Si notification-service est indisponible**, l'opération est quand même enregistrée. Seule la diffusion est perdue.
- **Modules socket, RMI et SOAP.** Ils restent des démonstrateurs pédagogiques de chaque paradigme. Ils ne deviennent pas des services. Entre services, le transport est volontairement simple : REST/HTTP en JSON (« dumb pipes »).

**Vérification contre le piège du monolithe distribué.** Si chaque évolution métier courante ne touche qu'un service, le découpage suit des capacités et non des couches :

| Évolution | Services modifiés |
|---|---|
| Ajouter une opération `POW` | calc-service seulement (l'historique stocke le type comme une simple chaîne) |
| Passer l'historique en base persistante, ajouter la pagination | history-service seulement |
| Remplacer WebSocket par SSE, ou ajouter une notification par courriel | notification-service seulement |

## 3. Alternatives envisagées puis écartées (A.3)

### Alternative 1 Fusionner calc-service et history-service (un seul « operation-service »)

C'est exactement le module `/rest` actuel.

- **Arguments pour.** Aucun appel réseau, un seul déploiement, aucune question de cohérence entre deux services. Et la loi de Conway plaide pour : le projet est développé par **une seule personne**, donc une seule équipe.
- **Critère qui a tranché : indépendance des changements et de la charge.**
  - Les deux capacités ne changent pas pour les mêmes raisons. Ajouter une opération ne touche que le calcul ; changer de stockage ne touche que l'historique.
  - Elles ne se dimensionnent pas pareil. Le calcul, sans état, se réplique librement. L'historique, avec état, doit sérialiser ses écritures (les `synchronized` de la séance 5).
  - Fusionnées, la moindre évolution du calcul redéploie le stockage, et inversement.
- **Verdict.** Écartée. Si le seul critère était la taille de l'équipe, la fusion serait le bon choix : c'est l'option de repli à reprendre si le coût opérationnel de trois services devient disproportionné.

### Alternative 2 Un microservice par opération (add-service, sub-service, mul-service, div-service)

- **Arguments pour.** Chaque opération serait déployable et dimensionnable séparément.
- **Critère qui a tranché : le contexte délimité (bounded context).**
  - « Opération » a exactement le même sens dans les quatre cas : mêmes entrées, même normalisation du type, même validation des nombres. Ces règles changent **ensemble**, donc une évolution commune toucherait quatre services à la fois.
  - Aucun des quatre ne posséderait de données.
  - Chacun contiendrait une seule ligne de calcul, pour quatre déploiements et quatre configurations.
  - Le client devrait connaître le découpage interne pour savoir à qui envoyer une addition.
- **Verdict.** Écartée. C'est un découpage par fonction, pas par capacité métier.

### Alternative 3 Garder la notification dans history-service

- **Arguments pour.** Une notification n'existe que parce qu'une opération est enregistrée. Il y aurait un service et un saut réseau de moins.
- **Critère qui a tranché : le couplage à l'exécution constaté en séance 5 (violation R5).**
  - Dans le module RMI, `record()` notifie chaque abonné **avant** de rendre la main : la durée d'un calcul dépend du client le plus lent.
  - Les données diffèrent : opérations d'un côté, abonnés de l'autre.
  - Les technologies diffèrent : des connexions longues et nombreuses (WebSocket) d'un côté, des requêtes REST courtes de l'autre. Elles ne se dimensionnent pas pareil.
- **Verdict.** Écartée. Le coût (un appel réseau supplémentaire) est absorbé par l'envoi non bloquant.

### Vérification découpage par couche technique (api-service, logique-service, données-service)

Ce découpage n'a pas été retenu comme candidat : c'est le piège du monolithe distribué. Il transformerait les couches de la séance 5 en services. Ajouter un champ (par exemple une unité) obligerait à modifier et redéployer les trois en même temps, et chaque calcul traverserait deux appels réseau sans gagner la moindre autonomie.

## 4. Coûts acceptés

| Coût | Conséquence concrète |
|---|---|
| Exploitation | 3 services et 1 base à déployer et à superviser, au lieu d'une application |
| Latence | Chaque calcul ajoute un aller-retour réseau calc-service → history-service |
| Disponibilité | Une panne de history-service bloque les calculs (choix assumé en section 2) |
| Tests | Le contrat de `POST /operations` doit être vérifié par un test de contrat entre calc-service et history-service |

## 5. Cohérence avec les diagrammes C4

- **Niveau 1 (Contexte).** Le système est une seule boîte, avec un utilisateur. Aucun système externe à ce jour (pas de courriel, d'annuaire ni de base tierce), et le diagramme le dit explicitement.
- **Niveau 2 (Conteneurs).** Il montre exactement les trois services du tableau, plus history-db et `client-cli`. Le client n'est pas un microservice : c'est l'application qui consomme les services, il apparaît donc comme conteneur mais pas dans le tableau.
- **Niveau 3 (Composants).** Il ouvre **history-service** seulement. calc-service est sans état (un contrôleur et un calcul : rien de plus à montrer), tandis que history-service porte des données, des règles et une dépendance sortante. Ses composants corrigent aussi deux violations de la séance 5 : E1, avec un `OperationRepository` dédié, et R5, avec un `NotificationPublisher` non bloquant.