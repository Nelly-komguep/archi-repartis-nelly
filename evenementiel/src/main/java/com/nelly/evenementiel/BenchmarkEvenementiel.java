package com.nelly.evenementiel;

import com.nelly.evenementiel.evenements.OperationCalculee;
import com.rabbitmq.client.ConnectionFactory;

import java.time.Duration;
import java.util.Arrays;

public class BenchmarkEvenementiel {

    static final int CHAUFFE = 10;
    static final int MESURES = 100;

    public static void main(String[] args) throws Exception {
        // 1. Producteur + corrélateur ouverts une seule fois
        ConnectionFactory fabrique = Topologie.fabrique();
        try (Producteur producteur = new Producteur(fabrique);
             CorrelateurResultats correlateur = new CorrelateurResultats(fabrique)) {

            // 2. Chauffe
            for (int i = 0; i < CHAUFFE; i++) {
                String id = producteur.publier("ADD", 3, 5);
                correlateur.attendre(id, Duration.ofSeconds(5));
            }

            // 3. Mesure : de la publication à la réception du résultat corrélé
            long[] durees = new long[MESURES];
            for (int i = 0; i < MESURES; i++) {
                long debut = System.nanoTime();
                String id = producteur.publier("ADD", 3, 5);
                OperationCalculee r = correlateur.attendre(id, Duration.ofSeconds(5));
                durees[i] = System.nanoTime() - debut;
                if (r == null || !r.estOk() || r.resultat() != 8.0) {
                    throw new IllegalStateException("Résultat incorrect ou absent : " + r);
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

            System.out.println("=== Événementiel : " + MESURES + " ADD(3, 5) publication -> résultat, " + CHAUFFE + " de chauffe ===");
            System.out.printf("Moyenne : %.3f ms%n", moyenne);
            System.out.printf("Médiane : %.3f ms%n", mediane);
            System.out.printf("Minimum : %.3f ms%n", min);
            System.out.printf("Maximum : %.3f ms%n", max);
        }
    }
}