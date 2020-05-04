
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int port = 5203;
    private ServerSocket server_socket;   //Create socket
    private ServerSocket fileServerSocket;  // create file socket

    private String path ="fileSystem/GroupFile";

    public Server() {
        try {
            //1-初始化
            server_socket = new ServerSocket(this.port);   // create server
            fileServerSocket = new ServerSocket(8888);  //create filer server socket


            //2-Open a new thread when a client send request
            while(true) {
                Socket client_socket = server_socket.accept();
                new ServerThread(client_socket).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class ServerThread extends Thread{
        private Socket client_socket;
        private BufferedReader server_in;
        private PrintWriter server_out;

        public ServerThread(Socket client_socket) {
            try {
                //initialization
                this.client_socket = client_socket;
                server_in = new BufferedReader(new InputStreamReader(this.client_socket.getInputStream()));
                server_out = new PrintWriter(this.client_socket.getOutputStream(),true);


            } catch (IOException e) {
            }
        }

        public void run() {
            try {
                String uploadFileName = null;
                String uploadFileSize = null;
                String fromClientData ;

                while((fromClientData = server_in.readLine()) != null){
                    //把服务器文件列表返回
                    if(fromClientData.startsWith("@action=loadFileList")){
                        File dir = new File(path);
                        if (dir.isDirectory()){
                            String[] list = dir.list();
                            String filelist = "@action=GroupFileList[";
                            for (int i = 0; i < list.length; i++) {
                                if (i == list.length-1){
                                    filelist  = filelist + list[i]+"]";
                                }else {
                                    filelist  = filelist + list[i]+":";
                                }
                            }
                            server_out.println(filelist);
                        }
                    }

                    //Request for file upload
                    if (fromClientData.startsWith("@action=Upload")){
                        uploadFileName = getUploadFileName(fromClientData);
                        uploadFileSize = getUploadFileSize(fromClientData);
                        System.out.println(fromClientData);
                        File f = new File(this.getClass().getResource("").getPath() + "/fileSystem/GroupFile");
                        path = f.toString();
                        File file = new File(path,uploadFileName);
                        //Check if file already exist
                        if (file.exists()){
                            //Dont upload file if existed
                            server_out.println("@action=Upload[null:null:NO]");
                        }else {
                            //Announce client about file upload
                            server_out.println("@action=Upload["+uploadFileName+":"+uploadFileSize+":YES]");
                            //open new thread for file upload
                            new HandleFileThread(1,uploadFileName,uploadFileSize).start();
                        }
                    }

                    //request for file download
                    if(fromClientData.startsWith("@action=Download")){
                        String fileName = getDownFileName(fromClientData);
                        File file = new File(path,fileName);
                        if(!file.exists()){
                            server_out.println("@action=Download[null:null:File doesn't exist]");
                        }else {
                            //tell client to start
                            server_out.println("@action=Download["+file.getName()+":"+file.length()+":OK]");
                            
                            new HandleFileThread(0,file.getName(),file.length()+"").start();

                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         *     file transfer thread
         */
        class HandleFileThread extends Thread{
            private String filename;
            private String filesize;
            private int mode;  //file transfer mode

            public HandleFileThread(int mode,String name,String size){

                filename = name;
                filesize = size;
                this.mode = mode;
            }

            public void run() {
                try {
                    Socket socket = fileServerSocket.accept();
                    //uploading mode
                    if(mode == 1){
                        //Accept file
                        BufferedInputStream file_in = new BufferedInputStream(socket.getInputStream());
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(path,filename)));

                        int len;
                        byte[] arr = new byte[8192];

                        while ((len = file_in.read(arr)) != -1){
                            bos.write(arr,0,len);
                            bos.flush();
                        }
                        File dir = new File(path);
                        if (dir.isDirectory()){
                            String[] list = dir.list();
                            String filelist = "@action=GroupFileList[";
                            for (int i = 0; i < list.length; i++) {
                                if (i == list.length-1){
                                    filelist  = filelist + list[i]+"]";
                                }else {
                                    filelist  = filelist + list[i]+":";
                                }
                            }
                            server_out.println(filelist);
                        }
                        //server_out.println("@action=Upload[null:null:Transfer completed]");
                        //server_out.println("\n");
                        bos.close();
                    }

                    //Download mode
                    if(mode == 0){
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(path,filename)));
                        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                        System.out.println("Server: Start Uploading");
                        int len;
                        byte[] arr =new byte[8192];
                        while((len = bis.read(arr)) != -1){
                            bos.write(arr,0,len);
                            bos.flush();
                        }

                        socket.shutdownOutput();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    public static String getDownFileName(String data){
        return data.substring(data.indexOf("[")+1,data.indexOf(":"));
    }
    public static String getDownFileSize(String data){
        return data.substring(data.indexOf(":")+1,data.lastIndexOf(":"));
    }
    public static String getDownResult(String data){
        return data.substring(data.lastIndexOf(":")+1,data.length()-1);
    }

    public static String getUploadFileName(String data){
        return data.substring(data.indexOf("[")+1,data.indexOf(":"));
    }
    public static String getUploadFileSize(String data){
        return data.substring(data.indexOf(":")+1,data.lastIndexOf(":"));
    }
    public static String getUploadResult(String data){
        data = data.substring(data.lastIndexOf(":")+1,data.length()-1);
        return data.substring(0, data.lastIndexOf("/")+1);
    }
    public static String[] getFileList(String data){
        String list = data.substring(data.indexOf("[")+1,data.length()-1);
        return  list.split(":");
    }
    //Start program
    public static void main(String[] args) {
        new Server();
    }
}