package ClientPackage;

import javafx.util.Pair;
import util.NetworkUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ReadThreadClient implements Runnable {
    private Thread thr;
    private NetworkUtil nc;
    public Client client;

    public ReadThreadClient(Client client, NetworkUtil nc) {
        this.client = client;
        this.nc = nc;
        this.thr = new Thread(this);
        thr.start();
    }

    public static List<byte[]> splitFile(File file, long chunkSize) throws IOException
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

                ///System.out.println(read + " " + byteChunk.length + " " + initFileSize);

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

    public void makeFile(String fileName)///downloaded file
    {
        String dir = System.getProperty("user.dir");

        dir += "/Downloads";
        File dirr = new File(dir);

        if (!dirr.exists()) dirr.mkdirs();

        fileName = dir + "/" + fileName;
        ///System.out.println(fileName);

        File ofile = new File(fileName);

        FileOutputStream fos;
        FileInputStream fis;
        //byte[] fileBytes;
        int bytesRead = 0;
        try {
            fos = new FileOutputStream(ofile, true);
            for (byte[] fileBytes : client.chunks) {
                ///System.out.println("chunky");
                //fis = new FileInputStream(file);
                //fileBytes = new byte[(int) file.length()];
                //bytesRead = fis.read(fileBytes, 0, (int) file.length());
                //assert (bytesRead == fileBytes.length);
                //assert (bytesRead == (int) file.length());
                fos.write(fileBytes);
                fos.flush();
                fileBytes = null;
                //fis.close();
                //fis = null;
            }
            fos.close();
            fos = null;
        }
        catch (IOException error)
        {
            error.printStackTrace();
        }
    }

    public void run()
    {
        try
        {
            while (true)
            {
                //System.out.println("Is");
                Object o = nc.read();
                if (o != null)
                {
                    if (o instanceof String)
                    {
                        String s = (String)o;
                        if (s.equalsIgnoreCase("all chunks are sent"))
                        {
                            makeFile(client.fileToReceive);
                            client.fileToReceive = null;
                            client.chunks.clear();
                        }
                        else
                        {
                            System.out.println(s);
                        }
                    }
                    else if (o instanceof Pair)
                    {
                        String rep = (String) ((Pair<?, ?>) o).getKey();
                        ///System.out.println(rep + "wow");

                        if (rep.equalsIgnoreCase("students"))
                        {
                            ///System.out.println("here");
                            Pair<HashSet<String>, HashSet<String>> p = (Pair) ((Pair<?, ?>) o).getValue();
                            for (String s : p.getKey())
                            {
                                System.out.print(s);
                                if (s.equalsIgnoreCase(client.name))
                                {
                                    System.out.println("(you)");
                                    continue;
                                }
                                if (p.getValue().contains(s)) System.out.print("(online)");
                                else System.out.print("(offline)");
                                System.out.println();
                            }
                        }
                        else if (rep.equalsIgnoreCase("allowFileTransmission"))
                        {
                            Pair<Long, Long> p = (Pair)((Pair<?, ?>) o).getValue();
                            List<byte[]> files = splitFile(client.fileToSend, p.getKey());

                            NetworkUtil nc1 = new NetworkUtil("127.0.0.1", 33333);
                            nc1.getSocket().setSoTimeout(30000);
                            nc1.write(p.getValue());
                            new FileSendThread(client, nc1, files, p.getValue());
                        }
                        else if (rep.equalsIgnoreCase("yourFiles"))
                        {
                            Pair<ArrayList<String>, ArrayList<String>> p = (Pair)((Pair<?, ?>) o).getValue();

                            if (p.getKey().size() > 0)
                            {
                                System.out.println("Private files: ");
                                for (String s : p.getKey())
                                {
                                    System.out.println("    * " + s);
                                }
                            }
                            if (p.getValue().size() > 0)
                            {
                                System.out.println("Public files: ");
                                for (String s : p.getValue())
                                {
                                    System.out.println("    * " + s);
                                }
                            }

                            if (p.getValue().size() == 0 && p.getKey().size() == 0)
                            {
                                System.out.println("You have not files");
                            }
                        }
                        else if (rep.equalsIgnoreCase("showFiles"))
                        {
                            Pair<String, ArrayList<String>> p = (Pair) ((Pair<?, ?>) o).getValue();
                            if (p.getValue().size() > 0)
                            {
                                System.out.println("The public files of " + p.getKey());
                                for (String s : p.getValue())
                                {
                                    System.out.println("    * " + s);
                                }
                            }
                            else
                            {
                                System.out.println(p.getKey() + " has no public files");
                            }
                        }
                        else if (rep.equalsIgnoreCase("newChunk"))
                        {
                            ///System.out.println("hererr");
                            Pair<String, byte[]> p = (Pair) ((Pair<?, ?>) o).getValue();
                            client.fileToReceive = p.getKey();
                            if (client.chunks == null) client.chunks = new ArrayList<>();
                            ///System.out.println("ok");
                            client.chunks.add(p.getValue());
                            ///System.out.println("no ok");
                        }
                        else if (rep.equalsIgnoreCase("messages"))
                        {
                            List<String> message = (List<String>) ((Pair<?, ?>) o).getValue();
                            System.out.println("Messages: ");
                            for (String s : message)
                            {
                                System.out.println("  * " + s);
                            }
                        }
                    }
                }
            }
        } catch (Exception e)
        {
            System.out.println(e);
        } finally
        {
            nc.closeConnection();
        }
    }
}



