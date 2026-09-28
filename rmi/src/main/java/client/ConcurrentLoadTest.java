package client;

import server.CalculatorService;
import server.HistoryService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ConcurrentLoadTest {

    private static final int NB_CLIENTS = 5;
    private static final int OPERATIONS_PAR_CLIENT = 20;

    public static void main(String[] args) throws Exception {

        Registry registry = LocateRegistry.getRegistry("localhost", 1099);

        CalculatorService calc = (CalculatorService) registry.lookup(
                "CalculatorService");

        HistoryService history = (HistoryService) registry.lookup(
                "HistoryService");

        // Taille de l'historique avant le test
        int historiqueAvant = history.getAll().size();

        System.out.println(
                "=== TEST DE CONCURRENCE RMI ===");

        System.out.println(
                "Clients concurrents : " + NB_CLIENTS);

        System.out.println(
                "Opérations par client : "
                        + OPERATIONS_PAR_CLIENT);

        System.out.println(
                "Opérations attendues : "
                        + (NB_CLIENTS
                                * OPERATIONS_PAR_CLIENT));

        System.out.println(
                "Historique avant test : "
                        + historiqueAvant);

        CountDownLatch startSignal = new CountDownLatch(1);

        CountDownLatch doneSignal = new CountDownLatch(NB_CLIENTS);

        for (int clientId = 1; clientId <= NB_CLIENTS; clientId++) {

            final int id = clientId;

            Thread clientThread = new Thread(() -> {

                try {

                    // Tous les clients commencent
                    // presque simultanément.
                    startSignal.await();

                    for (int i = 1; i <= OPERATIONS_PAR_CLIENT; i++) {

                        double a = id;
                        double b = i;

                        double result = calc.add(a, b);

                        System.out.println(
                                "Client "
                                        + id
                                        + " | opération "
                                        + i
                                        + " | "
                                        + a
                                        + " + "
                                        + b
                                        + " = "
                                        + result);
                    }

                } catch (Exception e) {

                    System.err.println(
                            "Erreur Client "
                                    + id
                                    + " : "
                                    + e.getMessage());

                } finally {

                    doneSignal.countDown();
                }

            });

            clientThread.start();
        }

        System.out.println();
        System.out.println(
                "Tous les clients sont prêts...");

        // Lancement simultané
        startSignal.countDown();

        // Attendre la fin des 5 clients
        doneSignal.await();

        System.out.println();
        System.out.println(
                "Tous les clients ont terminé.");

        // Vérification finale
        List<String> historiqueApres = history.getAll();

        int historiqueApresTaille = historiqueApres.size();

        int nouvellesOperations = historiqueApresTaille
                - historiqueAvant;

        int operationsAttendues = NB_CLIENTS
                * OPERATIONS_PAR_CLIENT;

        System.out.println();
        System.out.println(
                "=== VERIFICATION ===");

        System.out.println(
                "Historique avant : "
                        + historiqueAvant);

        System.out.println(
                "Historique après : "
                        + historiqueApresTaille);

        System.out.println(
                "Nouvelles opérations : "
                        + nouvellesOperations);

        System.out.println(
                "Opérations attendues : "
                        + operationsAttendues);

        if (nouvellesOperations == operationsAttendues) {

            System.out.println();
            System.out.println(
                    "SUCCÈS : aucune opération perdue.");

        } else {

            System.out.println();
            System.out.println(
                    "ÉCHEC : incohérence détectée.");
        }
    }
}