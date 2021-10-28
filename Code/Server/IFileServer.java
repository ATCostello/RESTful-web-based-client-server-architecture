import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface IFileServer extends Remote{
    void put(String name, byte[] data) throws RemoteException;
    byte[] get(String name) throws RemoteException;
    void operate(String filename, byte[] data, HashMap<String, Integer> operations) throws RemoteException;
    void operateJPG(String filename, byte[] data, HashMap<String, Integer> operationsJPG) throws RemoteException;
    void uploadFile(String filename, byte[] data) throws RemoteException;
    File[] getFiles() throws RemoteException;
    byte[] downloadFile(String filename) throws RemoteException;
} 