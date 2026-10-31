package com.nelly.evenementiel;

import com.nelly.evenementiel.evenements.OperationCalculee;
import com.nelly.evenementiel.evenements.OperationDemandee;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public final class CalculConsommateur {

    public static final String FILE = "calcul.demandes.calculateur";

    public static void main(String[] args) throws Exception {
        Connection connexion = Topologie.fabrique().newConnection("calcul-consommateur");
        Channel canal = connexion.createChannel();

        Topologie.declarerEchanges(canal);
        canal.queueDeclare(FILE, true, false, false, null);    // file durable
        canal.queueBind(FILE, Topologie.ECHANGE_DEMANDES, "");  // liée à l'échange des demandes
        canal.basicQos(10);

        // autoAck = false : c'est NOUS qui accuserons réception, après le travail
        canal.basicConsume(FILE, false, (tag, livraison) -> traiter(canal, livraison), tag -> { });

        System.out.println("[calcul] en attente sur « " + FILE + " » (Ctrl+C pour arrêter)");
        new CountDownLatch(1).await();   // garde le processus en vie
    }

    private static void traiter(Channel canal, Delivery livraison) throws IOException {
        long etiquette = livraison.getEnvelope().getDeliveryTag();

        OperationDemandee demande;
        try {
            demande = Topologie.depuisJson(livraison.getBody(), OperationDemandee.class);
        } catch (IOException illisible) {
            canal.basicReject(etiquette, false);   // message illisible : inutile de le re-livrer
            return;
        }

        OperationCalculee resultat = Calculateur.calculer(demande);

        canal.basicPublish(Topologie.ECHANGE_RESULTATS, "",
                Topologie.proprietes(resultat.id(), "OperationCalculee"),
                Topologie.versJson(resultat));

        canal.basicAck(etiquette, false);   // ack APRÈS la publication

        System.out.println("[calcul] " + resultat.id() + " " + resultat.op() + "(" + resultat.a() + ", "
                + resultat.b() + ") -> " + (resultat.estOk() ? resultat.resultat() : "ERREUR : " + resultat.erreur()));
    }
}