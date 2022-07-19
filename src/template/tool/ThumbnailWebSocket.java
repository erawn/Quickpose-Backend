package template.tool;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

@WebSocket
public class ThumbnailWebSocket {

    // Store sessions if you want to, for example, broadcast a message to all users
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    @OnWebSocketConnect
    public void connected(Session session) {
        if(!sessions.contains(session)){
            sessions.add(session);
            System.out.println("added session"+session.toString());
        }
        
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
        System.out.println("removed session"+session.toString());
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        //System.out.println("Got: " + message);   // Print message
        //session.getRemote().sendString(message); // and send it back
    }

    public void broadcastData(ByteBuffer message) throws IOException {
        for(Session session : sessions){
            if(session.isOpen()){
                message.rewind();
                //System.out.println(message.toString());
                session.getRemote().sendBytes(message);
            }
        }
    }
    public void broadcastString(String message) throws IOException {
        for(Session session : sessions){
            if(session.isOpen()){
                session.getRemote().sendString(message);
            }
        }
    }

}