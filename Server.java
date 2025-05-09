import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    public static final int PORT = 12345;
    private static Map<String, PrintWriter> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor TCP en puerto " + PORT);

            new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        new Thread(new ClientHandler(socket)).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        String adminMsg;
        while ((adminMsg = console.readLine()) != null) {
            broadcast(" [Servidor]: " + adminMsg, null);
        }
    }

    public static void broadcast(String message, String excludeUser) {
        synchronized (clients) {
            for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                if (!entry.getKey().equals(excludeUser)) {
                    entry.getValue().println(message);
                }
            }
        }
    }

    public static void sendPrivate(String toUser, String message, PrintWriter senderOut) {
        synchronized (clients) {
            PrintWriter target = clients.get(toUser);
            if (target != null) {
                target.println(message);
                senderOut.println("A " + toUser + ": " + message);
            } else {
                senderOut.println(" Usuario '" + toUser + "' no encontrado.");
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String userName;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Bienvenido al chat. Usa este formato:");
                out.println(" - Para mensaje público: escribe directamente el mensaje.");
                out.println(" - Para mensaje privado: /privado <usuario> <mensaje>");
                out.println(" - Para ver usuarios conectados: /usuarios");
                out.println("Ingresa tu nombre de usuario:");

                userName = in.readLine();

                synchronized (clients) {
                    if (clients.containsKey(userName)) {
                        out.println("Nombre en uso. Desconectado.");
                        socket.close();
                        return;
                    }
                    clients.put(userName, out);
                }

                broadcast( userName + " se ha conectado", null);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/privado ")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length == 3) {
                            String toUser = parts[1];
                            String privateMsg = parts[2];
                            sendPrivate(toUser, "🔒 " + userName + ": " + privateMsg, out);
                        } else {
                            out.println(" Formato inválido. Usa /privado <usuario> <mensaje>");
                        }
                    } else if (message.equals("/usuarios")) {
                        out.println("👥 Usuarios conectados:");
                        synchronized (clients) {
                            for (String name : clients.keySet()) {
                                out.println(" - " + name);
                            }
                        }
                    } else {
                        broadcast(userName + ": " + message, userName);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error con usuario " + userName);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {}
                synchronized (clients) {
                    clients.remove(userName);
                }
                broadcast( userName + " se ha desconectado", null);
            }
        }
    }
}
