
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

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
    private String url;

    /**
     * Basic HTTP Auth.
     */
    private String http_user;
    private String http_pass;

    /**
     * Elasticsearch Index & Type for BULK inserts
     */
    public String es_index;
    public String es_type;
    
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
     * Construct.
     * @param clientSocket client socket.
     * @param url 	Web-server URL.
     */
    public ClientHandler(Socket clientSocket, String url, String http_user, String http_pass, String es_index, String es_type) {

        this.clientSocket = clientSocket;
        this.url = url;

	// Optional HTTP Basic Auth
        this.http_user = http_user;
        this.http_pass = http_pass;

	// ES Index and Type
        this.es_index = es_index;
        this.es_type  = es_type;

        System.out.println("Client connected with Address " + clientSocket.getInetAddress().toString() + " on port: " + clientSocket.getPort() + "\n");

        try {
            // System.out.println("Initializing socket buffers size ... ");
            clientSocket.setReceiveBufferSize(2);
            clientSocket.setSendBufferSize(2);

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
            while ((line = in.readLine()) != null) {
                message += line + "\n";
            }
        }
        catch (IOException e) {
            System.err.println(e);
        }

        if (!message.isEmpty()) {
            // System.out.println("Received JSON: " + message + "\n");
            // System.out.println("Sending to web server ... ");
            sendToWebServer(message);
        }
        else {
            System.out.println("Message is empty\n");
        }
        disconnect();
    }

    /**
     * Sends message to Web-server via HTTP.
     * @param message RAW message from the probe.
     */
    private void sendToWebServer(String message) {
        try {
            // Encode the message to URLEncoded format
            // String newJson = "message=" + URLEncoder.encode(message, "UTF-8");

            // Prepend the message with JSON indexing for ES
	    String dateNow = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
            String newJson = "{\"index\":{\"_index\":\"" + es_index + "-" + dateNow + "\",\"_type\":\"" + es_type + "\"}}\n" + message;

            URL webServerUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)webServerUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(newJson.getBytes().length));
            connection.setRequestProperty("User-Agent", null);
            connection.setRequestProperty("Accept", null);
            connection.setRequestProperty("Host", null);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

	    if (http_user != null && !http_user.isEmpty() ) {
	    	String userAuth = http_user + ":" + http_pass;
	    	String encoding = new sun.misc.BASE64Encoder().encode(userAuth.getBytes());
	    	connection.setRequestProperty("Authorization", "Basic " + encoding);
	    }

            // Send the message to the server
            DataOutputStream out = new DataOutputStream (connection.getOutputStream());
            out.writeBytes(newJson);
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
            // System.out.print("HTTP request sent with result: " + result);

            connection.disconnect();
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }
}
