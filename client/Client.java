
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame {
    private int width = 400;
    private int height = 600;

    private JLabel groupLabel;
    private JButton uploadButton;
    private JLabel flushLabel;
    private String fileIconPath = "";
    private JScrollPane jScrollPane;
    private JPanel staffPanel;      

    private Socket client_socket;
    private PrintStream client_out;
    private BufferedReader client_in;
    private String ip = "127.0.0.1";
    private int port = 5203;

    private File currentUpploadFile;
    private String downloadSavePath;
    private int Y = 0;

    private JPanel panel1;
    private JPanel panel2;

    public Client() {
        //1-Initialization
        initVariable();
        //2-Connect with server
        connectServer();
        //3-register listener
        registerListener();
        //4-initialize window
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(width, height);
        this.setTitle("Data and Computer Communication File System");
        this.setLocationRelativeTo(null);//let window shows on middle
        this.setResizable(false);
        this.setVisible(true);
    }


    private void initVariable() {
        jScrollPane = new JScrollPane();
        this.getContentPane().add(jScrollPane);

        staffPanel = new JPanel();
        ///staffPanel.setLayout(new BoxLayout(staffPanel,BoxLayout.Y_AXIS));
        staffPanel.setLayout(null);
        staffPanel.setOpaque(false);
        staffPanel.setPreferredSize(new Dimension(width, height));

        jScrollPane.setViewportView(staffPanel);
        jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);//scroller
        jScrollPane.getViewport().setOpaque(false);  
        jScrollPane.setOpaque(false);  

        renderTop();
    }


    /**
     * read files names from server
     */
    private void loadGroupFile() {
        client_out.println("@action=loadFileList");
    }


    /**
     * render panel
     */
    private void renderTop() {
        staffPanel.removeAll();
        Y = 0;

        panel1 = new JPanel();
        panel1.setLayout(new GridLayout(1, 3, 3, 10));
        this.groupLabel = new JLabel("\t\t\t\t\tFile List ");
        this.uploadButton = new JButton("Upload file ");
        flushLabel = new JLabel(new ImageIcon(""));
        panel1.add(groupLabel);
        panel1.add(uploadButton);
        panel1.add(flushLabel);

        panel1.setBounds(2, Y, width, 30);
        this.staffPanel.add(panel1);
        Y += 30;
    }

    /**
     * render file lists
     */
    public void addToFileList(String filename) {
        JButton downloadBtn = new JButton("Download");
        final String fName = filename;
        JLabel fileNameLab = new JLabel(fName);

        panel2 = new JPanel();
        panel2.setLayout(new GridLayout(1, 3, 0, 0));
        panel2.add(fileNameLab);
        panel2.add(downloadBtn);
        //panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        panel2.setBounds(2, Y, width, 30);
        this.staffPanel.add(panel2);
        Y += 30;

        panel2.addMouseListener(new MouseAdapter() {
            
            public void mouseEntered(MouseEvent e) { // when mouse move to here
                panel2.setBackground(Color.orange);
                panel2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
            }

            public void mouseExited(MouseEvent e) { 
                panel2.setBackground(Color.white);
            }

        });

        //File Upload
        downloadBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                //1-Choose file
                JFileChooser f = new JFileChooser(); 
                f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                f.showOpenDialog(null);
                File file = f.getSelectedFile();

                if (file != null) {
                    downloadSavePath = file.getPath();
                    //Request download from server
                    client_out.println("@action=Download[" + fName + ":null:null]");
                }

            }
        });
    }

    /**
     * register and listening
     */
    private void registerListener() {
        //upload file    message format: @action=Upload["fileName":"fileSize":result]
        this.uploadButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JFileChooser f = new JFileChooser(); // search file
                f.showOpenDialog(null);
                currentUpploadFile = f.getSelectedFile();
                if (currentUpploadFile != null)
                    client_out.println("@action=Upload[" + currentUpploadFile.getName() + ":" + currentUpploadFile.length() + ":null]");

            }
        });

        //refresh buttons
        flushLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                loadGroupFile();
            }

            
            public void mouseEntered(MouseEvent e) { 
                flushLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
            }
        });
    }

    /**
     * connect with server
     */
    private void connectServer() {
        //connect with server
        try {
            //initialization
            client_socket = new Socket(ip, port);
            client_out = new PrintStream(client_socket.getOutputStream(), true);
            client_in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));

            //read file list
            client_out.println("@action=loadFileList");

            //listening on thread
            new ClientThread().start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Listening server information
     */
    class ClientThread extends Thread {
        public void run() {
            try {
                String fromServer_data;
                int flag = 0;

                while ((fromServer_data = client_in.readLine()) != null) {
                    //read file list
                    if (flag++ == 0) {
                        if (fromServer_data.startsWith("@action=GroupFileList")) {
                            String[] fileList = getFileList(fromServer_data);
                            for (String filename : fileList) {
                                addToFileList(filename);
                            }
                        }
                        continue;
                    }
                    if (fromServer_data.startsWith("@action=GroupFileList")) {
                        //rerender panel
                        renderTop();

                        //register listening
                        registerListener();

                        //render file panel
                        String[] fileList = getFileList(fromServer_data);
                        for (String filename : fileList) {
                            addToFileList(filename);
                        }

                    }

                    //file upload
                    if (fromServer_data.startsWith("@action=Upload")) {
                        String res = getUploadResult(fromServer_data);
                        if ("NO".equals(res)) {
                            JOptionPane.showMessageDialog(null, "File already existed !!");
                        } else if ("YES".equals(res)) {
                            //start uploading
                            if (currentUpploadFile != null) {
                                //start a new thread to upload
                                new HandelFileThread(1).start();
                            }

                        } else if ("Upload completed".equals(res)) {
                            JOptionPane.showMessageDialog(null, res);
                            loadGroupFile();
                        }

                    }

                    //file upload
                    if (fromServer_data.startsWith("@action=Download")) {
                        String res = getDownResult(fromServer_data);
                        if (res.equals("File does not exist")) {
                            JOptionPane.showMessageDialog(null, "No such file 404");
                        } else {
                            String downFileName = getDownFileName(fromServer_data);
                            String downFileSize = getDownFileSize(fromServer_data);
                            //start a new thread
                            new HandelFileThread(0, downFileName, downFileSize).start();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * ----------------------------------------------------------------------------------
         * file transfer thread
         */
        class HandelFileThread extends Thread {
            private int mode;  
            private String filename;
            private Long fileSize;

            public HandelFileThread(int mode) {
                this.mode = mode;
            }

            public HandelFileThread(int mode, String filename, String fileSize) {
                this.mode = mode;
                this.filename = filename;
                this.fileSize = Long.parseLong(fileSize);
            }

            public void run() {
                try {
                    //uploading mode
                    if (this.mode == 1) {
                        Socket socket = new Socket(ip, 8888);
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(currentUpploadFile));
                        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                        int len;
                        int i = 0;
                        double sum = 0;
                        byte[] arr = new byte[8192];
                        String schedule;

                        System.out.println("Start Uploading--File Size：" + currentUpploadFile.length());

                        while ((len = bis.read(arr)) != -1) {
                            bos.write(arr, 0, len);
                            bos.flush();
                            sum += len;
                            if (i++ % 100 == 0) {
                                schedule = "Uploading:" + 100 * sum / currentUpploadFile.length() + "%";
                                System.out.println(schedule);
                            }
                        }
                        //upload Completed
                        socket.shutdownOutput();
                        System.out.println("Completed:100%");
                    }

                    //Download
                    if (this.mode == 0) {
                        Socket socket = new Socket(ip, 8888);
                        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(downloadSavePath + "/" + filename));

                        int len;
                        byte[] arr = new byte[8192];
                        double sumDown = 0;
                        int i = 0;

                        System.out.println("Client Side start downloading ");
                        while ((len = bis.read(arr)) != -1) {
                            sumDown += len;
                            if (i++ % 100 == 0)
                                System.out.println("Downloading：" + 100 * sumDown / fileSize + "%");

                            bos.write(arr, 0, len);
                            bos.flush();
                        }

                        bos.close();
                        bis.close();
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static String getDownFileName(String data) {
        return data.substring(data.indexOf("[") + 1, data.indexOf(":"));
    }

    public static String getDownFileSize(String data) {
        return data.substring(data.indexOf(":") + 1, data.lastIndexOf(":"));
    }

    public static String getDownResult(String data) {
        return data.substring(data.lastIndexOf(":") + 1, data.length() - 1);
    }

    public static String getUploadFileName(String data) {
        return data.substring(data.indexOf("[") + 1, data.indexOf(":"));
    }

    public static String getUploadFileSize(String data) {
        return data.substring(data.indexOf(":") + 1, data.lastIndexOf(":"));
    }

    public static String getUploadResult(String data) {
        return data.substring(data.lastIndexOf(":") + 1, data.length() - 1);
    }

    public static String[] getFileList(String data) {
        String list = data.substring(data.indexOf("[") + 1, data.length() - 1);
        return list.split(":");
    }

    //Start program
    public static void main(String[] args) throws Exception {
        new Client();
    }
}