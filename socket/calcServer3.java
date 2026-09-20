import java.io.*;
import java.net.*;

public class CalcServer3 {

    public static void main(String[] args) throws IOException {

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println(
                    "Serveur calculatrice multi-client en ecoute sur le port "
                            + port
            );

            while (true) {

                Socket client = serverSocket.accept();

                System.out.println(
                        "Nouveau client connecte : "
                                + client.getInetAddress().getHostAddress()
                );

                Thread clientThread = new Thread(
                        new ClientHandler(client)
                );

                clientThread.start();
            }
        }
    }

    private static class ClientHandler implements Runnable {

        private final Socket client;

        public ClientHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {

            String clientAddress =
                    client.getInetAddress().getHostAddress();

            try (
                Socket socket = client;

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()
                        )
                );

                PrintWriter writer = new PrintWriter(
                        socket.getOutputStream(),
                        true
                )
            ) {

                String request = reader.readLine();

                if (request == null) {
                    System.out.println(
                            "Client deconnecte sans envoyer de requete : "
                                    + clientAddress
                    );
                    return;
                }

                System.out.println(
                        "Requete recue de "
                                + clientAddress
                                + " : "
                                + request
                );

                String response = calculate(request);

                writer.println(response);

                System.out.println(
                        "Reponse envoyee à "
                                + clientAddress
                                + " : "
                                + response
                );

            } catch (IOException e) {

                System.out.println(
                        "Erreur avec le client "
                                + clientAddress
                                + " : "
                                + e.getMessage()
                );
            }

            System.out.println(
                    "Connexion terminee : " + clientAddress
            );
        }
    }

    private static String calculate(String request) {

        String[] parts = request.split(";");

        if (parts.length != 3) {
            return "ERROR;Format invalide. Utilisez OPERATION;A;B";
        }

        String operation = parts[0].trim().toUpperCase();

        double a;
        double b;

        try {

            a = Double.parseDouble(parts[1].trim());
            b = Double.parseDouble(parts[2].trim());

        } catch (NumberFormatException e) {

            return "ERROR;A et B doivent etre des nombres";
        }

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