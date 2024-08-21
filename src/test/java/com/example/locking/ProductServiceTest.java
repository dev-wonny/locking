package com.example.locking;


import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.locking.products.Product;
import com.example.locking.products.ProductRepository;
import com.example.locking.products.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@Slf4j
@SpringBootTest
public class ProductServiceTest {
	@Autowired
	private ProductService productService;

	@Autowired
	private ProductRepository productRepository;

	/**
	 * 데이터를 수정하기 전까지 락을 걸지 않고
	 * 수정 시점에만 충돌을 확인하는 방식
	 * 주로 데이터의 버전 번호를 사용하여 동시성 문제를 해결
	 * <p>
	 * version이라는 필드를 엔티티에 추가합니다. 이 필드는 해당 엔티티의 수정 횟수를 추적하는 역할
	 * <p>
	 * 트랜잭션이 엔티티를 수정하고 저장하려고 할 때, 현재 데이터베이스에 저장된 버전 번호와 트랜잭션이 처음 읽어온 버전 번호를 비교
	 * <p>
	 * 낙관적 락과 트랜잭션 적용 결과
	 * thread1이 먼저 실행되어 Product의 버전이 이미 변경되었기 때문에,
	 * thread2는 ObjectOptimisticLockingFailureException 예외를 발생시킴
	 * 이 예외는 assertThrows 메서드에 의해 예상된 결과로 처리
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void 낙관적락Test() throws InterruptedException {
		// 초기 데이터 설정
		Product product = new Product();
		product.setName("Product 1");
		product.setPrice(100.0);
		productRepository.save(product);
		log.info("초기 데이터 설정: {}", product.toString());

		// 첫 번째 트랜잭션: 상품 가격을 200.0으로 업데이트
		Thread thread1 = new Thread(() -> {
			productService.updateProductPrice(product.getId(), 200.0);
			log.info("first thread 데이터 설정: {}", product.toString());
		});

		// 두 번째 트랜잭션: 상품 가격을 300.0으로 업데이트
		Thread thread2 = new Thread(() -> {
			assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
				productService.updateProductPrice(product.getId(), 300.0);
				log.info("second thread 데이터 설정: {}", product.toString());
			});
		});

		// 두 스레드를 동시에 실행
		thread1.start();
		thread2.start();

		// 두 스레드가 종료될 때까지 대기
		thread1.join();
		thread2.join();

		/*
		2024-08-21T22:15:38.346+09:00  INFO 47207 --- [locking] [    Test worker] com.example.locking.ProductServiceTest   : 초기 데이터 설정: Product(id=1, name=Product 1, price=100.0, version=0)
		2024-08-21T22:15:38.379+09:00  INFO 47207 --- [locking] [       Thread-4] c.e.locking.products.ProductService      : 저장 완료: Product(id=1, name=Product 1, price=200.0, version=0)
		2024-08-21T22:15:38.379+09:00  INFO 47207 --- [locking] [       Thread-4] com.example.locking.ProductServiceTest   : first thread 데이터 설정: Product(id=1, name=Product 1, price=100.0, version=0)
		2024-08-21T22:15:38.387+09:00 ERROR 47207 --- [locking] [       Thread-5] c.e.locking.products.ProductService      : 낙관적 락 충돌이 발생했습니다. 다른 트랜잭션이 먼저 데이터를 수정했습니다.
		 */

	}

}
