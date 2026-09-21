package client;

import server.CalculatorService;
import server.HistoryService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class Client {

    public static void main(String[] args) throws Exception {

        Registry registry =
                LocateRegistry.getRegistry("localhost", 1099);

        CalculatorService calc =
                (CalculatorService) registry.lookup("CalculatorService");

        HistoryService history =
                (HistoryService) registry.lookup("HistoryService");

        System.out.println("=== CLIENT RMI ===");

        System.out.println("4 + 7 = " + calc.add(4, 7));

        System.out.println("10 - 5 = " + calc.sub(10, 5));

        System.out.println("6 * 3 = " + calc.mul(6, 3));

        System.out.println("20 / 4 = " + calc.div(20, 4));

        try {
            System.out.println("10 / 0 = " + calc.div(10, 0));
        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Erreur division : " + e.getMessage()
            );
        }

        System.out.println();
        System.out.println("=== HISTORIQUE ===");

        List<String> entries = history.getAll();

        for (String entry : entries) {
            System.out.println(entry);
        }
    }
}