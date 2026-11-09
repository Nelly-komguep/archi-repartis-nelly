package com.nelly.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class BenchmarkGrpc {

    static final int CHAUFFE = 10;
    static final int MESURES = 100;

    public static void main(String[] args) throws Exception {
        // 1. Canal + stub bloquant
        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", ServeurGrpc.PORT)
                .usePlaintext()
                .build();
        ServiceCalculGrpc.ServiceCalculBlockingStub stub = ServiceCalculGrpc.newBlockingStub(canal);
        OperationRequest requete = OperationRequest.newBuilder().setA(3).setB(5).build();

        // 2. Chauffe : 10 appels dont on ignore le temps
        for (int i = 0; i < CHAUFFE; i++) {
            stub.additionner(requete);
        }

        // 3. Mesure : 100 appels chronométrés
        long[] durees = new long[MESURES];
        for (int i = 0; i < MESURES; i++) {
            long debut = System.nanoTime();
            OperationResponse rep = stub.additionner(requete);
            durees[i] = System.nanoTime() - debut;
            if (rep.getResultat() != 8.0) {
                throw new IllegalStateException("Résultat incorrect : " + rep.getResultat());
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

        System.out.println("=== gRPC : " + MESURES + " appels additionner(3, 5), " + CHAUFFE + " de chauffe ===");
        System.out.printf("Moyenne : %.3f ms%n", moyenne);
        System.out.printf("Médiane : %.3f ms%n", mediane);
        System.out.printf("Minimum : %.3f ms%n", min);
        System.out.printf("Maximum : %.3f ms%n", max);

        canal.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}