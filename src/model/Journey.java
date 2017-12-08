package model;

import java.util.Date;

/**
 * The description of a journey.
 *
 * @author Jean-Michel Busca
 */
public class Journey {

    private final String departureStation;
    private final String arrivalStation;
    private final int trainNumber;
    private final Date departureDate;
    private final Date arrivalDate;

    public Journey(String departureStation, String arrivalStation, int trainNumber, Date departureDate, Date arrivalDate) {
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;
        this.trainNumber = trainNumber;
        this.departureDate = departureDate;
        this.arrivalDate = arrivalDate;
    }

    @Override
    public String toString() {
        return "Journey{" + "departureStation=" + departureStation + ", arrivalStation=" + arrivalStation + ", trainNumber=" + trainNumber + ", departureDate=" + departureDate + ", arrivalDate=" + arrivalDate + '}';
    }

    public String getDepartureStation() {
        return departureStation;
    }

    public String getArrivalStation() {
        return arrivalStation;
    }

    public int getTrainNumber() {
        return trainNumber;
    }

    public Date getDepartureDate() {
        return departureDate;
    }

    public Date getArrivalDate() {
        return arrivalDate;
    }

}
