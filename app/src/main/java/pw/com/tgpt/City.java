package pw.com.tgpt;

/**
 * Created by PW on 2015-04-26.
 */
public class City {
    private int id;
    private String name;
    private double regularPrice;
    private double regularDiff;
    private double lastWeekRegular;
    private double lastMonthRegular;
    private double lastYearRegular;
    private Direction direction;

    public enum Direction {
        UP,
        DOWN,
        NO_CHANGE
    }

    public City() {
    }

    public City(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void SetID(int id) {
        this.id = id;
    }

    public void SetName(String name) {
        this.name = name;
    }

    public int GetID() {
        return id;
    }

    public String GetName() {
        return name;
    }

    public String toString()
    {
        return name;
    }

    public double getRegularPrice() {
        return regularPrice;
    }

    public void setRegularPrice(double regularPrice) {
        this.regularPrice = regularPrice;
    }

    public double getRegularDiff() {
        return regularDiff;
    }

    public void setRegularDiff(double regularDiff) {
        this.regularDiff = regularDiff;
    }

    public double getLastWeekRegular() {
        return lastWeekRegular;
    }

    public void setLastWeekRegular(double lastWeekRegular) {
        this.lastWeekRegular = lastWeekRegular;
    }

    public double getLastMonthRegular() {
        return lastMonthRegular;
    }

    public void setLastMonthRegular(double lastMonthRegular) {
        this.lastMonthRegular = lastMonthRegular;
    }

    public double getLastYearRegular() {
        return lastYearRegular;
    }

    public void setLastYearRegular(double lastYearRegular) {
        this.lastYearRegular = lastYearRegular;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
