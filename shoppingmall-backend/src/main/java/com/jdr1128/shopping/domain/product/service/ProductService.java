package com.jdr1128.shopping.domain.product.service;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.exception.CategoryNotFoundException;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.product.dto.ProductCreateRequest;
import com.jdr1128.shopping.domain.product.dto.ProductResponse;
import com.jdr1128.shopping.domain.product.dto.ProductUpdateRequest;
import com.jdr1128.shopping.domain.product.entity.Product;
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

    @Autowired
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    public ProductService(ProductRepository productRepository,
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
            Long sellerId){
        Category category = categoryRepository.findByCategoryIdAndIsDeletedFalse(request.getCategoryId())
                .orElseThrow(CategoryNotFoundException::new);

        Member seller = memberRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        Product product = new Product(
                category,
                seller,
                request.getProductName(),
                request.getPrice(),
                request.getStock(),
                request.getProductStatus()
        );

        Product savedProduct = productRepository.save(product);
        return ProductResponse.from(savedProduct);
    }


    //전체 상품 찾기
    public List<ProductResponse> getProducts(){
        return productRepository.findByIsDeletedFalse()
                .stream()
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
            Long productId,
            ProductUpdateRequest request,
            Long loginMemberId,
            Role loginMemberRole
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
    ) { //ADMIN
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
