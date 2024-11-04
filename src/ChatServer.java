import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 1234;
    private static Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");

        // Start a new thread to listen for server messages
        new Thread(() -> {
            try (BufferedReader serverInput = new BufferedReader(new InputStreamReader(System.in))) {
                String serverMessage;
                while ((serverMessage = serverInput.readLine()) != null) {
                    sendToAllClients("Server: " + serverMessage);
                }
            } catch (IOException e) {
                System.out.println("Error reading server input: " + e.getMessage());
            }
        }).start();

        // Start the server to accept client connections
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to send messages to all connected clients
    private static void sendToAllClients(String message) {
        System.out.println("Broadcasting: " + message);
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    System.out.println("Client: " + clientMessage);
                    sendToAllClients("Client: " + clientMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    synchronized (clientWriters) {
                        clientWriters.remove(out);
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
