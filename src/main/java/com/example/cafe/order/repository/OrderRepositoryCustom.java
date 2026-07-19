package com.example.cafe.order.repository;

import com.example.cafe.menu.dto.MenuOrderCount;
import java.util.List;

public interface OrderRepositoryCustom {

	List<MenuOrderCount> countOrdersByMenuIds(List<Long> menuIds);
}
