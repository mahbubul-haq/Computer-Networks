package ClientPackage;

import util.NetworkUtil;

import java.util.List;

public class FileSendThread implements Runnable{

    public Client client;
    public NetworkUtil nc;
    private Thread thr;
    public List<byte[]> files;
    public long fileId;
    public int chunknumber = 0;

    public FileSendThread(Client client, NetworkUtil nc, List<byte[]> files, long fileId)
    {
        this.client = client;
        this.nc = nc;
        this.fileId = fileId;
        this.files = files;
        this.thr = new Thread(this);
        thr.start();
    }
    @Override
    public void run() {


        try
        {
            while (true)
            {

                if (chunknumber == 0)
                {
                    nc.write(files.get(chunknumber));
                    chunknumber++;
                }

                Object o = nc.read();

                if (o != null)
                {
                    if (o instanceof String)
                    {
                        String s = (String) o;

                        if (s.equalsIgnoreCase("Chunk received"))
                        {
                            if (chunknumber == files.size())
                            {
                                nc.write("SendingComplete");
                            }
                            else
                            {
                                nc.write(files.get(chunknumber));
                                chunknumber++;
                            }
                        }
                        else
                        {
                            System.out.println(s);
                            nc.getSocket().close();
                        }
                    }
                }

            }

        }
        catch (Exception error)
        {
            error.printStackTrace();
            nc.write("Timeout");

        }

    }
}
