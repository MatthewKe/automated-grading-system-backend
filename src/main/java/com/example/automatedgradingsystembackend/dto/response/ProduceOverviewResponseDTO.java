package com.example.automatedgradingsystembackend.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProduceOverviewResponseDTO {
    private Map<Long, String> projectConfigs;
}
