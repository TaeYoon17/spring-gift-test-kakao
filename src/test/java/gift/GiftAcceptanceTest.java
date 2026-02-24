package gift;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;

class GiftAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Test
    @DisplayName("선물하기 성공 시 200 응답과 함께 재고가 차감된다")
    void giveGiftSuccess() {
        // given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Category category = categoryRepository.save(new Category("식품"));
        Product product = productRepository.save(
                new Product("아이스크림", 3000, "http://example.com/ice.png", category)
        );
        Option option = optionRepository.save(new Option("Tall", 10, product));

        // when
        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 3,
                        "receiverId", receiver.getId(),
                        "message", "맛있게 먹어!"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(200)
                .body(emptyOrNullString());

        // then — DB 상태 검증
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(7); // 10 - 3
    }

    @Test
    @DisplayName("재고 부족 시 선물하기가 실패(500)하고 재고가 변경되지 않는다")
    void giveGiftFailsWhenInsufficientStock() {
        // given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        Category category = categoryRepository.save(new Category("식품"));
        Product product = productRepository.save(
                new Product("아이스크림", 3000, "http://example.com/ice.png", category)
        );
        Option option = optionRepository.save(new Option("Tall", 1, product));

        // when
        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", option.getId(),
                        "quantity", 2,
                        "receiverId", receiver.getId(),
                        "message", "선물!"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(500);

        // then — 트랜잭션 롤백 확인
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(1); // 변경 없음
    }

    @Test
    @DisplayName("존재하지 않는 옵션으로 선물하기가 실패(500)한다")
    void giveGiftFailsWhenOptionNotFound() {
        // given
        Member sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        Member receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));

        // when & then
        given()
                .contentType(ContentType.JSON)
                .header("Member-Id", sender.getId())
                .body(Map.of(
                        "optionId", 999,
                        "quantity", 1,
                        "receiverId", receiver.getId(),
                        "message", "선물!"
                ))
        .when()
                .post("/api/gifts")
        .then()
                .statusCode(500);
    }
}
