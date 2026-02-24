package gift.cucumber;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

@Component
public class CategoryApiClient {

    public Response createCategory(final String name) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
            .when()
                .post("/api/categories");
    }

    public Response listCategories() {
        return given()
            .when()
                .get("/api/categories");
    }
}
