
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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

    private static Logger logger = Logger.getLogger("jstache");

    /**
     * Construct.
     * @param clientSocket client socket.
     * @param url 	Web-server URL.
     */
    public ClientHandler(Socket clientSocket, String url, String http_user, String http_pass) {

	FileHandler fh;
        try {
              // This block configure the logger with handler and formatter
              fh = new FileHandler("/var/log/jstache/jstache.log", true);
              logger.addHandler(fh);
              logger.setLevel(Level.ALL);
              SimpleFormatter formatter = new SimpleFormatter();
              fh.setFormatter(formatter);
            } catch (SecurityException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
        }


        this.clientSocket = clientSocket;
        this.url = url;

	// Optional HTTP Basic Auth
        this.http_user = http_user;
        this.http_pass = http_pass;

        logger.log(Level.INFO,"Client connected with Address " + clientSocket.getInetAddress().toString() + " on port: " + clientSocket.getPort() + "\n");

        try {
            // logger.log(Level.INFO,"Initializing socket buffers size ... ");
            clientSocket.setReceiveBufferSize(2);
            clientSocket.setSendBufferSize(2);

            // logger.log(Level.INFO,"Initializing input buffer ... ");
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // logger.log(Level.INFO,"Initializing output buffer ... ");
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // logger.log(Level.INFO,"Starting handler thread ... ");
            runningThread = new Thread(this);
            runningThread.start();
        }
        catch (Exception e) {
            logger.log(Level.INFO,"DISCONNECT: "+e);
            System.err.println(e);
            disconnect();
        }
    }

    /**
     * Disconnect client socket.
     * Interrupts handler thread, closes buffers and disconnects client.
     */
    private void disconnect() {
        logger.log(Level.INFO,"Disconnecting ... ");
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
        // logger.log(Level.INFO,"Reading input buffer ... ");
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
            // logger.log(Level.INFO,"Received JSON: " + message + "\n");
            // logger.log(Level.INFO,"Sending to web server ... ");
            sendToWebServer(message);
        }
        else {
            logger.log(Level.INFO,"Message is empty\n");
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
            // String urlParameters = "message=" + URLEncoder.encode(message, "UTF-8");

            // Prepend the message with JSON indexing for ES
	    String dateNow = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
            String newJson = "{\"index\":{\"_index\":\"nprobe-" + dateNow + "\",\"_type\":\"nProbe\"}}\n" + message;

	    // logger.log(Level.INFO,newJson);

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
            // logger.log(Level.INFO,"HTTP request sent with result: " + result);

            connection.disconnect();
        }
        catch (Exception e) {
            logger.log(Level.INFO,"ERROR: "+e);
        }
    }
}
