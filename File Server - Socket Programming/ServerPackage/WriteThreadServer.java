package ServerPackage;

import javafx.util.Pair;
import util.NetworkUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.StringTokenizer;
import ClientPackage.Client;

public class WriteThreadServer implements Runnable {

    private Thread thr;
    String name;
    Server server;
    Client client;

    public WriteThreadServer(Server server, String name, Client client) {
        this.client = client;
        this.server = server;
        this.name = name;
        this.thr = new Thread(this);
        thr.start();
    }

    public void run() {
        try {
            Scanner input = new Scanner(System.in);

            while (true)
            {
                String s = input.nextLine();

            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}



