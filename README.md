# archi-repartis-nelly

## Module `/evenementiel` (séance 7, RabbitMQ)

```powershell
cd evenementiel
docker compose up -d       # courtier ; interface web : http://localhost:15672 (guest/guest)

# 1 terminal par processus lancer les consommateurs AVANT le producteur
mvn -q compile exec:java "-Dexec.mainClass=com.nelly.evenementiel.CalculConsommateur"
mvn -q compile exec:java "-Dexec.mainClass=com.nelly.evenementiel.AuditConsommateur"
mvn -q compile exec:java "-Dexec.mainClass=com.nelly.evenementiel.Producteur"   # saisir : ADD 4 7

mvn test                   # prérequis : courtier + CalculConsommateur lancés

```

Détails du module (flux, garanties de livraison, tests) : [`evenementiel/README.md`](evenementiel/README.md).

## Module `/grpc` (séance 8, gRPC)

Contrat : [`grpc/src/main/proto/calcul.proto`](grpc/src/main/proto/calcul.proto), avec un appel unaire `Additionner` et un streaming serveur `HistoriqueEnFlux`.

```powershell
cd grpc

# 1. Générer le code et compiler
#    protobuf-maven-plugin génère les classes dans target/generated-sources/protobuf
#    (OperationRequest, OperationResponse, ServiceCalculGrpc...) à chaque build.
mvn compile

# 2. Lancer le serveur (port 50051), terminal 1
mvn -q exec:java "-Dexec.mainClass=com.nelly.grpc.ServeurGrpc"

# 3. Lancer le client, terminal 2 : 3 additions (stub bloquant) puis l'historique en flux (stub asynchrone)
mvn -q exec:java "-Dexec.mainClass=com.nelly.grpc.ClientGrpc"

# 4. Benchmark (10 appels de chauffe + 100 mesurés)
mvn -q exec:java "-Dexec.mainClass=com.nelly.grpc.BenchmarkGrpc"
```

Sortie attendue du client : `additionner(3.0, 5.0) = 8.0`, `11.0`, `12.5`, puis les trois mêmes résultats préfixés par `[flux]` et « Fin de l'historique ».

## Mesures comparatives (séance 8)

Protocole, latences, tailles réseau (Wireshark), lignes de code et conclusion : [`rest/COMPARATIF.md`](rest/COMPARATIF.md), section 11.

| Benchmark | Lancement |
| --- | --- |
| RMI | `cd rmi` ; `java -cp target/classes server.Server` puis `java -cp target/classes client.BenchmarkRMIProtocole` |
| REST | `cd rest` ; `.\mvnw.cmd spring-boot:run` puis `java -cp target/classes com.rest.nelly.rest.benchmark.BenchmarkRest` |
| gRPC | voir ci-dessus |
| Événementiel | `cd evenementiel` ; `docker compose up -d`, `mvn -q compile exec:java "-Dexec.mainClass=com.nelly.evenementiel.CalculConsommateur"` (terminal 1), puis `mvn -q exec:java "-Dexec.mainClass=com.nelly.evenementiel.BenchmarkEvenementiel"` |
