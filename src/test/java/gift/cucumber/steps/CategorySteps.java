package gift.cucumber.steps;

import gift.cucumber.CategoryApiClient;
import gift.cucumber.ScenarioContext;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

public class CategorySteps {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private CategoryApiClient apiClient;

    @When("이름이 {string}인 카테고리를 생성하면")
    public void 카테고리_생성(final String name) {
        Response response = apiClient.createCategory(name);
        context.setLastResponse(response);
    }

    @When("카테고리 목록을 조회하면")
    public void 카테고리_목록_조회() {
        Response response = apiClient.listCategories();
        context.setLastResponse(response);
    }
}
