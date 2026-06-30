package com.jdr1128.shopping.domain.product.repository;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("상품 저장 및 ID 조회")
    void 상품저장상품조회(){
        //given
        Member seller = new Member(
                "seller01",
                "encoded-password",
                "seller@test.com",
                Role.SELLER
        );
        memberRepository.save(seller);

        Category category = new Category("키보드");
        categoryRepository.save(category);

        Product product = new Product(
                category,
                seller,
                "기계식 키보드",
                50000,
                10,
                ProductStatus.SELLING
        );
        Product savedProduct = productRepository.save(product);

        //when
        Product foundProduct = productRepository.findById(savedProduct.getProductId())
                .orElseThrow();

        //then

        assertThat(foundProduct.getProductName()).isEqualTo("기계식 키보드");
        assertThat(foundProduct.getPrice()).isEqualTo(50000);
        assertThat(foundProduct.getStock()).isEqualTo(10);
        assertThat(foundProduct.getProductStatus()).isEqualTo(ProductStatus.SELLING);
        assertThat(foundProduct.getCategory().getCategoryName()).isEqualTo("키보드");
        assertThat(foundProduct.getSeller().getLoginId()).isEqualTo("seller01");
    }
}
