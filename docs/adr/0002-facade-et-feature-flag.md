# ADR-0002 : Mécanisme de bascule : façade côté serveur RMI pilotée par un feature flag

## Statut
Proposé (2026-11-10)

## Contexte
L'[ADR-0001](0001-migration-rmi-vers-grpc.md) impose de migrer **sans interrompre les clients
existants**. Or ces clients (`Client.java`, `BenchmarkRMIProtocole`) parlent JRMP : ils font
`registry.lookup("CalculatorService")` sur le port 1099 et gardent ensuite le stub résolu. Ils ne
peuvent pas devenir clients gRPC sans être modifiés et redéployés. Il faut donc pouvoir :
1. faire exécuter les calculs par la nouvelle implémentation gRPC **sans toucher aux clients RMI** ;
2. revenir à l'ancien chemin instantanément en cas de problème ;
3. ne basculer qu'une partie du trafic au début.

## Décision
Transformer `CalculatorServiceImpl` en **façade** (patron du figuier étrangleur) :
- l'interface RMI `CalculatorService` reste **inchangée** : les clients RMI ne voient aucune différence ;
- à chaque appel, la façade lit un feature flag `calcul.grpc.pourcentage` (0 à 100) :
  - chemin ancien : le calcul est fait localement, comme aujourd'hui ;
  - chemin nouveau : la façade transmet l'appel à `ServeurGrpc` (port 50051) par un stub gRPC bloquant
    et renvoie le résultat au client RMI ;
- le flag est lu dans un fichier `migration.properties` **relu à chaud** (toutes les 5 s) : changer le
  pourcentage ne demande **ni redémarrage, ni redéploiement**. Valeur par défaut sûre : 0 % si le
  fichier est absent ou illisible ;
- en cas d'erreur du chemin gRPC (`StatusRuntimeException`), la façade **revient au calcul local** pour
  cet appel et incrémente un compteur d'échecs ;
- la façade journalise pour chaque appel le chemin pris, la durée et l'appelant : ce sont ces mesures
  qui décident du passage à l'étape suivante du [plan](../plan-migration.md).

Les **nouveaux** clients appellent directement `ServeurGrpc`. Les clients RMI existants seront migrés
un par un vers gRPC une fois le chemin gRPC validé par la façade.

## Alternatives écartées
- **Proxy réseau devant RMI** (Envoy, nginx, load balancer) : écartée. RMI n'utilise pas HTTP mais
  JRMP, avec le registre sur 1099 puis un **port aléatoire** pour l'objet distant (constaté dans
  Wireshark en séance 8) : un proxy générique ne sait pas router ce trafic sans configuration lourde.
- **Bascule par constante compilée** (`if (UTILISER_GRPC)`) ou variable lue au démarrage : écartée.
  Chaque changement exige de recompiler et redémarrer `Server`, ce qui coupe les clients RMI connectés :
  le retour arrière ne serait pas instantané.
- **Double déploiement bleu/vert** (deux serveurs RMI, bascule du registre) : écartée. Le registre ne
  sait pas répartir un pourcentage du trafic, et les clients gardent le stub résolu au démarrage :
  ils ne verraient même pas la bascule.
- **Faire migrer tout de suite chaque client RMI vers gRPC** : écartée comme première étape, car c'est
  exactement « interrompre les clients existants ». C'est l'objectif final, client par client.

## Conséquences
- **Positives** : retour arrière en moins de 5 s (flag remis à 0) ; bascule progressive
  (1 %, 10 %, 50 %, 100 %) ; comparaison mesurée des deux chemins sur le trafic réel ; aucun client
  à modifier pendant la transition.
- **Négatives** :
  - un saut réseau de plus sur le nouveau chemin (client RMI → façade → gRPC) : la latence mesurée via
    la façade sera supérieure à celle d'un client gRPC direct (≈ 2,6 ms en séance 8), il ne faudra pas
    confondre les deux mesures ;
  - le serveur RMI dépend désormais du serveur gRPC : le repli local évite la panne, mais peut masquer
    une défaillance gRPC s'il n'est pas surveillé ;
  - la façade et le fichier de flag sont du code temporaire, supprimés au retrait de RMI
    ([ADR-0003](0003-retrait-du-service-rmi.md)).
