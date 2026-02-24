package gift.cucumber.steps;

import gift.cucumber.ScenarioContext;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

public class CommonSteps {

    @Autowired
    private ScenarioContext context;

    @Then("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드_확인(final int statusCode) {
        context.getLastResponse().then().statusCode(statusCode);
    }

    @Then("응답에 {string} 필드가 존재한다")
    public void 응답_필드_존재_확인(final String fieldName) {
        context.getLastResponse().then().body(fieldName, notNullValue());
    }

    @Then("응답의 {string} 필드는 {string}이다")
    public void 응답_필드_값_확인(final String fieldName, final String expectedValue) {
        context.getLastResponse().then().body(fieldName, equalTo(expectedValue));
    }

    @Then("응답 목록의 크기가 {int} 이상이다")
    public void 응답_목록_크기_확인(final int minSize) {
        context.getLastResponse().then().body("size()", greaterThanOrEqualTo(minSize));
    }
}
