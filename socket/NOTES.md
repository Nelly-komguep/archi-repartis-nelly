# NOTES  Séance 1

## Architecture logicielle et objets répartis

---

## B.1  Service d'écho TCP

### Objectif

Le serveur EchoServer écoute sur le port 5000. Il accepte une connexion TCP, lit une ligne envoyée par le client et renvoie exactement le même message.

### Test réalisé

Message envoyé par le client :

```text
nelly
```

Réponse obtenue :

```text
nelly
```

### Observation côté serveur

Le serveur a affiché successivement :

```text
Client connecté : ip address
Message reçu : nelly
Client déconnecté.
```

Le service d'écho fonctionne correctement.

### Test avec plusieurs clients

Le serveur mono-thread traite les connexions successivement. Lorsqu'un client est en cours de traitement, une nouvelle connexion peut être acceptée par le système mais son traitement par l'application dépend du retour du serveur à la boucle `accept()`.

Cette observation motive la mise en place d'un thread par connexion dans la partie B.3.

---

# B.2 Mini-calculatrice distante

Le protocole utilisé est :

```text
OPERATION;A;B
```

Les opérations prises en charge sont :

* `ADD`
* `SUB`
* `MUL`
* `DIV`

## Tests réalisés

### Addition

Requête :

```text
ADD;4;7
```

Réponse :

```text
RESULT;11.0
```

### Soustraction

Requête :

```text
SUB;10;5
```

Réponse :

```text
RESULT;5.0
```

### Nombres négatifs

Requête :

```text
ADD;-5;2
```

Réponse :

```text
RESULT;-3.0
```

### Multiplication avec nombre décimal et négatif

Requête :

```text
MUL;-2.5;4
```

Réponse :

```text
RESULT;-10.0
```

### Division par zéro

Requête :

```text
DIV;10;0
```

Réponse :

```text
ERROR;Division par zero impossible
```

Le serveur ne s'est pas arrêté et a continué à accepter les connexions suivantes.

### Message mal formé

Requête :

```text
ADD;5
```

Réponse :

```text
ERROR;Format invalide. Utilisez OPERATION;A;B
```

Le serveur a correctement détecté l'absence d'un champ.

---

# B.3 Serveur multi-client

Une troisième version du serveur a été créée :

```text
CalcServer3.java
```

Cette version utilise un thread dédié pour chaque connexion cliente.

## Test avec plusieurs clients

Deux clients ont notamment été connectés simultanément.

Le serveur a affiché :

```text
Nouveau client connecté : ip address
Nouveau client connecté : ip address
```

Les requêtes ont ensuite été traitées par les threads correspondants :

```text
Requête reçue de ip address : DIV;100;4
Réponse envoyée à ip address : RESULT;25.0
Connexion terminée : ip address

Requête reçue de ip address : MUL;5;6
Réponse envoyée à ip address : RESULT;30.0
Connexion terminée : ip address
```

### Observation

Le fait que deux messages `Nouveau client connecté` apparaissent avant le traitement des deux requêtes montre que le serveur peut accepter plusieurs connexions et leur attribuer des traitements indépendants.

L'ordre d'affichage des requêtes n'est pas nécessairement identique à l'ordre dans lequel les clients ont été lancés, car les traitements sont réalisés par des threads concurrents.

---

# Question de réflexion 1 Client connecté sans envoyer de données

Lorsqu'un client est connecté mais n'envoie aucune donnée, le thread associé au client reste bloqué sur :

```java
reader.readLine();
```

Le serveur ne reçoit aucune requête tant que le client n'envoie pas de ligne ou ne ferme pas la connexion.

Dans la version multi-client, ce blocage concerne uniquement le thread associé à ce client. Le serveur principal continue à accepter de nouvelles connexions.

---

# Question de réflexion 2 Déconnexion brutale du client

Lorsqu'un client est interrompu brutalement alors que le serveur attend des données, le serveur a effectivement affiché :

```text
Erreur avec le client ip address : Connection reset
Connexion terminée : ip address
```

### Observation

L'exception `Connection reset` indique que la connexion TCP a été réinitialisée brutalement par le client ou par la pile réseau.

Dans notre implémentation, cette exception est interceptée par :

```java
catch (IOException e)
```

Le serveur affiche l'erreur, termine le traitement du client concerné et continue son fonctionnement.

Le serveur n'est donc pas arrêté par la déconnexion brutale d'un client.

---

# Conclusion de la séance 1

Cette première séance a permis de mettre en œuvre manuellement les principaux mécanismes d'une communication distribuée par sockets TCP :

* établissement d'une connexion ;
* lecture d'un flux réseau ;
* écriture d'une réponse ;
* fermeture des ressources ;
* définition d'un protocole applicatif simple ;
* validation des données reçues ;
* gestion des erreurs ;
* traitement concurrent de plusieurs clients ;
* observation des effets d'une déconnexion réseau brutale.

Ces mécanismes seront comparés lors des prochaines séances avec les abstractions proposées par RMI, REST, les architectures événementielles et gRPC.
