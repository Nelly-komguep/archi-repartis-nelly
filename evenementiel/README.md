# Module `/evenementiel` service de calcul piloté par les événements (séance 7)

Troisième variante du service fil rouge, après RMI (séances 2-3) et REST (séance 4).
Le calcul n'est plus un appel qui attend une réponse : le producteur **publie** une demande,
et des consommateurs indépendants la traitent quand ils sont disponibles.

**Courtier choisi : RabbitMQ.** Une seule image Docker, avec une interface d'administration incluse.
Le besoin (faible volume, pas de rejeu de l'historique des messages) ne justifie pas la complexité
de Kafka (partitions, groupes de consommateurs).

## Flux

```
Producteur ──OperationDemandee──▶ [calcul.demandes] (fanout)
                                        │
                                        ▼  file « calcul.demandes.calculateur »
                                  CalculConsommateur
                                        │
                                        ▼
        ◀──OperationCalculee── [calcul.resultats] (fanout) ──▶ file « calcul.resultats.audit »
        (CorrelateurResultats,                                         │
         file temporaire du producteur)                                ▼
                                                               AuditConsommateur → audit.csv
```

| Fichier | Rôle |
|---|---|
| `evenements/OperationDemandee.java` | Événement publié par le producteur (`id`, `op`, `a`, `b`, `horodatage`) |
| `evenements/OperationCalculee.java` | Événement de résultat : même `id`, `resultat`, `statut` (OK / ERREUR), `erreur` |
| `Topologie.java` | Noms des échanges, connexion au courtier, sérialisation JSON |
| `Producteur.java` | Publie `OperationDemandee` ; client interactif |
| `CorrelateurResultats.java` | Retrouve le résultat d'une demande grâce à l'identifiant d'opération |
| `Calculateur.java` | Logique métier pure (ADD, SUB, MUL, DIV), sans aucune dépendance à RabbitMQ |
| `CalculConsommateur.java` | 1er consommateur : calcule et publie `OperationCalculee` |
| `AuditConsommateur.java` | 2nd consommateur, indépendant : journal `audit.csv`, sans doublon |

## Garanties de livraison

- **At-least-once.** Chaque consommateur n'envoie son accusé de réception (`basicAck`) qu'**après**
  avoir fait son travail. En cas de panne entre les deux, le message est re-livré, jamais perdu.
- **Idempotence de l'audit.** Une re-livraison peut produire un `OperationCalculee` en double.
  L'audit ignore tout identifiant déjà journalisé, et relit `audit.csv` au démarrage.

## Lancer le module

Prérequis : Docker, Java 17 ou plus, Maven.

```powershell
cd evenementiel

# 1. Courtier
docker compose up -d
# Interface d'administration : http://localhost:15672  (guest / guest)

# 2. Consommateur de calcul (terminal 1)
mvn -q compile exec:java "-Dexec.mainClass=com.nelly.evenementiel.CalculConsommateur"

# 3. Consommateur d'audit (terminal 2) : écrit audit.csv
mvn -q compile exec:java "-Dexec.mainClass=com.nelly.evenementiel.AuditConsommateur"

# 4. Producteur interactif (terminal 3)
mvn -q compile exec:java "-Dexec.mainClass=com.nelly.evenementiel.Producteur"
#   > ADD 4 7
#   Publié : id=... (aucun résultat à ce stade)
#   Résultat corrélé : ADD(4.0, 7.0) = 11.0
```

**Important : lancer chaque consommateur au moins une fois avant le producteur.** C'est le
consommateur qui déclare sa file. Tant qu'aucune file n'est liée à un échange fanout, RabbitMQ jette
les messages publiés, et la confirmation de publication est pourtant positive.

Une fois la file créée, le découplage temporel joue : une demande publiée pendant que le consommateur
de calcul est arrêté attend dans la file durable, et elle est traitée à son redémarrage.

Arrêt du courtier : `docker compose down`.

## Test d'intégration asynchrone

Prérequis : courtier démarré **et** `CalculConsommateur` lancé.

```powershell
mvn test
```

| Test | Ce qu'il vérifie |
|---|---|
| `une_addition_publiee_produit_un_resultat_correle` | `ADD 4 4` publié → `OperationCalculee` de même `id`, résultat 8.0 |
| `une_division_par_zero_produit_un_evenement_d_erreur` | `DIV 10 0` → événement de statut `ERREUR`, sans résultat |

Le test n'affirme jamais juste après la publication : il attend l'événement avec un délai maximal
explicite de 5 s (`await().atMost(...)`, Awaitility). Une première version qui vérifiait tout de suite
a échoué, alors que le résultat était bien calculé, quelques millisecondes plus tard. La version avec
attente bornée a réussi 7 fois sur 7.