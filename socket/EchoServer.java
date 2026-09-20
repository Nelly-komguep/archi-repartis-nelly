import java.io.*;
import java.net.*;

public class EchoServer {

    public static void main(String[] args) throws IOException {

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Serveur en ecoute sur le port " + port);

            while (true) {

                Socket client = serverSocket.accept();

                System.out.println(
                        "Client connecte : " +
                        client.getInetAddress().getHostAddress()
                );

                try (
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream())
                    );

                    PrintWriter writer = new PrintWriter(
                        client.getOutputStream(),
                        true
                    )
                ) {

                    String message = reader.readLine();

                    if (message != null) {
                        writer.println(message);

                        System.out.println(
                                "Message recu : " + message
                        );
                    }

                } finally {
                    client.close();
                    System.out.println("Client deconnecte.");
                }
            }
        }
    }
}