package TrustedCenter;

import Encryption.Encrypt;
import MessageStucture.MessageBody;
import MyDataBase.MySqlManager;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class MultiTrustedCenter implements Runnable  {
    private final String SERIAL_CODE = "TOP_SECRET" ;
    private SecretKey sessionKey  ;
    private SecretKey secretKey ;
    private PublicKey publicKey ;
    private PrivateKey privateKey ;
    private PublicKey serverPublicKey  ;
    private PublicKey clientPublicKey ;

    private Socket clientSocket ;
    private ObjectOutputStream clientObjectOutputStream ;
    private ObjectInputStream clientObjectInputStream ;

    private Socket serverSocket ;
    private ObjectOutputStream serverObjectOutputStream ;
    private ObjectInputStream serverObjectInputStream ;

    public MultiTrustedCenter(Socket clientSocket, Socket serverSocket, PublicKey publicKey, PrivateKey privateKey, PublicKey serverPubicKey) {
        try {
            this.clientSocket = clientSocket ;
            this.serverSocket = serverSocket ;
            this.clientObjectOutputStream = new ObjectOutputStream( this.clientSocket.getOutputStream());
            this.clientObjectInputStream = new ObjectInputStream(clientSocket.getInputStream());

            this.serverObjectOutputStream = new ObjectOutputStream(serverSocket.getOutputStream());
            this.serverObjectInputStream = new ObjectInputStream(serverSocket.getInputStream());

            this.secretKey = Encrypt.getKey( this.SERIAL_CODE ) ;
            this.publicKey = publicKey ;
            this.privateKey = privateKey ;
            this.serverPublicKey = serverPubicKey ;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.exchangePublicKeyWithClient()  ;
        this.handelTransferSystem() ;
    }

    private boolean exchangePublicKeyWithClient() {
        try {
            System.out.println("[ - ] Send My Public Key To Client ... ") ;
            this.clientObjectOutputStream.writeObject( this.publicKey );
            System.out.println("[ + ] Receive Client Public Key ... ");
            this.clientPublicKey = (PublicKey) this.clientObjectInputStream.readObject();
            return true ;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this.exchangePublicKeyWithClient() ;
    }

   private boolean handelTransferSystem() {
        try {
            System.out.println("[ - ] Start Transfer System ... ") ;
            // Read Session Key
            this.sessionKey = Encrypt.decryptSecretKey(Base64.getDecoder().decode(String.valueOf(this.clientObjectInputStream.readObject())) , this.privateKey ) ;

            // Read Signature Message
            String signatureMessage = (String) clientObjectInputStream.readObject();

            // Read Encrypted Message
            MessageBody messagebody = (MessageBody) this.clientObjectInputStream.readObject();


            // Signature Verify
            if ( !Encrypt.verify( messagebody.toString() , signatureMessage , this.clientPublicKey) ) {
                this.clientObjectOutputStream.writeObject( false );
                this.clientObjectOutputStream.writeObject("[ - ] Error In Verify Message ... Incorrect Message ");
                return this.handelTransferSystem() ;
            }

            // Decrypt The Message
            String uniqueId = Encrypt.decryptMessage( this.sessionKey , messagebody.getUniqueId()) ;
            long accountNumber = Long.parseLong(Objects.requireNonNull(Encrypt.decryptMessage(this.sessionKey, messagebody.getAccountNumber())));
            long amount = Long.parseLong(Objects.requireNonNull(Encrypt.decryptMessage(this.sessionKey, messagebody.getAmount())));
            String reasonMessage = Encrypt.decryptMessage( this.sessionKey , messagebody.getReasonMessage()) ;

            // check if Transaction unique Id exists in DataBase
            MySqlManager mySqlManager = new MySqlManager() ;
            if ( mySqlManager.findByUniqueId( uniqueId) ) {
                this.clientObjectOutputStream.writeObject( false );
                this.clientObjectOutputStream.writeObject("[ ! ] Error Repeated Unique Id .... ");
                System.out.println("[ ! ] Transferring System Fail... ");
                return this.handelTransferSystem() ;
            }

            // Insert New Transaction into DataBase
            mySqlManager.insertNewTransaction( uniqueId , accountNumber , amount , reasonMessage , signatureMessage , LocalDateTime.now().toString()) ;

            // After That Redirect Rest Of Work To The Server

            this.sessionKey = Encrypt.generateSecretKey() ;
            String sessionKeyEncrypted = Encrypt.encryptSecretKey( sessionKey , this.serverPublicKey)  ;

            String encryptedUniqueId = Encrypt.encryptMessage( this.sessionKey , UUID.randomUUID());
            String encryptedAccountNumber = Encrypt.encryptMessage( this.sessionKey , accountNumber ) ;
            String encryptedAmount = Encrypt.encryptMessage( this.sessionKey , amount ) ;
            String encryptedReasonMessage = Encrypt.encryptMessage( this.sessionKey , reasonMessage ) ;
            MessageBody messageToServer = new MessageBody( encryptedUniqueId , encryptedAccountNumber , encryptedAmount , encryptedReasonMessage ) ;
            String signatureMessageToServer = Encrypt.sign( messageToServer.toString() , this.privateKey) ;

            // Send Session Key To Server
            this.serverObjectOutputStream.writeObject( sessionKeyEncrypted );

            // Send Signature to Server
            this.serverObjectOutputStream.writeObject( signatureMessageToServer );

            // Send Encrypted Message To Trusted Center
            this.serverObjectOutputStream.writeObject( messageToServer );

            // Get Response From The Server
            boolean serverResponse = (boolean) this.serverObjectInputStream.readObject();
            String responseMessage = (String) this.serverObjectInputStream.readObject();

            if ( ! serverResponse ) {
                // Send Response To Client
                this.clientObjectOutputStream.writeObject(false);
                this.clientObjectOutputStream.writeObject( responseMessage);
                System.out.println("[ ! ] Transferring System Fail... ");
                return this.handelTransferSystem() ;
            }else {
                // Send Response To Client
                this.clientObjectOutputStream.writeObject( true );
                this.clientObjectOutputStream.writeObject( reasonMessage );
                // Waiting For Client More Operation Or not
                System.out.println("[ = ] Waiting For Client More Transactions....");
                boolean more = (boolean) this.clientObjectInputStream.readObject();
                if ( more ) {
                    // First Send More To Server
                    this.serverObjectOutputStream.writeObject(true);
                    System.out.println("[ ! ] Restart Transferring System For Another Operation... ");
                    return this.handelTransferSystem();
                }else {
                    this.serverObjectOutputStream.writeObject( false );
                    System.out.println("[ ! ] Transferring System End Successfully ... ");
                    return true;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this.handelTransferSystem() ;
   }
}





















