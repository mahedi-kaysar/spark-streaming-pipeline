package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Member entity in Meetup data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Member implements Serializable {
    private long member_id;
    private String member_name;
    private String photo;

    @Override
    public String toString() {
        return "Member{" +
            "member_id=" + member_id +
            ", member_name='" + member_name + '\'' +
            ", photo='" + photo + '\'' +
            '}';
    }
}