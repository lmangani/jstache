import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * TCP/IP Server.
 *
 * Listens port, accepts client connections and forwards them to ClientHandler in separate thread.
 */
public class Server {
    /**
     * Master server socket.
     */
    private static ServerSocket serverSocket = null;

    /**
     * Server configuration.
     */
    private static Properties config = new Properties();

    private static Logger logger = Logger.getLogger("jstache");

    /**
     * Loads configuration from file 'config.properties'.
     * First it checks user defined 'config.properties' file in the current working directory.
     * If file does not exist it loads predefined file from server classpath.
     */
    private static void initConfig() {
        // logger.log(Level.INFO,"Loading user config ... ");
        try {
            config.load(new FileInputStream("config.properties"));
        }
        catch (IOException e) {
            // logger.log(Level.INFO,"error:\n");
            System.out.println(e);
            logger.log(Level.INFO,"User config not found, Loading default config ... ");
            try {
                config.load(Server.class.getClassLoader().getResourceAsStream("config.properties"));
            }
            catch (IOException e1) {
                System.err.println(e1);
                System.exit(1);
            }
        }

        config.list(System.out);
	// Push Properties
	ClientHandler.configure(config);
        System.out.println();
    }

    /**
     * Main entry point.
     * @param args application command line parameters
     */
    public static void main(String[] args) {

        FileHandler fh;
        try {
	      // This block configure the logger with handler and formatter
	      fh = new FileHandler("/var/log/jstache/jstache.log", true);
	      logger.addHandler(fh);
	      logger.setLevel(Level.ALL);
	      SimpleFormatter formatter = new SimpleFormatter();
	      fh.setFormatter(formatter);
	      // the following statement is used to log any messages   
	    } catch (SecurityException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
        }

        logger.log(Level.INFO,"JStache started...");

        // Load configuration
        initConfig();

        // Run server on the selected port
        int port = Integer.parseInt(config.getProperty("server.port"));
        // logger.log(Level.INFO,"Starting server on port " + port + " ... ");
        try {
            serverSocket = new ServerSocket(port);
        }
        catch (IOException e) {
            logger.log(Level.INFO,"Could not bind, port " + port + " already in use.");
            System.out.println("Could not bind, port "+port+" already in use.");
            System.exit(1);
        }

        // Accept client connections
        logger.log(Level.INFO,"Jstash UP & Awaiting client connections... \n");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();

        	logger.log(Level.INFO,"Shipping packet to ES index: " + config.getProperty("es.index") +" type: "+ config.getProperty("es.type") );
                // Run separate thread for client handler
                // new ClientHandler(clientSocket, config.getProperty("handler.url"), config.getProperty("handler.http_user"), config.getProperty("handler.http_pass"), config.getProperty("es.index"), config.getProperty("es.type"));
                new ClientHandler(clientSocket);
            }
            catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
