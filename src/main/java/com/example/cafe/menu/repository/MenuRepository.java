package com.example.cafe.menu.repository;

import com.example.cafe.menu.entity.Menu;
import com.example.cafe.menu.entity.MenuStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

	Page<Menu> findAllByStatusIn(Collection<MenuStatus> statuses, Pageable pageable);
}
