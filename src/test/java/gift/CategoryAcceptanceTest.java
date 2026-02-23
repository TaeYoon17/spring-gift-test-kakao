package gift;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

class CategoryAcceptanceTest extends AcceptanceTestBase {

    @Test
    @DisplayName("카테고리를 생성하면 id와 name이 반환된다")
    void createCategory() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "식품"))
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("식품"));
    }

    @Test
    @DisplayName("카테고리 목록 조회 시 생성한 카테고리가 포함된다")
    void retrieveCategories() {
        // given
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "식품"))
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200);

        // when & then
        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }
}
