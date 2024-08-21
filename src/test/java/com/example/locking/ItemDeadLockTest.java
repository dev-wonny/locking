package com.example.locking;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.locking.items.Item;
import com.example.locking.items.ItemRepository;
import com.example.locking.items.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.TransactionSystemException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SpringBootTest
public class ItemDeadLockTest {

	@Autowired
	private ItemService itemService;

	@Autowired
	private ItemRepository itemRepository;

	@Test
	public void testDeadlock() throws InterruptedException {
		// 두 개의 아이템을 생성
		Item item1 = new Item();
		item1.setName("Item 1");
		item1.setQuantity(100);
		itemRepository.save(item1);

		Item item2 = new Item();
		item2.setName("Item 2");
		item2.setQuantity(200);
		itemRepository.save(item2);

		// 스레드 동기화를 위한 CountDownLatch 설정
		final CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Exception> exceptionInThread = new AtomicReference<>();

		Thread thread1 = new Thread(() -> {
			try {
				log.info("스레드 1 시작");
				latch.await();  // 다른 스레드가 준비될 때까지 대기
				log.info("스레드 1: 아이템 1 -> 아이템 2 업데이트 시도");
				itemService.updateItemsQuantityForDeadLock(item1.getId(), item2.getId());
				log.info("스레드 1: 업데이트 완료");
			} catch (TransactionSystemException e) {
				log.error("스레드 1: 트랜잭션 오류 발생 - {}", e.getMessage());
				exceptionInThread.set(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("스레드 1: 알 수 없는 오류 발생", e);
				exceptionInThread.set(e);
			}
		});

		Thread thread2 = new Thread(() -> {
			try {
				log.info("스레드 2 시작");
				log.info("스레드 2: 아이템 2 -> 아이템 1 업데이트 시도");
				itemService.updateItemsQuantityForDeadLock(item2.getId(), item1.getId());
				log.info("스레드 2: 업데이트 완료");
			} catch (TransactionSystemException e) {
				log.error("스레드 2: 트랜잭션 오류 발생 - {}", e.getMessage());
				exceptionInThread.set(e);
			} catch (Exception e) {
				log.error("스레드 2: 알 수 없는 오류 발생", e);
				exceptionInThread.set(e);
			}
		});

		// 두 스레드를 시작
		thread1.start();
		thread2.start();

		// 스레드가 준비되었음을 알리고 실행
		latch.countDown();

		// 두 스레드가 종료될 때까지 대기
		thread1.join();
		thread2.join();

		// 스레드 중 하나에서 TransactionSystemException이 발생했는지 확인
		assertThrows(Exception.class, () -> {
			if (exceptionInThread.get() != null) {
				throw exceptionInThread.get();
			}
		});
	}
	/*
	2024-08-21T23:25:06.109+09:00  INFO 88552 --- [locking] [       Thread-5] com.example.locking.ItemDeadLockTest     : 스레드 2 시작
	2024-08-21T23:25:06.109+09:00  INFO 88552 --- [locking] [       Thread-4] com.example.locking.ItemDeadLockTest     : 스레드 1 시작
	2024-08-21T23:25:06.110+09:00  INFO 88552 --- [locking] [       Thread-5] com.example.locking.ItemDeadLockTest     : 스레드 2: 아이템 2 -> 아이템 1 업데이트 시도
	2024-08-21T23:25:06.110+09:00  INFO 88552 --- [locking] [       Thread-4] com.example.locking.ItemDeadLockTest     : 스레드 1: 아이템 1 -> 아이템 2 업데이트 시도
	2024-08-21T23:25:06.111+09:00  INFO 88552 --- [locking] [       Thread-5] com.example.locking.items.ItemService    : 데드락 서비스 시작: 아이템 ID 1: 2, 아이템 ID 2: 1
	2024-08-21T23:25:06.111+09:00  INFO 88552 --- [locking] [       Thread-4] com.example.locking.items.ItemService    : 데드락 서비스 시작: 아이템 ID 1: 1, 아이템 ID 2: 2

	2024-08-21T23:25:06.171+09:00  INFO 88552 --- [locking] [       Thread-5] com.example.locking.items.ItemService    : 아이템 1 로드 완료: Item(id=2, name=Item 2, quantity=200)
	2024-08-21T23:25:06.176+09:00  INFO 88552 --- [locking] [       Thread-5] com.example.locking.items.ItemService    : 데드락 서비스 item1 저장: Item(id=2, name=Item 2, quantity=210)
	2024-08-21T23:25:11.191+09:00 ERROR 88552 --- [locking] [       Thread-5] com.example.locking.items.ItemService    : 알 수 없는 오류 발생

	2024-08-21T23:25:11.204+09:00  INFO 88552 --- [locking] [       Thread-4] com.example.locking.items.ItemService    : 아이템 1 로드 완료: Item(id=1, name=Item 1, quantity=100)
	2024-08-21T23:25:11.205+09:00  INFO 88552 --- [locking] [       Thread-4] com.example.locking.items.ItemService    : 데드락 서비스 item1 저장: Item(id=1, name=Item 1, quantity=110)
	2024-08-21T23:25:11.204+09:00 ERROR 88552 --- [locking] [       Thread-5] com.example.locking.ItemDeadLockTest     : 스레드 2: 알 수 없는 오류 발생

	*/

}
