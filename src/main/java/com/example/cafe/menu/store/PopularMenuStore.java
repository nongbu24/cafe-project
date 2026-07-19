package com.example.cafe.menu.store;

import com.example.cafe.menu.dto.PopularMenuCount;
import com.example.cafe.menu.dto.PopularMenuOrderItem;
import java.time.LocalDateTime;
import java.util.List;

public interface PopularMenuStore {

	void addPaidOrder(long orderId, LocalDateTime paidAt, List<PopularMenuOrderItem> items);

	List<PopularMenuCount> findPopularMenus(LocalDateTime from, LocalDateTime to, int limit);
}
