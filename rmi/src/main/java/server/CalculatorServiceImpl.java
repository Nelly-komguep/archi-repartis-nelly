package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class CalculatorServiceImpl
        extends UnicastRemoteObject
        implements CalculatorService {

    private HistoryService history;

    public CalculatorServiceImpl() throws RemoteException {
        super();
    }

    public void setHistoryService(HistoryService history) {
        this.history = history;
    }

    private void recordHistory(String entry) {

        if (history == null) {
            return;
        }

        try {
            history.record(entry);
        } catch (RemoteException e) {
            System.out.println(
                    "Impossible d'enregistrer l'historique : "
                            + e.getMessage()
            );
        }
    }

    @Override
    public double add(double a, double b) throws RemoteException {

        double result = a + b;

        recordHistory(
                "ADD(" + a + ", " + b + ") = " + result
        );

        return result;
    }

    @Override
    public double sub(double a, double b) throws RemoteException {

        double result = a - b;

        recordHistory(
                "SUB(" + a + ", " + b + ") = " + result
        );

        return result;
    }

    @Override
    public double mul(double a, double b) throws RemoteException {

        double result = a * b;

        recordHistory(
                "MUL(" + a + ", " + b + ") = " + result
        );

        return result;
    }

    @Override
    public double div(double a, double b) throws RemoteException {

        if (b == 0) {
            throw new IllegalArgumentException(
                    "Division par zéro"
            );
        }

        double result = a / b;

        recordHistory(
                "DIV(" + a + ", " + b + ") = " + result
        );

        return result;
    }
}