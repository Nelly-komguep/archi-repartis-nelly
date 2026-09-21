package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class HistoryServiceImpl
        extends UnicastRemoteObject
        implements HistoryService {

    private final List<String> history;

    public HistoryServiceImpl() throws RemoteException {
        super();
        history = new ArrayList<>();
    }

    @Override
    public synchronized void record(String entry) throws RemoteException {
        history.add(entry);
    }

    @Override
    public synchronized List<String> getAll() throws RemoteException {
        return new ArrayList<>(history);
    }
}