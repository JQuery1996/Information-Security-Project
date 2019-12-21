package Server;

import Encryption.Encrypt;
import Encryption.RsaEncrypt;
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
import java.util.Objects;

public class MultiThreadServer  implements Runnable {
    private ObjectOutputStream objectOutputStream ;
    private ObjectInputStream  objectInputStream ;
    private Socket client ;
    private ClientModel clientModel ;
    private SecretKey secretKey ;
    private PublicKey publicKey ;
    private PrivateKey privateKey ;
    private final String SERIAL_CODE = "TOP_SECRET" ;
    public MultiThreadServer ( Socket client ) {
        try {
            this.client = client ;
            // [ Input , Output ]  Configuration
            this.objectOutputStream = new ObjectOutputStream ( this.client.getOutputStream() ) ;
            this.objectInputStream  = new ObjectInputStream  ( this.client.getInputStream () ) ;
            this.secretKey = Encrypt.getKey( this.SERIAL_CODE ) ;
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
            long accountNumber = Long.parseLong(Objects.requireNonNull(Encrypt.decryptMessage(this.secretKey, String.valueOf(this.objectInputStream.readObject()))));
            String password = Encrypt.decryptMessage( this.secretKey , (String) this.objectInputStream.readObject());
            if ( ! makeAuthentication( accountNumber , password ) ) { System.out.println("[!] Login Operation Failed ....") ; this.handelLogin() ; }
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
                this.objectOutputStream.writeObject( true );
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
                this.objectOutputStream.writeObject( false );
                return false  ;
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return false  ;
    }
    private boolean handelTransferringSystem() {
        System.out.println("[ % ] Start Transferring System ....");
        try {
            SecretKey secretKeyForTransferringSystem = Encrypt.getKey( this.clientModel.getSerialCode() ) ;
            long amount = Long.parseLong(Objects.requireNonNull(Encrypt.decryptMessage(secretKeyForTransferringSystem, String.valueOf(this.objectInputStream.readObject()))));
            long accountNumber = Long.parseLong(Objects.requireNonNull(Encrypt.decryptMessage(secretKeyForTransferringSystem, String.valueOf(this.objectInputStream.readObject()))));
            String reasonMessage = Encrypt.decryptMessage( secretKeyForTransferringSystem , String.valueOf(this.objectInputStream.readObject())) ;

            // Amount > Current Deposit
           if ( clientModel.getDeposit() < amount ) {
               this.objectOutputStream.writeObject( false ) ;
               System.out.println("[ @ ] Transferring System Stop => ( Amount > Deposit ) ... ") ;
               return this.handelTransferringSystem() ;
           }

           if ( makeTransferring( amount , accountNumber ) ) {
               this.objectOutputStream.writeObject( true )  ;
               System.out.println("[ (- ] Transferring System Completed ... ");
               // Check if customer need to do more transferring
               boolean more = (boolean) this.objectInputStream.readObject();
               if ( more ) {
                   System.out.println("[ - ] Restart Transferring System For Another Transfer Operation ... ");
                   return this.handelTransferringSystem();
               }else {
                   System.out.println("[ * ] Transferring System End ... ");
                   return true;
               }
           } else {
               System.out.println("[ )- ] Transferring System Fail...");
               this.objectOutputStream.writeObject( false ) ;
           }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this.handelTransferringSystem() ;
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
