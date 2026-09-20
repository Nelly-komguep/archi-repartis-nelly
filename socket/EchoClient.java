import java.io.*;
import java.net.*;

public class EchoClient {

    public static void main(String[] args) throws IOException {

        String host = "localhost";
        int port = 5000;

        try (
            Socket socket = new Socket(host, port);

            BufferedReader keyboard = new BufferedReader(
                new InputStreamReader(System.in)
            );

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );

            PrintWriter writer = new PrintWriter(
                socket.getOutputStream(),
                true
            )
        ) {

            System.out.print("Entrez un message : ");

            String message = keyboard.readLine();

            writer.println(message);

            String response = reader.readLine();

            System.out.println("Reponse du serveur : " + response);
        }
    }
}