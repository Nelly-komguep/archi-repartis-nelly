package com.nelly.evenementiel;

import com.nelly.evenementiel.evenements.OperationCalculee;
import com.nelly.evenementiel.evenements.OperationDemandee;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public final class Producteur implements AutoCloseable {

    private final Connection connexion;
    private final Channel canal;

    public Producteur(ConnectionFactory fabrique) throws IOException, TimeoutException {
        this.connexion = fabrique.newConnection("producteur");
        this.canal = connexion.createChannel();
        Topologie.declarerEchanges(canal);
        canal.confirmSelect();
    }

    /** Publie la demande et renvoie SEULEMENT son identifiant, jamais le résultat. */
    public synchronized String publier(String op, double a, double b)
            throws IOException, InterruptedException, TimeoutException {
        OperationDemandee demande = new OperationDemandee(UUID.randomUUID().toString(), op, a, b,
                Instant.now().toString());
        canal.basicPublish(Topologie.ECHANGE_DEMANDES, "",
                Topologie.proprietes(demande.id(), "OperationDemandee"),
                Topologie.versJson(demande));
        canal.waitForConfirmsOrDie(5_000);
        return demande.id();
    }

    @Override
    public void close() throws IOException {
        if (connexion.isOpen()) {
            connexion.close();
        }
    }

    /** Client interactif : saisir par exemple « ADD 4 7 », ou « quitter ». */
    public static void main(String[] args) throws Exception {
        ConnectionFactory fabrique = Topologie.fabrique();
        try (Producteur producteur = new Producteur(fabrique);
             CorrelateurResultats correlateur = new CorrelateurResultats(fabrique)) {

            BufferedReader clavier = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            System.out.println("Format : OPERATION A B   (ex. ADD 4 7)  —  « quitter » pour sortir");

            while (true) {
                System.out.print("> ");
                String ligne = clavier.readLine();
                if (ligne == null || ligne.isBlank() || ligne.trim().equalsIgnoreCase("quitter")) {
                    break;
                }
                String[] p = ligne.trim().split("\\s+");
                if (p.length != 3) {
                    System.out.println("Format invalide. Exemple : ADD 4 7");
                    continue;
                }
                double a, b;
                try {
                    a = Double.parseDouble(p[1]);
                    b = Double.parseDouble(p[2]);
                } catch (NumberFormatException e) {
                    System.out.println("A et B doivent être des nombres.");
                    continue;
                }

                String id = producteur.publier(p[0], a, b);
                System.out.println("Publié : id=" + id + " (aucun résultat à ce stade)");

                OperationCalculee r = correlateur.attendre(id, Duration.ofSeconds(5));
                if (r == null) {
                    System.out.println("Aucun résultat en 5 s — le consommateur de calcul est-il lancé ?");
                } else if (r.estOk()) {
                    System.out.println("Résultat corrélé : " + r.op() + "(" + r.a() + ", " + r.b() + ") = " + r.resultat());
                } else {
                    System.out.println("Erreur corrélée : " + r.erreur());
                }
            }
        }
    }
}