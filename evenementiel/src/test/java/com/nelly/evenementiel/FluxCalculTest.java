package com.nelly.evenementiel;

import com.nelly.evenementiel.evenements.OperationCalculee;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Prérequis : courtier démarré ET CalculConsommateur lancé dans un terminal. */
class FluxCalculTest {

    private static final Duration DELAI_MAX = Duration.ofSeconds(5);

    private static Producteur producteur;
    private static Connection connexion;
    private static final Map<String, OperationCalculee> recus = new ConcurrentHashMap<>();

    @BeforeAll
    static void demarrer() throws Exception {
        ConnectionFactory fabrique = Topologie.fabrique();
        connexion = fabrique.newConnection("test");
        Channel canal = connexion.createChannel();
        Topologie.declarerEchanges(canal);
        String file = canal.queueDeclare().getQueue();
        canal.queueBind(file, Topologie.ECHANGE_RESULTATS, "");
        canal.basicConsume(file, true, (tag, livraison) -> {
            OperationCalculee r = Topologie.depuisJson(livraison.getBody(), OperationCalculee.class);
            recus.put(r.id(), r);
        }, tag -> { });
        producteur = new Producteur(fabrique);
    }

    @AfterAll
    static void arreter() throws Exception {
        producteur.close();
        connexion.close();
    }

    @Test
    void une_addition_publiee_produit_un_resultat_correle() throws Exception {
        String id = producteur.publier("ADD", 4, 4);

        // Attente BORNÉE : au plus 5 s, sinon le test échoue clairement (pas de boucle infinie)
        await().atMost(DELAI_MAX).until(() -> recus.containsKey(id));

        assertEquals(8.0, recus.get(id).resultat());
        assertEquals(OperationCalculee.OK, recus.get(id).statut());
    }

    @Test
    void une_division_par_zero_produit_un_evenement_d_erreur() throws Exception {
        String id = producteur.publier("DIV", 10, 0);

        await().atMost(DELAI_MAX).until(() -> recus.containsKey(id));

        assertEquals(OperationCalculee.ERREUR, recus.get(id).statut());
        assertNull(recus.get(id).resultat());
    }
}