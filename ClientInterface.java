import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote {
    void receiveMessage(String room, String sender, String message) throws RemoteException;

    void receivePrivateMessage(String sender, String message) throws RemoteException;
}
