package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CategoryAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("유효한 이름으로 카테고리를 생성하면 id와 name이 반환된다")
    void createCategory() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "교환권"))
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", is("교환권"));
    }

    @Test
    @DisplayName("카테고리 생성 후 목록 조회 시 해당 카테고리가 포함된다")
    void createAndRetrieveCategory() {
        카테고리를_생성한다("교환권");

        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("교환권"));
    }

    @Test
    @DisplayName("여러 카테고리를 생성하면 모두 목록에 포함된다")
    void createMultipleCategoriesAndRetrieve() {
        카테고리를_생성한다("교환권");
        카테고리를_생성한다("상품권");

        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("name", hasItems("교환권", "상품권"));
    }

    private void 카테고리를_생성한다(String name) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200);
    }
}
