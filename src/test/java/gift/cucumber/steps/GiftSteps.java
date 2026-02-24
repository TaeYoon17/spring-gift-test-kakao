package gift.cucumber.steps;

import gift.cucumber.GiftApiClient;
import gift.cucumber.ScenarioContext;
import gift.cucumber.TestDataBuilder;
import gift.model.GiftDelivery;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class GiftSteps {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Autowired
    private GiftApiClient apiClient;

    @Autowired
    private GiftDelivery giftDelivery;

    @Given("{string} 회원이 존재한다")
    public void 회원_생성(final String name) {
        Long memberId = testDataBuilder.createMember(name, name + "@test.com");
        context.set(name + "Id", memberId);
    }

    @Given("{string} 카테고리가 등록되어 있다")
    public void 카테고리_등록(final String name) {
        Long categoryId = testDataBuilder.createCategory(name);
        context.set("categoryId", categoryId);
    }

    @Given("이름이 {string}이고 가격이 {int}이고 이미지가 {string}인 상품이 등록되어 있다")
    public void 상품_등록(final String name, final int price, final String imageUrl) {
        Long categoryId = context.getId("categoryId");
        Long productId = testDataBuilder.createProduct(name, price, imageUrl, categoryId);
        context.set("productId", productId);
    }

    @Given("이름이 {string}이고 재고가 {int}인 옵션이 등록되어 있다")
    public void 옵션_등록(final String name, final int quantity) {
        Long productId = context.getId("productId");
        Long optionId = testDataBuilder.createOption(name, quantity, productId);
        context.set("optionId", optionId);
    }

    @When("{string}이 {string}에게 수량 {int}(으)로 {string} 메시지와 함께 선물하면")
    public void 선물하기(final String senderName, final String receiverName,
                     final int quantity, final String message) {
        Long senderId = context.getId(senderName + "Id");
        Long receiverId = context.getId(receiverName + "Id");
        Long optionId = context.getId("optionId");

        reset(giftDelivery);

        Response response = apiClient.sendGift(senderId, optionId, quantity, receiverId, message);
        context.setLastResponse(response);
    }

    @When("{string}이 {string}에게 존재하지 않는 옵션으로 선물하면")
    public void 존재하지_않는_옵션으로_선물하기(final String senderName, final String receiverName) {
        Long senderId = context.getId(senderName + "Id");
        Long receiverId = context.getId(receiverName + "Id");

        reset(giftDelivery);

        Response response = apiClient.sendGiftWithInvalidOption(senderId, receiverId);
        context.setLastResponse(response);
    }

    @Then("옵션의 재고는 {int}이다")
    public void 옵션_재고_확인(final int expectedQuantity) {
        Long optionId = context.getId("optionId");
        int actual = testDataBuilder.getOptionQuantity(optionId);
        assertThat(actual).isEqualTo(expectedQuantity);
    }

    @Then("선물 배달 서비스가 호출되었다")
    public void 배달_서비스_호출_확인() {
        verify(giftDelivery, times(1)).deliver(any());
    }

    @Then("선물 배달 서비스가 호출되지 않았다")
    public void 배달_서비스_미호출_확인() {
        verify(giftDelivery, never()).deliver(any());
    }
}
