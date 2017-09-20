/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat.server;



import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author samra
 */
public class DatabaseManagement {

    private static Connection con = null;

    public void connect() {
        //String host = "jdbc:odbc://";
        ///D:Chat Messenger//Server; create=true";
        String host = "jdbc:derby://localhost:1527/D:/Chat Messenger/Database;create=true;";
        String userName = "Administator";
        String conPassword = "password";
        String sqlCode = "SELECT * FROM ADMINISTATOR.USERS";
        try {
            System.out.println("Connecting to database..");
            //   con = DriverManager.getConnection(host);
            con = DriverManager.getConnection(host, userName, conPassword);
            System.out.println("Connected");
/*            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = statement.executeQuery(sqlCode);
            System.out.println("Sent statement");
            while (rs.next()) {
                System.out.println("Received");
                int id_col = rs.getInt("ID");
                String username = rs.getString("USERNAME");
                //String password = rs.getString("PASSWORD");
                System.out.println(id_col + " " + username);
            }
            rs.close();*/
            System.out.println("Done");
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(DatabaseManagement.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Can't connect to database");
            System.exit(1);
        }
    }
//"jdbc:derby://localhost:1527/D:/Chat Messenger/Database"


    public Object getItem(String username, String item) {
        try {
            PreparedStatement ps = con.prepareStatement("SELECT " + item + " FROM USERS WHERE USERNAME=? ");
            ps.setString(1, username);
            ResultSet results = ps.executeQuery();
            if (results.next()) {
                return results.getString(1);
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }


    public void updateDate(String user, String groupID, Timestamp date){
        try {
            con.prepareStatement("UPDATE USERS."+user+"_CHATS SET LATEST_MESSAGE='"+date+"' WHERE GROUP_ID='"+groupID+"'").execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean setItem(String username, String item, Object value) {
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE USERS SET " + item + "=? WHERE USERNAME =?");
            ps.setObject(1,value);
            ps.setString(2,username);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean logOut(String username) {
        try {
            con.createStatement().execute("UPDATE USERS SET ACTIVE =FALSE WHERE USERNAME ='" + username + "'");
            return true;
/*            Statement statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = statement.executeQuery("SELECT * FROM USERS");
            while (rs.next()) {
                if (username.equals(rs.getString("USERNAME"))) {
                    rs.updateBoolean("ACTIVE", false);
                    rs.insertRow();
                    System.out.println("LOGGED OUT");
                    statement.close();
                    rs.close();
                    return true;
                }
            }*/
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("FAILED TO LOG OUT");
        return false;
    }

    public boolean logIn(String username, String password) {
        try {
            Statement statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = statement.executeQuery("SELECT * FROM USERS");
            while (rs.next()) {
                if (username.equals(rs.getString("USERNAME"))) {
                    if (rs.getString("CHECK_KEY").equals(password)) {
                        // rs.updateBoolean("ACTIVE", true);
                        con.createStatement().execute("UPDATE USERS SET ACTIVE =TRUE WHERE USERNAME ='" + username + "'");
                        System.out.println("Logged in (DB CLASS, " + rs.getBoolean("ACTIVE"));
                        statement.close();
                        rs.close();
                        return true;
                    }
                }

            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean checkUsernameExists(String username) {
        try {
            Statement statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = statement.executeQuery("SELECT * FROM ADMINISTATOR.USERS");
            System.out.println("Checking if " + username + " exists");
            while (rs.next()) {
                System.out.println("The current username is: " + rs.getString("USERNAME"));
                if (username.equals(rs.getString("USERNAME"))) {
                    return true;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean newUser(String username, char[] password) {
        try {
            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = statement.executeQuery("SELECT * FROM USERS");
            int last = 1;
            if (rs.last()) {
                last = rs.getInt("ID") + 1;
            }
            AsymetricKey_ServerSide as = new AsymetricKey_ServerSide();
            String[] data = as.createDBKey(password);
            for (int x = 0; x < password.length; x++) {
                password[x] = 0;
            }
            rs.moveToInsertRow();
            rs.updateInt("ID", last);
            rs.updateString("USERNAME", username);
            rs.updateString("IV", data[0]);
            rs.updateString("MESSAGE_KEY", data[1]);
            rs.updateString("PUBLIC MESSAGE_KEY", data[1]);
            rs.updateString("SALT", data[2]);
            rs.updateString("CHECK_KEY", data[3]);
            rs.updateBoolean("NEW_MESSAGES", false);

            rs.insertRow();
            statement.close();
            rs.close();


            String createMessages = "CREATE TABLE USERS." + username + "_MESSAGES ("
                    + "MESSAGE_ID BIGINT,"
                    + "GROUP_ID VARCHAR(16) not NULL, "
                    + "SENDER_ID VARCHAR(16),"
                    + "TIME TIMESTAMP,"
                    + "SEEN BOOLEAN,"
                    + "CONTENT LONG VARCHAR,"
                    + "IV VARCHAR(24),"
                    + "PRIMARY KEY(MESSAGE_ID))";
            String createTempMessages = "CREATE TABLE USERS." + username + "_TEMP_MESSAGES ("
                    + "MESSAGE_ID BIGINT,"
                    + "GROUP_ID VARCHAR(16) not NULL, "
                    + "SENDER_ID VARCHAR(16),"
                    + "TIME TIMESTAMP,"
                    + "SEEN BOOLEAN,"
                    + "CONTENT LONG VARCHAR,"
                    + "IV VARCHAR(24),"
                    + "PRIMARY KEY(MESSAGE_ID))";


            String createChats = "CREATE TABLE USERS." + username + "_CHATS ("
                    + "ID INTEGER,"
                    + "GROUP_ID VARCHAR (16) not NULL, "
                    + "LATEST_MESSAGE TIMESTAMP NOT NULL,"
                    // + "CHAT_KEY VARCHAR(44) NOT NULL,"
                    + "PRIMARY KEY(ID))";
            Statement s = con.createStatement();
            s.executeUpdate(createMessages);
            s.executeUpdate(createTempMessages);
            s.executeUpdate(createChats);
        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            System.out.println("Username already exists");
            return false;
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseManagement.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public ArrayList<String> loadChatNames(String user) {
        try {
            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = statement.executeQuery("SELECT * FROM USERS." + user + "_CHATS ORDER BY LATEST_MESSAGE DESC");
            ArrayList<String> chatNames = new ArrayList();
            ArrayList<Timestamp> times = new ArrayList();
//            rs.absolute(0);
            while (rs.next()) {
              //  times.add(rs.getTimestamp("LATEST_MESSAGE"));
                chatNames.add(rs.getString("GROUP_ID"));
                System.out.println(rs.getString("GROUP_ID"));
            }
            /*ArrayList<Integer> scores = new ArrayList();
            while (!times.isEmpty()) {
                Timestamp highest = new Timestamp(0L);
                int count = 0;
                int top = 0;
                for (Timestamp t : times) {
                    if (t.after(highest)) {
                        highest = t;
                        top = count;
                    }
                    count++;
                }
                scores.add(top);
                times.remove(top);
            }
            ArrayList<String> ordered = new ArrayList<>();
            for (int x : scores) {
                System.out.println(chatNames.get(x));
                ordered.add(chatNames.get(x));
            }
            return ordered;*/
            return chatNames;
/*
            String sql = "SELECT GROUP_ID FROM USERS." + user + "_CHATS";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs2 = ps.executeQuery();
            rs2 = ps.executeQuery();

            while (rs2.next()) {

                System.out.println(rs2.getString(1));
            }
            return new ArrayList<String>();
        */
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseManagement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public ArrayList[] loadChat(String user, String groupID) {
        Statement statement = null;
        try {
            statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = statement.executeQuery("SELECT * FROM USERS." + user + "_MESSAGES WHERE GROUP_ID='" + groupID + "'");
            ArrayList<String> messages = new ArrayList<>();
            ArrayList<String> senders = new ArrayList<>();
            ArrayList<String> salt = new ArrayList<>();
            while (rs.next()) {
                senders.add(rs.getString("SENDER_ID") + ": ");
                messages.add(rs.getString("CONTENT"));
                salt.add(rs.getString("IV"));
            }
            System.out.println("Set seen true");
            con.prepareStatement("UPDATE USERS."+user+"_MESSAGES SET SEEN='TRUE' WHERE GROUP_ID='"+groupID+"' AND SEEN=FALSE").execute();


            System.out.println("\nUPDATE USERS."+user+"_MESSAGES SET SEEN='TRUE' WHERE GROUP_ID='"+groupID+"' AND SEEN='FALSE'\n" +
                    "Set seen true\n");

            rs.close();
            statement.close();
            return new ArrayList[]{senders, messages, salt};
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<String> checkChats(String user) {
        Statement statement = null;
        try {
            statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = statement.executeQuery("SELECT * FROM USERS."+user+"_CHATS");
            ArrayList<String> users = new ArrayList<>();
            while (rs.next()) {
                if (getNewMessages(user, rs.getString("GROUP_ID"))) {
                    users.add(rs.getString("GROUP_ID"));
                }
            }
            return users;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<String>();
    }

    public boolean getNewMessages(String user, String groupID) {
        Statement statement = null;
        try {
            statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = statement.executeQuery("SELECT * FROM USERS." + user + "_MESSAGES WHERE GROUP_ID='" + groupID + "' AND SEEN=FALSE");
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateMessages(String user,String messKey){
        try {
            Statement setStatement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            Statement getStatement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet get = getStatement.executeQuery("SELECT * FROM USERS." + user+"_TEMP_MESSAGES");
            ResultSet set = setStatement.executeQuery("SELECT * FROM USERS." + user + "_MESSAGES");
            int last = 1;
            if (set.last()) {
                last = set.getInt("MESSAGE_ID") + 1;
            }
            AsymetricKey_ServerSide ak=new AsymetricKey_ServerSide();
            String[] content;
            while (get.next()) {
                content=ak.encryptMessage(ak.decryptMessage(get.getString("CONTENT"),ChatServer_ServerSide.adminPassword,get.getString("IV")),messKey);
                set.moveToInsertRow();
                set.updateInt("MESSAGE_ID", last);
                set.updateString("GROUP_ID", get.getString("GROUP_ID"));
                set.updateString("SENDER_ID", get.getString("GROUP_ID"));
                set.updateTimestamp("TIME", get.getTimestamp("TIME"));
                set.updateBoolean("SEEN", false);
                set.updateString("CONTENT", content[1]);
                set.updateString("IV", content[0]);
                set.insertRow();
                last++;
            }
            setStatement.close();
            getStatement.close();
            set.close();
            get.close();
            con.createStatement().execute("TRUNCATE TABLE USERS."+user+"_TEMP_MESSAGES");
            con.createStatement().execute("UPDATE USERS SET NEW_MESSAGES =FALSE WHERE USERNAME ='" + user + "'");


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean newMessages(String user) {
        try {
            return con.createStatement().execute("SELECT DISTINCT NEW_MESSAGES FROM USERS WHERE USERNAME ='" + user + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void newChat(String username, String attempt, boolean first) {
        try {
            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = statement.executeQuery("SELECT * FROM USERS." + username + "_CHATS");
            int last = 1;
            if (rs.last()) {
                last = rs.getInt("ID") + 1;
            }
            rs.moveToInsertRow();
            rs.updateInt("ID", last);
            rs.updateString("GROUP_ID", attempt);
            rs.updateTimestamp("LATEST_MESSAGE", new Timestamp(System.currentTimeMillis()));
            //    rs.updateString("CHAT_KEY",);
            rs.insertRow();
            statement.close();
            rs.close();
            if (first) {
                newChat(attempt, username, false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public boolean newMessage(String user, String sender, String groupID, Timestamp time, String content, Boolean seen, String iv) {
        Statement statement = null;
        try {
            statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = statement.executeQuery("SELECT * FROM USERS." + user + "_MESSAGES");
            int last = 1;
            if (rs.last()) {
                last = rs.getInt("MESSAGE_ID") + 1;
            }
            rs.moveToInsertRow();
            rs.updateInt("MESSAGE_ID", last);
            rs.updateString("GROUP_ID", groupID);
            rs.updateString("SENDER_ID", sender);
            rs.updateTimestamp("TIME", time);
            rs.updateBoolean("SEEN", seen);
            rs.updateString("CONTENT", content);
            rs.updateString("IV", iv);

            rs.insertRow();
            statement.close();
            rs.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }

    public void createTable() {
      /*  String createTable = "CREATE TABLE USERS "
                + "( MESSAGE_ID BIGINT,"
                + "GROUP_ID INTEGER not NULL, "
                + "SENDER_ID VARCHAR(16),"
                + "TIME TIMESTAMP,"
                + "CONTENT LONG VARCHAR,"
                + "PRIMARY KEY(MESSAGE_ID))";
      */
        String createTable = "CREATE TABLE USERS "
                + " (ID INTEGER NOT NULL,"
                + "USERNAME VARCHAR(16) NOT NULL,"
                + "IV VARCHAR(24) NOT NULL,"
                + "MESSAGE_KEY VARCHAR(44) NOT NULL,"
                + "SALT VARCHAR(24) NOT NULL,"
                + "CHECK_KEY VARCHAR(24) NOT NULL,"
                + "ACTIVE BOOLEAN,"
                + "NEW_MESSAGES BOOLEAN,"
                + "PRIMARY KEY (ID))";
        Statement s = null;
        try {
            s = con.createStatement();
            s.executeUpdate(createTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void clearUsers() {
        try {
            Statement statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = statement.executeQuery("SELECT * FROM ADMINISTATOR.USERS");
            while (rs.next()) {
                try {
                    con.createStatement().execute("DROP TABLE USERS." + rs.getString("USERNAME") + "_CHATS");
                    con.createStatement().execute("DROP TABLE USERS." + rs.getString("USERNAME") + "_MESSAGES");
                    con.createStatement().execute("DROP TABLE USERS." + rs.getString("USERNAME") + "_TEMP_MESSAGES");
                    System.out.println("The current username is: " + rs.getString("USERNAME"));
                } catch (SQLSyntaxErrorException ex) {
                    System.out.println(rs.getString("USERNAME") + " does not exist");
                }
            }
            rs.close();
            statement.close();
            con.createStatement().execute("TRUNCATE TABLE USERS");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
