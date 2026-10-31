package com.nelly.evenementiel.evenements;

public record OperationCalculee(String id, String op, double a, double b, Double resultat,
                                String statut, String erreur, String horodatage) {

    public static final String OK = "OK";
    public static final String ERREUR = "ERREUR";

    public boolean estOk() {
        return OK.equals(statut);
    }
}