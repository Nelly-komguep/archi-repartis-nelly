# Plan de migration RMI → gRPC par étapes réversibles (séance 9, partie D)

Scénario : migrer le service de calcul RMI vers gRPC sans interrompre les clients existants.
Décisions associées : [ADR-0001](adr/0001-migration-rmi-vers-grpc.md) (cible gRPC),
[ADR-0002](adr/0002-facade-et-feature-flag.md) (façade + feature flag),
[ADR-0003](adr/0003-retrait-du-service-rmi.md) (retrait de RMI).

## Prérequis (étape 0)

Avant toute bascule, le contrat `calcul.proto` est complété (`Soustraire`, `Multiplier`, `Diviser`,
division par zéro en `Status.INVALID_ARGUMENT`) et `ServeurGrpc` est déployé **à côté** de RMI, sans
recevoir de trafic. Retour arrière : arrêter `ServeurGrpc`, aucun client n'en dépend encore.

## Étapes

| Étape | Ce qui bascule | Condition pour passer à l'étape suivante | Condition de retour en arrière |
|---|---|---|---|
| **1** | `CalculatorServiceImpl` devient une façade (ADR-0002), flag `calcul.grpc.pourcentage = 0` : **100 % du trafic reste sur l'ancien chemin** (calcul local). La façade journalise déjà chaque appel (chemin, durée, appelant). | Latence RMI via la façade restée dans la plage de la séance 8 (≈ 0,16 à 0,20 ms) sur 3 exécutions de `BenchmarkRMIProtocole` ; aucune erreur nouvelle. | Redéployer l'ancien `CalculatorServiceImpl` (commit précédent) : aucun impact client, l'interface RMI n'a pas changé. |
| **2** | Flag porté à **10 %** (puis 50 %) : une partie des appels RMI est transmise par la façade à `ServeurGrpc`. `CalculatorService` est marquée `@Deprecated`, le README annonce la date de retrait (ADR-0003). | Sur 1 semaine : 0 résultat différent entre les deux chemins, taux de repli < 1 %, latence du chemin gRPC via la façade ≤ 5 ms. | **Flag remis à 0** dans `migration.properties` : retour à 100 % ancien chemin en moins de 5 s, sans redémarrage ni redéploiement. |
| **3** | Flag à **100 %** : tout le calcul passe par gRPC ; le calcul local reste dans la façade **en secours** (repli automatique). Les clients RMI sont migrés un par un vers un client gRPC direct ; les callbacks sont remplacés par `HistoriqueEnFlux`. | Critères de l'ADR-0003, fixés à l'avance : 4 semaines à 100 %, 0 appel RMI direct et 0 repli pendant les 2 dernières semaines, latence gRPC dans la plage ≈ 2,2 à 2,8 ms. | **Flag réabaissé** (0 ou 10 %) sans redéploiement ; un client migré peut revenir à son ancien client RMI, toujours servi. |
| **4** | **Retrait définitif** de RMI : module `rmi/`, registre 1099, façade et `migration.properties` supprimés, après `git tag avant-retrait-rmi`. | — | **Irréversible.** Condition : l'étape 3 est stable depuis la durée définie à l'avance (4 semaines), tous les critères de l'ADR-0003 sont remplis. Si un critère manque, le retrait est reporté, pas forcé. |

Chaque étape se pilote avec les outils déjà construits dans le projet : `BenchmarkRMIProtocole` et
`BenchmarkGrpc` (même protocole qu'en séance 8 : 10 appels de chauffe, 100 mesurés) et Wireshark
pour vérifier quel port reçoit réellement le trafic (1099/port aléatoire contre 50051).

## Synthèse : pourquoi ce plan évite le scénario Netscape

En 1998, Netscape a choisi de jeter le code de son navigateur et de tout réécrire. Pendant près de
trois ans, l'entreprise n'a livré aucune nouvelle version : l'ancien code n'évoluait plus, le nouveau
n'était pas prêt, et la concurrence a pris le marché. Le problème n'était pas la nouvelle technologie,
mais la **bascule en un seul bloc, sans retour possible** et sans rien livrer entre-temps.

Notre plan fait l'inverse sur trois points. D'abord, **l'ancien système continue de fonctionner à
chaque étape** : les clients RMI gardent la même interface `CalculatorService` du début à la fin de la
transition, et la façade ne change rien pour eux tant que le flag est à 0. Ensuite, **chaque étape sauf
la dernière se défait en quelques secondes**, en remettant un chiffre dans `migration.properties`, sans
recompiler ni redéployer : une erreur sur le chemin gRPC coûte au pire quelques appels, qui retombent
de toute façon sur le calcul local. Enfin, **on avance sur des mesures, pas sur une impression** : le
passage d'une étape à l'autre dépend de critères chiffrés fixés à l'avance (latence dans la plage
mesurée en séance 8, 0 résultat divergent, 0 appel RMI direct), et la seule étape irréversible, le
retrait de RMI, n'a lieu qu'après des semaines de fonctionnement stable à 100 %.

Là où Netscape a misé toute l'entreprise sur un « grand soir », nous remplaçons RMI comme le figuier
étrangleur remplace son arbre hôte : petit à petit, autour de lui, en le laissant debout jusqu'à ce
qu'il ne porte plus rien.
