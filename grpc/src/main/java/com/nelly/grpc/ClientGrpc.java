package com.nelly.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientGrpc {

    public static void main(String[] args) throws Exception {
        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", ServeurGrpc.PORT)
                .usePlaintext() // pas de TLS en local
                .build();
        try {
            // 1. Appel unaire avec le stub bloquant
            ServiceCalculGrpc.ServiceCalculBlockingStub bloquant = ServiceCalculGrpc.newBlockingStub(canal);
            for (double[] ab : new double[][] {{3, 5}, {4, 7}, {10, 2.5}}) {
                OperationResponse rep = bloquant.additionner(
                        OperationRequest.newBuilder().setA(ab[0]).setB(ab[1]).build());
                System.out.println("additionner(" + ab[0] + ", " + ab[1] + ") = "
                        + rep.getResultat() + "  id=" + rep.getIdOperation());
            }

            // 2. Bonus : streaming serveur avec le stub asynchrone
            ServiceCalculGrpc.ServiceCalculStub asynchrone = ServiceCalculGrpc.newStub(canal);
            CountDownLatch fin = new CountDownLatch(1);
            asynchrone.historiqueEnFlux(HistoriqueRequest.getDefaultInstance(), new StreamObserver<>() {
                @Override
                public void onNext(OperationResponse op) {
                    System.out.println("  [flux] " + op.getResultat() + "  id=" + op.getIdOperation());
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Erreur du flux : " + t);
                    fin.countDown();
                }

                @Override
                public void onCompleted() {
                    System.out.println("Fin de l'historique");
                    fin.countDown();
                }
            });
            fin.await(5, TimeUnit.SECONDS); // le main attend la fin du flux asynchrone
        } finally {
            canal.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
