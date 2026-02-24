package gift.cucumber;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

@Component
public class GiftApiClient {

    public Response sendGift(final Long memberId, final Long optionId,
                             final int quantity, final Long receiverId, final String message) {
        return given()
                .contentType(ContentType.JSON)
                .header("Member-Id", memberId)
                .body(Map.of(
                        "optionId", optionId,
                        "quantity", quantity,
                        "receiverId", receiverId,
                        "message", message
                ))
            .when()
                .post("/api/gifts");
    }

    public Response sendGiftWithInvalidOption(final Long memberId, final Long receiverId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Member-Id", memberId)
                .body(Map.of(
                        "optionId", 999,
                        "quantity", 1,
                        "receiverId", receiverId,
                        "message", "선물!"
                ))
            .when()
                .post("/api/gifts");
    }
}
