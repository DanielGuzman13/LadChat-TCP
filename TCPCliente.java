import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPCliente {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        try (
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        ) {
        
            System.out.println(in.readLine());
            String userName = console.readLine();
            out.println(userName);

            new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    System.out.println("Desconectado del servidor.");
                }
            }).start();

            String msg;
            while ((msg = console.readLine()) != null) {
                out.println(msg);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}