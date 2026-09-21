\# Module RMI — Service de calcul distribué



\## 1. Présentation



Ce module implémente un service de calcul distribué en Java RMI dans le cadre du projet fil rouge du cours « Architecture logicielle et objets répartis ».



Le service principal `CalculatorService` permet d'effectuer à distance les opérations suivantes :



\- addition ;

\- soustraction ;

\- multiplication ;

\- division.



Un second service distant, `HistoryService`, conserve l'historique des opérations réussies.



\## 2. Architecture



Le module est organisé en deux packages :



```text

rmi/

├── pom.xml

├── README.md

├── NOTES.md

└── src/

&#x20;   └── main/

&#x20;       └── java/

&#x20;           ├── client/

&#x20;           │   └── Client.java

&#x20;           │

&#x20;           └── server/

&#x20;               ├── CalculatorService.java

&#x20;               ├── CalculatorServiceImpl.java

&#x20;               ├── HistoryService.java

&#x20;               ├── HistoryServiceImpl.java

&#x20;               └── Server.java

