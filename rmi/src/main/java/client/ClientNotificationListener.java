package client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientNotificationListener
        extends UnicastRemoteObject
        implements NotificationListener {

    public ClientNotificationListener()
            throws RemoteException {
        super();
    }

    @Override
    public void onNewOperation(String entry)
            throws RemoteException {

        System.out.println(
                "[NOTIFICATION] Nouvelle opération : "
                        + entry
        );
    }
}