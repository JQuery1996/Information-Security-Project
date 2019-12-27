package MessageStucture;

import java.io.Serializable;

public class MessageBody implements Serializable {
    private String  uniqueId ;
    private String  accountNumber ;
    private String amount ;
    private String reasonMessage ;

    public MessageBody() {}
    public MessageBody(String uniqueId, String accountNumber, String amount, String reasonMessage) {
        this.uniqueId = uniqueId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.reasonMessage = reasonMessage;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getReasonMessage() {
        return reasonMessage;
    }

    public void setReasonMessage(String reasonMessage) {
        this.reasonMessage = reasonMessage;
    }

    @Override
    public String toString() {
        return this.getUniqueId() + '_' + this.getAccountNumber() + '_' + this.getAmount()+ '_' + this.getReasonMessage() ;
    }
}
