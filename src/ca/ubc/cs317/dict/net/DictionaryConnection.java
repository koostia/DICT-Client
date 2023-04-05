package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jonatan on 2017-09-09.
 */
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    /** Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {

        // TODO Add your code here

        try {

            // Creating socket from host to port
            socket = new Socket(host, port);
            // Initializing input to read input stream from the server
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Initializing output to write to output stream to the server
            output = new PrintWriter(socket.getOutputStream(), true);
            // Reading the first line (Status message)
            Status status = Status.readStatus(input);

            if (status.getStatusCode() != 220) {

                // If code 220 is not sent, initial connection is not established
                throw new DictConnectionException(status.getDetails());

            }

        } catch (Exception e) {

            throw new DictConnectionException(e);

        }

    }

    /** Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /** Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     *
     */
    public synchronized void close() {

        // TODO Add your code here

        if (socket.isConnected()) {

            try {

                // Output to server the message "QUIT"
                output.println("QUIT");
                // Read following input stream
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Assign and read status message
                Status status = Status.readStatus(input);

                if (status.getStatusCode() == 221) {

                    // If status code 221 is sent, close the connection
                    socket.close();


                } else {

                    throw new DictConnectionException(status.getDetails());

                }

            } catch (Exception e) {

                // Do nothing

            }
        }

    }

    /** Requests and retrieves all definitions for a specific word.
     *
     * @param word The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();

        // TODO Add your code here

        if (socket.isConnected()) {

            try {

                // Output to the server the DEFINE command
                output.println("DEFINE " + database.getName() + " " + word);

                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Assign and read status message
                Status status = Status.readStatus(input);

                if (status.getStatusCode() == 150) {

                    // Splitting the status details into an array to get the number of definitions
                    String[] details = status.getDetails().split(" ");

                    // Assigning the number of definitions from string to int
                    Integer numDef = Integer.parseInt(details[0]);

                    int i = 0;

                    do {

                        // Splitting the definition status code details
                        String[] definitionsDetail = input.readLine().split(" ", 4);

                        if (definitionsDetail[0].equals("151")) {

                            // Creating a definitions class with word and the database it's from
                            Definition def = new Definition(word, definitionsDetail[2]);

                            boolean end = false;

                            do {

                                // Read each of the following line
                                String line = input.readLine();

                                if (!line.equals("")) {

                                    if (line.equals(".") && line.length() == 1) {

                                        // If line reaches the end (Reached when "." is printed), end is equal to true
                                        end = true;

                                    } else {

                                        // If end is not reached, append the definition with the new line
                                        def.appendDefinition(line);

                                    }

                                } else {

                                    // If there is a blank line, add a blank line to the definition
                                    def.appendDefinition("");

                                }

                            } while (!end);

                            // Add the completed definition to the set
                            set.add(def);
                            // Increase the counter and move on the next definition
                            i++;

                        }

                    } while (i != numDef);

                } else if (status.getStatusCode() == 552) {

                    // If no match, then return an empty set
                    return set;

                } else if (status.getStatusCode() == 550){

                    // If it's not a valid database, throw DictConnectionException
                    throw new DictConnectionException("Invalid database, use \"SHOW DB\" for list of databases");

                } else {

                    // If all other responses fail, then it must be an unknown command
                    throw new DictConnectionException("500 unknown command");

                }

            } catch (Exception e) {

                // Do nothing

            }

        } else {

            throw new DictConnectionException("Socket is not connected");

        }

        return set;
    }

    /** Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();

        // TODO Add your code here

        if (socket.isConnected()) {

            try {

                // Output to the server the MATCH command
                output.println("MATCH " + database.getName() + " " + strategy.getName() + " " + word);

                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Assign and read status message
                Status status = Status.readStatus(input);

                if (status.getStatusCode() == 152) {

                    // Splitting the match status code details
                    String[] details = status.getDetails().split(" ");

                    // Assigning the total number of word matches found to matches
                    Integer matches = Integer.parseInt(details[0]);

                    for (int i = 0; i < matches; i++) {

                        // Splitting each line
                        String[] arr = input.readLine().split(" ", 2);

                        // Taking out all the quotation for each word matches
                        arr[1] = arr[1].replaceAll("\"", "");

                        // Adding all the word matches to the set
                        set.add(arr[1]);

                    }

                } else if (status.getStatusCode() == 552) {

                    // If no matches are found, return empty set
                    return set;

                } else if (status.getStatusCode() == 550) {

                    // If it's not a valid database, throw DictConnectionException
                    throw new DictConnectionException("550 Invalid database, use \"SHOW DB\" for list of databases");

                } else if (status.getStatusCode() == 551) {

                    // If it's not a valid strategy, throw DictConnectionException
                    throw new DictConnectionException("551 Invalid strategy, use \"SHOW STRAT\" for a list of strategies");

                } else {

                    // If all other responses fail, then it must be an unknown command
                    throw new DictConnectionException("500 unknown command");

                }

            } catch (Exception e) {

                // Do nothing

            }

        } else {

            throw new DictConnectionException("Socket is not connected");

        }

        return set;
    }

    /** Requests and retrieves a map of database name to an equivalent database object for all valid databases used in the server.
     *
     * @return A map of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        Map<String, Database> databaseMap = new HashMap<>();

        // TODO Add your code here

        if (socket.isConnected()) {

            try {

                // Output the server the SHOW DB command
                output.println("SHOW DB");

                // Assign and read status message
                Status status = Status.readStatus(input);

                if (status.getStatusCode() == 110) {

                    // Splitting the SHOW DB status code details
                    String[] details = status.getDetails().split(" ");

                    // Assigning the total number of databases found to numDB
                    Integer numDB = Integer.parseInt(details[0]);

                    for (int i = 0; i < numDB; i++) {

                        // Splitting each line
                        String[] arr = input.readLine().split(" ", 2);

                        // Taking out all the quotation for each database
                        arr[1] = arr[1].replaceAll("\"", "");

                        // Adding each database to the databaseMap
                        databaseMap.put(arr[0], new Database(arr[0], arr[1]));


                    }

                } else if (status.getStatusCode() == 554) {

                    // If no database are present, throw DictConnectionException
                    throw new DictConnectionException("554 No databases present");

                } else {

                    // If all other responses fail, then it must be an unknown command
                    throw new DictConnectionException("500 unknown command");

                }

            } catch (Exception e) {

                // Do nothing

            }

        } else {

            throw new DictConnectionException("Socket is not connected");

        }

        return databaseMap;
    }

    /** Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();

        // TODO Add your code here

        if (socket.isConnected()) {

            try {

                // Output to the server the SHOW STRAT command
                output.println("SHOW STRAT");

                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Assign and read status message
                Status status = Status.readStatus(input);

                if (status.getStatusCode() == 111) {

                    // Splitting the SHOW STRAT status code details
                    String[] details = status.getDetails().split(" ");

                    // Assigning the total number of strategies found to numSTRAT
                    Integer numSTRAT = Integer.parseInt(details[0]);

                    for (int i = 0; i < numSTRAT; i++) {

                        // Split each line
                        String[] arr = input.readLine().split(" ", 2);

                        // Taking out all the quotation for each strategy
                        arr[1] = arr[1].replaceAll("\"", "");

                        // Create a new MatchingStrategy class with details from line
                        MatchingStrategy ms = new MatchingStrategy(arr[0], arr[1]);

                        // Add the MatchingStrategy to a set
                        set.add(ms);

                    }

                } else if (status.getStatusCode() == 555) {

                    // If no strategies are available, throw DictConnectionException
                    throw new DictConnectionException("555 No strategies available");

                } else {

                    // If all other responses fail, then it must be an unknown command
                    throw new DictConnectionException("500 unknown command");

                }

            } catch (Exception e) {

                // Do nothing

            }

        } else {

            throw new DictConnectionException("Socket is not connected");

        }

        return set;
    }

    /** Requests and retrieves detailed information about the currently selected database.
     *
     * @return A string containing the information returned by the server in response to a "SHOW INFO <db>" command.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized String getDatabaseInfo(Database d) throws DictConnectionException {
	StringBuilder sb = new StringBuilder();

        // TODO Add your code here

        if (socket.isConnected()) {

            try {

                // Output to server the SHOW INFO command
                output.println("SHOW INFO " + d.getName());

                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Assign and read status message
                Status status = Status.readStatus(input);

                if (status.getStatusCode() == 112) {

                    boolean end = false;

                    do {

                        // Read each line
                        String line = input.readLine();

                        if (!line.equals("")) {

                            if (line.equals(".") && line.length() == 1) {

                                // If line reaches the end (Reached when "." is printed), end is equal to true
                                end = true;

                            } else {

                                // Append the line with a blank space following it
                                sb.append(line + "\n");

                            }
                        }

                    } while (!end);

                } else if (status.getStatusCode() == 550){

                    // If it's not a valid database, throw DictConnectionException
                    throw new DictConnectionException("Invalid database, use \"SHOW DB\" for list of databases");

                } else {

                    // If all other responses fail, then it must be an unknown command
                    throw new DictConnectionException("500 unknown command");

                }

            } catch (Exception e) {

                // Do nothing

            }

        } else {

            throw new DictConnectionException("Socket is not connected");

        }

        return sb.toString();

    }
}
