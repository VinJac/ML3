package test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import model.DataAccess;

/**
 * A simple test program for {@link DataAccess}.
 *
 * @author Jean-Michel Busca
 *
 */
public class SimpleTest {

    //
    // CONSTANTS
    //
    private static final int MAX_CUSTOMERS = 5;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //
    // CLASS FIELDS
    //
    private static int testTotal = 0;
    private static int testOK = 0;

    //
    // HELPER CLASSES
    //
    /**
     * Emulates a user interacting with the system These operations are
     * defined in the {@link #run()} method.
     * <p>
     * This class is used to perform multi-user tests. See the
     * {@link SimpleTest#main(String[])} method.
     *
     * @author Jean-Michel Busca
     *
     */
    static class UserEmulator extends Thread {

        private final DataAccess data;
        private final String user;

        /**
         * Creates a new user emulator with the specified name, using the
         * specified data acces object.
         *
         * @param data the data access object to use
         * @param user the name of the user running the test
         */
        public UserEmulator(DataAccess data, String user) {
            this.data = data;
            this.user = user;
        }

        @Override
        public String toString() {
            return user + "[" + data + "]";
        }

        @Override
        public void run() {
            System.out.println(this + ": starting");

            // TODO complete the test
            
            System.out.println(this + ": exiting");
        }

    }

    //
    // HELPER METHODS
    //
    /**
     * Checks whether the specified test was successful and updates the fields
     * <code>testTotal</code> and <code>testOK</code> accordingly.
     *
     * @param test the name of the test
     * @param ok <code>true</code> if the test was sucessful and
     * <code>false</code> otherwise
     */
    private synchronized static void check(String test, boolean ok) {
        testTotal += 1;
        System.out.print(test + ": ");
        if (ok) {
            testOK += 1;
            System.out.println("ok");
        } else {
            System.out.println("FAILED");
        }
    }

    /**
     * Runs a single-user test suite on the specified data access object, on
     * behalf of the specified user.
     *
     * @param data the data access object to use
     * @param user the name of the user running the test
     *
     * @throws Exception if anything goes wrong
     */
    private static void singleUserTests(DataAccess data, String user)
        throws Exception {

        // NOTE: the tests below throw an NullPointerException because the methods
        // are not implemented yet
        // 
        check("getAvailableSeats", 
            data.getAvailableSeats(6607, dateFormat.parse("2017-10-29 00:00:00"), "Lyon", "Avignon").size() == 1093);


        // TODO complete the test
    }

    //
    // MAIN
    //
    /**
     * Runs the simple test program.
     *
     * @param args url login password
     *
     */
    public static void main(String[] args) {

        // check parameters
        if (args.length != 3) {
            System.err.println("usage: SimpleTest <url> <login> <password>");
            System.exit(1);
        }

        DataAccess data = null;
        List<DataAccess> datas = new ArrayList<>();
        try {

            // create the main data access object
            data = new DataAccess(args[0], args[1], args[2]);
            
            // create and population the database
            data.initDatabase();

            // execute single-user tests
            System.out.println("Running single-user tests...");
            //singleUserTests(data, "single user");

            // execute multi-users tests
            System.out.println("Running multi-users tests...");
            List<UserEmulator> emulators = new ArrayList<>();
            for (int i = 0; i < MAX_CUSTOMERS; i++) {
                DataAccess data2 = new DataAccess(args[0], args[1], args[2]);
                datas.add(data2);
                UserEmulator emulator = new UserEmulator(data2, "user#" + i);
                emulators.add(emulator);
                emulator.start();
            }

            // wait for the test to complete
            for (UserEmulator e : emulators) {
                e.join();
            }

            // you may add some tests here:
            // TODO
        } catch (Exception e) {

            System.err.println("test aborted: " + e);
            e.printStackTrace();

        } finally {

            if (data != null) {
                try {
                    data.close();
                } catch (Exception e) {
                    System.err.println("unexpected exception: " + e);
                }
            }

            for (DataAccess m : datas) {
                try {
                    m.close();
                } catch (Exception e) {
                    System.err.println("unexpected exception: " + e);
                }
            }

        }

        // print test results
        if (testTotal == 0) {
            System.out.println("no test performed");
        } else {
            String r = "test results: ";
            r += "total=" + testTotal;
            r += ", ok=" + testOK + " (" + ((testOK * 100) / testTotal) + "%)";
            System.out.println(r);
        }

    }
}
