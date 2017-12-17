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
    
    // private attribute representing connection to the database
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
     * Getting the period from a given date
     *
     * @param date
     *
     * @return the corresponding period, null if this date matches with no
     * period in the database
     *
     * @throws SQLException if an unrecoverable error occurs
     */
    private String getPeriodFromDate(Date date)
        throws SQLException {
        
        // extracting year, month and day from the given year
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;        // 0 => 11 otherwise
        int day = cal.get(Calendar.DAY_OF_MONTH);
        
        // query preparation
        PreparedStatement st = connection.prepareStatement(""
                + "SELECT couleurPeriode "
                + "FROM PlageDates "
                + "WHERE (? >= YEAR(debut) AND ? >= MONTH(debut) AND ? >= DAY(debut)) AND "
                + "(? <= YEAR(fin) AND ? <= MONTH(fin) AND ? <= DAY(fin))");
        st.setInt(1, year); 
        st.setInt(2, month); 
        st.setInt(3, day); 
        st.setInt(4, year); 
        st.setInt(5, month); 
        st.setInt(6, day); 
        
        // query execution
        ResultSet result = st.executeQuery();
        
        // returns the period if any was found, null otherwise
        return (result.next()) ? result.getString(1) : null; 
    }
    
     /**
     * Getting a train's planning during the given DAY
     *
     * @param train
     * @param date
     *
     * @return the [station => datetime] mapping for the train during this day
     * null if the train does not travel during this day
     * @throws SQLException if an unrecoverable error occurs
     */
    private Map<String, Date> getTrainPlanning(int train, Date date)
        throws SQLException {
        
        // the planning to return
        Map<String, Date> planning = new HashMap<String, Date>();
        
        // getting the corresponding period
        String period = getPeriodFromDate(date);
        
        // query preparation (rk: we only store one segment for both directions)
        PreparedStatement st = connection.prepareStatement(""
                + "SELECT TS.gareDepart, TS.gareArrivee, vitesse, rang, S.longueur, horaire "
                + "FROM (Train_Segment TS NATURAL JOIN Depart) JOIN Segment S ON "
                + "(TS.gareDepart = S.gareDepart AND TS.gareArrivee = S.gareArrivee) OR "
                + "(TS.gareDepart = S.gareArrivee AND TS.gareArrivee = S.gareDepart) "
                + "WHERE numeroTrain = ? AND "
                + "couleurPeriode = ? "
                + "ORDER BY rang");   
        st.setInt(1, train);
        st.setString(2, period);
        
        // query execution
        ResultSet result = st.executeQuery();
        
        // constructing the initial station (returns null if result is empty)
        if(!result.next())
            return null;
        
        // main calendar to store the complete dates of station serving
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        
        // calendar only representing time (because Time class is deprecated)
        Calendar time = Calendar.getInstance();
        time.setTime(result.getTime(6, cal));                   // initial time, "horaire" field
        
        // applying the initial TIME to the given DAY
        cal.set(Calendar.HOUR, time.get(Calendar.HOUR_OF_DAY)); 
        cal.set(Calendar.MINUTE, time.get(Calendar.MINUTE)); 
        cal.set(Calendar.SECOND, time.get(Calendar.SECOND)); 
        
        // declaration of the loop variables
        String depStation = result.getString(1);        // departureStation
        String arrStation = null;                       // arrivalStation
        double speed = 0.0d, distance = 0.0d;
        double addedRawHours = 0.0d;
        int addedHours, addedMinutes, addedSeconds;
        
        // we already know the data for the first station
        planning.put(depStation, cal.getTime());
        
        do {
            /// adding the arrival station to the planning
            // getting the name of the segment's arrival station
            arrStation = result.getString(2);
            
            // computing the added time to reach the station
            speed = result.getDouble(3);
            distance = result.getDouble(5);
            
            addedRawHours = distance/speed;                                 // t = d/v => X.XXXX hours
            
            // conversion into time and addition to the previous date
            addedHours = (int)addedRawHours;
            // Decimal part of hours x 60, int truncated
            addedMinutes = (int)((addedRawHours - (double)addedHours)*60); 
            // Decimal part of minutes x 60, rounded this time because we have no more precision
            addedSeconds = (int)Math.round((((addedRawHours - (double)addedHours)*60) - (double)addedMinutes)*60);
            
            cal.add(Calendar.HOUR_OF_DAY, addedHours);
            cal.add(Calendar.MINUTE, addedMinutes);
            cal.add(Calendar.SECOND, addedSeconds);
               
            planning.put(arrStation, cal.getTime());
            
        }while(result.next());                                      // Leaving or going on to the next segment             
        
        return planning;
    }
    
     /**
     * Getting the list of trains matching a given journey
     *
     * @param departureStation
     * @param arrivalStation
     *
     * @return the corresponding list of trains, null if no train matches the journey
     *
     * @throws SQLException if an unrecoverable error occurs
     */
    private List<Integer> getTrainsMatchingJourney(String departureStation, String arrivalStation)
        throws SQLException {
        
        // the list to return
        List<Integer> trains = new ArrayList<Integer>();
        
        // query preparation
        PreparedStatement st = connection.prepareStatement(""
                + "SELECT T1.numeroTrain "
                + "FROM Train_Segment T1 JOIN Train_Segment T2 ON "
                + "T1.numeroTrain = T2.numeroTrain "
                + "WHERE T1.gareDepart = ? AND T2.gareArrivee = ? AND "
                + "T1.rang <= T2.rang");
        st.setString(1, departureStation);
        st.setString(2, arrivalStation);
        
        // query execution
        ResultSet result = st.executeQuery();
        
        // returning the mathcing trains, null if none
        if(!result.next())
            return null;
        do {
            trains.add(result.getInt(1));                   // autoboxing int => Integer
        }while(result.next());
        
        return trains;
    }
    
     /**
     * Getting the list of trains matching a given journey during a given period
     *
     * @param departureStation
     * @param arrivalStation
     * @param period
     * @return the corresponding list of trains, null if no train matches the journey during that period
     *
     * @throws SQLException if an unrecoverable error occurs
     */
    private List<Integer> getTrainsMatchingJourney(String departureStation, String arrivalStation, String period)
        throws SQLException {
        
        // the list to return
        List<Integer> trains = new ArrayList<Integer>();
        
        // query preparation: only the trains that match stations during the specified period
        PreparedStatement st = connection.prepareStatement(""
                + "SELECT T1.numeroTrain "
                + "FROM (Train_Segment T1 JOIN Train_Segment T2 ON "
                + "T1.numeroTrain = T2.numeroTrain) JOIN Depart ON Depart.numeroTrain = T1.numeroTrain "
                + "WHERE T1.gareDepart = ? AND T2.gareArrivee = ? AND "
                + "T1.rang <= T2.rang AND "
                + "couleurPeriode = ?");
        st.setString(1, departureStation);
        st.setString(2, arrivalStation);
        st.setString(3, period);
        
        // query execution
        ResultSet result = st.executeQuery();
        
        // returning the mathcing trains, null if none
        if(!result.next())
            return null;
        do {
            trains.add(result.getInt(1));                   // autoboxing int => Integer
        }while(result.next());
        
        return trains;
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
        
        // the list to return
        List<Journey> journeys = new ArrayList<Journey>();
        
        // the journey to add
        Journey journey = null; 
        
        // planning of the current train during the current day
        Map<String, Date> trainPlanning = null; 
        
        // dates manipulation
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.setTime(fromDate);
        end.setTime(toDate);
        
        // we need to loop over the DAYS, so we set the same times for the calendars
        start.set(Calendar.HOUR_OF_DAY, 1); 
        start.set(Calendar.MINUTE, 1);
        start.set(Calendar.SECOND, 1); 
        end.set(Calendar.HOUR_OF_DAY, 1); 
        end.set(Calendar.MINUTE, 1);
        end.set(Calendar.SECOND, 1); 
        
        // encapsulate data queries into an ACID transaction 
        try {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            
            // getting the trains that match the stations (in the right order)
            List<Integer> trains = getTrainsMatchingJourney(departureStation, arrivalStation); 
            if(trains == null) {
                // no train in the database matches the stations
                connection.commit();
                return journeys;
            }
                
            // for each given day
            Date date = start.getTime();
            while(!start.after(end))
            {
                for(Integer train : trains) {
                   trainPlanning = getTrainPlanning(train, date);
                   if(trainPlanning != null) {
                       // if the departure is after the full fromDate and arrival before the full toDate, keep the journey
                        if(trainPlanning.get(departureStation).after(fromDate) && trainPlanning.get(arrivalStation).before(toDate))
                            journeys.add(new Journey(departureStation, arrivalStation, train, trainPlanning.get(departureStation), trainPlanning.get(arrivalStation)));
                   }
                }
                // adding a day to the starting date
                start.add(Calendar.DATE, 1);
                date = start.getTime();
            }
            // committing the transaction - next transaction will start after the next SQL statement
            connection.commit();
        }
        catch(SQLException e) {
            // making sure the transaction is aborted
            try {
                connection.rollback();
            }
            catch (SQLException ee) {
                throw new DataAccessException("Failing rollbacking transaction in 2.1.1: " + ee.getMessage());
            }
            throw new DataAccessException("Error occured in 2.1.1: " + e.getMessage());
        }
        return journeys; 
    }

     /**
     * Getting the distance (in km) between two stations crossed by a train
     *
     * @param train
     * @param departureStation
     * @param arrivalStation
     * @return the corresponding distance
     *
     * @throws SQLException if an unrecoverable error occurs
     */
    private Float getDistance(int train, String departureStation, String arrivalStation)
        throws SQLException {
        
        // the train has to travel to the passed stations
        if(!getTrainsMatchingJourney(departureStation, arrivalStation).contains(train))
            return null;
        
        // the distance to return
        Float distance = new Float(0.0f);
        
        // query preparation
        PreparedStatement st = connection.prepareStatement(""
                + "SELECT TS.gareDepart, TS.gareArrivee, S.longueur "
                + "FROM Train_Segment TS JOIN Segment S ON "
                + "(TS.gareDepart = S.gareDepart AND TS.gareArrivee = S.gareArrivee) OR "
                + "(TS.gareDepart = S.gareArrivee AND TS.gareArrivee = S.gareDepart) "
                + "WHERE TS.numeroTrain = ? "
                + "ORDER BY TS.rang");
        st.setInt(1, train);
        
        // query execution
        ResultSet result = st.executeQuery();
        
        // if unknown train return null
        if(!result.next())
            return null;
        
        // wait for the departure station to come in the list of segments
        do {
            if(result.getString(1).equals(departureStation))
                break; 
        }while(result.next());
        
        // we can now begin to increase the distance
        do {
            distance += result.getFloat(3);                         // add the segment length to global distance
            if(result.getString(2).equals(arrivalStation))          // break loop if we arrived
                break;
        }while(result.next());
        
        return distance;
    }

     /**
     * Getting the total price of a train ticket (without reservation)
     *
     * @param period
     * @param travelClass
     * @param distance
     * @param passengerCount
     * 
     * @return the corresponding ticket price
     *
     * @throws SQLException if an unrecoverable error occurs
     */
    public Float getPrice(String period, String travelClass, Float distance, int passengerCount)
        throws SQLException {
        
        // price by km query preparation
        PreparedStatement stPriceKm = connection.prepareStatement(""
                + "SELECT prixAuKm "
                + "FROM Classe "
                + "WHERE nomClasse = ?");
        stPriceKm.setString(1, travelClass);

        // price variation query preparation
        PreparedStatement stPriceVar = connection.prepareStatement(""
                + "SELECT variationTarif "
                + "FROM Periode "
                + "WHERE couleurPeriode = ?");
        stPriceVar.setString(1, period);
        
        // queries execution
        ResultSet resKm = stPriceKm.executeQuery();
        ResultSet resVar = stPriceVar.executeQuery();
        
        if(!resKm.next() || !resVar.next())
            return null;
       
        // compute the price
        Float price = new Float((float)passengerCount * distance * resKm.getFloat(1) * resVar.getFloat(1));
        
        // round the price to 2 decimals: no half cent
        price = Math.round(price * 100.0f)/100.0f; 
        return price;
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
       
        Float distance = 0.0f;
        Float price = 0.0f;
        
        // period conversion
        String period;
        switch(travelPeriod) {
            case BLUE: period = "bleue";
            break;
            case WHITE: period = "blanche";
            break;
            case RED: period = "rouge";
            break;
            default: period = null;    
        }
        // class conversion
        String tClass;
        switch(travelClass) {
            case FIRST: tClass = "premiere";
            break;
            case SECOND: tClass = "seconde";
            break;
            default: tClass = null;
        }

        // if invalid period or class (not likely because of the enum) return null
        if(period == null || tClass == null)
            return null;
        
        // encapsulate data queries into an ACID transaction 
        try {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            
            // we first check that the wanted journey (stations + period) is possible with available trains
            List<Integer> trains = getTrainsMatchingJourney(departureStation, arrivalStation, period); 
            if(trains == null) {
                // no train in the database matches the ticket
                connection.commit();
                return null;
            }
            
            // compute the distance separating the stations, using one of the matching trains
            distance = getDistance(trains.get(0), departureStation, arrivalStation);
            
            // compute the final price of the ticket, giving it all necessary data
            price = getPrice(period, tClass, distance, passengerCount);
            
            // committing the transaction - next transaction will start after the next SQL statement
            connection.commit();
            if(distance != null && price != null) {
                return new Ticket(departureStation, arrivalStation, travelPeriod, passengerCount, travelClass, price);
            }
        }
        catch(SQLException e) {
            // making sure the transaction is aborted
            try {
                connection.rollback();
            }
            catch (SQLException ee) {
                throw new DataAccessException("Failing rollbacking transaction in 2.1.2: " + ee.getMessage());
            }
            throw new DataAccessException("Error occured in 2.1.2: " + e.getMessage());
        }
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
