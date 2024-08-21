package com.example.locking.products;

import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductService {
	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public void updateProductPrice(Long productId, Double newPrice) {
		try {
			final Product product = productRepository.findById(productId)
					.orElseThrow(() -> new RuntimeException("Product not found"));

			product.setPrice(newPrice);

			// 저장 시 버전 충돌이 발생하면 예외가 발생합니다.
			productRepository.save(product);
			log.info("저장 완료: {}", product.toString());

		} catch (ObjectOptimisticLockingFailureException e) {
			// 낙관적 락 예외 처리
			log.error("낙관적 락 충돌이 발생했습니다. 다른 트랜잭션이 먼저 데이터를 수정했습니다.");
			throw e;
		}

	}
}
