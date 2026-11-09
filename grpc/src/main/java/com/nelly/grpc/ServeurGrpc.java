package com.nelly.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServeurGrpc {

    public static final int PORT = 50051;

    public static void main(String[] args) throws Exception {
        Server serveur = ServerBuilder.forPort(PORT)
                .addService(new ServiceCalculImpl())
                .build()
                .start();
        System.out.println("Serveur gRPC démarré sur le port " + PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(serveur::shutdown));
        serveur.awaitTermination();
    }
}
