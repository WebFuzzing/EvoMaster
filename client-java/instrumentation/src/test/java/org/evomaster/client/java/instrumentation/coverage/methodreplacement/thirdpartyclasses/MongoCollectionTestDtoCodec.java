package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class MongoCollectionTestDtoCodec implements Codec<MongoCollectionTestDto> {

    @Override
    public void encode(BsonWriter writer, MongoCollectionTestDto value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeInt32("age", value.age);
        writer.writeString("city", value.city);
        writer.writeString("name", value.name);
        writer.writeEndDocument();
    }

    @Override
    public MongoCollectionTestDto decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();
        MongoCollectionTestDto result = new MongoCollectionTestDto();
        result.age = reader.readInt32("age");
        result.city = reader.readString("city");
        result.name = reader.readString("name");
        reader.readEndDocument();
        return result;
    }

    @Override
    public Class<MongoCollectionTestDto> getEncoderClass() {
        return MongoCollectionTestDto.class;
    }
}
