package client;

import server.CalculatorService;
import server.HistoryService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws Exception {

        Registry registry =
                LocateRegistry.getRegistry("localhost", 1099);

        CalculatorService calc =
                (CalculatorService) registry.lookup("CalculatorService");

        HistoryService history =
                (HistoryService) registry.lookup("HistoryService");

        // Création du listener distant
        ClientNotificationListener listener =
                new ClientNotificationListener();

        // Abonnement aux notifications
        history.subscribe(listener);

        System.out.println("=== CLIENT RMI ===");
        System.out.println("Client abonné aux notifications.");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        boolean running = true;

        while (running) {

            System.out.println();
            System.out.println("===== MENU =====");
            System.out.println("1. Addition");
            System.out.println("2. Soustraction");
            System.out.println("3. Multiplication");
            System.out.println("4. Division");
            System.out.println("5. Afficher l'historique");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");

            String choix = scanner.nextLine();

            try {

                switch (choix) {

                    case "1":
                        System.out.print("Premier nombre : ");
                        double a1 = Double.parseDouble(
                                scanner.nextLine()
                        );

                        System.out.print("Deuxième nombre : ");
                        double b1 = Double.parseDouble(
                                scanner.nextLine()
                        );

                        System.out.println(
                                "Résultat = " + calc.add(a1, b1)
                        );
                        break;

                    case "2":
                        System.out.print("Premier nombre : ");
                        double a2 = Double.parseDouble(
                                scanner.nextLine()
                        );

                        System.out.print("Deuxième nombre : ");
                        double b2 = Double.parseDouble(
                                scanner.nextLine()
                        );

                        System.out.println(
                                "Résultat = " + calc.sub(a2, b2)
                        );
                        break;

                    case "3":
                        System.out.print("Premier nombre : ");
                        double a3 = Double.parseDouble(
                                scanner.nextLine()
                        );

                        System.out.print("Deuxième nombre : ");
                        double b3 = Double.parseDouble(
                                scanner.nextLine()
                        );

                        System.out.println(
                                "Résultat = " + calc.mul(a3, b3)
                        );
                        break;

                    case "4":
                        System.out.print("Premier nombre : ");
                        double a4 = Double.parseDouble(
                                scanner.nextLine()
                        );

                        System.out.print("Deuxième nombre : ");
                        double b4 = Double.parseDouble(
                                scanner.nextLine()
                        );

                        System.out.println(
                                "Résultat = " + calc.div(a4, b4)
                        );
                        break;

                    case "5":
                        System.out.println();
                        System.out.println("=== HISTORIQUE ===");

                        List<String> entries =
                                history.getAll();

                        for (String entry : entries) {
                            System.out.println(entry);
                        }
                        break;

                    case "0":
                        running = false;
                        System.out.println(
                                "Déconnexion du client."
                        );
                        break;

                    default:
                        System.out.println(
                                "Choix invalide."
                        );
                }

            } catch (NumberFormatException e) {

                System.out.println(
                        "Erreur : veuillez entrer un nombre valide."
                );

            } catch (IllegalArgumentException e) {

                System.out.println(
                        "Erreur : " + e.getMessage()
                );
            }
        }

        scanner.close();
    }
}