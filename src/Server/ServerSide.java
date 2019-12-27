package Server;

import Encryption.RsaEncrypt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ServerSide {
    private static final int PORT_NUMBER = 9090 ;

    private PublicKey publicKey ;
    private PrivateKey privateKey ;
    private PublicKey trustedCenterPublicKey ;

    private Socket trustedCenterSocket   ;
    private ObjectInputStream trustedCenterObjectInputStream ;
    private ObjectOutputStream trustedCenterObjectOutputStream ;


    public ServerSide() {
        RsaEncrypt rsaEncrypt = new RsaEncrypt() ;
        this.publicKey = rsaEncrypt.getPublicKey() ;
        this.privateKey = rsaEncrypt.getPrivateKey() ;
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket( ServerSide.PORT_NUMBER ) ;
            while ( true ){
                ServerSide serverSide = new ServerSide() ;
                System.out.println("[ % ] Waiting For Trusted Center Connection ... ");
                serverSide.trustedCenterSocket = serverSocket.accept() ;
                System.out.println("[ % ] Trusted Center Connected ... ");
                serverSide.exchangePublicKeyWithTrustedCenter() ;

                System.out.println("[ / ] Waiting For Clients Connections...");
                Socket clientSocket = serverSocket.accept() ;
                System.out.println("[ / ] Client Connection...");
                new Thread( new MultiThreadServer( clientSocket , serverSide.trustedCenterSocket , serverSide.publicKey , serverSide.privateKey , serverSide.trustedCenterPublicKey )).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean exchangePublicKeyWithTrustedCenter() {
        try {
            this.trustedCenterObjectOutputStream = new ObjectOutputStream( this.trustedCenterSocket.getOutputStream());
            this.trustedCenterObjectInputStream = new ObjectInputStream(   this.trustedCenterSocket.getInputStream());
            System.out.println("[ # ] Receive Trusted Center Public Key ... ") ;
            this.trustedCenterPublicKey = (PublicKey) this.trustedCenterObjectInputStream.readObject();
            System.out.println("[ # ] Send My Public Key To the Trusted Center... ");
            this.trustedCenterObjectOutputStream.writeObject( this.publicKey );
            return true ;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this.exchangePublicKeyWithTrustedCenter() ;
    }
}














