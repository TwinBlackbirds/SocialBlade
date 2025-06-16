// Name: Michael Amyotte
// Date: 6/13/25
// Purpose: Example database object for SQLite database

package tbb.db.Schema;

import java.time.Duration;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "channels")
public class Channel {

    @Id
    @Column(name = "id", nullable = false) // ID column in the table
    public String ID; // youtube.com/c/?

    @Column(name = "name", nullable = false) // Name column in the table
    public String Name;

    @Column(name = "subscriber_count", nullable = false) // SubscriberCount column
    public int SubscriberCount;

    @Column(name = "last_subscriber_count", nullable = false) // LastSubscriberCount column
    public int LastSubscriberCount;

    @Column(name = "checked", nullable = false) // Checked timestamp
    public LocalDateTime Checked;

    @Column(name = "last_checked") // LastChecked timestamp (optional, so no nullable=false)
    public LocalDateTime LastChecked;

    @Transient // Since Duration is not directly supported by JPA, mark it as Transient
    public Duration TimeSinceLastFound;

    @Transient // SubscriberCountDifference is a calculated field, not stored in the DB
    public int SubscriberCountDifference;

    // Default constructor (JPA requirement)
    public Channel() {
    }

    // Getters and setters (optional but recommended for JPA)
    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public int getSubscriberCount() {
        return SubscriberCount;
    }

    public void setSubscriberCount(int subscriberCount) {
        this.SubscriberCount = subscriberCount;
    }

    public int getLastSubscriberCount() {
        return LastSubscriberCount;
    }

    public void setLastSubscriberCount(int lastSubscriberCount) {
        this.LastSubscriberCount = lastSubscriberCount;
    }

    public LocalDateTime getChecked() {
        return Checked;
    }

    public void setChecked(LocalDateTime checked) {
        this.Checked = checked;
    }

    public LocalDateTime getLastChecked() {
        return LastChecked;
    }

    public void setLastChecked(LocalDateTime lastChecked) {
        this.LastChecked = lastChecked;
    }

    public Duration getTimeSinceLastFound() {
        return Duration.between(LastChecked, Checked);
    }

    public int getSubscriberCountDifference() {
        return getSubscriberCount() - getLastSubscriberCount();
    }

}
