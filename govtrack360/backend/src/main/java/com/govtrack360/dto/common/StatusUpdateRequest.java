package com.govtrack360.dto.common;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;
}
