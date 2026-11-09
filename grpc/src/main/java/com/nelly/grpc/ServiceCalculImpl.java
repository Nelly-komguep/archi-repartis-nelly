package com.nelly.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServiceCalculImpl extends ServiceCalculGrpc.ServiceCalculImplBase {

    private final List<OperationResponse> historique = new CopyOnWriteArrayList<>();

    @Override
    public void additionner(OperationRequest req, StreamObserver<OperationResponse> responseObserver) {
        try {
            OperationResponse rep = OperationResponse.newBuilder()
                    .setResultat(req.getA() + req.getB())
                    .setIdOperation(UUID.randomUUID().toString())
                    .build();
            historique.add(rep);
            responseObserver.onNext(rep);
            responseObserver.onCompleted(); // sans onCompleted(), le client reste bloqué
        } catch (RuntimeException e) {
            // une exception non gérée laisserait le flux ouvert : on le ferme avec une erreur
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void historiqueEnFlux(HistoriqueRequest req, StreamObserver<OperationResponse> responseObserver) {
        for (OperationResponse op : historique) {
            responseObserver.onNext(op); // un élément envoyé à la fois
        }
        responseObserver.onCompleted();
    }
}
