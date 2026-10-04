package com.govtrack360.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UnreadCountResponse {
    private long unreadCount;
}
