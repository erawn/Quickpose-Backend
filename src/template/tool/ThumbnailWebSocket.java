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
    private static Queue<Session> sessions;
    private static org.slf4j.Logger logger;
    private static java.util.logging.Logger archiver;
    private static ScheduledExecutorService executor;
    private static Queue<String> messageQueue;

    public ThumbnailWebSocket(Queue<String> msgQueue, Queue<Session> sessionQueue, org.slf4j.Logger log,java.util.logging.Logger archive){
        sessions = sessionQueue;
        messageQueue = msgQueue;
        logger = log;
        archiver = archive; 
        executor = Executors.newScheduledThreadPool(2);
    }
    @OnWebSocketConnect
    public void connected(Session session) {
        if(!sessions.contains(session)){
            sessions.add(session);
            archiver.info("added session");

        }
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
        archiver.info("removed session");
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        //System.out.println("Got: " + message);   // Print message
        messageQueue.add(message);
        //session.getRemote().sendString(message); // and send it back
    }

    public void broadcastData(ByteBuffer message) throws IOException {   
        //System.out.println(message.toString());
        executor.schedule(new Runnable(){
            public void run(){
                for(Session session : sessions){
                    if(session.isOpen()){
                        try {
                            message.rewind();
                            session.getRemote().sendBytes(message);
                        } catch (IOException e) {
                            archiver.info(e.getMessage());
                        }
                    }
                }
            }
        },0,TimeUnit.SECONDS);
    }
    public void broadcastString(String message) throws IOException {
        for(Session session : sessions){
            if(session.isOpen()){
                session.getRemote().sendString(message);
            }
        }
    }

}