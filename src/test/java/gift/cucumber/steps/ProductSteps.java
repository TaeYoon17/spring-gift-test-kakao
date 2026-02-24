package gift.cucumber.steps;

import gift.cucumber.ProductApiClient;
import gift.cucumber.ScenarioContext;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

public class ProductSteps {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private ProductApiClient apiClient;

    @When("이름이 {string}이고 가격이 {int}이고 이미지가 {string}인 상품을 생성하면")
    public void 상품_생성(final String name, final int price, final String imageUrl) {
        Long categoryId = context.getId("categoryId");
        Response response = apiClient.createProduct(name, price, imageUrl, categoryId);
        context.setLastResponse(response);
    }

    @When("상품 목록을 조회하면")
    public void 상품_목록_조회() {
        Response response = apiClient.listProducts();
        context.setLastResponse(response);
    }
}
