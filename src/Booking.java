/**
 * Represents a single Booking entity in the system.
 * This follows the "Model" part of the MVC pattern.
 */
public class Booking {
    private String id;
    private String username;
    private String eventName;
    private String date;
    private String status;
    private String amount;

    public Booking(String id, String username, String eventName, String date, String status, String amount) {
        this.id = id;
        this.username = username;
        this.eventName = eventName;
        this.date = date;
        this.status = status;
        this.amount = amount;
    }

    // Standard Getters and Setters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEventName() { return eventName; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
    public String getAmount() { return amount; }

    public void setStatus(String status) { this.status = status; }
    public void setDate(String date) { this.date = date; }
    public void setEventName(String eventName) { this.eventName = eventName; }
}