import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.rmi.RemoteException;

public class Server implements ChatInterface {
    private Map<String, Set<String>> chatRooms = new HashMap<>();
    private Map<String, Integer> messageStats = new HashMap<>();
    private Map<String, ClientInterface> clients = new HashMap<>();
    private Map<String, byte[]> files = new HashMap<>();
    private long startTime;

    public Server() {
        startTime = System.currentTimeMillis();
        chatRooms.put("general", new HashSet<>());
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            ChatInterface stub = (ChatInterface) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("ChatService", stub);
            System.out.println("Server is running...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void registerClient(String username, ClientInterface client) throws RemoteException {
        clients.put(username, client);
        messageStats.put(username, 0);
    }

    @Override
    public void sendMessage(String room, String sender, String message) throws RemoteException {
        if (!chatRooms.containsKey(room)) {
            throw new RemoteException("Room does not exist");
        }

        messageStats.put(sender, messageStats.getOrDefault(sender, 0) + 1);

        for (String user : chatRooms.get(room)) {
            if (clients.containsKey(user)) {
                clients.get(user).receiveMessage(room, sender, message);
            }
        }
    }

    @Override
    public void sendPrivateMessage(String sender, String recipient, String message) throws RemoteException {
        if (!clients.containsKey(recipient)) {
            throw new RemoteException("Recipient not found");
        }
        messageStats.put(sender, messageStats.getOrDefault(sender, 0) + 1);
        clients.get(recipient).receivePrivateMessage(sender, message);
    }

    @Override
    public void createRoom(String roomName) throws RemoteException {
        if (!chatRooms.containsKey(roomName)) {
            chatRooms.put(roomName, new HashSet<>());
        }
    }

    @Override
    public List<String> getAvailableRooms() throws RemoteException {
        return new ArrayList<>(chatRooms.keySet());
    }

    @Override
    public void joinRoom(String username, String roomName) throws RemoteException {
        if (!chatRooms.containsKey(roomName)) {
            throw new RemoteException("Room does not exist");
        }
        chatRooms.get(roomName).add(username);
    }

    @Override
    public void leaveRoom(String username, String roomName) throws RemoteException {
        if (chatRooms.containsKey(roomName)) {
            chatRooms.get(roomName).remove(username);
        }
    }

    @Override
    public Map<String, Integer> getChatStats() throws RemoteException {
        return new HashMap<>(messageStats);
    }

    @Override
    public long getUptime() throws RemoteException {
        return (System.currentTimeMillis() - startTime) / 1000; // Returns uptime in seconds
    }

    @Override
    public void uploadFile(String sender, String fileName, byte[] fileData) throws RemoteException {
        files.put(fileName, fileData);
    }

    @Override
    public byte[] downloadFile(String fileName) throws RemoteException {
        return files.get(fileName);
    }

    @Override
    public List<String> getAvailableFiles() throws RemoteException {
        return new ArrayList<>(files.keySet());
    }
}
