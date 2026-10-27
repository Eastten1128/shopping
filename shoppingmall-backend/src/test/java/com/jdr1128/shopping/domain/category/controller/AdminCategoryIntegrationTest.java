package com.jdr1128.shopping.domain.category.controller;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-categories;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-only-admin-category-secret-key-long-enough-123456789",
        "jwt.access-token-expiration-milliseconds=1800000",
        "jwt.refresh-token-expiration-milliseconds=604800000",
        "app.frontend-origin=http://localhost:5173"
})
@AutoConfigureMockMvc
@Transactional
class AdminCategoryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categories;
    @Autowired EntityManager entityManager;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreatesRenamesAndSoftDeletesCategory() throws Exception {
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{\"categoryName\":\"키보드\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.deleted").value(false));
        Long id = categories.findByCategoryName("키보드").orElseThrow().getCategoryId();
        mvc.perform(patch("/api/admin/categories/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{\"categoryName\":\"기계식 키보드\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoryName").value("기계식 키보드"));
        mvc.perform(delete("/api/categories/{id}", id)).andExpect(status().isNoContent());
        categories.flush();
        entityManager.clear();
        Category deleted = categories.findById(id).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.getDeleteAt()).isNotNull();
        mvc.perform(get("/api/admin/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/admin/categories").param("includeDeleted", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].deleted").value(true));
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/categories/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void renamingDeletedCategoryDoesNotRestoreIt() throws Exception {
        Category category = categories.save(new Category("삭제된 이름"));
        category.delete();
        var deletedAt = category.getDeleteAt();
        mvc.perform(patch("/api/admin/categories/{id}", category.getCategoryId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"categoryName\":\"새 이름\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(true));
        assertThat(category.getCategoryName()).isEqualTo("새 이름");
        assertThat(category.isDeleted()).isTrue();
        assertThat(category.getDeleteAt()).isEqualTo(deletedAt);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicatesRemainRejectedForCreateAndRename() throws Exception {
        categories.save(new Category("중복 이름"));
        Category second = categories.save(new Category("다른 이름"));
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{\"categoryName\":\"중복 이름\"}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(patch("/api/admin/categories/{id}", second.getCategoryId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"categoryName\":\"중복 이름\"}"))
                .andExpect(status().is4xxClientError());
        assertThat(second.getCategoryName()).isEqualTo("다른 이름");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blankNamesAreRejected() throws Exception {
        Category category = categories.save(new Category("정상 이름"));
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{\"categoryName\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/categories/{id}", category.getCategoryId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"categoryName\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousCannotUseAdminApi() throws Exception {
        mvc.perform(get("/api/admin/categories")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{\"categoryName\":\"이름\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userAndSellerCannotReadOrMutateAdminCategories() throws Exception {
        for (String role : new String[]{"USER", "SELLER"}) {
            mvc.perform(get("/api/admin/categories").with(user("visitor").roles(role))).andExpect(status().isForbidden());
            mvc.perform(post("/api/categories").with(user("visitor").roles(role)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"categoryName\":\"이름\"}")).andExpect(status().isForbidden());
            mvc.perform(patch("/api/admin/categories/1").with(user("visitor").roles(role)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"categoryName\":\"이름\"}")).andExpect(status().isForbidden());
            mvc.perform(delete("/api/categories/1").with(user("visitor").roles(role))).andExpect(status().isForbidden());
        }
    }
}
