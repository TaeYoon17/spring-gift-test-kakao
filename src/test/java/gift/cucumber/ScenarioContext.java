package gift.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioContext {

    private final Map<String, Object> context = new HashMap<>();

    public void set(final String key, final Object value) {
        context.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final String key, final Class<T> type) {
        return (T) context.get(key);
    }

    public Long getId(final String key) {
        return get(key, Long.class);
    }

    public Response getLastResponse() {
        return get("lastResponse", Response.class);
    }

    public void setLastResponse(final Response response) {
        set("lastResponse", response);
    }
}
