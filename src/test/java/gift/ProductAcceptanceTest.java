package gift;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

class ProductAcceptanceTest extends AcceptanceTestBase {

    @Test
    @DisplayName("카테고리 생성 후 해당 카테고리로 상품을 생성하면 id, name, category가 반환된다")
    void createProduct() {
        // given
        Long categoryId = createCategory("식품");

        // when & then
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "아이스크림",
                        "price", 3000,
                        "imageUrl", "http://example.com/ice.png",
                        "categoryId", categoryId
                ))
        .when()
                .post("/api/products")
        .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("아이스크림"))
                .body("category.id", equalTo(categoryId.intValue()));
    }

    @Test
    @DisplayName("상품 목록 조회 시 생성한 상품이 포함된다")
    void retrieveProducts() {
        // given
        Long categoryId = createCategory("식품");
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "아이스크림",
                        "price", 3000,
                        "imageUrl", "http://example.com/ice.png",
                        "categoryId", categoryId
                ))
        .when()
                .post("/api/products")
        .then()
                .statusCode(200);

        // when & then
        given()
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    private Long createCategory(String name) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200)
                .extract().jsonPath().getLong("id");
    }
}
