package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Venue entity in Meetup data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Venue implements Serializable {

    private long venue_id;
    private String venue_name;
    private double lon;
    private double lat;

    @Override
    public String toString() {
        return "Venue{" +
            "venue_id=" + venue_id +
            ", venue_name='" + venue_name + '\'' +
            ", lon=" + lon +
            ", lat=" + lat +
            '}';
    }
}