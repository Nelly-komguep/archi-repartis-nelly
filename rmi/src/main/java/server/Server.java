package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {

    public static void main(String[] args) throws Exception {

        // Création du registre RMI sur le port 1099
        Registry registry = LocateRegistry.createRegistry(1099);

        // Création du service History
        HistoryServiceImpl historyImpl =
                new HistoryServiceImpl();

        // Enregistrement du service History
        registry.rebind("HistoryService", historyImpl);

        // Récupération du stub du service History
        HistoryService historyStub =
                (HistoryService) registry.lookup("HistoryService");

        // Création du service Calculator
        CalculatorServiceImpl calcImpl =
                new CalculatorServiceImpl();

        // Injection du service History dans Calculator
        calcImpl.setHistoryService(historyStub);

        // Enregistrement du service Calculator
        registry.rebind("CalculatorService", calcImpl);

        System.out.println( "Registre RMI démarré sur le port 1099"
        );

        System.out.println(  "Service enregistré : HistoryService"
        );

        System.out.println(   "Service enregistré : CalculatorService"
        );

        System.out.println(  "Serveur RMI prêt."
        );

        // Maintenir le serveur en fonctionnement
        Thread.currentThread().join();
    }
}