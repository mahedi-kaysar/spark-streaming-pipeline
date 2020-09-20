package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * GroupTopics entity in Meetup data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupTopics implements Serializable {
    private String urlkey;
    private String topic_name;

    @Override
    public String toString() {
        return "GroupTopics{" +
            "urlkey='" + urlkey + '\'' +
            ", topic_name='" + topic_name + '\'' +
            '}';
    }
}