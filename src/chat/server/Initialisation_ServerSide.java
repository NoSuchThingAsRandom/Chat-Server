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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Initialisation_ServerSide implements Runnable {

    private static Logger logger;
    private static DatabaseManagement db;
    private static Initialisation_ServerSide init;
    
    public Initialisation_ServerSide(DatabaseManagement dm) {
        init=ChatServer_ServerSide.init;
        db = dm;
    }

    public static void createLogger(String name) {
        try {
            logger = Logger.getLogger(name);
            FileHandler fh;
            fh = new FileHandler("Logs/" + name + ".log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.setLevel(logger.getLevel());
            logger.info("Log Folder Created");
        } catch (IOException ex) {
            logger.severe((Supplier<String>) ex);
        } catch (SecurityException ex) {
            logger.severe((Supplier<String>) ex);
        }
    }

    private static final int portNumber = 4000;

    public static void Server() {
        try {
            ExecutorService executor = Executors.newFixedThreadPool(5);
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("Listening server");
            while (true) {
                System.out.println("\n\nSERVER STARTED__________");
                Socket socket = serverSocket.accept();
                System.out.println("Connected");

                ServerThread st = new ServerThread(new PrintWriter(socket.getOutputStream(), true), db,init);
                executor.execute(st);
                System.out.println("Thread started");

                socket.close();
                System.out.println("Closed");
                //  break;
            }
        } catch (IOException ex) {
            System.out.println("Can't listen on current port.\n" + ex);
            Logger.getLogger(Initialisation_ServerSide.class.getName()).log(Level.SEVERE, null, ex);
            // logger.severe((Supplier<String>) ex);
            System.exit(1);
        }
    }

    @Override
    public void run() {
        // createLogger("InitServer");
        Server();
    }

}
