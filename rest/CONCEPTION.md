# CONCEPTION — API REST du service de calcul

## 1. Objectif

Le module REST réexpose le même besoin métier que les modules Socket et RMI développés précédemment :

* additionner deux nombres ;
* soustraire deux nombres ;
* multiplier deux nombres ;
* diviser deux nombres ;
* conserver l'historique des opérations ;
* consulter une opération précise.

L'objectif est de construire une API REST de niveau 2 selon le modèle de maturité de Richardson.

La conception des ressources est réalisée avant l'implémentation afin d'éviter une simple traduction des méthodes RMI en routes HTTP.

---

## 2. Ressource REST

La ressource principale du domaine est une **opération de calcul**.

Une opération représente un calcul effectué par le service et enregistré dans l'historique.

Une opération possède les informations suivantes :

* `id` : identifiant unique ;
* `type` : type de calcul (`ADD`, `SUB`, `MUL`, `DIV`) ;
* `a` : première valeur ;
* `b` : deuxième valeur ;
* `result` : résultat du calcul ;
* `timestamp` : date et heure d'exécution.

La collection des opérations est représentée par :

```text
/operations
```

Une opération individuelle est représentée par :

```text
/operations/{id}
```

---

## 3. Tableau de conception

| Méthode + route        | Rôle                                  | Succès attendu                                  | Erreurs attendues                                                                                                                      |
| ---------------------- | ------------------------------------- | ----------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| POST `/operations`     | Calculer et enregistrer une opération | `201 Created` + en-tête `Location` + objet JSON | `400 Bad Request` si le corps est invalide ou incomplet ; `422 Unprocessable Entity` pour une erreur métier comme la division par zéro |
| GET `/operations`      | Consulter l'historique des opérations | `200 OK` + tableau JSON                         | Aucune erreur métier prévue                                                                                                            |
| GET `/operations/{id}` | Consulter une opération précise       | `200 OK` + objet JSON                           | `404 Not Found` si l'identifiant n'existe pas                                                                                          |

---

## 4. Création d'une opération

### Requête

```http
POST /operations
Content-Type: application/json
```

Exemple :

```json
{
  "type": "ADD",
  "a": 4,
  "b": 7
}
```

### Réponse en cas de succès

```http
HTTP/1.1 201 Created
Location: /operations/1
Content-Type: application/json
```

Exemple :

```json
{
  "id": 1,
  "type": "ADD",
  "a": 4,
  "b": 7,
  "result": 11,
  "timestamp": "2026-09-29T..."
}
```

Le code `201 Created` indique que la nouvelle opération a été créée et enregistrée dans l'historique.

---

## 5. Consultation de l'historique

### Requête

```http
GET /operations
```

### Réponse

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

Exemple :

```json
[
  {
    "id": 1,
    "type": "ADD",
    "a": 4,
    "b": 7,
    "result": 11,
    "timestamp": "2026-09-29T..."
  },
  {
    "id": 2,
    "type": "MUL",
    "a": 6,
    "b": 3,
    "result": 18,
    "timestamp": "2026-09-29T..."
  }
]
```

---

## 6. Consultation d'une opération

### Requête

```http
GET /operations/1
```

### Si l'opération existe

```http
HTTP/1.1 200 OK
```

Le serveur retourne l'opération correspondant à l'identifiant demandé.

### Si l'opération n'existe pas

```http
HTTP/1.1 404 Not Found
```

---

## 7. Gestion des erreurs

### 7.1 Corps de requête incomplet

Exemple :

```json
{
  "a": 4,
  "b": 7
}
```

Le champ `type` est absent.

Réponse attendue :

```http
400 Bad Request
```

Cette erreur correspond à un problème de format ou de validation de la requête.

---

### 7.2 Division par zéro

Exemple :

```json
{
  "type": "DIV",
  "a": 10,
  "b": 0
}
```

Réponse attendue :

```http
422 Unprocessable Entity
```

La requête est correctement formée, mais l'opération demandée ne peut pas être exécutée en raison d'une règle métier.

L'API ne doit pas transformer cette erreur prévisible en :

```http
500 Internal Server Error
```

---

## 8. Niveau Richardson

L'API vise le **niveau 2** du modèle de maturité de Richardson.

Elle utilise :

* des ressources nommées au pluriel ;
* des URI orientées ressources ;
* les verbes HTTP selon leur sémantique ;
* les codes de statut HTTP pour représenter les résultats.

Les routes prévues sont :

```text
POST /operations
GET  /operations
GET  /operations/{id}
```

Aucune route ne contient de verbe métier.

Les routes suivantes sont donc volontairement exclues :

```text
/add
/sub
/multiply
/divide
/getOperations
```

Cette conception évite de reproduire un modèle RPC déguisé en HTTP.

---

## 9. Correspondance avec le module RMI

Le service REST conserve le même besoin métier que le module RMI.

### RMI

```text
CalculatorService
├── add()
├── sub()
├── mul()
└── div()

HistoryService
└── getAll()
```

### REST

```text
/operations
├── POST
│   └── effectuer et enregistrer un calcul
├── GET
│   └── consulter l'historique
└── GET /{id}
    └── consulter une opération précise
```

Le paradigme d'exposition change, mais la logique métier du calcul reste conceptuellement la même.

---

## 10. Idempotence de la lecture

La requête :

```http
GET /operations/1
```

est une opération de lecture.

Deux appels successifs sur la même ressource doivent retourner la même représentation tant que cette ressource n'a pas été modifiée.

Ce comportement sera vérifié avec Postman.

---

## 11. Synthèse

La ressource principale retenue est :

```text
Operation
```

La collection est :

```text
/operations
```

Les routes prévues sont :

```text
POST /operations
GET  /operations
GET  /operations/{id}
```

Cette conception respecte le principe « resource-first » demandé dans la séance.

Elle constitue le contrat de référence de l'implémentation du module REST.
