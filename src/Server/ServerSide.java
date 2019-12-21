package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSide {
    private static final int PORT_NUMBER = 9090 ;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket( ServerSide.PORT_NUMBER ) ;
            while ( true ){
                System.out.println("[/] Waiting For Clients Connections...");
                Socket socket = serverSocket.accept() ;
                System.out.println("[/] Client Connection...");
                new Thread( new MultiThreadServer( socket )).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
