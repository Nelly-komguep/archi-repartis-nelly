# ADR-0003 : Stratégie de dépréciation et de retrait du service RMI

## Statut
Proposé (2026-11-10)

## Contexte
Pendant la transition (cf. [ADR-0001](0001-migration-rmi-vers-grpc.md) et
[ADR-0002](0002-facade-et-feature-flag.md)), deux implémentations coexistent. La double maintenance a
un coût, mais le retrait de RMI est la seule étape **irréversible** du plan : une fois le registre 1099
et `CalculatorServiceImpl` supprimés, un client RMI oublié ne fonctionne plus du tout. Il faut donc
décider quand et à quelles conditions on retire RMI, et comment prévenir les clients.

## Décision
1. **Annonce** : dès l'étape 2 du plan, `CalculatorService` est marquée `@Deprecated` dans le code et
   le README indique la date de retrait prévue.
2. **Gel** : plus aucune nouvelle fonctionnalité côté RMI ; seules les corrections de bugs bloquants
   sont acceptées.
3. **Critères mesurables de retrait**, tous requis :
   - 100 % du trafic sur gRPC (flag à 100) depuis une **durée fixée à l'avance : 4 semaines** ;
   - **0 appel RMI direct** pendant les 2 dernières semaines : la façade journalise chaque appelant,
     et tous doivent avoir migré vers un client gRPC direct ;
   - **0 repli** de la façade vers le calcul local pendant la même période ;
   - latence gRPC mesurée avec le protocole de la séance 8 restée dans la plage observée (≈ 2,2 à 2,8 ms).
4. **Retrait** : suppression du module `rmi/` (callbacks d'abord, puis `CalculatorService`), de la façade et du fichier `migration.properties`, dans un
   commit unique et étiqueté (`git tag avant-retrait-rmi`) pour pouvoir consulter l'ancien code.

## Alternatives écartées
- **Garder RMI indéfiniment** en parallèle : écarté. Double maintenance permanente, et le port
  aléatoire + l'adresse `172.21.112.1` restent un risque réseau ouvert.
- **Retrait à date fixe sans critère d'usage** : écarté. Rien ne garantit qu'aucun client n'appelle
  encore RMI ; c'est précisément ce que le compteur d'appels mesure.
- **Retrait immédiat dès que gRPC atteint 100 %** : écarté. On perdrait le secours de l'étape 3, qui
  est la dernière occasion de revenir en arrière sans redéploiement.

## Conséquences
- **Positives** : décision de retrait fondée sur des chiffres, pas sur une impression ; la date est
  connue des clients ; le tag Git conserve l'historique.
- **Négatives** : au moins 4 semaines de double maintenance après la bascule complète ; il faut ajouter
  un compteur d'appels côté RMI ; le retrait reste irréversible (un client oublié devra migrer en urgence).
