package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Group entity in Meetup data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Group implements Serializable {
    private long group_id;
    private String group_name;
    private String group_city;
    private String group_state;
    private String group_country;
    private double group_lon;
    private double group_lat;
    private String group_urlname;
    private List<GroupTopics> group_topics;

    @Override
    public String toString() {
        return "Group{" +
            "group_id=" + group_id +
            ", group_name='" + group_name + '\'' +
            ", group_city='" + group_city + '\'' +
            ", group_state='" + group_state + '\'' +
            ", group_country='" + group_country + '\'' +
            ", group_lon=" + group_lon +
            ", group_lat=" + group_lat +
            ", group_urlname='" + group_urlname + '\'' +
            ", group_topics=" + group_topics +
            '}';
    }
}