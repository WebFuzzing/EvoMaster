package org.evomaster.protobuf;


import com.google.protobuf.DescriptorProtos;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

public class ProtobufParserTest {

    @Test @Ignore
    public void testParserHelloWorld() throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("helloworld.proto");
        assertNotNull(is);
        DescriptorProtos.FileDescriptorProto rv = DescriptorProtos.FileDescriptorProto.parseFrom(is);
    }

    @Test @Ignore
    public void testParserInd1() throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("ind_1.proto");
        assertNotNull(is);
        DescriptorProtos.FileDescriptorProto rv = DescriptorProtos.FileDescriptorProto.parseFrom(is);
    }
}
