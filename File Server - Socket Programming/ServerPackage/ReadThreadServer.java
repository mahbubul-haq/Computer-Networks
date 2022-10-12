package ServerPackage;

import ClientPackage.Client;
import javafx.util.Pair;
import sun.nio.cs.ext.MacHebrew;
import util.NetworkUtil;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReadThreadServer implements Runnable {
    private Thread thr;
    private NetworkUtil nc;
    Server server;
    Client client;

    public ReadThreadServer(Server server, NetworkUtil nc, Client client) {
        this.server = server;
        this.client = client;
        this.nc = nc;
        this.thr = new Thread(this);
        thr.start();
    }



    public boolean checkBufferSpace(long fileSize)
    {
        long curBuffer = 0;
        for (Pair<String, Long> p : server.getFileInfo.values())
        {
            curBuffer += p.getValue();
        }

        if (curBuffer + fileSize <= server.max_buffer_size) return true;

        return false;
    }

    public long getRandomChunkSize()
    {
        return server.min_chunk_size + (long) (Math.random() * (server.max_chunk_size - server.min_chunk_size));
    }

    public static List<byte[]> splitFile(File file, long chunkSize) throws IOException///download request
    {
        List<byte[]> files = new ArrayList<>();
        FileInputStream fis;
        String fname = file.getName();
        FileOutputStream chunk;
        int fileSize = (int) file.length();
        int initFileSize= fileSize;
        int nChunks = 0, read = 0, readLength = (int)chunkSize;

        byte[] byteChunk;
        try {
            fis = new FileInputStream(file);
            int numOfchunks = 0;
            while (fileSize > 0)
            {
                numOfchunks++;
                if (fileSize <= chunkSize) {
                    readLength = fileSize;
                }
                byteChunk = new byte[readLength];
                read = fis.read(byteChunk, 0, readLength);
                fileSize -= read;
                assert (read == byteChunk.length);

               /// System.out.println(read + " " + byteChunk.length + " " + initFileSize);

                files.add(byteChunk);
            }

            ///System.out.println(numOfchunks);
            fis.close();
            fis = null;
        }
        catch (IOException error)
        {
            error.printStackTrace();
        }

        return files;
    }

    public static String getFileName(String stdId, String fileType, String fileName)//during download request
    {
        String dir = System.getProperty("user.dir");

        dir += "/Files";
        File dirr = new File(dir);

        if (!dirr.exists()) dirr.mkdirs();

        dir += "/" + stdId;

        dirr = new File(dir);
        if (!dirr.exists()) dirr.mkdirs();

        dir += "/" + fileType;
        dirr = new File(dir);
        if (!dirr.exists()) dirr.mkdirs();

        dir += "/" + fileName;
        return dir;
    }

    public void run() {
        try {
            while (true) {

                //checkUserSockets();

                Object o = nc.read();
                if (o != null)
                {
                    if (o instanceof Client)
                    {

                    }
                    else if (o instanceof Pair)
                    {
                        ///System.out.println("pair_ser");
                        String input = (String)((Pair<?, ?>) o).getKey();
                        String [] arra = input.split(" ");
                        if (input.equalsIgnoreCase("fileSendRequest public") ||
                                input.equalsIgnoreCase("fileSendRequest private"))//fileSendRequest
                        {
                            String type = null;
                            if (input.equalsIgnoreCase("fileSendRequest public"))
                                type = "public";
                            else type = "private";
                            ///System.out.println("pair1");

                            Pair<String, Long> pair = (Pair)((Pair<?, ?>) o).getValue();
                            ///System.out.println("pair2");
                            boolean available = checkBufferSpace(pair.getValue());
                            ///System.out.println("pair3");

                            if (available)
                            {
                               /// System.out.println("available");
                                long chunkSize = getRandomChunkSize();
                                long fileId = server.getFileId();

                                server.getStudentIdFileType.put(fileId, new Pair<>(client.name, type));
                                server.getFileInfo.put(fileId, new Pair<>(pair.getKey(), pair.getValue()));
                                server.getChunks.put(fileId, new ArrayList<>());
                                nc.write(new Pair<>("allowFileTransmission", new Pair(chunkSize, fileId)));//file1 means
                            }
                            else
                            {
                                nc.write("Exceeds server buffer!");
                            }

                        }
                        else if (arra[0].equalsIgnoreCase("requestedFile"))
                        {
                            Pair<String, Long> pair = (Pair)((Pair<?, ?>) o).getValue();
                           /// System.out.println("pair2");
                            boolean available = checkBufferSpace(pair.getValue());
                           /// System.out.println("pair3");

                            long requestId = Long.parseLong(arra[1]);

                            if (available)
                            {
                               /// System.out.println("available");
                                long chunkSize = getRandomChunkSize();
                                long fileId = server.getFileId();

                                server.getStudentIdFileType.put(fileId, new Pair<>(client.name, "public"));
                                server.getFileInfo.put(fileId, new Pair<>(pair.getKey(), pair.getValue()));
                                server.getChunks.put(fileId, new ArrayList<>());
                                nc.write(new Pair<>("allowFileTransmission", new Pair(chunkSize, fileId)));//file1 means

                                String requester = server.requestIdStudent.get(requestId);
                                if (requester != null)
                                {
                                    if (server.messages.get(requester) == null) server.messages.put(requester, new ArrayList<>());
                                    server.messages.get(requester).add(client.name + " has uploaded your requested file.");
                                }
                            }
                            else
                            {
                                nc.write("Exceeds server buffer!");
                            }
                        }
                    }
                    else if (o instanceof String) {
                        String input = (String) o;
                        String []arr = input.split(" ");

                        if (input.equalsIgnoreCase("students"))
                        {
                            nc.write(new Pair<>("students", new Pair<>(server.allUsers, server.activeUsers)));
                        }
                        else if (input.equalsIgnoreCase("close"))
                        {
                            ///System.out.println(client.name);
                            server.activeUsers.remove(client.name);
                            System.out.println(client.name + " has been disconnected");
                            nc.getSocket().close();
                        }
                        else if (input.equalsIgnoreCase("myfiles"))
                        {
                            nc.write(new Pair<>("yourFiles", server.uploadedFiles.get(client.name)));
                        }
                        else if (arr[0].equalsIgnoreCase("showFiles")) {
                            if (arr.length > 1) {
                                if (server.uploadedFiles.get(arr[1]) != null)
                                    nc.write(new Pair<>("showFiles", new Pair(arr[1], server.uploadedFiles.get(arr[1]).getValue())));

                            }
                        }
                        else if (arr[0].equalsIgnoreCase("download"))
                        {
                            String stId = arr[1];
                            String fileName = arr[2];

                            String fName = getFileName(stId, "public", fileName);
                            File file = new File(fName);

                            if (!file.exists() && stId.equalsIgnoreCase(client.name))
                            {
                                fName = getFileName(stId, "private", fileName);
                                file = new File(fName);
                            }

                            if (file.exists())
                            {
                                List<byte[]> files = splitFile(file, server.max_chunk_size);
                                for (byte[] chunk : files)
                                {
                                    nc.write(new Pair<>("newChunk", new Pair(fileName, chunk)));
                                }
                                nc.write("all chunks are sent");
                                if (server.messages.get(client.name) == null) server.messages.put(client.name, new ArrayList<>());
                                server.messages.get(client.name).add("File Download Complete");

                            }
                            else {
                                nc.write("File not found");
                            }

                        }
                        else if (arr[0].equalsIgnoreCase("request"))
                        {
                            String []ara = input.split(" ", 2);

                            long id = server.getFileRequestId();

                            for (String s : server.allUsers)
                            {
                                if (!s.equalsIgnoreCase(client.name))
                                {
                                    if (server.messages.get(s) == null) server.messages.put(s, new ArrayList<>());
                                    server.messages.get(s).add("File request Id: " + id + ", Description: " + ara[1]);
                                }
                            }

                            server.requestIdStudent.put(id, client.name);
                        }
                        else if (input.equalsIgnoreCase("messages"))
                        {
                            if (server.messages.get(client.name) == null || server.messages.get(client.name).size() == 0)
                            {
                                nc.write("No messages for you!");
                            }
                            else
                            {
                                nc.write(new Pair<>("messages", server.messages.get(client.name)));
                                server.messages.put(client.name, new ArrayList<>());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            nc.closeConnection();
        }
    }
}



