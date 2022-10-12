package ServerPackage;

import ClientPackage.FileSendThread;
import util.NetworkUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class FileUploadThread implements Runnable{///when clients upload a file

    public Server server;
    public NetworkUtil nc;
    public long fileId;
    private Thread thr;


    public FileUploadThread(Server server, NetworkUtil nc, long fileId)
    {
        this.server = server;
        this.nc = nc;
        this.fileId = fileId;
        this.thr = new Thread(this);
        thr.start();
    }

    public static String getFileName(String stdId, String fileType, String fileName)
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

    public void makeFile(String fileName)
    {
        File ofile = new File(fileName);

        FileOutputStream fos;
        FileInputStream fis;
        //byte[] fileBytes;
        int bytesRead = 0;
        try {
            fos = new FileOutputStream(ofile, true);
            for (byte[] fileBytes : server.getChunks.get(fileId)) {
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

    @Override
    public void run() {
        try
        {
            while (true)
            {
                Object o = nc.read();

                if (o != null)
                {
                    if (o instanceof byte[])
                    {
                        byte[] chunk = (byte[]) o;
                        server.getChunks.get(fileId).add(chunk);
                        nc.write("Chunk received");
                    }
                    else if (o instanceof String)
                    {
                        String s = (String) o;
                        if (s.equalsIgnoreCase("Timeout"))
                        {
                            server.getChunks.remove(fileId);
                            server.getStudentIdFileType.remove(fileId);
                            server.getFileInfo.remove(fileId);
                            nc.getSocket().close();
                            break;
                        }
                        else if (s.equalsIgnoreCase("SendingComplete"))
                        {
                            long totalChunkSize = 0;
                            for (byte[] b : server.getChunks.get(fileId))
                            {
                                totalChunkSize += b.length;
                            }
                            if (server.getFileInfo.get(fileId).getValue() == totalChunkSize)
                            {
                                String stdId = server.getStudentIdFileType.get(fileId).getKey();
                                String fileType = server.getStudentIdFileType.get(fileId).getValue();
                                String fileName = server.getFileInfo.get(fileId).getKey();

                                if (server.messages.get(stdId) == null) server.messages.put(stdId, new ArrayList<>());
                                server.messages.get(stdId).add("File upload successful");

                                makeFile(getFileName(stdId, fileType, fileName));

                                if (fileType.equalsIgnoreCase("public")) {
                                    server.uploadedFiles.get(stdId).getValue().add(fileName);
                                }
                                else {
                                    server.uploadedFiles.get(stdId).getKey().add(fileName);
                                }

                                server.getChunks.remove(fileId);
                                server.getStudentIdFileType.remove(fileId);
                                server.getFileInfo.remove(fileId);

                            }
                            else
                            {
                                String stdId = server.getStudentIdFileType.get(fileId).getKey();
                                if (server.messages.get(stdId) == null) server.messages.put(stdId, new ArrayList<>());
                                server.messages.get(stdId).add("File upload failed");
                            }
                        }
                    }
                }

            }

        }
        catch (Exception error)
        {
            error.printStackTrace();
            server.getChunks.remove(fileId);
            server.getStudentIdFileType.remove(fileId);
            server.getFileInfo.remove(fileId);

            try
            {
                nc.getSocket().close();
            }catch (Exception error1)
            {
                error1.printStackTrace();
            }
        }
    }
}
