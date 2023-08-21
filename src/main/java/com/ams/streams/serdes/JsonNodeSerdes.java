package com.ams.streams.serdes;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;

public class JsonNodeSerdes extends Serdes.WrapperSerde<JsonNode> {

    public JsonNodeSerdes() {
        super(new JsonSerializer(), new JsonDeserializer());
    }

    public static Serde<JsonNode> serdes() {
        JsonSerializer serializer = new org.apache.kafka.connect.json.JsonSerializer();
        JsonDeserializer deserializer = new org.apache.kafka.connect.json.JsonDeserializer();
        return Serdes.serdeFrom(serializer, deserializer);
    }
}
