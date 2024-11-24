import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface ChatInterface extends Remote {
    void sendMessage(String room, String sender, String message) throws RemoteException;

    void sendPrivateMessage(String sender, String recipient, String message) throws RemoteException;

    void createRoom(String roomName) throws RemoteException;

    List<String> getAvailableRooms() throws RemoteException;

    void joinRoom(String username, String roomName) throws RemoteException;

    void leaveRoom(String username, String roomName) throws RemoteException;

    Map<String, Integer> getChatStats() throws RemoteException;

    long getUptime() throws RemoteException;

    void uploadFile(String sender, String fileName, byte[] fileData) throws RemoteException;

    byte[] downloadFile(String fileName) throws RemoteException;

    List<String> getAvailableFiles() throws RemoteException;

    void registerClient(String username, ClientInterface client) throws RemoteException;
}
