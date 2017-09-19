/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ChatServer_ServerSide {



    public static String adminPassword;
    public static final String rawPassword="password";
    public static final String derbyServerLocation = "D:\\Chat Messenger\\Database Software (Derby)\\bin";
    public static Initialisation_ServerSide init;
    public static void main(String[] args) {
        //Starts the database
/*        Thread thread = new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "startNetworkServer.bat");
                File dir = new File(derbyServerLocation);
                pb.directory(dir);
                Process p = pb.start();
                System.out.println("Database started");
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("    Database Feed: " + line);
                    if(line.equals(" java.net.BindException: Address already in use: JVM_Bind")){
                        System.out.println("Already Running!");
                        System.exit(1);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ChatServer_ServerSide.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        thread.start();*/
        //  Thread.sleep(1000);


        /*Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "stopNetworkServer.bat");
                File dir = new File(derbyServerLocation);
                pb.directory(dir);
                pb.start();
                System.out.println("Database Stopped");
            } catch (IOException ex) {
                Logger.getLogger(ChatServer_ServerSide.class.getName()).log(Level.SEVERE, null, ex);
            }
        }));*/

/*        SecureRandom rand = new SecureRandom();
        byte[] salt = new byte[16];
        rand.nextBytes(salt);
        System.out.println(Base64.getEncoder().encodeToString(salt));*/
        String strSalt="Y+2SEbJCWXjwwVyHbTj3pg==";
        byte[] salt=Base64.getDecoder().decode(strSalt);
        AsymetricKey_ServerSide ak=new AsymetricKey_ServerSide();
        adminPassword=ak.genAdminPassword(rawPassword.toCharArray(),salt);



        DatabaseManagement db = new DatabaseManagement();
        db.connect();
       // db.clearUsers();
        //db.createTable();
        System.out.println("Connected");
        init = new Initialisation_ServerSide(db);
        Thread initServer = new Thread(init);
        //  logger.info("Listening Server Started");
          initServer.start();

    }

}
