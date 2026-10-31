package com.nelly.evenementiel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

public final class Topologie {

    public static final String ECHANGE_DEMANDES = "calcul.demandes";
    public static final String ECHANGE_RESULTATS = "calcul.resultats";

    public static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Topologie() {
    }

    public static ConnectionFactory fabrique() {
        ConnectionFactory f = new ConnectionFactory();
        f.setHost("localhost");
        f.setPort(5672);
        f.setUsername("guest");
        f.setPassword("guest");
        return f;
    }

    /** Déclare les deux échanges. Idempotent : sans effet s'ils existent déjà. */
    public static void declarerEchanges(Channel canal) throws IOException {
        canal.exchangeDeclare(ECHANGE_DEMANDES, BuiltinExchangeType.FANOUT, true);
        canal.exchangeDeclare(ECHANGE_RESULTATS, BuiltinExchangeType.FANOUT, true);
    }

    /** Message persistant, en JSON, portant l'id d'opération comme identifiant de corrélation. */
    public static AMQP.BasicProperties proprietes(String idOperation, String type) {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2)
                .messageId(idOperation)
                .correlationId(idOperation)
                .type(type)
                .build();
    }

    public static byte[] versJson(Object evenement) throws IOException {
        return JSON.writeValueAsBytes(evenement);
    }

    public static <T> T depuisJson(byte[] corps, Class<T> type) throws IOException {
        return JSON.readValue(corps, type);
    }
}