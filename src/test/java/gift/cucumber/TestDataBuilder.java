package gift.cucumber;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class TestDataBuilder {

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;

    public TestDataBuilder(
            final MemberRepository memberRepository,
            final CategoryRepository categoryRepository,
            final ProductRepository productRepository,
            final OptionRepository optionRepository
    ) {
        this.memberRepository = memberRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
    }

    public Long createMember(final String name, final String email) {
        return memberRepository.save(new Member(name, email)).getId();
    }

    public Long createCategory(final String name) {
        return categoryRepository.save(new Category(name)).getId();
    }

    public Long createProduct(final String name, final int price, final String imageUrl, final Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        return productRepository.save(new Product(name, price, imageUrl, category)).getId();
    }

    public Long createOption(final String name, final int quantity, final Long productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        return optionRepository.save(new Option(name, quantity, product)).getId();
    }

    public int getOptionQuantity(final Long optionId) {
        return optionRepository.findById(optionId).orElseThrow().getQuantity();
    }
}
