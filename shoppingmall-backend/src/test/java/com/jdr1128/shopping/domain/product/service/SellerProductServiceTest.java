package com.jdr1128.shopping.domain.product.service;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.product.dto.ProductResponse;
import com.jdr1128.shopping.domain.product.dto.ProductUpdateRequest;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import com.jdr1128.shopping.domain.product.expection.ProductAccessDeniedException;
import com.jdr1128.shopping.domain.product.expection.ProductNotFoundException;
import com.jdr1128.shopping.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerProductServiceTest {
    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    @Mock MemberRepository members;
    @InjectMocks ProductService service;

    @Test
    void nonSellersCannotReadSellerPages() {
        for (Role role : List.of(Role.USER, Role.ADMIN)) {
            assertThatThrownBy(() -> service.getSellerProducts(1L, role, true))
                    .isInstanceOf(ProductAccessDeniedException.class);
            assertThatThrownBy(() -> service.getSellerProduct(1L, 1L, role))
                    .isInstanceOf(ProductAccessDeniedException.class);
            assertThatThrownBy(() -> service.updateSellerProduct(1L, request(), 1L, role))
                    .isInstanceOf(ProductAccessDeniedException.class);
        }
        verifyNoInteractions(products, categories);
    }

    @Test
    void defaultListQueriesOnlyOwnersActiveProducts() {
        Product product = product();
        when(products.findBySeller_MemberIdAndIsDeletedFalse(1L)).thenReturn(List.of(product));
        assertThat(service.getSellerProducts(1L, Role.SELLER, false))
                .extracting(ProductResponse::getProductId).containsExactly(10L);
        verify(products, never()).findBySeller_MemberId(anyLong());
    }

    @Test
    void checkedListIncludesOwnersDeletedProductsWithDeletionFlag() {
        Product stopped = product();
        stopped.delete();
        when(products.findBySeller_MemberId(1L)).thenReturn(List.of(stopped));
        ProductResponse response = service.getSellerProducts(1L, Role.SELLER, true).get(0);
        assertThat(response.isDeleted()).isTrue();
        assertThat(response.getProductStatus()).isEqualTo(ProductStatus.STOPPED);
        verify(products, never()).findBySeller_MemberIdAndIsDeletedFalse(anyLong());
    }

    @Test
    void otherSellersProductCannotBeReadOrEdited() {
        when(products.findByProductIdAndSeller_MemberId(10L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSellerProduct(10L, 2L, Role.SELLER))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.updateSellerProduct(10L, request(), 2L, Role.SELLER))
                .isInstanceOf(ProductNotFoundException.class);
        verifyNoInteractions(categories);
    }

    @Test
    void editsOwnersProductFields() {
        Product product = product();
        when(products.findByProductIdAndSeller_MemberId(10L, 1L)).thenReturn(Optional.of(product));
        when(categories.findByCategoryIdAndIsDeletedFalse(3L)).thenReturn(Optional.of(category()));
        ProductResponse response = service.updateSellerProduct(10L, request(), 1L, Role.SELLER);
        assertThat(response.getProductName()).isEqualTo("수정 상품");
        assertThat(response.getCategoryId()).isEqualTo(3L);
        assertThat(response.getPrice()).isEqualTo(25000);
        assertThat(response.getStock()).isZero();
        assertThat(response.getProductStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        assertThat(response.isDeleted()).isFalse();
    }

    @Test
    void editingStoppedProductNeverResumesSale() {
        Product product = product();
        product.delete();
        var deletedAt = product.getDeleteAt();
        when(products.findByProductIdAndSeller_MemberId(10L, 1L)).thenReturn(Optional.of(product));
        when(categories.findByCategoryIdAndIsDeletedFalse(3L)).thenReturn(Optional.of(category()));
        ProductResponse response = service.updateSellerProduct(10L, request(), 1L, Role.SELLER);
        assertThat(response.isDeleted()).isTrue();
        assertThat(response.getProductStatus()).isEqualTo(ProductStatus.STOPPED);
        assertThat(response.getProductName()).isEqualTo("수정 상품");
        assertThat(product.getDeleteAt()).isEqualTo(deletedAt);
    }

    @Test
    void stopUsesExistingSoftDeleteAndStoppedStatus() {
        Product product = product();
        when(products.findByProductIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));
        service.deleteProduct(10L, 1L, Role.SELLER);
        assertThat(product.isDeleted()).isTrue();
        assertThat(product.getProductStatus()).isEqualTo(ProductStatus.STOPPED);
        assertThat(product.getDeleteAt()).isNotNull();
        verify(products, never()).delete(any(Product.class));
    }

    @Test
    void otherSellerCannotStopProduct() {
        Product product = product();
        when(products.findByProductIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> service.deleteProduct(10L, 2L, Role.SELLER))
                .isInstanceOf(ProductAccessDeniedException.class);
        assertThat(product.isDeleted()).isFalse();
    }

    private Product product() {
        Member owner = new Member("seller", "password", "seller@test.com", Role.SELLER);
        ReflectionTestUtils.setField(owner, "memberId", 1L);
        Product product = new Product(category(), owner, "상품", 10000, 5, ProductStatus.SELLING);
        ReflectionTestUtils.setField(product, "productId", 10L);
        return product;
    }

    private Category category() {
        Category category = new Category("키보드");
        ReflectionTestUtils.setField(category, "categoryId", 3L);
        return category;
    }

    private ProductUpdateRequest request() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        ReflectionTestUtils.setField(request, "categoryId", 3L);
        ReflectionTestUtils.setField(request, "productName", "수정 상품");
        ReflectionTestUtils.setField(request, "price", 25000);
        ReflectionTestUtils.setField(request, "stock", 0);
        ReflectionTestUtils.setField(request, "productStatus", ProductStatus.SOLD_OUT);
        return request;
    }
}
