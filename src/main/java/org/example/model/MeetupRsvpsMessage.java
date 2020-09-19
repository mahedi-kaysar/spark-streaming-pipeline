package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeetupRsvpsMessage implements Serializable {

    private Venue venue;
    private String visibility;
    private String response;
    private long guests;
    private Member member;
    private long rsvp_id;
    private long mtime;
    private Event event;
    private Group group;

    @Override
    public String toString() {
        return "RsvpMessage{" +
                "venue=" + venue +
                ", visibility='" + visibility + '\'' +
                ", response='" + response + '\'' +
                ", guests=" + guests +
                ", member=" + member +
                ", rsvp_id=" + rsvp_id +
                ", mtime=" + mtime +
                ", event=" + event +
                ", group=" + group +
                '}';
    }
}