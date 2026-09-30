# Comparatif RMI vs REST

## 1. Objectif

Cette étude compare l'implémentation du service de calcul réalisée avec **Java RMI** et celle réalisée avec **REST** dans le cadre du même besoin fonctionnel.

Les deux services permettent notamment de réaliser les opérations :

* addition ;
* soustraction ;
* multiplication ;
* division ;
* consultation de l'historique des opérations.

La comparaison porte sur le couplage, la lisibilité du contrat, les performances mesurées, la gestion des erreurs et les moyens de test.

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

1. `POST Addition - 201`
2. `POST Division par zéro - 422`
3. `POST Champ manquant - 400`
4. `GET Toutes les opérations - 200`
5. `GET Opération existante - 200`
6. `GET Opération inexistante - 404`
7. `GET Même opération - Idempotence`

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

## 8. Synthèse

| Critère                        | RMI                               | REST                                |
| ------------------------------ | --------------------------------- | ----------------------------------- |
| Communication                  | Appels de méthodes distantes      | HTTP                                |
| Format principal               | Objets/classes Java               | JSON                                |
| Dépendance au langage          | Forte dépendance à Java           | Indépendant du langage côté client  |
| Contrat                        | Interface Java                    | Ressources + HTTP + JSON            |
| Gestion des erreurs            | Exceptions Java / RemoteException | Codes HTTP + JSON                   |
| Test avec outils HTTP          | Non natif                         | Oui                                 |
| Performance mesurée dans ce TP | 0,733777 ms/appel                 | 5,966458 ms/appel                   |
| Interopérabilité               | Principalement environnement Java | Clients HTTP de différents langages |

---

## 9. Conclusion

L'expérimentation montre deux styles d'architecture différents.

RMI propose une abstraction proche de l'appel de méthode Java et permet au client de manipuler directement une interface distante. Cette approche est particulièrement intégrée à l'écosystème Java.

REST expose au contraire des ressources accessibles par HTTP. L'utilisation des méthodes HTTP, des codes de statut et de JSON permet de construire une interface indépendante du langage du client.

Dans notre expérimentation, les 100 appels mesurés ont donné un temps moyen de **0,733777 ms pour RMI** et de **5,966458 ms pour REST**. Ces valeurs correspondent à l'environnement de test utilisé et doivent être interprétées comme des observations expérimentales.

Le choix entre les deux approches dépend donc du contexte du système, notamment du niveau d'interopérabilité recherché, du type de clients, du contrat souhaité et des contraintes de communication.
