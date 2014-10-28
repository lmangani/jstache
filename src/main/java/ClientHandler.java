
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

/**
 * TCP/IP client connection handler.
 *
 * Runs in separate thread.
 * Accepts message via TCP/IP and forwards it to the Web-server via HTTP.
 */
public class ClientHandler implements Runnable {
    /**
     * Client socket.
     */
    private Socket clientSocket;

    /**
     * Web-server URL.
     */
    private static String url;

    /**
     * Basic HTTP Auth.
     */
    private static String http_user;
    private static String http_pass;

    /**
     * Elasticsearch Index & Type for BULK inserts
     */
    private static String es_index;
    private static String es_type;

    /**
     * Send/Receive buffers
     */
    private static String buffers;
    private static boolean debugme;

    /**
     * Client socket IN-buffer.
     */
    private BufferedReader in;

    /**
     * Client socket OUT-buffer.
     */
    private PrintWriter out;

    /**
     * Handler thread.
     */
    private Thread runningThread;

    /**
     * JSON Validation
     */
    public boolean isJson(String test)
	{
	JSONParser parser = new JSONParser();
	try {
	      parser.parse(test);
	    } catch (org.json.simple.parser.ParseException e) {
	            return false;
	    }
	    return true;
	}

    /**
     * Properties Handler
     */
    public static void configure(Properties properties)
	{
		url = properties.getProperty("handler.url", "http://127.0.0.1:9200/_bulk");
		http_user = properties.getProperty("handler.http_user", "");
		http_pass = properties.getProperty("handler.http_pass", "");
		es_index  = properties.getProperty("es.index", "logstash");
		es_type   = properties.getProperty("es.type", "jstache");
		buffers   = properties.getProperty("handler.buffers", "2");

		if (!"".equals(properties.getProperty("handler.debug",""))) { 
			debugme = true; 
		} else { 
			debugme = false; 
		}

	}

    /**
     * Construct.
     * @param clientSocket client socket.
     * @param url 	Web-server URL.
     */
    public ClientHandler(Socket clientSocket) {

        this.clientSocket = clientSocket;
	// Bulk API Url
        this.url = url;
	// Optional HTTP Basic Auth
        this.http_user = http_user;
        this.http_pass = http_pass;
	// ES Index and Type
        this.es_index = es_index;
        this.es_type  = es_type;

	// Set TZ to UTC for @timestamp field
	TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        if(debugme) System.out.println("Client connected with Address " + clientSocket.getInetAddress().toString() + " on port: " + clientSocket.getPort() + "\n");

        try {
            // System.out.println("Initializing socket buffers size ... ");
            clientSocket.setReceiveBufferSize(Integer.parseInt(buffers));
            clientSocket.setSendBufferSize(Integer.parseInt(buffers));

            // System.out.println("Initializing input buffer ... ");
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // System.out.println("Initializing output buffer ... ");
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // System.out.println("Starting handler thread ... ");
            runningThread = new Thread(this);
            runningThread.start();
        }
        catch (Exception e) {
            System.err.println(e);
            disconnect();
        }
    }

    /**
     * Disconnect client socket.
     * Interrupts handler thread, closes buffers and disconnects client.
     */
    private void disconnect() {
        System.out.println("Disconnecting ... ");
        if (runningThread != null) {
            runningThread.interrupt();
            runningThread = null;
        }

        if (in != null) {
            try {
                in.close();
            } catch (Exception e){
                System.err.println(e);
            }
            in = null;
        }

        if (out != null) {
            try {
                out.close();
            } catch(Exception e){
                System.err.println(e);
            }
            out = null;
        }

        try {
            clientSocket.close();
        } catch(Exception e){
            System.err.println(e);
        }
        clientSocket = null;

    }

    /**
     * Runs client socket handler.
     */
    @Override
    public void run() {
        String line;
        String message = "";
        try {
            while ((line = in.readLine()) != null ) {
		//System.out.print("LINE: "+line);
                message = line.trim() + "\n";

		// Work Socket Line-by-Line
	        if (!message.isEmpty() && isJson(message) ) {
	            if(debugme) System.out.println("Received JSON: " + message );
		      // No @timestamp? No @problem! TODO: add as optional in config
		      if (!message.contains("@timestamp")) {
		    	String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());
		    	message = message.startsWith("{") ? "{\"@timestamp\":\""+dateStr+"\","+message.substring(1) : message;
		      }
	              // Prepend the message with JSON indexing for ES Bulk indexing
		      String dateNow = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
	              message = "{\"index\":{\"_index\":\"" + es_index + "-" + dateNow + "\",\"_type\":\"" + es_type + "\" }}\n" + message;

	            // System.out.println("Sending to web server ... ");
	            sendToWebServer(message);
	        }
	        else {
	             if(debugme) System.out.println("String empty or invalid.");
	        }

            }
        }
        catch (IOException e) {
            System.err.println(e);
	    disconnect();
        }

        disconnect();
    }

    /**
     * Sends message to Web-server via HTTP.
     * @param message RAW message from the probe.
     */
    private void sendToWebServer(String message) {
        try {

            URL webServerUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)webServerUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(message.getBytes().length));
            connection.setRequestProperty("User-Agent", "JStache");
            connection.setRequestProperty("Accept", null);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

	    if (http_user != null && !http_user.isEmpty() ) {
	    	String pairAuth = http_user + ":" + http_pass;
		byte[] byteArray = Base64.encodeBase64(pairAuth.getBytes());
		String userAuth = new String(byteArray);
	    	connection.setRequestProperty("Authorization", "Basic " + userAuth);
	    }

            // Send the message to the server
            DataOutputStream out = new DataOutputStream (connection.getOutputStream());
            out.writeBytes(message);
            out.flush();
            out.close();

            // Read response from the server
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            String result = "";
            while ((inputLine = in.readLine()) != null) {
                result += inputLine + "\n";
            }
            in.close();
            if(debugme) System.out.println("HTTP result: " + result);

            connection.disconnect();
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }
}
