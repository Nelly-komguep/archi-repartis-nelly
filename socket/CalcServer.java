import java.io.*;
import java.net.*;

public class CalcServer {

    public static void main(String[] args) throws IOException {

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Serveur calculatrice en ecoute sur le port " + port);

            while (true) {

                try (Socket client = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(client.getInputStream()));
                     PrintWriter writer = new PrintWriter(
                             client.getOutputStream(), true)) {

                    System.out.println("Client connecte.");

                    String request = reader.readLine();

                    if (request == null) {
                        System.out.println("Le client s'est deconnecte.");
                        continue;
                    }

                    System.out.println("Requete recue : " + request);

                    String response = calculate(request);

                    writer.println(response);

                    System.out.println("Reponse envoyee : " + response);

                } catch (IOException e) {
                    System.out.println(
                            "Erreur de communication avec le client : "
                                    + e.getMessage()
                    );
                }
            }
        }
    }

    private static String calculate(String request) {

        String[] parts = request.split(";");

        // Vérification du nombre de champs
        if (parts.length != 3) {
            return "ERROR;Format invalide. Utilisez OPERATION;A;B";
        }

        String operation = parts[0].trim().toUpperCase();

        double a;
        double b;

        // Conversion des nombres
        try {
            a = Double.parseDouble(parts[1].trim());
            b = Double.parseDouble(parts[2].trim());
        } catch (NumberFormatException e) {
            return "ERROR;A et B doivent etre des nombres";
        }

        // Exécution de l'opération
        switch (operation) {

            case "ADD":
                return "RESULT;" + (a + b);

            case "SUB":
                return "RESULT;" + (a - b);

            case "MUL":
                return "RESULT;" + (a * b);

            case "DIV":

                if (b == 0) {
                    return "ERROR;Division par zero impossible";
                }

                return "RESULT;" + (a / b);

            default:
                return "ERROR;Operation inconnue : " + operation;
        }
    }
}