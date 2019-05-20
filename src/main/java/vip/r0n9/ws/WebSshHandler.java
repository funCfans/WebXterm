package vip.r0n9.ws;

import com.jcraft.jsch.JSchException;
import org.springframework.stereotype.Component;
import vip.r0n9.util.SshSession;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ssh/{id}", configurator = WebSocketConfigrator.class)
@Component
public class WebSshHandler {

    private static Map<String, SshSession> map = new ConcurrentHashMap<String, SshSession>();

    @OnOpen
    public void onOpen(final Session session, @PathParam("id") String id) throws JSchException, IOException, EncodeException, InterruptedException {
        System.out.println("有新链接 " + session.getUserProperties().get("ClientIP") + " 加入!当前在线人数为" + getOnlineCount());

        Map<String,String> parammap = new HashMap<String,String>();
        String[] param =  session.getQueryString().split("&");
        for(String keyvalue:param){
           String[] pair = keyvalue.split("=");
           if(pair.length==2){
        	   parammap.put(pair[0], pair[1]);
           }
        }
        
        String hostname = parammap.get("hostname");
        String password = parammap.get("password");
        Integer port,cols,rows;
        try {
        	port = Integer.valueOf(parammap.get("port"));
        }catch(Exception e) {
        	port = 22;
        }
        String username = parammap.get("username");
        try {
        	rows = Integer.valueOf(parammap.get("rows"));
        }catch(Exception e) {
        	rows = 24;
        }
        try {
        	cols = Integer.valueOf(parammap.get("cols"));
        }catch(Exception e) {
        	cols = 80;
        }
        
        SshSession sshSession;
        sshSession = new SshSession(hostname, port, username, password, session, rows, cols);
        map.put(session.getId(), sshSession);
    }

    @OnClose
    public void onClose(Session session) {
        SshSession sshsession = map.remove(session.getId());
        sshsession.close();
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException, JSchException {
    	map.get(session.getId()).getMessage(message);
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
    	throwable.printStackTrace();
    	try {
			session.getBasicRemote().sendText(throwable.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static synchronized int getOnlineCount() {
        return map.size();
    }
}
