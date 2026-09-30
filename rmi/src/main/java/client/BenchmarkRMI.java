package client;

import server.CalculatorService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class BenchmarkRMI {

    public static void main(String[] args) throws Exception {

        Registry registry =
                LocateRegistry.getRegistry("localhost", 1099);

        CalculatorService calculator =
                (CalculatorService)
                        registry.lookup("CalculatorService");

        int numberOfCalls = 100;

        long totalNanos = 0;
        long minimumNanos = Long.MAX_VALUE;
        long maximumNanos = Long.MIN_VALUE;

        System.out.println("Démarrage du benchmark RMI...");
        System.out.println("100 appels ADD(4, 7)");
        System.out.println();

        // Appel de chauffe pour initialiser la connexion/stub
        calculator.add(4, 7);

        for (int i = 1; i <= numberOfCalls; i++) {

            long start = System.nanoTime();

            double result = calculator.add(4, 7);

            long end = System.nanoTime();

            long duration = end - start;

            totalNanos += duration;

            if (duration < minimumNanos) {
                minimumNanos = duration;
            }

            if (duration > maximumNanos) {
                maximumNanos = duration;
            }

            if (result != 11) {
                throw new RuntimeException(
                        "Résultat incorrect : " + result
                );
            }

            System.out.println(
                    "Appel " + i + " / " + numberOfCalls
            );
        }

        double totalMs = totalNanos / 1_000_000.0;
        double averageMs =
                totalMs / numberOfCalls;
        double minimumMs =
                minimumNanos / 1_000_000.0;
        double maximumMs =
                maximumNanos / 1_000_000.0;

        System.out.println();
        System.out.println("================================");
        System.out.println("       MESURE RMI - 100 APPELS");
        System.out.println("================================");
        System.out.println(
                "Nombre d'appels : " + numberOfCalls
        );
        System.out.println(
                "Temps total     : " + totalMs + " ms"
        );
        System.out.println(
                "Temps moyen     : " + averageMs + " ms"
        );
        System.out.println(
                "Temps minimum   : " + minimumMs + " ms"
        );
        System.out.println(
                "Temps maximum   : " + maximumMs + " ms"
        );
        System.out.println("================================");
    }
}
