package Server;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class ClientHandler {
    private Socket socket;
    private MyServer myServer;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String nick;

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(MyServer.TIMEOUT)); // timeout 120 sec.
            socket.setTcpNoDelay(true);
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        Message message = new Message();
        myServer.unsubscribe(this);
        message.setMessage(nick + " вышел из чата");
        myServer.broadcastMessage(message);
        try {
            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void authentication() {
        while (true) {
            try {
                AuthMessage message = new Gson().fromJson(dataInputStream.readUTF(), AuthMessage.class);
                String nick = myServer.getAuthService().getNickByLoginAndPass(message.getLogin(), message.getPassword());
                if (nick != null && !myServer.isNickBusy(nick)) {
                    message.setAuthenticated(true);
                    message.setNick(nick);
                    this.nick = nick;
                    socket.setSoTimeout(0);

                    dataOutputStream.writeUTF(new Gson().toJson(message));
                    Message broadcastMsg = new Message();
                    broadcastMsg.setMessage(nick + " вошел в чат");
                    myServer.broadcastMessage(broadcastMsg);
                    myServer.subscribe(this);
                    return;
                } else {
                    message.setAuthenticated(false);
                    message.setNick("/wrong");
                    dataOutputStream.writeUTF(new Gson().toJson(message));
                }
            } catch (SocketTimeoutException ignored) {
                System.out.println("Authentication timeout");
                closeConnection();
            } catch (IOException ignored) {
            }
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            Message message = new Gson().fromJson(dataInputStream.readUTF(), Message.class);
            message.setNick(nick);

            System.out.println(message);
            if(!message.getMessage().startsWith("/")) {
                myServer.broadcastMessage(message);
                continue;
            }
            String[] command = message.getMessage().split("\\s");
            switch (command[0]) {
                case "/end": {
                    return;
                }
                case "/w": {
                    if (command.length < 3) {
                        Message msg = new Message();
                        msg.setMessage("Не хватает параметров, необходимо отправить команду следующего вида: /w <ник> <сообщение>");
                        this.sendMessage(msg);
                    }
                    String nick = command[1];
                    StringBuilder sbMessage = new StringBuilder();
                    for (int i = 2; i < command.length; i++) {
                        sbMessage.append(command[i]);
                        sbMessage.append(" ");
                    }
                    myServer.sendMessageToClient(this, nick, sbMessage.toString());
                }
            }
        }
    }

    public void sendMessage(Message message) {
        try {
            dataOutputStream.writeUTF(new Gson().toJson(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }
}
