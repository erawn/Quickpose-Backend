package template.tool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@WebSocket
public class ThumbnailWebSocket {

    // Store sessions if you want to, for example, broadcast a message to all users
    private static Queue<Session> sessions;
    private static java.util.logging.Logger archiver;
    private static java.util.logging.Logger usageData;
    private static ScheduledExecutorService executor;
    private static Queue<String> messageQueue;

    public ThumbnailWebSocket(Queue<String> msgQueue, Queue<Session> sessionQueue, java.util.logging.Logger archive, java.util.logging.Logger usageLogger){
        sessions = sessionQueue;
        messageQueue = msgQueue;
        archiver = archive; 
        usageData = usageLogger;
        executor = Executors.newScheduledThreadPool(2);
    }
    @OnWebSocketConnect
    public void connected(Session session) {
        if(!sessions.contains(session)){
            sessions.add(session);
            archiver.info("added session");
            usageData.info("added session");
            

        }
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
        archiver.info("removed session");
        usageData.info("removed session");
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