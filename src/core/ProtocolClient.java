package core;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;

//TODO WRAP LABEL GENERATION IN METHODS WITH CORRESPONDING TYPES AS PARAMETER FOR CONFIGURING LABEL STYLE


/**
 * Created by bulibas@yahoo.com on 8/20/17.
 */
public class ProtocolClient implements Runnable {

    private static final int SERVER_PORT = 9090;

    private static final String SERVER_IP = "127.0.0.1";

    private Map<Integer, String> idToUsername;

    private Map<String, Integer> usernameToId;

    private volatile int counter;

    private Controller controller;

    private Socket socket;

    private int id;

    private PrintStream ps;

    private InputStream in;

    public ProtocolClient(Controller c) {
        this.controller = c;
    }

    @Override
    public void run() {
        try {
            startClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startClient() throws IOException {
        idToUsername = new HashMap<>();
        usernameToId = new HashMap<>();
        socket = new Socket(SERVER_IP, SERVER_PORT);
        ps = new PrintStream(socket.getOutputStream());
        in = socket.getInputStream();
       // BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Text t = new Text("enter username:\n");
                controller.addMessageToList(t);
                System.out.println("enter username:");
            }
        });
    }

    private void startChat() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendUsersGet();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (true) {
                    int n;
                    try {
                        if ((n = in.available()) > 0) {
                            byte[] b = new byte[n];
                            in.read(b);
                            byte commnandId = b[5];
                            System.out.println("COMMAND RECEIVED: " + commnandId);

                            switch (commnandId) {
                                case ProtocolCommands.COMMAND_MESSAGE_PERSONAL:
                                    handleIncomingMessage(b);
                                    break;
                                case ProtocolCommands.COMMAND_MESSAGE_GROUP:
                                    handleIncomingMessage(b);
                                    break;
                                case ProtocolCommands.COMMAND_USER_LOGGED:
                                    handleUserLogged(b);
                                    break;
                                case ProtocolCommands.COMMAND_FAILURE:
                                    byte type = b[10];
                                    System.out.println("failure: " + type); //TODO HANDLE FAILURE
                                    break;
                                case ProtocolCommands.COMMAND_USERS_REPORT:
                                    handleUsersReport(b);
                            }
                            /*if (commnandId == ProtocolCommands.COMMAND_MESSAGE_PERSONAL || commnandId == ProtocolCommands.COMMAND_MESSAGE_GROUP) {
                                handleIncomingMessage(b);
                            } else if (commnandId == 7) {
                                handleUserLogged(b);
                            } else if (commnandId == 10) {

                            } else if (commnandId == 6) {
                                handleUsersReport(b);
                            }*/
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    private void processLogin() {
        while (true) {

            byte[] loginResponse = new byte[6 + 2];
            //while(true) {
            try {
                in.read(loginResponse, 0, loginResponse.length);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println(Arrays.toString(loginResponse));

            id = ((loginResponse[6] << 8) << 0xFF00) | (loginResponse[7] & 0xFF);
            if (id == 0) {
                Text t = new Text("username already taken, please try another:\n");
                t.setFill(Color.RED);
                controller.addMessageToList(t);
                System.out.println("username already taken!\nplease try another:\n");
                return;
            } else {
                Text l = new Text("login successfull\n");
                l.setFill(Color.GREEN);
                controller.addMessageToList(l);
                System.out.println("login successfull");
                break;
            }
        }

        startChat();
    }

    public void sendMessageFromGUI(String msg) throws IOException {
        if (msg.equals("")) {
            return;
        }
        if (id == 0) {
            sendTryLogin(ps, msg);
            processLogin();
        } else {
            String[] parts = msg.split("#", 2);
            if (parts.length == 2) {
                String m = parts[1];
                if (parts[0].equals("all")) {
                    sendBroadcast(ps, m);
                } else {
                    int dest = usernameToId.get(parts[0]);
                    sendPersonal(ps, dest, m);
                }
            } else {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Text t = new Text("this message is not parsable for now. \nPattern: username#message or all#message for broadcast\n");
                        t.setFill(Color.RED);
                        controller.addMessageToList(t);
                    }
                });
            }

        }
    }

    private void handleIncomingMessage(byte[] b) {
        int from = ((b[6] << 8) << 0xFF00) | (b[7] & 0xFF);
        byte[] bmsg = Arrays.copyOfRange(b, 8, b.length);
        String msg = new String(bmsg);
        System.out.println("from " + from + ": " + msg);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Text l = new Text(idToUsername.get(from) + ": " + msg + "\n");
                l.setWrappingWidth(380);
                if (from == id) {
                    l.setFill(Color.BLUE);
                }
                controller.addMessageToList(l);
            }
        });
    }

    private void handleUsersReport(byte[] b) {
        System.out.println("USERS REPORT: " + Arrays.toString(b));
        int numUsers = ((b[8] << 8) << 0xFF00) | (b[9] & 0xFF);
        System.out.println("numusers: " + numUsers);
        //for (int i = 0; i < numUsers; i++) {
        for (int j = 10; j < b.length; ) {
            int userid = ((b[j++] << 8) << 0xFF00) | (b[j++] & 0xFF);
            int len = ((b[j++] << 8) << 0xFF00) | (b[j++] & 0xFF);
            //System.out.println("len" + i + "=" + len);
            byte[] namebytes = new byte[len];
            for (int k = 0; k < namebytes.length; k++) {
                namebytes[k] = b[j++];
            }
            String s = new String(namebytes);
            //System.out.println("name" + i + "=" + s);
            Text l = new Text();
            l.setText(s);
            if (userid == id) {
                l.setFill(Color.BLUE);
            }
            controller.addUserToList(l);
            idToUsername.put(userid, s);
            usernameToId.put(s, userid);
        }
    }

    private void handleUserLogged(byte[] b) {
        int len = b[10];
        int loggedid = ((b[6] << 8) << 0xFF00) | (b[9] & 0xFF);
        byte[] msg = Arrays.copyOfRange(b, 11, 11 + len);
        if (id != loggedid) {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    String str = new String(msg);
                    usernameToId.put(str, loggedid);
                    idToUsername.put(loggedid, str);
                    Text l = new Text();
                    l.setText(str);
                    controller.addUserToList(l);

                    Text t = new Text("[user_logged] [" + str + "]");
                    t.setFill(Color.GREEN);
                    controller.addMessageToList(t);
                }
            });
        }
    }

    private void sendTryLogin(PrintStream ps, String username) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(7 + username.length());
        b.put((byte) 2);
        b.put((byte) 2);
        b.put((byte) 2);
        b.put((byte) ((byte) counter >> 8));
        b.put((byte) counter);
        b.put((byte) 1);//command login
        b.put((byte) username.length());//command login
        b.put(username.getBytes());
        ps.write(b.array());
        ps.flush();
        System.out.println("sended to server:" + Arrays.toString(b.array()));
        counter++;
    }

    private void sendPersonal(PrintStream ps, int to, String msg) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(10 + msg.length());
        b.put((byte) 2);
        b.put((byte) 2);
        b.put((byte) 2);
        b.put((byte) ((byte) counter >> 8));
        b.put((byte) counter);
        b.put((byte) 3);
        b.put((byte) ((byte) id >> 8));
        b.put((byte) id);
        b.put((byte) ((byte) to >> 8));
        b.put((byte) to);
        b.put(msg.getBytes());
        ps.write(b.array());
        ps.flush();
        counter++;
    }

    private void sendBroadcast(PrintStream ps, String msg) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(10 + msg.length());
        b.put((byte) 2);
        b.put((byte) 2);
        b.put((byte) 2);
        b.put((byte) ((byte) counter >> 8));
        b.put((byte) counter);
        b.put((byte) 4);
        b.put((byte) ((byte) id >> 8));
        b.put((byte) id);
        b.put((byte) 0);
        b.put((byte) 0);
        b.put(msg.getBytes());
        ps.write(b.array());
        ps.flush();
        counter++;
    }

    private void sendUsersGet() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(8);
        b.put((byte) 2);
        b.put((byte) 2);
        b.put((byte) 2);
        b.put((byte) ((byte) counter >> 8));
        b.put((byte) counter);
        b.put((byte) 5);
        b.put((byte) ((byte) id >> 8));
        b.put((byte) id);
        ps.write(b.array());
        ps.flush();
        counter++;
    }

}
