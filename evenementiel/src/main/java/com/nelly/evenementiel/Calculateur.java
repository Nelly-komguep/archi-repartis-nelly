package com.nelly.evenementiel;

import com.nelly.evenementiel.evenements.OperationCalculee;
import com.nelly.evenementiel.evenements.OperationDemandee;

import java.time.Instant;
import java.util.Locale;

public final class Calculateur {

    private Calculateur() {
    }

    public static OperationCalculee calculer(OperationDemandee d) {
        String op = d.op() == null ? "" : d.op().trim().toUpperCase(Locale.ROOT);
        double a = d.a();
        double b = d.b();
        String maintenant = Instant.now().toString();

        double resultat;
        switch (op) {
            case "ADD" -> resultat = a + b;
            case "SUB" -> resultat = a - b;
            case "MUL" -> resultat = a * b;
            case "DIV" -> {
                if (b == 0) {
                    return new OperationCalculee(d.id(), op, a, b, null,
                            OperationCalculee.ERREUR, "Division par zéro impossible", maintenant);
                }
                resultat = a / b;
            }
            default -> {
                return new OperationCalculee(d.id(), op, a, b, null,
                        OperationCalculee.ERREUR, "Opération inconnue : " + d.op(), maintenant);
            }
        }
        return new OperationCalculee(d.id(), op, a, b, resultat, OperationCalculee.OK, null, maintenant);
    }
}