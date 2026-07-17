package com.example.cafe.auth.store;

import java.time.LocalDateTime;

public interface TokenBlacklistStore {

	void add(String tokenId, LocalDateTime expiresAt);

	boolean contains(String tokenId);
}
