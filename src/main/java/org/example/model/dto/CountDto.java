package org.example.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CountDto implements Serializable {
    private String urlkey;
    private int count;

    @Override
    public String toString() {
        return "CountDto{" +
                "urlkey='" + urlkey + '\'' +
                ", count=" + count +
                '}';
    }
}
