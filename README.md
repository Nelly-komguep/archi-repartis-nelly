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

Détails du module (flux, garanties de livraison, tests) : [`evenementiel/README.md`](evenementiel/README.md).
```
