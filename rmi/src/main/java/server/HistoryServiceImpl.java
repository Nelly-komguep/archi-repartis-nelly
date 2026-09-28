package server;

import client.NotificationListener;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HistoryServiceImpl
        extends UnicastRemoteObject
        implements HistoryService {

    private final List<String> history;
    private final CopyOnWriteArrayList<NotificationListener> listeners;

    public HistoryServiceImpl() throws RemoteException {
        super();

        history = new ArrayList<>();
        listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void record(String entry)
            throws RemoteException {

        // Protection de l'historique
        synchronized (history) {
            history.add(entry);
        }

        // CopyOnWriteArrayList permet de parcourir
        // les listeners sans ConcurrentModificationException.
        for (NotificationListener listener : listeners) {

            try {

                listener.onNewOperation(entry);

            } catch (RemoteException e) {

                // Le client est probablement déconnecté.
                // On retire uniquement ce listener.
                listeners.remove(listener);

                System.out.println(
                        "Client déconnecté : listener supprimé."
                );
            }
        }
    }

    @Override
    public List<String> getAll()
            throws RemoteException {

        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    @Override
    public void subscribe(
            NotificationListener listener)
            throws RemoteException {

        if (listener != null && !listeners.contains(listener)) {

            listeners.add(listener);

            System.out.println(
                    "Nouveau client abonné aux notifications."
            );
        }
    }
}