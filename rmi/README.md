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


# Séance 3 — Callbacks, concurrence et analyse du couplage RMI

## 1. Objectifs

Cette troisième séance a permis d'enrichir le projet RMI avec :

* un mécanisme de callback serveur → client ;
* un système d'abonnement aux notifications ;
* une gestion des clients déconnectés ;
* une protection contre les accès concurrents ;
* un test de charge avec plusieurs clients ;
* une analyse critique du couplage introduit par RMI.

---

## 2. Architecture du callback

Le mécanisme de notification repose sur une interface distante implémentée côté client :

```java
public interface NotificationListener extends Remote {

    void onNewOperation(String entry)
            throws RemoteException;
}
```

Le client crée ensuite un objet distant :

```java
ClientNotificationListener listener =
        new ClientNotificationListener();
```

Puis il s'abonne auprès du serveur :

```java
history.subscribe(listener);
```

Lorsqu'une opération est réalisée, le serveur enregistre l'opération dans l'historique puis notifie les clients abonnés :

```text
Client
   │
   │ appel RMI
   ▼
CalculatorService
   │
   ▼
HistoryService
   │
   │ callback
   ▼
NotificationListener
   │
   ▼
Client
```

Contrairement à une approche basée sur le polling, le client n'a pas besoin d'interroger périodiquement le serveur pour savoir si une nouvelle opération existe.

---

## 3. Gestion des clients déconnectés

Le serveur doit également gérer le cas où un client est brutalement arrêté.

Lorsqu'une notification échoue avec une `RemoteException`, le listener concerné est supprimé :

```java
catch (RemoteException e) {

    listeners.remove(listener);

    System.out.println(
            "Client déconnecté : listener supprimé."
    );
}
```

Cette approche permet de conserver les autres abonnements actifs.

### Test réalisé

Deux clients ont été lancés simultanément.

Un client a effectué une opération et le second a reçu automatiquement la notification.

Ensuite, l'un des clients a été arrêté avec :

```text
Ctrl + C
```

Une nouvelle opération a ensuite été effectuée depuis le client restant.

Le client encore connecté a continué à fonctionner et à recevoir les notifications.

**Résultat : test validé.**

---

## 4. Gestion de la concurrence

Le service d'historique doit pouvoir être utilisé simultanément par plusieurs clients.

L'historique est protégé lors des accès concurrents :

```java
synchronized (history) {
    history.add(entry);
}
```

et :

```java
synchronized (history) {
    return new ArrayList<>(history);
}
```

Pour les listeners, le projet utilise :

```java
CopyOnWriteArrayList<NotificationListener>
```

Cette collection est adaptée à une situation dans laquelle les listeners sont principalement parcourus pour envoyer des notifications et peuvent être ajoutés ou supprimés en cas d'abonnement ou de déconnexion.

---

## 5. Test de charge

Un programme spécifique `ConcurrentLoadTest` a été développé afin de tester le comportement du serveur avec plusieurs clients concurrents.

Configuration du test :

```text
Nombre de clients : 5
Opérations par client : 20
Total attendu : 100 opérations
```

Les cinq clients sont lancés sous forme de threads et commencent leurs opérations de manière concurrente.

Le programme compare la taille de l'historique avant et après l'exécution :

```text
Nouvelles opérations = historique après
                     - historique avant
```

Le résultat attendu et obtenu est :

```text
Nouvelles opérations : 100
Opérations attendues : 100
SUCCÈS : aucune opération perdue.
```

**Résultat : test de concurrence validé.**

---

## 6. Commandes de test

### Démarrer le serveur

Depuis le dossier `rmi` :

```powershell
mvn exec:java "-Dexec.mainClass=server.Server"
```

### Démarrer un client

```powershell
mvn exec:java "-Dexec.mainClass=client.Client"
```

Deux instances du client peuvent être lancées dans deux terminaux différents afin de tester les callbacks.

### Lancer le test de concurrence

```powershell
mvn exec:java "-Dexec.mainClass=client.ConcurrentLoadTest"
```

---

## 7. Fichiers ajoutés

La séance 3 a ajouté ou modifié les éléments suivants :

```text
src/main/java/client/
├── Client.java
├── NotificationListener.java
├── ClientNotificationListener.java
└── ConcurrentLoadTest.java

src/main/java/server/
├── HistoryService.java
└── HistoryServiceImpl.java

ANALYSE.md
```

---

## 8. Analyse critique

L'expérimentation montre que RMI facilite considérablement les appels entre objets distribués Java, mais ne supprime pas les problématiques propres aux systèmes distribués.

Les principaux points observés sont :

* couplage au langage Java ;
* dépendance au réseau et au registre RMI ;
* gestion nécessaire des déconnexions ;
* nécessité de maintenir la compatibilité des interfaces ;
* gestion explicite de la concurrence et des données partagées.

L'analyse détaillée de ces limites est disponible dans le fichier :

```text
ANALYSE.md
```

---

## 9. Bilan de la séance

| Fonctionnalité                 | Résultat   |
| ------------------------------ | ---------- |
| Callback serveur → client      | ✅ Validé   |
| Deux clients simultanés        | ✅ Validé   |
| Notification sans polling      | ✅ Validé   |
| Gestion d'un client déconnecté | ✅ Validé   |
| Historique thread-safe         | ✅ Validé   |
| 5 clients concurrents          | ✅ Validé   |
| 20 opérations par client       | ✅ Validé   |
| 100 opérations au total        | ✅ Validé   |
| Analyse du couplage RMI        | ✅ Réalisée |

La troisième séance a ainsi permis de compléter le projet RMI par un mécanisme de communication bidirectionnelle et de vérifier son comportement dans un contexte concurrent.
