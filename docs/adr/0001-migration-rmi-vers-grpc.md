# ADR-0001 : Migration du service de calcul de RMI vers gRPC

## Statut
Proposé (2026-11-10)

## Contexte
Le service RMI impose Java des deux côtés et casse tout client existant au moindre changement de
signature de `CalculatorService` (cf. `rmi/ANALYSE.md`, séance 3). C'est le seul composant du projet
couplé fortement sur les trois axes (cf. [cartographie](../cartographie-couplage.md)).

En séance 8, Wireshark a montré que l'objet distant RMI est exporté sur un **port aléatoire** (64672) et
annoncé à l'adresse **172.21.112.1** (carte virtuelle Hyper-V) au lieu de `localhost` : en dehors du
poste de développement, il faudrait ouvrir une plage de ports et fixer `java.rmi.server.hostname`.

Le comparatif polyglotte de la séance 8 (`rest/COMPARATIF.md`, section 11, 10 appels de chauffe,
100 mesures, `add(3, 5)`, même machine) donne :

| | Latence (médiane des moyennes) | Octets / aller-retour | Paquets / appel |
|---|---:|---:|---:|
| RMI | ≈ 0,18 ms | ≈ 290 | ≈ 4,5 |
| REST | 2,297 ms | ≈ 770 | ≈ 8,1 |
| gRPC | 2,569 ms | ≈ 330 | ≈ 4,2 |
| Événementiel | 33,497 ms | ≈ 2 080 | ≈ 12,9 |

## Décision
Migrer progressivement le service de calcul vers le contrat gRPC `calcul.proto` (`ServiceCalcul`),
via une façade de routage placée dans le serveur RMI et un feature flag
(cf. [ADR-0002](0002-facade-et-feature-flag.md)), sans interruption de service. Le `.proto` devient la
**référence unique** du service : il est complété avec `Soustraire`, `Multiplier` et `Diviser`, la
division par zéro est renvoyée en `Status.INVALID_ARGUMENT`, et `HistoriqueEnFlux` remplace les
callbacks RMI (`HistoryService.subscribe`, `NotificationListener`). Le service RMI est retiré seulement après une période de stabilité
définie à l'avance (cf. [ADR-0003](0003-retrait-du-service-rmi.md)).

## Alternatives écartées
- **Réécriture totale en une fois** (arrêt de RMI le jour du déploiement gRPC) : écartée. C'est le
  risque analogue au cas Netscape vu en cours : aucune réversibilité si le nouveau service a un défaut
  en production, et tous les clients RMI cassent en même temps.
- **Migration vers REST plutôt que gRPC** : écartée. Nos mesures donnent une latence équivalente
  (2,3 ms contre 2,6 ms, écart inférieur à la variation entre essais), mais REST envoie **2,3 fois plus
  d'octets** et **2 fois plus de paquets** par appel (≈ 770 contre ≈ 330 octets), et son contrat est
  implicite : renommer `result` casse un client sans erreur de compilation. gRPC fournit en plus le
  streaming (`HistoriqueEnFlux`) qui peut remplacer les callbacks RMI.
- **Garder RMI et le corriger** (port fixe, `java.rmi.server.hostname`) : écartée. Cela règle le
  problème réseau mais pas le couplage technologique (clients Java uniquement) ni la fragilité du contrat.
- **Migrer vers l'événementiel** : écartée pour ce service. 33,5 ms par calcul (≈ 15 fois REST/gRPC),
  et l'appelant n'a plus de résultat immédiat ; l'événementiel reste pertinent pour l'audit.

## Conséquences
- **Positives** : clients possibles dans n'importe quel langage ; contrat explicite et versionnable
  (règle : ne jamais réutiliser un numéro de champ) ; un seul port fixe (50051) ; moins d'octets que REST.
- **Négatives** :
  - double maintenance temporaire RMI + gRPC pendant la transition ;
  - latence plus élevée que RMI en local (≈ 2,6 ms contre ≈ 0,18 ms), acceptable pour un service de
    calcul interactif mais à surveiller ;
  - le couplage **temporel** reste fort (gRPC est synchrone) ;
  - le contrat gRPC actuel ne couvre que l'addition : il faut l'étendre (cf. décision) avant toute bascule ;
  - build plus lourd (plugins `protoc`, pom de 77 lignes).
