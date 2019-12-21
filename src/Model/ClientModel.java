package Model;

public class ClientModel {
    private long id             ;
    private String firstName    ;
    private String lastName     ;
    private long accountNumber  ;
    private long deposit        ;
    private String publicKey    ;
    private String privateKey   ;
    private String serialCode  ;
    private String password     ;

    public ClientModel() {
    }

    public ClientModel(long id, String firstName, String lastName, long accountNumber, long deposit, String publicKey, String privateKey, String serialCode, String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.accountNumber = accountNumber;
        this.deposit = deposit;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.serialCode = serialCode ;
        this.password = password;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public long getDeposit() {
        return deposit;
    }

    public void setDeposit(long deposit) {
        this.deposit = deposit;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getSerialCode() {
        return serialCode;
    }

    public void setSerialCode(String serialCode) {
        this.serialCode = serialCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void printClientModel() {
        System.out.println("id = " + this.id );
        System.out.println("First Name = " + this.firstName );
        System.out.println("Last Name = " + this.lastName );
        System.out.println("Account Number = " + this.accountNumber );
        System.out.println("Deposit = " + this.deposit );
        System.out.println("Public Key = " + this.publicKey );
        System.out.println("Private Key = " + this.privateKey );
        System.out.println("Constant key = " + this.serialCode  );
        System.out.println("Password = " + this.password );
    }
}
