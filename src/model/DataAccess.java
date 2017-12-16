package model;

import java.util.*;
import java.util.Date;                  // Specifies Dates given below are from java.util

import java.sql.*;                      // Provides with JDBC Classes

import java.io.File;                    // Needed for initDatabase()
import java.io.FileInputStream;


/**
 * Provides the application with high-level methods to access the persistent
 * data store. The class hides the fact that data are stored in a RDBMS and also
 * hides all the complex SQL machinery required to access it.
 * <p>
 * The constructor and the methods of this class all throw a
 * {@link DataAccessException} whenever an unrecoverable error occurs, e.g. the
 * connexion to the database is lost.
 * <p>
 * <b>Note to the implementors</b>: You <b>must not</b> alter the interface of
 * this class' constructor and methods, including the exceptions thrown.
 *
 * @author Jean-Michel Busca
 */
public class DataAccess {
    
    // Private attribute representing connection to the database
    private Connection connection = null;
    
    /**
     * Creates a new <code>DataAccess</code> object that interacts with the
     * specified database, using the specified login and password. Each object
     * maintains a <b>dedicated</b> connection to the database until the
     * {@link close} method is called.
     *
     * @param url the url of the database to connect to
     * @param login the (application) login to use
     * @param password the password
     *
     * @throws DataAccessException if an unrecoverable error occurs
     */
    public DataAccess(String url, String login, String password) throws DataAccessException {
    	try {
            connection = DriverManager.getConnection(url, login, password);
    	}
    	catch(SQLException e) {
            throw new DataAccessException("Error getting connection : " + e.getMessage());
    	}
    }

   /**
     * Creates and populates the database according to all the examples provided
     * in the requirements of marked lab 2. If the database already exists
     * before the method is called, the method discards the database and creates
     * it again from scratch.
     * <p>
     * This implementation executes the SQL script named
     * <code>database.sql</code> located in the project's root directory. It
     * assumes the <code>DataAccess</code> class declares the connection
     * attribute as follows:
     * <p>
     * <code>private Connection connection;</code>
     *
     * @throws DataAccessException if an unrecoverable error occurs
     */
    public void initDatabase() throws DataAccessException {
        int okCount = 0;
        try {
            // read file
            File file = new File("database.sql");
            FileInputStream stream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            stream.read(data);
            stream.close();

            // split contents into statements
            String contents = new String(data);
            String statements[] = contents.split(";");

            // execute statements
            Statement jdbc = connection.createStatement();
            for (String statement : statements) {
                // remove comments
                statement = statement.replaceAll(" *-- .*(\\n|\\r)", "");
                // remove end of lines 
                statement = statement.replaceAll(" *(\\n|\\r)", "");
                if (statement.isEmpty()) {
                    continue;
                }
                String message = "initDatabase(): '" + statement + "': ";
                try {
                    jdbc.executeUpdate(statement);
                    System.err.println(message + "ok");
                    okCount += 1;
                } catch (SQLException e) {
                    System.err.println(message + "FAILED (" + e.getMessage() + ")");
                }
            }
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
        if (okCount == 0) {
            throw new DataAccessException("failed to create database");
        }
    }
    
    /**
     * See Operation 2.1.1.
     *
     * @param departureStation
     * @param arrivalStation
     * @param fromDate
     * @param toDate
     *
     * @return the corresponding list of journeys, including the empty list if
     * no journey is found
     *
     * @throws DataAccessException if an unrecoverable error occurs
     */
    public List<Journey> getTrainTimes(String departureStation, String arrivalStation, Date fromDate, Date toDate)
        throws DataAccessException {
        return null;
    }

    /**
     * See Operation 2.1.2
     *
     * @param departureStation
     * @param arrivalStation
     * @param travelPeriod
     * @param passengerCount
     * @param travelClass
     *
     * @return the bought ticket, or <code>null</code> if some parameter was
     * incorrect
     *
     * @throws DataAccessException if an unrecoverable error occurs
     */
    public Ticket buyTicket(String departureStation, String arrivalStation, Period travelPeriod, int passengerCount, Class travelClass)
        throws DataAccessException {
        return null;
    }

    /**
     * See Operation 2.1.3.
     *
     * @param trainNumber
     * @param departureDate
     * @param departureStation
     * @param arrivalStation
     * @param passengerCount
     * @param travelClass
     * @param customerEmail
     *
     * @return the booking, or <code>null</code> if some parameter was incorrect
     * or not enough seats were available
     *
     * @throws DataAccessException if an unrecoverable error occurs
     */
    public Booking buyTicketAndBook(int trainNumber, Date departureDate, String departureStation, String arrivalStation, int passengerCount, Class travelClass, String customerEmail)
        throws DataAccessException {
        // TODO
        return null;
    }

    /**
     * See Operation 2.1.4
     *
     * @param bookingID
     * @param customerEmail
     *
     * @return <code>true</code> if the booking was cancelled, and
     * <code>false</code> otherwise
     *
     * @throws DataAccessException if an unrecoverable error occurs
     */
    public boolean cancelBooking(String bookingID, String customerEmail)
        throws DataAccessException {
        // TODO
        return false;
    }

    /**
     * See Operation 2.2.2
     *
     * @param trainNumber
     * @param departureDate
     * @param beginStation
     * @param endStation
     *
     * @return the list of available seats, including the empty list if no seat
     * is available
     *
     * @throws DataAccessException if an unrecoverable error occurs
     */
    public List<Integer> getAvailableSeats(int trainNumber, Date departureDate, String beginStation, String endStation)
        throws DataAccessException {
        // TODO
        return null;
    }

    /**
     * Closes the underlying connection and releases all related ressources. The
     * application must call this method when it is done accessing the data
     * store.
     *
     * @throws DataAccessException if an unrecoverable error occurs
     */
    public void close() throws DataAccessException {
        // Closing the connection
    	try {
    		connection.close();
    	}
    	catch(SQLException e) {
    		throw new DataAccessException("Error closing connection : " + e.getMessage());
    	}
    }
}
