package Client;

import Encryption.Encrypt;
import Encryption.RsaEncrypt;
import MessageStucture.MessageBody;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

public class ClientSide {
    private static final int SERVER_PORT_NUMBER = 9090 ;
    private static final String SERVER_HOST_NAME = "localhost" ;

    private static final int TRUSTED_CENTER_PORT_NUMBER = 9091 ;
    private static final String TRUSTED_CENTER_HOST_NAME = "localhost" ;

    private Socket clientSocket;
    private ObjectInputStream clientObjectInputStream = null ;
    private ObjectOutputStream clientObjectOutputStream = null ;

    private Socket trustedCenterSocket ;
    private ObjectOutputStream trustedCenterObjectOutputStream ;
    private ObjectInputStream trustedCenterObjectInputStream ;

    private BufferedReader keyboardReader  ;
    private SecretKey secretKey  ;
    private final String SECRET_CODE = "TOP_SECRET" ;
    private SecretKey sessionKey ;
    private PublicKey publicKey ;
    private PrivateKey privateKey ;
    private PublicKey serverPublicKey;

    private PublicKey trustedCenterPublicKey ;


    private ClientSide() {
        try {
            this.clientSocket = new Socket( ClientSide.SERVER_HOST_NAME, ClientSide.SERVER_PORT_NUMBER) ;
            this.clientObjectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream()) ;
            this.clientObjectInputStream = new ObjectInputStream(clientSocket.getInputStream());

            this.trustedCenterSocket = new Socket( ClientSide.TRUSTED_CENTER_HOST_NAME , ClientSide.TRUSTED_CENTER_PORT_NUMBER ) ;
            this.trustedCenterObjectOutputStream = new ObjectOutputStream( this.trustedCenterSocket.getOutputStream()) ;
            this.trustedCenterObjectInputStream = new ObjectInputStream( this.trustedCenterSocket.getInputStream());


            this.keyboardReader = new BufferedReader( new InputStreamReader( System.in )) ;
            this.secretKey = Encrypt.generateSecretKey() ;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        ClientSide clientSide  = new ClientSide () ;
        clientSide.loginOperation()  ;
        clientSide.exchangePublicKeyWithTrustedCenter() ;
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
            this.clientObjectOutputStream.writeObject( Encrypt.encryptMessage( this.secretKey , accountNumber ));
            this.clientObjectOutputStream.writeObject( Encrypt.encryptMessage( this.secretKey , password ));

            // Get Response From The Server
            boolean loginStatus = (boolean) this.clientObjectInputStream.readObject();
            // if Login Failed Then Redo Full Operation
            if ( ! loginStatus ) {
                System.out.println("[ ! ] Login Fail Please Reenter Your Information");
                return this.loginOperation() ;
            }

            // Receive keys
            this.publicKey = new RsaEncrypt().StringToPublicKey(String.valueOf(this.clientObjectInputStream.readObject())) ;
            this.privateKey = new RsaEncrypt().StringToPrivateKey( String.valueOf( this.clientObjectInputStream.readObject())) ;
            this.serverPublicKey = (PublicKey) this.clientObjectInputStream.readObject();

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
            String sessionKeyEncrypted = Encrypt.encryptSecretKey( sessionKey , this.trustedCenterPublicKey)  ;

            String encryptedUniqueId = Encrypt.encryptMessage( this.sessionKey , UUID.randomUUID());
            String encryptedAccountNumber = Encrypt.encryptMessage( this.sessionKey , accountNumber ) ;
            String encryptedAmount = Encrypt.encryptMessage( this.sessionKey , amount ) ;
            String encryptedReasonMessage = Encrypt.encryptMessage( this.sessionKey , reasonMessage ) ;
            MessageBody messageBody = new MessageBody( encryptedUniqueId , encryptedAccountNumber , encryptedAmount , encryptedReasonMessage ) ;
            String signatureMessage = Encrypt.sign( messageBody.toString() , this.privateKey ) ;

            // Send Encrypted Session Key To Trusted Center
            this.trustedCenterObjectOutputStream.writeObject( sessionKeyEncrypted ) ;

            // Send Signature to Trusted Center
            this.trustedCenterObjectOutputStream.writeObject( signatureMessage );

            // Send Encrypted Message To Trusted Center
            this.trustedCenterObjectOutputStream.writeObject( messageBody );

            // Get The Response From The Server
            boolean transferStatus = (boolean) this.trustedCenterObjectInputStream.readObject() ;
            String responseMessage = (String) this.trustedCenterObjectInputStream.readObject();
            if ( !transferStatus ) {
                System.out.println(responseMessage) ;
                return this.transferSystem() ;
            }
            System.out.println("[ - ] Transfer Completed .... ") ;
            String answer  ;
            do {
                System.out.println("[ ? ] Do You Want To Make Another Transferring [ yes or no ] : ");
                answer = this.keyboardReader.readLine() ;
            }while (!answer.equalsIgnoreCase("yes") && !answer.equalsIgnoreCase("no"))  ;

            this.trustedCenterObjectOutputStream.writeObject(answer.equalsIgnoreCase("yes"));
            if ( answer.equalsIgnoreCase("yes") )
                return this.transferSystem();
            else
                return true;


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this.transferSystem() ;
    }
    private boolean exchangePublicKeyWithTrustedCenter() {
        try {
            System.out.println("[ + ] Receive Trusted Center Public Key ... ");
            this.trustedCenterPublicKey = (PublicKey) this.trustedCenterObjectInputStream.readObject();
            System.out.println("[ _ ] Send My Public Key To Trusted Center ... ");
            this.trustedCenterObjectOutputStream.writeObject( this.publicKey );
            return true  ;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this.exchangePublicKeyWithTrustedCenter() ;
    }
}
