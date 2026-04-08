package com.knowledgebase.dto;

import lombok.Data;
import java.util.List;

@Data
public class DocumentPreviewDTO {
    private String html;
    private List<ExcelSheetDTO> sheets;
}
