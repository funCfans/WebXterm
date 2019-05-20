package vip.r0n9.ws;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Builder;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.JSchException;

@ServerEndpoint(value = "/websockify/{token}", configurator = WebSocketConfigrator.class)
@Component
public class WebSockifyHandler {
    private static Map<String, MsgWebSocketClient> map = new ConcurrentHashMap<String, MsgWebSocketClient>();

    private static Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();
    @OnOpen
    public void onOpen(final Session session, @PathParam("token") String token) throws JSchException, IOException, EncodeException, InterruptedException {
        System.out.println("有新链接 " + session.getUserProperties().get("ClientIP") + " 加入!当前在线人数为");
        
        Map<String,String> parammap = new HashMap<String,String>();
        String[] param =  session.getQueryString().split("&");
        for(String keyvalue:param){
           String[] pair = keyvalue.split("=");
           if(pair.length==2){
        	   parammap.put(pair[0], pair[1]);
           }
        }
        System.out.println(parammap.get("ip"));
        System.out.println(parammap.get("token"));
        System.out.println("ws://" + parammap.get("ip") + "/websockify");
        final String tokens = parammap.get("token");
		WebSocketContainer wsc = ContainerProvider.getWebSocketContainer();
	    Builder configBuilder = ClientEndpointConfig.Builder.create();
        configBuilder.configurator(new Configurator() {
        	@Override
            public void beforeRequest(Map<String, List<String>> headers) {
                List<String> cookieList = headers.get("Cookie");
                if (null == cookieList) {
                    cookieList = new ArrayList<>();
                }
                cookieList.add("token=" + tokens);     // set your cookie value here
                headers.put("Cookies", cookieList);
                super.beforeRequest(headers);
            }
        });
        ClientEndpointConfig clientEndpointConfig = configBuilder.build();
        Websockify websockify = new Websockify(session);
        try {
		    Session wsSession = wsc.connectToServer(
		    		websockify,
		            clientEndpointConfig,
		            new URI("ws://" + parammap.get("ip") + "/websockify"));
		    MessageHandler mh = new MessageHandler.Whole<String>() {
				@Override
				public void onMessage(String message) {
			    	System.out.println("有新链接 " + session.getUserProperties().get("ClientIP") + "给数据");
					byte[] byteset = message.getBytes();
					ByteBuffer byteBuffer = ByteBuffer.wrap(byteset, 0, message.length());
					try {
						session.getBasicRemote().sendBinary(byteBuffer);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			wsSession.addMessageHandler(mh);
			sessionMap.put(session.getId(), wsSession);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (DeploymentException e) {
			e.printStackTrace();
		}

    }

    @OnClose
    public void onClose(Session session) {
    	System.out.println("有新链接 " + session.getUserProperties().get("ClientIP") + "关闭");
    	try {
			sessionMap.remove(session.getId()).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
    	System.out.println("有新链接 " + session.getUserProperties().get("ClientIP") + "错误");
    	throwable.printStackTrace();
    	try {
			sessionMap.remove(session.getId()).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException, JSchException {
    	System.out.println("有新链接 " + session.getUserProperties().get("ClientIP") + "给数据");
		byte[] byteset = message.getBytes();
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteset, 0, message.length());
		try {
			sessionMap.get(session.getId()).getBasicRemote().sendBinary(byteBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

}

class Websockify extends Endpoint{

	MessageHandler mh;
	Session session;
	public Websockify(Session session) {
		this.session = session;
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
	}
/*
	@OnMessage
	public void onMessage(String message) {
		byte[] byteset = message.getBytes();
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteset, 0, message.length());
		try {
			session.getBasicRemote().sendBinary(byteBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
*/
}

class MsgWebSocketClient extends WebSocketClient{
	
	private Session session;

	public MsgWebSocketClient(URI serverUri, Session session) {
		super(serverUri);
		this.session = session;

	}
	
	public MsgWebSocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders, int connectTimeout, Session session) {
		super(serverUri, protocolDraft, httpHeaders, connectTimeout);
		this.session = session;
	}

	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		System.out.println("有新链接 " + arg0 + "_" + arg1 + "_" + arg2 + "关闭");
		try {
			session.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onError(Exception arg0) {
		System.out.println("有新链接 " + arg0.getMessage() + "出错");
		arg0.printStackTrace();
		try {
			session.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(String arg0) {
		byte[] byteset = arg0.getBytes();
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteset, 0, arg0.length());
		try {
			session.getBasicRemote().sendBinary(byteBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onOpen(ServerHandshake hs) {
		hs.getContent();
	}
	
}
