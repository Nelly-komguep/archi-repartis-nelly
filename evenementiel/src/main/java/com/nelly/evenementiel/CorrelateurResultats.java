package com.nelly.evenementiel;

import com.nelly.evenementiel.evenements.OperationCalculee;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Retrouve le résultat d'une demande grâce à son identifiant d'opération. */
public final class CorrelateurResultats implements AutoCloseable {

    private final Connection connexion;
    private final Map<String, CompletableFuture<OperationCalculee>> enAttente = new ConcurrentHashMap<>();

    public CorrelateurResultats(ConnectionFactory fabrique) throws IOException, TimeoutException {
        this.connexion = fabrique.newConnection("correlateur-resultats");
        Channel canal = connexion.createChannel();
        Topologie.declarerEchanges(canal);
        String file = canal.queueDeclare().getQueue();   // file temporaire : exclusive, nom généré
        canal.queueBind(file, Topologie.ECHANGE_RESULTATS, "");
        canal.basicConsume(file, true, (tag, livraison) -> {
            OperationCalculee r = Topologie.depuisJson(livraison.getBody(), OperationCalculee.class);
            futur(r.id()).complete(r);   // range le résultat sous son id
        }, tag -> { });
    }

    /** Attend le résultat de l'opération id, au plus `delai`. Renvoie null si rien n'arrive. */
    public OperationCalculee attendre(String id, Duration delai) throws InterruptedException {
        try {
            return futur(id).get(delai.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return null;
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } finally {
            enAttente.remove(id);
        }
    }

    private CompletableFuture<OperationCalculee> futur(String id) {
        return enAttente.computeIfAbsent(id, k -> new CompletableFuture<>());
    }

    @Override
    public void close() throws IOException {
        if (connexion.isOpen()) {
            connexion.close();
        }
    }
}