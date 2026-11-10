package com.rest.nelly.rest.benchmark;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class BenchmarkRest {

    static final int CHAUFFE = 10;
    static final int MESURES = 100;

    public static void main(String[] args) throws Exception {
        // 1. Un seul client HTTP (connexion réutilisée) et une seule requête
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest requete = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/operations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"type\":\"ADD\",\"a\":3,\"b\":5}"))
                .build();

        // 2. Chauffe
        for (int i = 0; i < CHAUFFE; i++) {
            client.send(requete, HttpResponse.BodyHandlers.ofString());
        }

        // 3. Mesure
        long[] durees = new long[MESURES];
        for (int i = 0; i < MESURES; i++) {
            long debut = System.nanoTime();
            HttpResponse<String> rep = client.send(requete, HttpResponse.BodyHandlers.ofString());
            durees[i] = System.nanoTime() - debut;
            if (rep.statusCode() != 201 || !rep.body().contains("\"result\":8.0")) {
                throw new IllegalStateException("Réponse incorrecte : " + rep.statusCode() + " " + rep.body());
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

        System.out.println("=== REST : " + MESURES + " appels POST /operations ADD(3, 5), " + CHAUFFE + " de chauffe ===");
        System.out.printf("Moyenne : %.3f ms%n", moyenne);
        System.out.printf("Médiane : %.3f ms%n", mediane);
        System.out.printf("Minimum : %.3f ms%n", min);
        System.out.printf("Maximum : %.3f ms%n", max);
    }
}