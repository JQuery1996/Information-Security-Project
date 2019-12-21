package Client;

import Encryption.Encrypt;
import Encryption.RsaEncrypt;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.PublicKey;

public class ClientSide {
    private static final int PORT_NUMBER = 9090 ;
    private static final String HOST_NAME = "localhost" ;
    private Socket socket  ;
    private ObjectInputStream objectInputStream  = null ;
    private ObjectOutputStream objectOutputStream = null ;
    private BufferedReader keyboardReader  ;
    private SecretKey secretKey  ;
    private final String SECRET_CODE = "TOP_SECRET" ;
    private SecretKey sessionKey ;
    private PublicKey publicKey ;
    private PublicKey serverpublicKey   ;

    private ClientSide() {
        try {
            this.socket = new Socket( ClientSide.HOST_NAME , ClientSide.PORT_NUMBER ) ;
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream()) ;
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());

            this.keyboardReader = new BufferedReader( new InputStreamReader( System.in )) ;
            this.secretKey = Encrypt.generateSecretKey() ;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        ClientSide clientSide  = new ClientSide () ;
        clientSide.loginOperation()  ;
        clientSide.transferSystem() ;

    }

    private boolean loginOperation() {
        System.out.println("[ / ] Login Operation [ / ]");
        try {
            System.out.println("[ * ] Enter Your Account Number : ")  ;
            long accountNumber = Long.parseLong(this.keyboardReader.readLine());

            System.out.println("[ * ] Enter Your Password : ") ;
            String password = this.keyboardReader.readLine() ;

            System.out.println("[ * ] Confirm Your Password : ") ;
            String confirmedPassword  =  this.keyboardReader.readLine() ;
//
//            System.out.println("[*] Enter Your Serial Code : ");
//            String serialCode = keyboardReader.readUTF() ;

            this.secretKey = Encrypt.getKey( this.SECRET_CODE );
            // if password != confirmedPassword Then Redo Full operation
            if ( !password.equals( confirmedPassword ) ) return this.loginOperation() ;


            // Send Login Information To Server Side To Check Authentication
            this.objectOutputStream.writeObject( Encrypt.encryptMessage( this.secretKey , accountNumber ));
            this.objectOutputStream.writeObject( Encrypt.encryptMessage( this.secretKey , password ));

            // Get Response From The Server
            boolean loginStatus = (boolean) this.objectInputStream.readObject();
            // if Login Failed Then Redo Full Operation
            if ( ! loginStatus ) {
                System.out.println("[ ! ] Login Fail Please Reenter Your Information");
                return this.loginOperation() ;
            }

            // Receive keys
            this.publicKey = new RsaEncrypt().StringToPublicKey(String.valueOf(this.objectInputStream.readObject())) ;
            this.serverpublicKey = (PublicKey) this.objectInputStream.readObject();

            System.out.println("[ (: ] Login Success...");


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false ;
    }
    private boolean transferSystem() {
        try {
            System.out.println("[ * ] Welcome To Transfer System....") ;
            System.out.println("[ > ] Enter Transfer Amount : ") ;
            long amount = Long.parseLong(this.keyboardReader.readLine());
            System.out.println("[ > ] Enter The Target Account Number : ") ;
            long accountNumber = Long.parseLong(this.keyboardReader.readLine()) ;
            System.out.println("[ > ] Enter The Reason For This Operation : ") ;
            String reasonMessage = this.keyboardReader.readLine() ;

            this.sessionKey = Encrypt.generateSecretKey() ;
            String sessionKeyEncrypted = Encrypt.encryptSecretKey( sessionKey , this.serverpublicKey )  ;

            // Send Encrypted Seession Key To Server
            this.objectOutputStream.writeObject( sessionKeyEncrypted ) ;
            // Send Encrypted Data To The Server
            this.objectOutputStream.writeObject( Encrypt.encryptMessage( this.sessionKey , amount ))  ;
            this.objectOutputStream.writeObject( Encrypt.encryptMessage( this.sessionKey, accountNumber  ))  ;
            this.objectOutputStream.writeObject( Encrypt.encryptMessage( this.sessionKey, reasonMessage ))  ;

            // Get The Response From The Server
            boolean transferStatus = (boolean) this.objectInputStream.readObject() ;
            if ( !transferStatus ) {
                System.out.println("[ $ ] Error Happened ... Please Try Again With Another Data .... " ) ;
                return this.transferSystem() ;
            }
            System.out.println("[ - ] Transfer Completed .... ") ;
            String answer  ;
            do {
                System.out.println("[ ? ] Do You Want To Make Another Transferring [ yes or no ] : ");
                answer = this.keyboardReader.readLine() ;
            }while (!answer.equalsIgnoreCase("yes") && !answer.equalsIgnoreCase("no"))  ;

            this.objectOutputStream.writeObject(answer.equalsIgnoreCase("yes"));
            if ( answer.equalsIgnoreCase("yes") )
                return this.transferSystem();
            else
                return true;


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false ;
    }
}
