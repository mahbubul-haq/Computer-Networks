package ClientPackage;

import javafx.util.Pair;
import util.NetworkUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

public class WriteThreadClient implements Runnable {

    private Thread thr;
    private NetworkUtil nc;
    public Client client;
    File [] file;

    public WriteThreadClient(Client client, NetworkUtil nc) {
        this.nc = nc;
        this.client = client;
        this.thr = new Thread(this);
        thr.start();
        file = new File[1];
    }

    public void chooseFile()
    {
        final File[] fileToSend = new File[1];

        JFrame jFrame = new JFrame("Client");
        jFrame.setSize(500, 500);
        jFrame.setLayout(new BoxLayout(jFrame.getContentPane(), BoxLayout.Y_AXIS));

        JLabel jlTitle = new JLabel("File Chooser");
        jlTitle.setFont(new Font("Arial", Font.BOLD, 25));
        jlTitle.setBorder(new EmptyBorder(20,0,10,0));
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel jlFileName = new JLabel("Choose a file to send");
        jlFileName.setFont(new Font("Arial", Font.BOLD, 20));
        jlFileName.setBorder(new EmptyBorder(50,0,0,0));
        jlFileName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel jpButton = new JPanel();
        jpButton.setBorder(new EmptyBorder(75, 0,10, 0));

        /*JButton jbSendFile = new JButton("Send FIle");
        jbSendFile.setPreferredSize(new Dimension(150, 75));
        jbSendFile.setFont(new Font("Arial", Font.BOLD, 20));*/

        JButton jbChooseFIle = new JButton("Choose File");
        jbChooseFIle.setPreferredSize(new Dimension(250, 75));
        jbChooseFIle.setFont(new Font("Arial", Font.BOLD, 20));

        jpButton.add(jbChooseFIle);


        jbChooseFIle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setDialogTitle("Choose a file to send");

                if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
                {
                    fileToSend[0] = jFileChooser.getSelectedFile();
                    jlFileName.setText("The file you want to send is: " + fileToSend[0].getName());
                    ///========================================================================================================
                    //System.out.println(fileToSend[0].getName());
                    //System.out.println(fileToSend[0].getAbsolutePath());
                    //System.out.println(fileToSend[0].getParent());
                    file[0] = fileToSend[0];
                }
            }
        });

        jFrame.add(jlTitle);
        jFrame.add(jlFileName);
        jFrame.add(jpButton);
        jFrame.setVisible(true);
    }

    public void run()
    {
        try {
            Scanner input = new Scanner(System.in);
            while (true)
            {
                String s = input.nextLine();

                if (s.equalsIgnoreCase("Upload a public file") || s.equalsIgnoreCase("Upload a private file"))
                {
                    String type = null;
                    long requestId = -1;

                    if (s.equalsIgnoreCase("Upload a public file")) {
                        type = "public";
                        System.out.println("Provide request Id. Input -1 for no request Id: ");
                        requestId = input.nextLong();
                    }
                    else type = "private";

                    file[0] = null;
                    File [] file1 = new File[1];
                    chooseFile();
                    long start = System.currentTimeMillis();

                    while((System.currentTimeMillis() - start) / 1000 < 60)
                    {
                        if ((System.currentTimeMillis() - start) / 1000 > 55)
                        {
                            System.out.println("Failed to upload file");
                            break;
                        }
                        file1[0] = file[0];
                        if (file1[0] != null) break;
                    }
                    client.fileToSend = file[0];

                    if (requestId != -1)
                    {
                        nc.write(new Pair<>("requestedFile " + requestId, new Pair(client.fileToSend.getName(), client.fileToSend.length())));
                    }
                    else
                        nc.write(new Pair<>("fileSendRequest " + type, new Pair(client.fileToSend.getName(), client.fileToSend.length())));
                }
                else
                {
                    String [] arr = s.split(" ");
                    if (arr[0].equalsIgnoreCase("download"))///before sedning download request, clean chunks
                    {
                        if (client.chunks == null) client.chunks = new ArrayList<>();
                        client.fileToReceive = null;
                        client.chunks.clear();
                    }
                    nc.write(s);
                }



            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            nc.closeConnection();
        }
    }
}



