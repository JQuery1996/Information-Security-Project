package MyDataBase;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MySqlManager {
    private final String USERNAME        = "root"        ;
    private final String PASSWORD        = "root"        ;
    private final String  DATABASE_NAME  = "bank"        ;
    private final String  SERVER_NAME    = "localhost"   ;
    private final int  PORT_NUMBER       = 3306          ;
    private Connection connection ;

    public MySqlManager() {
        try {
            MysqlDataSource mysqlDataSource = new MysqlDataSource() ;
            mysqlDataSource.setUser(this.USERNAME);
            mysqlDataSource.setPassword(this.PASSWORD);
            mysqlDataSource.setDatabaseName(this.DATABASE_NAME);
            mysqlDataSource.setServerName(this.SERVER_NAME);
            mysqlDataSource.setPortNumber(this.PORT_NUMBER);
            this.connection = mysqlDataSource.getConnection() ;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet checkAuthentication( long accountNumber , String password ) {
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement("SELECT * FROM bank.`client` where account_number = ? and password = ?") ;
            preparedStatement.setLong( 1 , accountNumber );
            preparedStatement.setString( 2 , password );
            return preparedStatement.executeQuery() ;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null ;
    }


    public boolean setKeys( long accountNumber , String publicKey , String privateKey ) {
        try {
            PreparedStatement statement = this.connection.prepareStatement("UPDATE bank.`client` SET public_key = ? , private_key = ? WHERE account_number = ? ") ;
            statement.setString( 1 , publicKey );
            statement.setString( 2 , privateKey );
            statement.setLong  ( 3 , accountNumber);
            statement.executeUpdate() ;
            return true ;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false ;
    }

    public ResultSet getClientByAccountNumber( long accountNumber ) {
        try {
            PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM bank.`client` where account_number = ?") ;
            statement.setLong( 1 , accountNumber );
            return statement.executeQuery() ;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null ;
    }


    public boolean makeTransfer( long From , long To  , long FromNewDeposit , long ToNewDeposit ) {
        try{
            this.connection.setAutoCommit( false );
            PreparedStatement fromStatement = this.connection.prepareStatement("UPDATE bank.`client` SET deposit = ? WHERE account_number = ? ") ;
            PreparedStatement toStatement = this.connection.prepareStatement("UPDATE bank.`client` SET deposit = ? WHERE account_number = ? ") ;
            fromStatement.setLong(1 , FromNewDeposit );
            fromStatement.setLong( 2 , From );
            toStatement.setLong( 1 , ToNewDeposit );
            toStatement.setLong( 2 , To );
            fromStatement.executeUpdate() ;
            toStatement.executeUpdate() ;
            this.connection.commit();
            return true  ;
        } catch (SQLException e) {
            try {
                this.connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
        return false ;
    }




}
