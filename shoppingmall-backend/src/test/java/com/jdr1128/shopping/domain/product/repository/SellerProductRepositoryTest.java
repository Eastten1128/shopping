package com.jdr1128.shopping.domain.product.repository;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:seller-products;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SellerProductRepositoryTest {
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired MemberRepository members;
    @Autowired EntityManager entityManager;

    @Test
    void ownerQueriesExcludeOtherSellersAndOptionallyIncludeDeletedRows() {
        Category category = categories.save(new Category("키보드"));
        Member first = member("first");
        Member second = member("second");
        Product active = save(category, first, "판매중");
        Product stopped = save(category, first, "판매중지");
        stopped.delete();
        Product other = save(category, second, "다른 판매자");
        products.flush();
        entityManager.clear();

        assertThat(products.findBySeller_MemberIdAndIsDeletedFalse(first.getMemberId()))
                .extracting(Product::getProductId).containsExactly(active.getProductId());
        assertThat(products.findBySeller_MemberId(first.getMemberId()))
                .extracting(Product::getProductId).containsExactlyInAnyOrder(active.getProductId(), stopped.getProductId());
        assertThat(products.findByProductIdAndSeller_MemberId(other.getProductId(), first.getMemberId())).isEmpty();
        assertThat(products.findByProductIdAndSeller_MemberId(stopped.getProductId(), first.getMemberId())).isPresent();
    }

    @Test
    void stoppedProductRemainsInDatabaseWithStoppedStatusAndDeletionDate() {
        Product product = save(categories.save(new Category("마우스")), member("seller"), "상품");
        product.delete();
        products.flush();
        entityManager.clear();

        Product stored = products.findById(product.getProductId()).orElseThrow();
        assertThat(stored.isDeleted()).isTrue();
        assertThat(stored.getProductStatus()).isEqualTo(ProductStatus.STOPPED);
        assertThat(stored.getDeleteAt()).isNotNull();
        assertThat(products.findByProductIdAndIsDeletedFalse(product.getProductId())).isEmpty();
    }

    private Member member(String loginId) {
        return members.save(new Member(loginId, "encoded-password", loginId + "@test.com", Role.SELLER));
    }

    private Product save(Category category, Member seller, String name) {
        return products.save(new Product(category, seller, name, 10000, 5, ProductStatus.SELLING));
    }
}
