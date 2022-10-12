package ClientPackage;

import util.NetworkUtil;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
Different commands to perform tasks(in client):

students                - To see all the students
upload a public file    - To upload a public file
upload a private file   - for private file
close                   - To close the connection///due to inputstream.read() gets stuck
myfiles                 - to see own files
showFiles studentId     - to see the public files of a particular students
download studentId fileName - to download a file from server. To generalize task for downloaing
            own files & others files for downloading own files he will have to give his studentId.
            if it is his file then fileName will be searched in private and public both folder.
request fileDescription - to request for a file
messages                - to see the messages
*/

public class Client implements Serializable {
    public String name;
    public File fileToSend;
    List<byte[]> chunks;
    public String fileToReceive;

    public Client(String serverAddress, int serverPort) {
        try {
            System.out.print("Please enter your student ID: ");
            Scanner scanner = new Scanner(System.in);
            name = scanner.nextLine();
            NetworkUtil networkUtil = new NetworkUtil(serverAddress, serverPort);
            networkUtil.write(this);

            new ReadThreadClient(this, networkUtil);
            new WriteThreadClient(this, networkUtil);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String args[]) {
        String serverAddress = "127.0.0.1";
        int serverPort = 33333;
        Client client = new Client(serverAddress, serverPort);
    }
}

