package client;

import server.CalculatorService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class BenchmarkRMIProtocole {

    static final int CHAUFFE = 10;
    static final int MESURES = 100;

    public static void main(String[] args) throws Exception {
        // 1. Récupération du stub dans le registre
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        CalculatorService calculator = (CalculatorService) registry.lookup("CalculatorService");

        // 2. Chauffe
        for (int i = 0; i < CHAUFFE; i++) {
            calculator.add(3, 5);
        }

        // 3. Mesure
        long[] durees = new long[MESURES];
        for (int i = 0; i < MESURES; i++) {
            long debut = System.nanoTime();
            double resultat = calculator.add(3, 5);
            durees[i] = System.nanoTime() - debut;
            if (resultat != 8.0) {
                throw new IllegalStateException("Résultat incorrect : " + resultat);
            }
        }

        // 4. Statistiques en millisecondes
        Arrays.sort(durees);
        double total = 0;
        for (long d : durees) {
            total += d;
        }
        double moyenne = total / MESURES / 1_000_000.0;
        double min = durees[0] / 1_000_000.0;
        double max = durees[MESURES - 1] / 1_000_000.0;
        double mediane = (durees[MESURES / 2 - 1] + durees[MESURES / 2]) / 2.0 / 1_000_000.0;

        System.out.println("=== RMI : " + MESURES + " appels add(3, 5), " + CHAUFFE + " de chauffe ===");
        System.out.printf("Moyenne : %.3f ms%n", moyenne);
        System.out.printf("Médiane : %.3f ms%n", mediane);
        System.out.printf("Minimum : %.3f ms%n", min);
        System.out.printf("Maximum : %.3f ms%n", max);
    }
}