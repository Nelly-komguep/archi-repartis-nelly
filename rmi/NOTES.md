# NOTES Module RMI

## 1. Exception observée lorsque le serveur est arrêté

Lorsque le serveur RMI est arrêté et que le client est lancé, le client ne peut plus contacter le registre RMI sur le port `1099`.

L'exception observée est :

```text
java.rmi.ConnectException: Connection refused to host: localhost; nested exception is:
        java.net.ConnectException: Connection refused: connect
```

L'exception complète commence notamment par :

```text
java.rmi.ConnectException
    at sun.rmi.transport.tcp.TCEndpoint.newSocket
    at sun.rmi.transport.tcp.TCPChannel.createConnection
    at sun.rmi.transport.tcp.TCPChannel.newConnection
    at sun.rmi.server.UnicastRef.newCall
    at sun.rmi.registry.RegistryImpl_Stub.lookup
    at client.Client.main(Client.java:18)
```

### Interprétation

L'erreur indique que la connexion TCP vers le registre RMI sur `localhost:1099` a été refusée parce que le serveur RMI n'est plus en fonctionnement.

---

## 2. À quel moment du cycle RMI l'erreur apparaît-elle ?

Dans le client, les instructions suivantes sont exécutées :

```java
Registry registry =
        LocateRegistry.getRegistry("localhost", 1099);
```

Puis :

```java
CalculatorService calc =
        (CalculatorService) registry.lookup("CalculatorService");
```

L'appel :

```java
LocateRegistry.getRegistry("localhost", 1099)
```

ne réalise pas immédiatement une connexion réseau au registre. Il fournit une référence permettant d'accéder au registre.

La connexion est réellement tentée lors de :

```java
registry.lookup("CalculatorService");
```

C'est donc à cette étape que la connexion au registre RMI est refusée lorsque le serveur est arrêté.

L'erreur apparaît **avant l'appel de la méthode distante `calc.add()`**.

Le cycle est donc :

```text
Client
   │
   │ LocateRegistry.getRegistry()
   ▼
Référence vers le registre
   │
   │ registry.lookup("CalculatorService")
   ▼
Connexion TCP vers localhost:1099
   │
   ├── Serveur disponible
   │       │
   │       ▼
   │   Récupération du stub
   │       │
   │       ▼
   │   calc.add(...)
   │
   └── Serveur arrêté
           │
           ▼
   java.rmi.ConnectException
```

---

## 3. Que se passe-t-il si HistoryService tombe en panne ?

Dans cette implémentation, `HistoryService` est considéré comme un service **non critique** pour le calcul.

Après chaque opération réussie, `CalculatorServiceImpl` tente d'enregistrer l'opération :

```java
history.record(entry);
```

Cet appel est protégé par un bloc `try/catch` :

```java
try {
    history.record(entry);
} catch (RemoteException e) {
    System.out.println(
            "Impossible d'enregistrer l'historique : "
                    + e.getMessage()
    );
}
```

Ainsi, si `HistoryService` devient indisponible :

1. le calcul est effectué ;
2. le résultat est obtenu ;
3. l'appel vers `HistoryService` échoue ;
4. l'erreur est enregistrée côté serveur ;
5. le résultat du calcul est quand même retourné au client.

Par exemple, si `HistoryService` est indisponible pendant :

```java
calc.add(4, 7);
```

le résultat :

```text
11.0
```

peut toujours être retourné.

### Choix de conception

Le choix effectué est donc :

> **Une panne du service d'historique ne doit pas empêcher le fonctionnement du service de calcul.**

Ce choix permet de séparer la fonction principale, qui est le calcul, d'une fonction secondaire, qui est la conservation de l'historique.

---

## 4. Cas particulier : registre RMI arrêté

Il faut distinguer deux situations.

### Registre RMI arrêté

Si le registre RMI est arrêté, le client ne peut pas récupérer le stub de `CalculatorService`.

L'erreur intervient alors lors de :

```java
registry.lookup("CalculatorService");
```

Le calcul ne peut donc pas commencer.

### HistoryService indisponible après récupération des stubs

Si le registre est disponible et que le client possède déjà le stub de `CalculatorService`, mais que l'appel vers `HistoryService` échoue pendant une opération, `CalculatorServiceImpl` intercepte la `RemoteException`.

Le calcul peut alors continuer et retourner son résultat.

---

## 5. Tests réalisés

### Test des opérations

Les opérations suivantes ont été exécutées avec succès :

```text
4 + 7 = 11.0
10 - 5 = 5.0
6 * 3 = 18.0
20 / 4 = 5.0
```

La division par zéro a également été testée :

```text
Erreur division : Division par zéro
```

### Test de l'historique

Les opérations réussies ont été enregistrées dans `HistoryService` :

```text
ADD(4.0, 7.0) = 11.0
SUB(10.0, 5.0) = 5.0
MUL(6.0, 3.0) = 18.0
DIV(20.0, 4.0) = 5.0
```

### Test d'arrêt du serveur

Après arrêt du serveur, le lancement du client produit :

```text
java.rmi.ConnectException: Connection refused to host: localhost; nested exception is:
java.net.ConnectException: Connection refused: connect
```

Cela confirme que le client ne peut plus accéder au registre RMI sur le port `1099`.

---

## 6. Schéma général du fonctionnement

```text
                 ┌─────────────────────┐
                 │       Client        │
                 └──────────┬──────────┘
                            │
                            │ lookup
                            ▼
                 ┌─────────────────────┐
                 │   Registre RMI      │
                 │      :1099          │
                 └──────────┬──────────┘
                            │
                    ┌───────┴────────┐
                    ▼                ▼
          ┌─────────────────┐ ┌─────────────────┐
          │ CalculatorService│ │  HistoryService │
          └────────┬────────┘ └────────┬────────┘
                   │                   ▲
                   ▼                   │
          ┌─────────────────┐          │
          │CalculatorService│──────────┘
          │      Impl       │  record()
          └─────────────────┘
```

`CalculatorServiceImpl` joue donc deux rôles :

* objet distant appelé par le client ;
* client distant de `HistoryService`.

---

## 7. Maintien du serveur en fonctionnement

Le serveur est lancé avec Maven à l'aide de :

```powershell
mvn exec:java "-Dexec.mainClass=server.Server"
```

Afin de maintenir le processus actif lorsque le serveur est lancé avec Maven Exec, l'instruction suivante a été ajoutée à la fin de `Server.java` :

```java
Thread.currentThread().join();
```

Elle permet au serveur de rester en fonctionnement et de continuer à accepter les appels RMI jusqu'à son arrêt manuel.

---

## 8. Conclusion

Le module RMI respecte les fonctionnalités demandées :

* création d'un registre RMI sur le port `1099` ;
* exposition du service `CalculatorService` ;
* implémentation des quatre opérations ;
* gestion de la division par zéro ;
* création du service distant `HistoryService` ;
* enregistrement des opérations réussies ;
* récupération de l'historique ;
* test du fonctionnement client/serveur ;
* identification de l'exception produite lorsque le serveur est arrêté ;
* définition explicite du comportement lorsque le service d'historique devient indisponible.
