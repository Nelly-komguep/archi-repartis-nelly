import java.io.*;
import java.net.*;

public class CalcClient {

    public static void main(String[] args) throws IOException {

        String host = "localhost";
        int port = 5000;

        try (
            Socket socket = new Socket(host, port);

            BufferedReader keyboard = new BufferedReader(
                    new InputStreamReader(System.in));

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter writer = new PrintWriter(
                    socket.getOutputStream(), true)
        ) {

            System.out.println("=== CALCULATRICE DISTANTE ===");
            System.out.println("Format : OPERATION;A;B");
            System.out.println("Exemple : ADD;4;7");
            System.out.print("Entrez une operation : ");

            String operation = keyboard.readLine();

            writer.println(operation);

            String response = reader.readLine();

            System.out.println("Reponse du serveur : " + response);
        }
    }
}