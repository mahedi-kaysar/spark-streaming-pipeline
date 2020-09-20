package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event entity in Meetup data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event implements Serializable {
    private String event_id;
    private String event_name;
    private String event_url;
    private long time;

    @Override
    public String toString() {
        return "Event{" +
            "event_id=" + event_id +
            ", event_name='" + event_name + '\'' +
            ", event_url='" + event_url + '\'' +
            ", time=" + time +
            '}';
    }
}