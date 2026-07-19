package com.example.cafe.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
	@NotBlank(message = "메뉴 이름은 필수로 입력해야 합니다.")
	@Size(max = 100, message = "메뉴 이름은 100자 이하여야 합니다.")
	String name,

	@NotNull(message = "메뉴 가격은 필수로 입력해야 합니다.")
	@Positive(message = "메뉴 가격은 1 이상이어야 합니다.")
	Long price
) {
}
