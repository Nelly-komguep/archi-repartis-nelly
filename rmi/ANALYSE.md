# Analyse critique du couplage RMI

## 1. Introduction

L'implémentation réalisée dans ce projet permet d'observer concrètement les avantages et les limites de Java RMI. Le projet expose deux services distants : `CalculatorService`, qui fournit les opérations arithmétiques, et `HistoryService`, qui conserve l'historique et gère les abonnements aux notifications.

La mise en œuvre des callbacks a également permis d'étudier la communication bidirectionnelle entre le serveur et les clients. Enfin, un test de concurrence avec cinq clients réalisant chacun vingt opérations a permis de vérifier le comportement du service lorsque plusieurs requêtes sont exécutées simultanément.

---

## 2. Couplage au langage et à la plateforme

Le premier niveau de couplage concerne le langage de programmation.

L'interface distante `CalculatorService` est définie directement en Java :

```java
public interface CalculatorService extends Remote {

    double add(double a, double b) throws RemoteException;

    double sub(double a, double b) throws RemoteException;

    double mul(double a, double b) throws RemoteException;

    double div(double a, double b) throws RemoteException;
}
```

Le client utilise directement cette interface Java :

```java
CalculatorService calc =
        (CalculatorService) registry.lookup("CalculatorService");
```

Cette approche est simple lorsque le serveur et les clients sont tous développés en Java. Cependant, un client développé dans un autre langage ne peut pas utiliser directement cette interface Java comme le ferait un client Java.

Par exemple, un client Python ou JavaScript devrait disposer d'une autre couche d'interopérabilité ou d'une réimplémentation spécifique du protocole de communication.

Le couplage est donc notamment visible au niveau :

* du langage Java ;
* des interfaces Java ;
* des types et exceptions Java ;
* du mécanisme de registre RMI ;
* de l'utilisation des objets distants Java.

Cette caractéristique constitue un avantage pour un environnement homogène Java, mais limite l'interopérabilité avec des applications développées dans d'autres technologies.

---

## 3. Disponibilité et comportement en cas de panne

La communication RMI dépend de la disponibilité du serveur et du registre RMI.

Lorsqu'un client tente d'accéder au registre alors que le serveur n'est pas disponible, l'application produit une exception de type :

```text
java.rmi.ConnectException:
Connection refused to host: localhost
```

L'exception observée dans ce projet apparaît lors de l'appel :

```java
registry.lookup("CalculatorService");
```

Cela montre que l'utilisation d'un stub RMI ne signifie pas que le service distant est toujours disponible. La communication réseau réelle intervient lorsqu'une opération distante est effectuée.

Le mécanisme de callback introduit également une dépendance supplémentaire. Le serveur conserve les références distantes des clients abonnés :

```java
private final CopyOnWriteArrayList<NotificationListener> listeners;
```

Lorsqu'une opération est enregistrée, le serveur appelle :

```java
listener.onNewOperation(entry);
```

Si un client est brutalement arrêté, cet appel peut provoquer une `RemoteException`.

Le projet traite cette situation de manière robuste :

```java
catch (RemoteException e) {

    listeners.remove(listener);

    System.out.println(
            "Client déconnecté : listener supprimé."
    );
}
```

Le test réalisé avec `Ctrl+C` sur un client a montré que le client restant pouvait continuer à recevoir les notifications.

Ainsi, la panne d'un client abonné ne doit pas empêcher le fonctionnement des autres clients.

---

## 4. Évolutivité de l'interface distante

Une autre limite concerne l'évolution des interfaces.

Dans la version actuelle, l'opération est définie ainsi :

```java
double add(double a, double b)
        throws RemoteException;
```

Supposons qu'une nouvelle exigence impose d'identifier l'utilisateur ayant effectué l'opération. L'interface pourrait devenir :

```java
double add(double a, double b, String userId)
        throws RemoteException;
```

Cette modification change le contrat de l'interface distante.

Le serveur devra modifier l'implémentation de `CalculatorService`, tandis que les clients devront également être adaptés afin de fournir le nouveau paramètre.

Un ancien client qui utilise toujours :

```java
calc.add(10, 5);
```

ne correspondrait plus à la nouvelle signature.

Le problème apparaît donc au niveau du contrat RMI et de la compatibilité entre les versions du client et du serveur.

Dans un environnement distribué, cette évolution doit être maîtrisée car les clients et le serveur peuvent ne pas être mis à jour simultanément.

Une évolution d'interface distante nécessite donc de prendre en compte la compatibilité des versions déployées.

---

## 5. Concurrence : responsabilité du développeur

Le test réalisé avec cinq clients concurrents et vingt opérations par client a permis d'effectuer :

```text
5 clients × 20 opérations = 100 opérations
```

La vérification de l'historique a permis de comparer le nombre d'opérations avant et après le test.

Le projet protège l'accès à l'historique :

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

Les listeners utilisent quant à eux :

```java
private final CopyOnWriteArrayList<NotificationListener> listeners;
```

Cette structure permet de parcourir la liste des abonnés tout en limitant les risques liés aux modifications concurrentes de cette collection.

Le résultat du test montre que les opérations concurrentes ont été correctement enregistrées et qu'aucune opération attendue n'a été perdue.

Cependant, RMI ne garantit pas automatiquement la cohérence des données métier. RMI fournit principalement les mécanismes permettant :

* la localisation du service ;
* la communication distante ;
* la sérialisation des paramètres et résultats ;
* la gestion des appels distants.

La protection des données partagées reste une responsabilité de l'application.

Cette expérience montre donc que la frontière entre le middleware et le code métier est importante : le middleware facilite la communication distribuée, mais le développeur doit toujours gérer les problèmes de concurrence, de synchronisation, de disponibilité et de cohérence.

---

## 6. Bilan critique

L'ajout des callbacks a montré que RMI permet non seulement au client d'appeler le serveur, mais également au serveur d'appeler un objet distant exposé par le client.

Le fonctionnement obtenu est le suivant :

```text
Client A
   │
   │ opération distante
   ▼
CalculatorService
   │
   ▼
HistoryService
   │
   │ callback
   ├──────────────► Client A
   │
   └──────────────► Client B
```

Cette architecture fonctionne efficacement dans un environnement Java relativement homogène.

Cependant, l'expérience met également en évidence plusieurs limites :

* dépendance importante à Java ;
* dépendance au registre et au réseau ;
* gestion nécessaire des déconnexions ;
* évolution des interfaces nécessitant une attention particulière à la compatibilité ;
* responsabilité du développeur concernant la concurrence et la cohérence des données.

Ainsi, RMI simplifie fortement la mise en œuvre des appels distribués, mais il ne supprime pas les problématiques fondamentales des systèmes distribués.

---

## 7. Conclusion

L'expérimentation réalisée sur le projet RMI a permis de dépasser un simple appel distant de type RPC. L'ajout du mécanisme de callback, la gestion des clients déconnectés et le test de concurrence ont permis d'observer plusieurs problématiques concrètes des systèmes distribués.

Le projet montre notamment que la simplicité d'utilisation de RMI repose sur un couplage relativement fort entre les applications participantes. La communication est facilitée, mais l'interopérabilité, la disponibilité et l'évolution des contrats doivent être prises en compte lors de la conception.

L'expérience confirme également que l'utilisation d'un middleware ne dispense pas le développeur de gérer les aspects liés à la concurrence, à la résilience et à la cohérence des données.
