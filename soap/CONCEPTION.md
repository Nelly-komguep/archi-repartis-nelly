# Conception du service SOAP — Approche Top-Down

## 1. Objectif

L'objectif de ce module est de concevoir et implémenter un service de calcul distribué utilisant le protocole **SOAP**.

Le service doit permettre à un client distant d'exécuter plusieurs opérations à travers des messages XML conformes à un contrat **WSDL**.

Les opérations proposées sont :

* addition ;
* soustraction ;
* multiplication ;
* division ;
* consultation de l'historique.

---

## 2. Choix de l'approche Top-Down

Pour ce module, l'approche **Top-Down** a été retenue.

Cette approche consiste à commencer par définir le **contrat du service** avant de développer son implémentation.

Le contrat décrit notamment :

* les opérations disponibles ;
* les paramètres des opérations ;
* les types de données ;
* les messages SOAP ;
* le namespace du service ;
* les réponses attendues.

Le fichier `calculator.wsdl` constitue donc la référence principale du service.

---

## 3. Processus de conception

Le processus suivi est :

```text
┌───────────────────────┐
│   Définition WSDL     │
│   calculator.wsdl     │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│      wsimport         │
│ Génération des classes│
│       Java            │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│ Spring Web Services   │
│ Implémentation du     │
│ service SOAP          │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│    Serveur SOAP       │
│  localhost:8080/ws    │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│       SoapUI          │
│ Tests des opérations  │
└───────────────────────┘
```

---

## 4. Définition du contrat WSDL

Le fichier :

```text
src/main/resources/calculator.wsdl
```

définit le contrat du service.

Le namespace principal utilisé est :

```text
http://nelly.com/calculator
```

Le contrat expose les opérations suivantes :

```text
add
sub
mul
div
getHistory
```

Chaque opération possède une requête et une réponse correspondante.

Exemple :

```text
addRequest
     │
     ├── a
     └── b
          │
          ▼
     addResponse
          │
          └── result
```

---

## 5. Génération des classes Java

À partir du WSDL, le plugin `jaxws-maven-plugin` utilise l'outil `wsimport`.

La génération est exécutée avec :

```bash
mvn clean compile
```

Les classes générées sont placées dans :

```text
target/generated-sources/wsimport/
```

Elles comprennent notamment :

* `AddRequest`
* `AddResponse`
* `SubRequest`
* `SubResponse`
* `MulRequest`
* `MulResponse`
* `DivRequest`
* `DivResponse`
* `GetHistoryResponse`
* `CalculatorPortType`
* `ObjectFactory`

Cette génération permet de garantir que l'implémentation Java respecte la structure définie dans le contrat WSDL.

---

## 6. Implémentation avec Spring Web Services

L'application utilise **Spring Web Services** pour recevoir et traiter les messages SOAP.

La classe :

```text
CalculatorEndpoint.java
```

constitue le point d'entrée du traitement des requêtes SOAP.

Les annotations `@Endpoint` et `@PayloadRoot` permettent d'associer les requêtes SOAP aux méthodes correspondantes.

Exemple conceptuel :

```text
addRequest
    │
    ▼
@PayloadRoot
    │
    ▼
add()
    │
    ▼
Addition
    │
    ▼
addResponse
```

Le namespace utilisé pour le routage est :

```text
http://nelly.com/calculator
```

---

## 7. Configuration du serveur

La configuration du service est assurée par :

```text
SoapConfig.java
```

Le servlet `MessageDispatcherServlet` permet à Spring Web Services de recevoir les requêtes SOAP.

L'endpoint du service est :

```text
http://localhost:8080/ws/calculator
```

Le WSDL est accessible à :

```text
http://localhost:8080/ws/calculator.wsdl
```

---

## 8. Format des échanges

SOAP repose sur des messages XML structurés dans une enveloppe.

Exemple d'une requête d'addition :

```xml
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:cal="http://nelly.com/calculator">

    <soapenv:Header/>

    <soapenv:Body>

        <cal:addRequest>
            <cal:a>10</cal:a>
            <cal:b>5</cal:b>
        </cal:addRequest>

    </soapenv:Body>

</soapenv:Envelope>
```

Le serveur traite le contenu du `Body` puis renvoie une réponse SOAP.

---

## 9. Gestion des erreurs

Le service prend notamment en compte le cas de division par zéro.

Lorsqu'une requête contient :

```text
a = 10
b = 0
```

le service détecte l'erreur avant d'effectuer l'opération.

Cette vérification permet d'éviter un résultat numérique invalide et de signaler explicitement le problème au client SOAP.

---

## 10. Gestion de l'historique

Le service conserve les opérations exécutées avec succès dans une structure d'historique.

Chaque entrée contient :

* la date et l'heure ;
* le type d'opération ;
* les valeurs utilisées ;
* le résultat.

Exemple :

```text
2026-09-30T09:30:00 - ADD(10.0, 5.0) = 15.0
```

L'opération `getHistory` permet ensuite au client de récupérer cet historique.

---

## 11. Validation avec SoapUI

SoapUI a été utilisé comme client de test afin de vérifier le comportement du service.

Les scénarios suivants ont été exécutés :

| Scénario          | Entrées | Résultat             |
| ----------------- | ------- | -------------------- |
| Addition          | 10, 5   | 15.0                 |
| Soustraction      | 10, 5   | 5.0                  |
| Multiplication    | 10, 5   | 50.0                 |
| Division          | 10, 5   | 2.0                  |
| Division par zéro | 10, 0   | Erreur               |
| Historique        | —       | Liste des opérations |

---

## 12. Flux global d'une requête

Le fonctionnement global peut être résumé ainsi :

```text
Client SoapUI
     │
     │ Requête SOAP XML
     ▼
MessageDispatcherServlet
     │
     ▼
Spring Web Services
     │
     │ Routage par namespace
     │ + localPart
     ▼
CalculatorEndpoint
     │
     ▼
Traitement de l'opération
     │
     ▼
Réponse Java
     │
     ▼
Réponse SOAP XML
     │
     ▼
Client SoapUI
```

---

## 13. Avantages de l'approche Top-Down

L'approche utilisée présente plusieurs avantages :

### Contrat défini avant l'implémentation

Le WSDL permet de formaliser le service avant son développement.

### Génération automatique

Les classes Java sont générées automatiquement à partir du contrat.

### Interopérabilité

SOAP repose sur des messages XML standardisés, ce qui facilite l'interaction entre systèmes développés avec différentes technologies.

### Séparation contrat / implémentation

Le contrat du service est indépendant de son implémentation interne.

### Validation facilitée

SoapUI permet de vérifier directement que les messages échangés respectent le contrat défini.

---

## 14. Conclusion

L'approche **Top-Down** a permis de construire le service SOAP à partir d'un contrat WSDL préalablement défini.

Le processus a suivi les principales étapes suivantes :

```text
WSDL
 ↓
Génération des classes
 ↓
Implémentation Spring Web Services
 ↓
Déploiement du service
 ↓
Tests SoapUI
```

Les opérations de calcul ainsi que la consultation de l'historique ont été testées avec succès.

Le module constitue ainsi une implémentation complète d'un service distribué SOAP basé sur une démarche de conception **contract-first / Top-Down**.
