package com.govtrack360.dto.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCommentRequest {

    @NotBlank(message = "Comment cannot be empty")
    private String comment;
}
