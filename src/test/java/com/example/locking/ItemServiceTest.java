package com.example.locking;

import com.example.locking.items.Item;
import com.example.locking.items.ItemService;
import com.example.locking.items.ItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class ItemServiceTest {

	@Autowired
	private ItemService itemService;

	@Autowired
	private ItemRepository itemRepository;

	/**
	 * 데이터베이스에서 특정 row, 테이블을 락을 걸어
	 * 다른 트랜잭션이 동시에 해당 데이터를 읽기 X, 수정 X
	 * 해당 데이터에 대한 모든 읽기 및 쓰기 작업이 락이 해제될 때까지 대기
	 * 트랜잭션이 종료되거나 커밋될 때 해제
	 * <p>
	 * 비관적 락과 트랜잭션 적용 결과
	 * 첫 번째 트랜잭션이 완료될 때까지 두 번째 트랜잭션은 대기
	 * 이로 인해 두 번째 트랜잭션이 첫 번째 트랜잭션이 완료된 후 실행되며,
	 * 최종 결과는 예측 가능한 값(마지막으로 업데이트된 값)
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void 비관적락test() throws InterruptedException {

		log.info("초기 아이템 데이터를 설정합니다.");
		Item item = new Item();
		item.setName("Item 1");
		item.setQuantity(10);
		itemRepository.save(item);

		// 첫 번째 트랜잭션: 아이템 수량을 20으로 업데이트
		Thread thread1 = new Thread(() -> {
			log.info("스레드 1: 아이템 수량 업데이트를 시도합니다.");
			itemService.updateItemQuantity(item.getId(), 20);
			log.info("스레드 1: 아이템 수량 업데이트 완료.");
		});

		// 두 번째 트랜잭션: 아이템 수량을 30으로 업데이트
		Thread thread2 = new Thread(() -> {
			log.info("스레드 2: 아이템 수량 업데이트를 시도합니다.");
			itemService.updateItemQuantity(item.getId(), 30);
			log.info("스레드 2: 아이템 수량 업데이트 완료.");
		});

		// 두 스레드를 동시에 실행
		thread2.start();
		thread1.start();


		// 두 스레드가 종료될 때까지 대기
		thread1.join();
		thread2.join();

		// 최종 결과를 확인합니다.
		final Item finalItem = itemService.findItemById(item.getId());
		log.info("최종 아이템 수량: {}", finalItem.getQuantity());

		/*
		2024-08-21T21:59:31.364+09:00  INFO 37245 --- [locking] [       Thread-5] com.example.locking.ItemServiceTest      : 스레드 2: 아이템 수량 업데이트를 시도합니다.
		2024-08-21T21:59:31.364+09:00  INFO 37245 --- [locking] [       Thread-4] com.example.locking.ItemServiceTest      : 스레드 1: 아이템 수량 업데이트를 시도합니다.
		2024-08-21T21:59:31.447+09:00  INFO 37245 --- [locking] [       Thread-4] com.example.locking.items.ItemService    : 저장 완료: Item(id=1, name=Item 1, quantity=20)
		2024-08-21T21:59:31.465+09:00  INFO 37245 --- [locking] [       Thread-4] com.example.locking.ItemServiceTest      : 스레드 1: 아이템 수량 업데이트 완료.
		2024-08-21T21:59:31.466+09:00  INFO 37245 --- [locking] [       Thread-5] com.example.locking.items.ItemService    : 저장 완료: Item(id=1, name=Item 1, quantity=30)
		2024-08-21T21:59:31.467+09:00  INFO 37245 --- [locking] [       Thread-5] com.example.locking.ItemServiceTest      : 스레드 2: 아이템 수량 업데이트 완료.
		2024-08-21T21:59:31.478+09:00  INFO 37245 --- [locking] [    Test worker] com.example.locking.ItemServiceTest      : 최종 아이템 수량: 30
		 */

	}

}
