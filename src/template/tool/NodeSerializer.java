package template.tool;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class NodeSerializer extends StdSerializer<Node> {

    protected NodeSerializer(Class<Node> t) {
        super(t);
    }

    public void serialize(Node node, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
            throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("id", node.id);
        jsonGenerator.writeStringField("data", node.data.path);
        jsonGenerator.writeEndObject();
    }
}