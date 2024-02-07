package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class SocketClassReplacementTest {

    private static final String fake = "www.reallyIdonotexistasahostname23423523.com";

    @Timeout(5)
    @Test
    public void testGetHostName() throws Exception{
        /*
            should never call socketAddress.getHostName() in EM as it can take forever
            on Windows when dealing non-existent hostnames in a reversed-lookup
         */

        String ip = "127.1.2.3";
        //InetAddress address = InetAddress.getByName(ip);

        for(int i=0; i<1000; i++) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                address.getHostName();
            } catch (Exception e) {
            }
        }
    }


    @Timeout(5)
    @Test
    public void testGetByName() throws Exception{

        for(int i=0; i<1000; i++) {
            try {
                InetAddress.getByName(fake);
                /*
                    either fast, or cached. so should take only few milliseconds
                 */
            } catch (Exception e) {
            }
        }
    }
}