package com.jdr1128.shopping.domain.product.service;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.exception.CategoryNotFoundException;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.exception.MemberNotFoundException;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.product.dto.ProductCreateRequest;
import com.jdr1128.shopping.domain.product.dto.ProductResponse;
import com.jdr1128.shopping.domain.product.dto.ProductUpdateRequest;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import com.jdr1128.shopping.domain.product.expection.ProductAccessDeniedException;
import com.jdr1128.shopping.domain.product.expection.ProductNotFoundException;
import com.jdr1128.shopping.domain.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            MemberRepository memberRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.memberRepository = memberRepository;
    }


    //상품 생성
    @Transactional
    public ProductResponse createProduct(
            ProductCreateRequest request,
            Long sellerId
    ){
        //request 받은 categoryId가 존재하는지 체크하고 category 객체 생성
        Category category = categoryRepository.findByCategoryIdAndIsDeletedFalse(request.getCategoryId())
                .orElseThrow(CategoryNotFoundException::new);

        //판매자가 존재하는지 체크하고 Member 객체 생성
        Member seller = memberRepository.findById(sellerId)
                //.orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
                .orElseThrow(MemberNotFoundException::new);

        //Product 객체 생성
        Product product = new Product(
                category,
                seller,
                request.getProductName(),
                request.getPrice(),
                request.getStock(),
                request.getProductStatus()
        );

        //상품 저장
        Product savedProduct = productRepository.save(product);
        //컨트롤러에 response 리턴
        return ProductResponse.from(savedProduct);
    }


    //전체 상품 찾기
    @Transactional(readOnly = true)
    public List<ProductResponse> getSellerProducts(Long sellerId, Role role, boolean includeStopped) {
        validateSeller(role);
        List<Product> products = includeStopped
                ? productRepository.findBySeller_MemberId(sellerId)
                : productRepository.findBySeller_MemberIdAndIsDeletedFalse(sellerId);
        return products.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getSellerProduct(Long productId, Long sellerId, Role role) {
        return ProductResponse.from(findSellerProduct(productId, sellerId, role));
    }

    @Transactional
    public ProductResponse updateSellerProduct(Long productId, ProductUpdateRequest request, Long sellerId, Role role) {
        Product product = findSellerProduct(productId, sellerId, role);
        Category category = categoryRepository.findByCategoryIdAndIsDeletedFalse(request.getCategoryId())
                .orElseThrow(CategoryNotFoundException::new);
        // 판매중지는 DELETE로만 처리하고, 중지된 상품을 수정해도 판매를 재개하지 않는다.
        if (!product.isDeleted() && request.getProductStatus() == ProductStatus.STOPPED) {
            throw new ProductAccessDeniedException();
        }
        product.updateProduct(category, request.getProductName(), request.getPrice(), request.getStock(),
                product.isDeleted() ? ProductStatus.STOPPED : request.getProductStatus());
        return ProductResponse.from(product);
    }

    private Product findSellerProduct(Long productId, Long sellerId, Role role) {
        validateSeller(role);
        return productRepository.findByProductIdAndSeller_MemberId(productId, sellerId)
                .orElseThrow(ProductNotFoundException::new);
    }

    private void validateSeller(Role role) {
        if (role != Role.SELLER) {
            throw new ProductAccessDeniedException();
        }
    }

    public List<ProductResponse> getProducts(Long categoryId){

        List<Product> products;
        if (categoryId == null){
            products = productRepository.findByIsDeletedFalse();
        } else {
            products = productRepository.findByCategory_CategoryIdAndIsDeletedFalse(categoryId);
        }
        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    //상품 찾기 (ProductId)
    public ProductResponse getProduct(Long productId){
        Product product = findActiveProduct(productId);
        return ProductResponse.from(product);
    }

    //상품 수정
    @Transactional
    public ProductResponse updateProduct(
            Long productId, //컨트롤러에서 받은 상품id
            ProductUpdateRequest request,   //수정request
            Long loginMemberId,     //회원Id체크
            Role loginMemberRole //권한 체크용
    ){
        Product product = findActiveProduct(productId);

        //Role 검증
        validateProductOwnerOrAdmin(product, loginMemberId, loginMemberRole);

        Category category = categoryRepository.findByCategoryIdAndIsDeletedFalse(request.getCategoryId())
                .orElseThrow(CategoryNotFoundException::new);

        product.updateProduct(
                category,
                request.getProductName(),
                request.getPrice(),
                request.getStock(),
                request.getProductStatus()
        );

        return ProductResponse.from(product);
    }


    //상품 삭제
    @Transactional
    public void deleteProduct(
            Long productId,
            Long loginMemberId,
            Role loginMemberRole
    ){
        Product product = findActiveProduct(productId);

        validateProductOwnerOrAdmin(product, loginMemberId, loginMemberRole);

        product.delete();
    }


    //존재하는 상품 찾기
    public Product findActiveProduct(Long productId){
        return productRepository.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(ProductNotFoundException::new);

    }

    //SELLER, ADMIN 검증
    private void validateProductOwnerOrAdmin(
            Product product,
            Long loginMemberId,
            Role loginMemberRole
    ) { //ADMIN 일 시 return
        if(loginMemberRole == Role.ADMIN){
            return;
        }

        Long sellerId = product.getSeller().getMemberId();

        //상품sellerId와 인가받은 loginMemberId 비교
        if(!sellerId.equals(loginMemberId)){
            throw new ProductAccessDeniedException();
        }
    }





}
