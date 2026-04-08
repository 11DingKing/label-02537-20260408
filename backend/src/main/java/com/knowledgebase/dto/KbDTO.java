package com.knowledgebase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KbDTO {
    private Long id;
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "名称最长100字符")
    private String name;
    @Size(max = 500, message = "描述最长500字符")
    private String description;
}
