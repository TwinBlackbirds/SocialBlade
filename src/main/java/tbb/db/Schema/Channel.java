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
    @Column(name = "id", nullable = false) 
    public String ID; // youtube.com/@?
    
    @Column(name = "last_video", nullable = false)
    public String LastVideo; // video id
    
    @Column(name = "name", nullable = false) 
    public String Name;

    @Column(name = "subscriber_count", nullable = false)
    public int SubscriberCount;

    @Column(name = "last_subscriber_count", nullable = false)
    public int LastSubscriberCount;

    @Column(name = "checked", nullable = false) // Checked timestamp
    public LocalDateTime Checked;

    @Column(name = "last_checked") // LastChecked timestamp (optional, so no nullable=false)
    public LocalDateTime LastChecked;

    @Transient // Since Duration is not directly supported by JPA, mark it as Transient
    public Duration TimeSinceLastFound;

    
    
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
    
    // TODO: implement these
    public String getLastVideo() {
        return LastVideo;
    }

    public void setLastVideo(String video) {
        this.LastVideo = video;
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

}
