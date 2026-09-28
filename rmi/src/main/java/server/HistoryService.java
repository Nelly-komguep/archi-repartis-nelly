package server;

import client.NotificationListener;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface HistoryService extends Remote {

    void record(String entry) throws RemoteException;

    List<String> getAll() throws RemoteException;

    void subscribe(NotificationListener listener) throws RemoteException;
}