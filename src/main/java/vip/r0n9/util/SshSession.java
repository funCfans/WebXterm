package vip.r0n9.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Iterator;

import javax.websocket.Session;

import com.fasterxml.jackson.databind.JsonNode;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

import vip.r0n9.JsonUtil;
import vip.r0n9.ws.WebSshHandler;

public class SshSession {

    private Session websession;

    private StringBuilder dataToDst = new StringBuilder();

    private JSch jsch = new JSch();

    private com.jcraft.jsch.Session jschSession;

    private ChannelShell channel;

    private InputStream inputStream;
    private BufferedReader stdout;

    private OutputStream outputStream;
    private PrintWriter printWriter;
    
    public SshSession() {}
    
    public SshSession(String hostname,int port,String username, String password, final Session session2, int rows, int cols) throws JSchException, IOException {
        this.websession = session2;
    	jschSession = jsch.getSession(username, hostname, port);
        jschSession.setPassword(password);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        jschSession.setConfig(config);
        jschSession.connect();

        channel = (ChannelShell) jschSession.openChannel("shell");
        channel.setPty(true);
        channel.setPtyType("xterm");
        channel.setPtySize(cols, rows, cols*8, rows*16);
        inputStream = channel.getInputStream();
        
        outputStream = channel.getOutputStream();
        printWriter = new PrintWriter(outputStream,false);
        channel.connect();
        
        outputStream.write("\r".getBytes());
        outputStream.flush();
        //this.printWriter.write("/bin/bash -i\r\n");
        //this.printWriter.flush();
        Thread thread = new Thread() {

            @Override
            public void run() {

                try {
                	byte[] byteset = new byte[3072];
                	int res = inputStream.read(byteset);
                	if(res == -1)res = 0;
                    while (session2 != null && session2.isOpen()) { // 这里会阻塞，所以必须起线程来读取channel返回内容
                        ByteBuffer byteBuffer = ByteBuffer.wrap(byteset, 0, res);
                        synchronized (this) {
                        	if(res != 0)
                        		session2.getBasicRemote().sendBinary(byteBuffer);
                        }
                    	res = inputStream.read(byteset);
                    	if(res == -1)res = 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    public void close() {
        channel.disconnect();
        jschSession.disconnect();
        try {
			this.websession.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        try {
			this.inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        try {
			this.outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void getMessage(String message) throws IOException, JSchException {

    	Session mysession = this.websession;
        System.out.println("来自客户端 " + mysession.getUserProperties().get("ClientIP") + " 的消息:" + message);

        JsonNode node = JsonUtil.strToJsonObject(message);

        if (node.has("resize")) {
            Iterator<JsonNode> myiter = node.get("resize").elements();
            int col = myiter.next().asInt();
            int row = myiter.next().asInt();
            channel.setPtySize(col, row, col*8, row*16);
            return;
        }

        if (node.has("data")) {
            String str = node.get("data").asText();

            outputStream.write(str.getBytes("utf-8"));
            outputStream.flush();
            //printWriter.write(str);
            //printWriter.flush();

            return;
        }

    }

	public StringBuilder getDataToDst() {
		return dataToDst;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

}
