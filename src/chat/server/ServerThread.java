/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author samra
 */
public class ServerThread implements Runnable {

    DatabaseManagement db;
    Initialisation_ServerSide init;
    private String sessKey;
    private String messKey;
    private String username;
    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter out;
    ;
    private BufferedReader in;
    private AsymetricKey_ServerSide ak;
    private boolean loggedIn = false;

    public ServerThread(PrintWriter out, DatabaseManagement dbA, Initialisation_ServerSide is) throws IOException {
        serverSocket = new ServerSocket(0);
        System.out.println("Listening on port " + serverSocket.getLocalPort());
        out.println(serverSocket.getLocalPort());
        out.close();
        db = dbA;
        init = is;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                db.logOut(username);
                socket.close();
                out.close();
                in.close();
                System.out.println("Done");
            } catch (IOException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }));
        startUp();
    }

    public String getMsg() {
        try {
            String iv = in.readLine();
            String input = in.readLine();
            String msg = (ak.decryptMessage(input, sessKey, iv));
            System.out.println("Recieved Message: " + msg);
            return msg;
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void sendMsg(String msg) {
        System.out.println("Sending Message: " + msg);
        String[] toSend = ak.encryptMessage(msg, sessKey);
        out.println(toSend[0]);
        System.out.println(toSend[0]);
        out.println(toSend[1]);
        System.out.println(toSend[1]);
    }

    public void startUp() {
        try {
            socket = serverSocket.accept();
            System.out.println("Connected to client");
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            ak = new AsymetricKey_ServerSide();
            String pubKey = in.readLine();
            System.out.println("Pub key is " + pubKey);
            sessKey = ak.keyGen();
            System.out.println("Sess key is " + sessKey);
            out.println(ak.encryptKey(sessKey, pubKey));
            System.out.println("Sent keys");


            while (true) {
                String msg = getMsg();
                System.out.println("RECEIVED " + msg);
                if (msg.equals("//LOGIN//")) {
                    System.out.println("Log in attempt");
                    logIn();
                    if (loggedIn) {
                        db.setItem(username, "ACTIVE", true);
                        System.out.println("Startup complete");
                        mainFeatures();
                        db.setItem(username, "ACTIVE", false);
                        //       db.logOut(username);
                        System.out.println("NOT MEANT TO HAPPEN");
                        socket.close();
                        out.close();
                        in.close();
                        //   System.exit(1);
                    }
                } else if (msg.equals("//NEW_USER//")) {
                    newUser();
                } else {
                    System.out.println("UNKNOWN RESPONSE");
                }
            }


        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void mainFeatures() {
        System.out.println("Running Main Features");
        String msg;
        while ((msg = getMsg()) != null) {
            switch (msg) {
                case ("//NEW_MESSAGE//"):
                    System.out.println("New msg");
                    String name = getMsg();
                    String mess = getMsg();
                    System.out.println("The message to encrypt is: " + mess + "\nwith a key of: " + messKey);
                    String[] content = ak.encryptMessage(mess, messKey);
                    System.out.println("TEST: " + ak.decryptMessage(content[1], messKey, content[0]));
                    Timestamp time = new Timestamp(System.currentTimeMillis());
                    db.updateDate(username,name,time);
                    System.out.println("Received Message from: " + username + "\nSending message to: " + name);
                    if (db.newMessage(username, username, name, time, content[1],true, content[0])) {//Recipient
                        content = ak.encryptMessage(mess, ChatServer_ServerSide.adminPassword);
                        db.setItem(name, "NEW_MESSAGES", true);
                        if (db.newMessage(name + "_TEMP", username, username, time, content[1], false,content[0])) {//Sender
                            sendMsg("//SUCCESS//");
                            break;
                        }
                    }
                    sendMsg("//FAIL//");
                    break;
                case ("//LOAD_CHAT_NAMES//"):
                    System.out.println("Load chat names");
                    for (String s : db.loadChatNames(username)) {
                        sendMsg(s);
                    }
                    sendMsg("//DONE//");
                    break;
                case ("//LOAD_CHAT//"):
                    updateMessages();
                    System.out.println("Load chat");
                    String toLoad = getMsg();
                    ArrayList[] info = db.loadChat(username, toLoad);
                    String decoded = null;
                    for (int x = 0; x < info[0].size(); x++) {
                        decoded = ak.decryptMessage((String) info[1].get(x), messKey, (String) info[2].get(x));
                        System.out.println("The database message is: " + info[1].get(x) + "\nthe message key is: " + messKey + "\nand the iv is: " + info[2].get(x) + "\nand the decoded message is: " + decoded);
                        sendMsg(info[0].get(x) + decoded);
                    }
                    sendMsg("//DONE//");
                    break;
                case ("//NEW_CHAT//"):
                    System.out.println("New chat");
                    String user = getMsg();
                    System.out.println("The user to connect to is: " + user);
                    if (db.checkUsernameExists(user)) {
                        db.newChat(username, user, true);
                        sendMsg("//SUCCESS//");
                    } else {
                        sendMsg("//USER_NOT_FOUND//");
                    }
                break;
                case ("//CHECK_MESSAGES//"):
                    updateMessages();
                    System.out.println("New messages to load");
                    ArrayList<String> check = db.checkChats(username);
                    System.out.println("Chats with unread messages: ");
                    for (String s : check) {
                        System.out.println(s);
                        sendMsg(s);
                    }

                    sendMsg("//DONE//");
                    break;
                default:
                    System.out.println("//UNKNOWN MESSAGE");
            }


        }
    }

    private boolean updateMessages() {
        System.out.println("Checking for messages");
        if ((db.getItem(username, "NEW_MESSAGES")).toString().toLowerCase().equals("true")) {
            db.updateMessages(username, messKey);
            //   if(db.newMessages(username)){
            return true;
        }
        return false;
    }

    private void newUser() {
        System.out.println("New user");
        System.out.println("Sent confirmed");
        String name = getMsg();
        System.out.println("RECIEVED new name " + name);
        if (!db.checkUsernameExists(name)) {
            System.out.println("Name valid");
            sendMsg("//VALID//");
            System.out.println("Name valid");

            int len = Integer.parseInt(getMsg());
            char[] pass = new char[len];
            for (int x = 0; x < len; x++) {
                try {
                    String iv = in.readLine();
                    pass[x] = ak.decryptMessage(in.readLine(), sessKey, iv).charAt(0);
                } catch (IOException ex) {
                    Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (db.newUser(name, pass)) {
                sendMsg("//SUCCESS//");
                for (int x = 0; x < pass.length; x++) {
                    pass[x] = 0;
                }
                //   logIn();
            } else {
                System.out.println("NEW USER FAILED");
            }
        } else {
            System.out.println("NAME ALREADY TAKEN");
            sendMsg("//INVALID_NAME//");
        }
    }

    private void logIn() {
        String name = getMsg();
        if (db.checkUsernameExists(name)) {
            username = name;
            //Inputing the password
            int len = Integer.parseInt(getMsg());
            char[] pass = new char[len];
            for (int x = 0; x < len; x++) {
                try {
                    String iv = in.readLine();
                    pass[x] = ak.decryptMessage(in.readLine(), sessKey, iv).charAt(0);
                } catch (IOException ex) {
                    Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }


            byte[] salt = Base64.getDecoder().decode((String) db.getItem(name, "SALT"));
            byte[][] keys = ak.genPasswords(pass, salt);
            String password = Base64.getEncoder().encodeToString(keys[1]);
            String unlockKey = Base64.getEncoder().encodeToString(keys[0]);
            //Clearing the password
            for (int x = 0; x < len; x++) {
                pass[x] = 0;
            }
            System.out.println("RECEIVED log in name: " + name);
            if (db.logIn(name, password)) {
                byte[] iv = Base64.getDecoder().decode((String) db.getItem(name, "IV"));
                messKey = ak.decryptMessage((String) db.getItem(username, "MESSAGE_KEY"), unlockKey, (String) db.getItem(name, "IV"));
                System.out.println("The message key is: " + messKey);
                System.out.println("Password success");
                loggedIn = true;
                sendMsg("//SUCCESSFUL//");

            } else {
                System.out.println("Invalid login");
                sendMsg("//INVALID_LOGIN//");
            }

        } else if (name.equals("//NEW_USER//")) {
            newUser();
        } else if (name.equals("//LOGIN//")) {
            logIn();
        } else {
            System.out.println("Invalid login");
            sendMsg("//INVALID_LOGIN//");

        }
        /*
        String iv = in.readLine();
        String name = ak.DecryptMessage(in.readLine(), sessKey, iv);
        iv = in.readLine();
        String pass = ak.DecryptMessage(in.readLine(), sessKey, iv);
        if (db.logIn(name, pass) == false) {
            sendMsg("Log In Failed");
        } else {
            System.out.println("Logged in");
        }*/

    }

}
