package com.example.locking.items;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ItemService {
	private final ItemRepository itemRepository;

	public ItemService(ItemRepository itemRepository) {
		this.itemRepository = itemRepository;
	}

	// 비관적 락 적용
	@Transactional
	public void updateItemQuantity(Long itemId, Integer newQuantity) {
		final Item item = itemRepository.findByIdWithLock(itemId);

		item.setQuantity(newQuantity);
		itemRepository.save(item);
		log.info("저장 완료: {}", item.toString());
	}

	@Transactional(readOnly = true)
	public Item findItemById(Long itemId) {
		return itemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Item not found"));
	}


	// 데드락 적용
	@Transactional(timeout = 1, isolation = Isolation.SERIALIZABLE)
	public void updateItemsQuantityForDeadLock(Long itemId1, Long itemId2) {
		log.info("데드락 서비스 시작: 아이템 ID 1: {}, 아이템 ID 2: {}", itemId1, itemId2);
		// 첫 번째 아이템에 락을 건 후 업데이트
		try {
			final Item item1 = itemRepository.findByIdWithLock(itemId1);
			log.info("아이템 1 로드 완료: {}", item1);
			item1.setQuantity(item1.getQuantity() + 10);
			itemRepository.save(item1);
			log.info("데드락 서비스 item1 저장: {}", item1);

			// 잠시 대기
			Thread.sleep(5000);  // 대기 시간을 늘려 데드락을 명확히 확인

			// 두 번째 아이템에 락을 건 후 업데이트
			final Item item2 = itemRepository.findByIdWithLock(itemId2);
			log.info("아이템 2 로드 완료: {}", item2);
			item2.setQuantity(item2.getQuantity() + 20);
			itemRepository.save(item2);
			log.info("데드락 서비스 item2 저장: {}", item2);

		} catch (TransactionSystemException e) {
			log.error("트랜잭션 오류 발생 - {}", e.getMessage());
			throw e;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("스레드 인터럽트 발생 - {}", e.getMessage());

		} catch (Exception e) {
			log.error("알 수 없는 오류 발생", e);
			throw e;
		}
	}
}
