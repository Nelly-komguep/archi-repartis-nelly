package com.nelly.evenementiel;

import com.nelly.evenementiel.evenements.OperationCalculee;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/** Second consommateur, indépendant : journal d'audit de chaque OperationCalculee. */
public final class AuditConsommateur {

    public static final String FILE = "calcul.resultats.audit";
    private static final String ENTETE = "horodatage_audit;id;op;a;b;statut;resultat_ou_erreur";

    private static final Set<String> dejaJournalises = ConcurrentHashMap.newKeySet();
    private static Path journal;

    public static void main(String[] args) throws Exception {
        journal = Path.of(args.length > 0 ? args[0] : "audit.csv");
        chargerIdentifiantsExistants();

        Connection connexion = Topologie.fabrique().newConnection("audit-consommateur");
        Channel canal = connexion.createChannel();
        Topologie.declarerEchanges(canal);
        canal.queueDeclare(FILE, true, false, false, null);
        canal.queueBind(FILE, Topologie.ECHANGE_RESULTATS, "");   // sa PROPRE file sur l'échange des résultats
        canal.basicQos(1);

        canal.basicConsume(FILE, false, (tag, livraison) -> traiter(canal, livraison), tag -> { });
        System.out.println("[audit] journalisation dans " + journal.toAbsolutePath());
        new CountDownLatch(1).await();
    }

    private static void traiter(Channel canal, Delivery livraison) throws IOException {
        long etiquette = livraison.getEnvelope().getDeliveryTag();
        OperationCalculee r;
        try {
            r = Topologie.depuisJson(livraison.getBody(), OperationCalculee.class);
        } catch (IOException illisible) {
            canal.basicReject(etiquette, false);
            return;
        }

        if (dejaJournalises.add(r.id())) {            // add() renvoie false si l'id est déjà connu
            String ligne = String.join(";", Instant.now().toString(), r.id(), r.op(),
                    String.valueOf(r.a()), String.valueOf(r.b()), r.statut(),
                    r.estOk() ? String.valueOf(r.resultat()) : r.erreur());
            Files.writeString(journal, ligne + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("[audit] " + ligne);
        } else {
            System.out.println("[audit] doublon ignoré : " + r.id());
        }
        canal.basicAck(etiquette, false);   // ack APRÈS l'écriture
    }

    /** Relit le journal au démarrage : un id déjà écrit ne sera jamais réécrit. */
    private static void chargerIdentifiantsExistants() throws IOException {
        if (!Files.exists(journal)) {
            Files.writeString(journal, ENTETE + System.lineSeparator(), StandardCharsets.UTF_8);
            return;
        }
        for (String ligne : Files.readAllLines(journal, StandardCharsets.UTF_8)) {
            String[] champs = ligne.split(";");
            if (champs.length > 1 && !ligne.equals(ENTETE)) {
                dejaJournalises.add(champs[1]);
            }
        }
    }
}