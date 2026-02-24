package gift.cucumber;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.boot.test.web.server.LocalServerPort;

public class RestAssuredHook {

    @LocalServerPort
    private int port;

    @Before(order = 1)
    public void setUpRestAssured() {
        RestAssured.port = port;
    }
}
