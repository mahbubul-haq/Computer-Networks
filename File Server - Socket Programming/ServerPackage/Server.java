package ServerPackage;

import ClientPackage.Client;
import javafx.util.Pair;
import util.NetworkUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    public long max_buffer_size = 1024L * 1024L * 1024L * 1024L;
    public long id = 0;//file upload id
    public long min_chunk_size = 1024, max_chunk_size=1024 * 1024L;
    private ServerSocket serverSocket;
    public HashMap<String, Pair<Client,NetworkUtil> > getClient;//id-client-nc
    public HashSet<String > allUsers;
    public HashSet<String> activeUsers;
    public HashMap<String, Pair<ArrayList<String>, ArrayList<String>>> uploadedFiles;//id-private-public;
    public HashMap<Long, Pair<String, String>> getStudentIdFileType;///fileID - studntId - public/private
    public HashMap<Long, Pair<String, Long>> getFileInfo;//id-fileName-filesize
    public HashMap<Long, List<byte[]>> getChunks;///chunks of the file

    public long fileRequestId = 0;
    public HashMap<Long, String> requestIdStudent;
    public HashMap<String, List<String>> messages;

    public long getFileRequestId()
    {
        fileRequestId++;
        requestIdStudent.remove(fileRequestId - 1000);
        if (fileRequestId >= Long.MAX_VALUE) fileRequestId = 0;
        return fileRequestId;
    }

    public long getFileId()
    {
        id++;
        if (id >= Long.MAX_VALUE) id = 0;
        return id;
    }

    Server() {
        getClient = new HashMap<>();
        allUsers = new HashSet<>();
        activeUsers = new HashSet<>();
        getStudentIdFileType = new HashMap<>();
        getFileInfo = new HashMap<>();
        getChunks = new HashMap<>();
        uploadedFiles = new HashMap<>();
        requestIdStudent = new HashMap<>();
        messages = new HashMap<>();

        try {
            serverSocket = new ServerSocket(33333);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                serve(clientSocket);
            }
        } catch (Exception e) {
            System.out.println("Server starts:" + e);
        }
    }

    public void serve(Socket clientSocket)
    {
        NetworkUtil nc = new NetworkUtil(clientSocket);

        Object o = nc.read();

        if (o instanceof Long)
        {
            try {
                nc.getSocket().setSoTimeout(30000);
                long fileId = (long) o;
                new FileUploadThread(this, nc, fileId);
                return;
            }catch (Exception error)
            {
                error.printStackTrace();
            }
            return;
        }

        Client client = (Client) o;

        if (activeUsers.contains(client.name))
        {
            nc.write("The student ID has active login from another device.");
            return;
        }
        else if (!allUsers.contains(client.name))
        {
            makeFolder(client.name);
            uploadedFiles.put(client.name, new Pair<>(new ArrayList<>(), new ArrayList<>()));
            System.out.println("New user " + client.name + " is added");
        }
        else
        {
            System.out.println("User " + client.name + " reconnected");
        }

        allUsers.add(client.name);
        activeUsers.add(client.name);
        getClient.put(client.name, new Pair<>(client, nc));


        new WriteThreadServer(this, "Server", client);

        new ReadThreadServer(this, nc, client);
    }

    public static void makeFolder(String id)
    {
        String dir = System.getProperty("user.dir");

        dir += "/Files";
        File dirr = new File(dir);

        if (!dirr.exists()) dirr.mkdirs();

        dir += "/" + id;

        dirr = new File(dir);
        if (!dirr.exists()) dirr.mkdirs();

        String pub = dir + "/public";
        dirr = new File(pub);
        if (!dirr.exists()) dirr.mkdirs();

        dirr = new File(dir + "/private");
        if (!dirr.exists()) dirr.mkdirs();
    }

    public static void main(String args[]) {
        Server server = new Server();
    }
}
