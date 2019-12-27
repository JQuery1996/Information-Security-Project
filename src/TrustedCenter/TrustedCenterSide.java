package TrustedCenter;

import Encryption.RsaEncrypt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

public class TrustedCenterSide {
    private static final int     TRUSTED_CENTER_PORT_NUMBER = 9091 ;
    private static final String  TRUSTED_CENTER_HOST_NAME = "localhost" ;

    private static final int      SERVER_PORT_NUMBER  = 9090 ;
    private static final String  SERVER_HOST_NAME    = "localhost" ;

    private PublicKey publicKey ;
    private PrivateKey privateKey ;
    private PublicKey serverPubicKey ;
    private Socket clientSocket ;
    private ObjectInputStream clientObjectInputStream ;
    private ObjectOutputStream clientObjectOutputStream ;

    private Socket serverSocket ;
    private ObjectInputStream serverObjectInputStream = null ;
    private ObjectOutputStream serverObjectOutputStream = null ;

    public TrustedCenterSide() {
        RsaEncrypt rsaEncrypt = new RsaEncrypt() ;
        this.publicKey = rsaEncrypt.getPublicKey() ;
        this.privateKey = rsaEncrypt.getPrivateKey() ;
    }

    public static void main(String[] args) {
        try {

            ServerSocket trustedCenterSocket = new ServerSocket( TrustedCenterSide.TRUSTED_CENTER_PORT_NUMBER ) ;

            while( true ) {
                TrustedCenterSide trustedCenterSide = new TrustedCenterSide() ;
                trustedCenterSide.serverSocket = new Socket( TrustedCenterSide.SERVER_HOST_NAME , TrustedCenterSide.SERVER_PORT_NUMBER ) ;
                trustedCenterSide.exchangePublicKeyWithServer() ;
                System.out.println("[ * ] Waiting For Clients Connections ...");
                Socket clientSocket = trustedCenterSocket.accept() ;
                System.out.println("[ - ] Client Connected ... ");
                new Thread( new MultiTrustedCenter( clientSocket , trustedCenterSide.serverSocket , trustedCenterSide.publicKey , trustedCenterSide.privateKey , trustedCenterSide.serverPubicKey)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean exchangePublicKeyWithServer () {
        try {
            this.serverObjectOutputStream = new ObjectOutputStream( this.serverSocket.getOutputStream() );
            this.serverObjectInputStream = new ObjectInputStream( this.serverSocket.getInputStream() ) ;
            System.out.println("[ - ] Send My Public Key To The Server .... ");
            this.serverObjectOutputStream.writeObject( this.publicKey ) ;
            System.out.println("[ - ] Receive Server Public Key ... ") ;
            this.serverPubicKey = (PublicKey) this.serverObjectInputStream.readObject();
            return true  ;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this.exchangePublicKeyWithServer();
    }
}

























