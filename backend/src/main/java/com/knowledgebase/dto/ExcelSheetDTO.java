package com.knowledgebase.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ExcelSheetDTO {
    private String name;
    private List<String> headers;
    private List<Map<Integer, String>> rows;
}
