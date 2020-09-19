package org.example.deserializer;

import com.fasterxml.jackson.databind.*;
import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Deserializer;
import org.example.model.MeetupRsvpsMessage;

import java.util.Map;

public class MeetupRsvpsDeserializer implements Deserializer<MeetupRsvpsMessage> {
    private Gson gson = new Gson();
    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> arg0, boolean arg1) {
    }

    @Override
    public MeetupRsvpsMessage deserialize(String arg0, byte[] arg1) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MeetupRsvpsMessage record = null;
        try {
            record = mapper.readValue(new String(arg1, "UTF-8"), MeetupRsvpsMessage.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return record;
    }
}
