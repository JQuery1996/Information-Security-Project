package Server;

import Encryption.Encrypt;
import Encryption.RsaEncrypt;
import MessageStucture.MessageBody;
import Model.ClientModel;
import MyDataBase.MySqlManager;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Objects;

public class MultiThreadServer  implements Runnable {

    private final String SERIAL_CODE = "TOP_SECRET" ;
    private SecretKey sessionKey  ;
    private SecretKey secretKey ;
    private PublicKey publicKey ;
    private PrivateKey privateKey ;

    private PublicKey clientPublicKey ;
    private PublicKey trustedCenterPublicKey ;

    private Socket clientSocket ;
    private ObjectOutputStream clientObjectOutputStream ;
    private ObjectInputStream clientObjectInputStream ;

    private Socket trustedCenterSocket ;
    private ObjectOutputStream trustedCenterObjectOutputStream ;
    private ObjectInputStream trustedCenterObjectInputStream ;

    private ClientModel clientModel ;


    public MultiThreadServer ( Socket clientSocket  , Socket trustedCenterSocket , PublicKey publicKey  , PrivateKey privateKey , PublicKey trustedCenterPublicKey ) {
        try {
            this.clientSocket = clientSocket ;
            this.trustedCenterSocket = trustedCenterSocket ;
            // [ Input , Output ]  Configuration
            this.clientObjectOutputStream = new ObjectOutputStream ( this.clientSocket.getOutputStream() ) ;
            this.clientObjectInputStream = new ObjectInputStream  ( this.clientSocket.getInputStream () ) ;

            this.trustedCenterObjectOutputStream = new ObjectOutputStream(trustedCenterSocket.getOutputStream());
            this.trustedCenterObjectInputStream = new ObjectInputStream(  this.trustedCenterSocket.getInputStream());

            this.secretKey = Encrypt.getKey( this.SERIAL_CODE ) ;

            this.publicKey = publicKey ;
            this.privateKey = privateKey ;
            this.trustedCenterPublicKey = trustedCenterPublicKey  ;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.handelLogin() ;
        this.handelTransferringSystem() ;
    }

    private boolean handelLogin() {
        System.out.println("[---] Start Login Handler ");
        try {
            long accountNumber = Long.parseLong(Objects.requireNonNull(Encrypt.decryptMessage(this.secretKey, String.valueOf(this.clientObjectInputStream.readObject()))));
            String password = Encrypt.decryptMessage( this.secretKey , (String) this.clientObjectInputStream.readObject());
            if ( ! makeAuthentication( accountNumber , password ) ) { System.out.println("[!] Login Operation Failed ....") ; this.handelLogin() ; }

            // Send Keys To Client
            this.clientObjectOutputStream.writeObject( this.clientModel.getPublicKey() );
            this.clientObjectOutputStream.writeObject( this.clientModel.getPrivateKey() );
            this.clientObjectOutputStream.writeObject( this.publicKey ) ;

            System.out.println("[#] Login Operation Has Been Completed Successfully...");


            return true ;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("[!] Login Operation Failed ....");
        return this.handelLogin() ;
    }
    private boolean  makeAuthentication(long accountNumber, String password) {
        MySqlManager mySqlManager = new MySqlManager() ;
        ResultSet clientRecord = mySqlManager.checkAuthentication ( accountNumber , password ) ;
        try {
            if ( clientRecord.next() ) {
                // Send Response To The Server
                this.clientObjectOutputStream.writeObject( true );
                this.clientModel = new ClientModel(
                        clientRecord.getLong("id") ,
                        clientRecord.getString("first_name") ,
                        clientRecord.getString("last_name") ,
                        clientRecord.getLong("account_number") ,
                        clientRecord.getLong("deposit") ,
                        clientRecord.getString("public_key") ,
                        clientRecord.getString("private_key") ,
                        clientRecord.getString("serial_code") ,
                        clientRecord.getString("password")
                ) ;

                if ( this.clientModel.getPublicKey() == null || this.clientModel.getPrivateKey() == null ) {
                    RsaEncrypt rsaEncrypt = new RsaEncrypt() ;
                    this.clientModel.setPublicKey( rsaEncrypt.PublicKeyToString(rsaEncrypt.getPublicKey()));
                    this.clientModel.setPrivateKey( rsaEncrypt.PrivateKeyToString( rsaEncrypt.getPrivateKey()));
                    mySqlManager.setKeys( this.clientModel.getAccountNumber() , this.clientModel.getPublicKey() , this.clientModel.getPrivateKey()) ;
                }
                return true  ;
            }else {
                this.clientObjectOutputStream.writeObject( false );
                return false  ;
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return false  ;
    }
    private boolean handelTransferringSystem() {
        try {

            System.out.println("[ % ] Start Transferring System ....");
            // Read Session Key
            this.sessionKey = Encrypt.decryptSecretKey(Base64.getDecoder().decode(String.valueOf(this.trustedCenterObjectInputStream.readObject())) , this.privateKey ) ;

            // Read Signature Message
            String signatureMessage = (String) trustedCenterObjectInputStream.readObject();

            // Read Encrypted Message
            MessageBody messagebody = (MessageBody) this.trustedCenterObjectInputStream.readObject();

            // Signature Verify
            if ( !Encrypt.verify( messagebody.toString() , signatureMessage , this.trustedCenterPublicKey) ) {
                this.trustedCenterObjectOutputStream.writeObject( false );
                this.trustedCenterObjectOutputStream.writeObject("[ - ] Error In Verify Message ... Incorrect Message ");
                return this.handelTransferringSystem();
            }
            // Decrypt The Message
            String uniqueId = Encrypt.decryptMessage( this.sessionKey , messagebody.getUniqueId())  ;
            long accountNumber = Long.parseLong(Objects.requireNonNull(Encrypt.decryptMessage(this.sessionKey, messagebody.getAccountNumber())));
            long amount = Long.parseLong(Objects.requireNonNull(Encrypt.decryptMessage(this.sessionKey, messagebody.getAmount())));
            String reasonMessage = Encrypt.decryptMessage( this.sessionKey , messagebody.getReasonMessage()) ;

            // Amount > Current Deposit
           if ( clientModel.getDeposit() < amount ) {
               this.trustedCenterObjectOutputStream.writeObject( false ) ;
               this.trustedCenterObjectOutputStream.writeObject("[ @ ] Transferring System Stop => ( Amount > Deposit ) ... "); ;
               return this.handelTransferringSystem() ;
           }

           if ( makeTransferring( amount , accountNumber ) ) {
               this.trustedCenterObjectOutputStream.writeObject( true )  ;
               this.trustedCenterObjectOutputStream.writeObject("[ (- ] Transferring System Completed ...");
               System.out.println("[ (- ] Transferring System Completed ... ");
               // Check if customer need to do more transferring
               System.out.println("[ = ] Waiting For Client More Transactions....");
               boolean more = (boolean) this.trustedCenterObjectInputStream.readObject();
               if ( more ) {
                   System.out.println("[ - ] Restart Transferring System For Another Transfer Operation ... ");
                   return this.handelTransferringSystem();
               }else {
                   System.out.println("[ * ] Transferring System End ... ");
                   return true;
               }
           } else {
               System.out.println("[ )- ] Transferring System Fail...");
               this.trustedCenterObjectOutputStream.writeObject( false ) ;
               this.trustedCenterObjectOutputStream.writeObject("[ )- ] Transferring System Fail...");
               return this.handelTransferringSystem() ;
           }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false ;
    }

    private boolean makeTransferring(long amount, long accountNumber) {
        try {
            MySqlManager mySqlManager = new MySqlManager() ;
            ResultSet targetClient = mySqlManager.getClientByAccountNumber( accountNumber ) ;
            if ( !targetClient.next() ) {
                return false ;
            }else {
                ClientModel targetClientModel = new ClientModel(
                        targetClient.getLong("id") ,
                        targetClient.getString("first_name") ,
                        targetClient.getString("last_name") ,
                        targetClient.getLong("account_number") ,
                        targetClient.getLong("deposit") ,
                        targetClient.getString("public_key") ,
                        targetClient.getString("private_key") ,
                        targetClient.getString("serial_code") ,
                        targetClient.getString("password")
                );

                boolean isCompleted = mySqlManager.makeTransfer( this.clientModel.getAccountNumber() , targetClientModel.getAccountNumber() ,
                this.clientModel.getDeposit() - amount , targetClientModel.getDeposit() + amount ) ;
                if ( isCompleted ) {
                    this.clientModel.setDeposit( this.clientModel.getDeposit() - amount ) ;
                    return true  ;
                }else {
                    return false ;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false ;
    }
}
