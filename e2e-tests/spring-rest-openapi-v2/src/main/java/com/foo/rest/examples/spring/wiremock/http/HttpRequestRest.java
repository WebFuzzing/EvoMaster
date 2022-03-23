package com.foo.rest.examples.spring.wiremock.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rest.examples.spring.strings.StringsResponseDto;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping(path = "/api/wiremock")
public class HttpRequestRest {

    @RequestMapping(
            value = "/external/url",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto usingURLConnection() {
        /**
         * proxyprint, catwatch, and caw all are using HttpURLConnection as the base library
         * to call external services via HTTP.
         * */
        StringsResponseDto stringsResponseDto = new StringsResponseDto();

        try {
            URL url = new URL("http://foo.bar:8080/api/echo/foo");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");

            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            MockApiResponse result = mapper.readValue(responseStream, MockApiResponse.class);

            if (result.message.equals("foo")) {
                stringsResponseDto.valid = true;
            } else {
                stringsResponseDto.valid = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            stringsResponseDto.valid = false;
        }

        return stringsResponseDto;
    }

    @RequestMapping(
            value = "/external/http",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto usingHttpClient() {
        StringsResponseDto stringsResponseDto = new StringsResponseDto();
        // Note: HttpClient is only available under Java 11 and above, not implemented for now

        stringsResponseDto.valid = true;
        return stringsResponseDto;
    }

    @RequestMapping(
            value = "/external/apache",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto usingApacheHttpClient() {
        StringsResponseDto stringsResponseDto = new StringsResponseDto();

        ObjectMapper mapper = new ObjectMapper();

        try(CloseableHttpClient client = HttpClients.createDefault()) {

            HttpGet request = new HttpGet("http://foo.bar:8080/api/echo/foo");
            MockApiResponse result = client.execute(request, httpResponse ->
                    mapper.readValue(httpResponse.getEntity().getContent(), MockApiResponse.class));

            if (result.message.equals("foo")) {
                stringsResponseDto.valid = true;
            } else {
                stringsResponseDto.valid = false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            stringsResponseDto.valid = false;
        }

        return stringsResponseDto;
    }

    @RequestMapping(
            value = "/external/okhttp",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto usingOkHttpClient() {
        StringsResponseDto stringsResponseDto = new StringsResponseDto();

        ObjectMapper mapper = new ObjectMapper();

       try {
           OkHttpClient client = new OkHttpClient();
           Request request = new Request.Builder()
                   .url("http://foo.bar:8080/api/echo/foo")
                   .build();
           Response response = client.newCall(request).execute();
           MockApiResponse result = mapper.readValue(response.body().byteStream(), MockApiResponse.class);

           if (result.message.equals("foo")) {
               stringsResponseDto.valid = true;
           } else {
               stringsResponseDto.valid = false;
           }
       } catch (IOException e) {
           e.printStackTrace();
           stringsResponseDto.valid = false;
       }

        return stringsResponseDto;
    }
}
