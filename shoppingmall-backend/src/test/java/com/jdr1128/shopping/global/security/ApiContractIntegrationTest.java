package com.jdr1128.shopping.global.security;

import com.jayway.jsonpath.JsonPath;
import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import com.jdr1128.shopping.domain.product.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-contract;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "jwt.secret=test-only-api-contract-secret-key-long-enough-123456789",
        "jwt.expiration-milliseconds=1800000"
})
@AutoConfigureMockMvc
@Transactional
class ApiContractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categories;
    @Autowired MemberRepository members;
    @Autowired ProductRepository products;
    @Autowired PasswordEncoder passwords;

    @Test
    void productWriteRequestAndSellerProductMatchRealApi() throws Exception {
        Member seller = member("seller", Role.SELLER);
        Category category = categories.save(new Category("키보드"));
        String access = access(login(seller.getLoginId()));
        String body = "{\"categoryId\":" + category.getCategoryId()
                + ",\"productName\":\"상품명\",\"price\":10000,\"stock\":5,\"productStatus\":\"SELLING\"}";
        MvcResult created = mvc.perform(post("/api/products").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.productName").value("상품명"))
                .andExpect(jsonPath("$.sellerLoginId").value("seller"))
                .andExpect(jsonPath("$.sellerId").value(seller.getMemberId()))
                .andExpect(jsonPath("$.deleted").value(false)).andExpect(jsonPath("$.createdAt").exists()).andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.productId");
        mvc.perform(get("/api/seller/products/{id}", id).header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoryId").value(category.getCategoryId()));
        mvc.perform(patch("/api/seller/products/{id}", id).header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("상품명", "수정명")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productName").value("수정명"));
        mvc.perform(delete("/api/products/{id}", id).header("Authorization", "Bearer " + access)).andExpect(status().isNoContent());
        mvc.perform(get("/api/seller/products").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/seller/products").param("includeStopped", "true").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].deleted").value(true)).andExpect(jsonPath("$[0].productStatus").value("STOPPED"));
        String otherAccess = access(login(member("other", Role.SELLER).getLoginId()));
        mvc.perform(get("/api/seller/products/{id}", id).header("Authorization", "Bearer " + otherAccess)).andExpect(status().isNotFound());
    }

    @Test
    void categoryFilterIsAppliedByPublicProductsApi() throws Exception {
        Member seller = member("filter", Role.SELLER);
        Category first = categories.save(new Category("첫 분류"));
        Category second = categories.save(new Category("둘째 분류"));
        products.save(new Product(first, seller, "첫 상품", 1000, 1, ProductStatus.SELLING));
        products.save(new Product(second, seller, "둘째 상품", 2000, 2, ProductStatus.SELLING));
        mvc.perform(get("/api/products").param("categoryId", second.getCategoryId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("둘째 상품"));
    }

    @Test
    void adminLoginRefreshAndLogoutWorkWithActualCookiesAndJwt() throws Exception {
        MvcResult login = login(member("admin", Role.ADMIN).getLoginId());
        mvc.perform(get("/api/admin/categories").header("Authorization", "Bearer " + access(login))).andExpect(status().isOk());
        assertThat(login.getResponse().getHeader("Set-Cookie")).contains("HttpOnly", "Path=/api/members");
        MvcResult refresh = mvc.perform(post("/api/members/refresh").cookie(cookie(login)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN")).andReturn();
        mvc.perform(get("/api/admin/categories").header("Authorization", "Bearer " + access(refresh))).andExpect(status().isOk());
        mvc.perform(post("/api/members/refresh").cookie(cookie(login))).andExpect(status().isUnauthorized());
        Cookie newCookie = cookie(refresh);
        mvc.perform(post("/api/members/logout").cookie(newCookie)).andExpect(status().isNoContent());
        mvc.perform(post("/api/members/refresh").cookie(newCookie)).andExpect(status().isUnauthorized());
    }

    private Member member(String loginId, Role role) {
        return members.save(new Member(loginId, passwords.encode("test-password"), loginId + "@test.com", role));
    }

    private MvcResult login(String loginId) throws Exception {
        return mvc.perform(post("/api/members/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn();
    }

    private String access(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private Cookie cookie(MvcResult result) {
        String value = result.getResponse().getHeader("Set-Cookie").split(";", 2)[0].substring("refreshToken=".length());
        return new Cookie("refreshToken", value);
    }
}
