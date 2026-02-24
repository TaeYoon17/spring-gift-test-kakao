package gift.cucumber;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

@Component
public class ProductApiClient {

    public Response createProduct(final String name, final int price,
                                  final String imageUrl, final Long categoryId) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "price", price,
                        "imageUrl", imageUrl,
                        "categoryId", categoryId
                ))
            .when()
                .post("/api/products");
    }

    public Response listProducts() {
        return given()
            .when()
                .get("/api/products");
    }
}
