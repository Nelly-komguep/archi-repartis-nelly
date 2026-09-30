# Module SOAP — Architecture Logicielle et Objets Répartis

## 1. Présentation

Ce module met en œuvre un service de calcul distribué basé sur le protocole **SOAP (Simple Object Access Protocol)**.

L'objectif est de permettre à un client distant d'invoquer plusieurs opérations de calcul à travers des messages SOAP structurés selon un contrat **WSDL**.

L'implémentation utilise une approche **Top-Down** : le contrat WSDL est défini en premier, puis les classes Java nécessaires sont générées automatiquement à partir de ce contrat.

---

## 2. Technologies utilisées

* Java 17
* Spring Boot 3.4.3
* Spring Web Services
* Maven
* SOAP
* WSDL
* JAXB
* JAX-WS
* SoapUI

---

## 3. Approche Top-Down

L'approche suivie est la suivante :

```text
              calculator.wsdl
                    │
                    ▼
              wsimport Maven
                    │
                    ▼
       Classes Java générées
                    │
                    ▼
        Spring Web Services
                    │
                    ▼
             Service SOAP
                    │
                    ▼
                SoapUI
```

Le contrat `calculator.wsdl` constitue donc le point de départ du développement.

Les classes Java sont générées automatiquement à l'aide du plugin Maven `jaxws-maven-plugin`.

---

## 4. Architecture du module

```text
soap/
├── pom.xml
├── README.md
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── nelly/
│       │           └── soap/
│       │               ├── SoapApplication.java
│       │               ├── SoapConfig.java
│       │               └── CalculatorEndpoint.java
│       │
│       └── resources/
│           └── calculator.wsdl
│
└── target/
    └── generated-sources/
        └── wsimport/
            └── com/
                └── nelly/
                    └── soap/
                        └── generated/
```

---

## 5. Opérations disponibles

Le service expose cinq opérations :

| Opération    | Description                                 |
| ------------ | ------------------------------------------- |
| `add`        | Addition de deux nombres                    |
| `sub`        | Soustraction de deux nombres                |
| `mul`        | Multiplication de deux nombres              |
| `div`        | Division de deux nombres                    |
| `getHistory` | Récupération de l'historique des opérations |

---

## 6. Endpoint SOAP

Le service SOAP est accessible à l'adresse :

```text
http://localhost:8080/ws/calculator
```

Le contrat WSDL est accessible à :

```text
http://localhost:8080/ws/calculator.wsdl
```

Namespace utilisé :

```text
http://nelly.com/calculator
```

---

## 7. Génération des classes

La génération des classes Java est réalisée automatiquement avec Maven :

```bash
mvn clean compile
```

Le plugin `jaxws-maven-plugin` utilise le fichier :

```text
src/main/resources/calculator.wsdl
```

pour générer les classes Java dans :

```text
target/generated-sources/wsimport
```

---

## 8. Démarrage du service

Depuis le dossier `soap` :

```bash
mvn spring-boot:run
```

Le serveur démarre sur le port :

```text
8080
```

---

## 9. Tests avec SoapUI

Le service a été testé avec **SoapUI**.

### Test 1 — Addition

Requête :

```xml
<cal:addRequest>
    <cal:a>10</cal:a>
    <cal:b>5</cal:b>
</cal:addRequest>
```

Résultat obtenu :

```text
15.0
```

### Test 2 — Soustraction

```text
10 - 5 = 5.0
```

### Test 3 — Multiplication

```text
10 × 5 = 50.0
```

### Test 4 — Division

```text
10 / 5 = 2.0
```

### Test 5 — Division par zéro

La requête :

```text
10 / 0
```

produit une erreur indiquant que la division par zéro est impossible.

### Test 6 — Historique

L'opération `getHistory` permet de récupérer les opérations précédemment exécutées.

Exemple :

```text
ADD(10.0, 5.0) = 15.0
SUB(10.0, 5.0) = 5.0
MUL(10.0, 5.0) = 50.0
DIV(10.0, 5.0) = 2.0
```

---

## 10. Résultats des tests

| Test                      | Résultat |
| ------------------------- | -------- |
| WSDL accessible           | ✅        |
| Connexion SoapUI          | ✅        |
| Addition                  | ✅        |
| Soustraction              | ✅        |
| Multiplication            | ✅        |
| Division                  | ✅        |
| Gestion division par zéro | ✅        |
| Historique                | ✅        |

---

## 11. Conclusion

L'implémentation SOAP permet d'exposer les opérations de calcul sous la forme d'un service distribué basé sur un contrat WSDL.

L'approche **Top-Down** a permis de partir du contrat de service pour générer les classes nécessaires, puis d'implémenter les traitements métier avec Spring Web Services.

Les différents scénarios fonctionnels ont été validés avec SoapUI, notamment les opérations arithmétiques, la gestion d'une erreur de division par zéro et la récupération de l'historique.
