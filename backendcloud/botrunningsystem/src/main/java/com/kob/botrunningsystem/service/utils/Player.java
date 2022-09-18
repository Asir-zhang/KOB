package com.kob.botrunningsystem.service.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    Integer userId;
    String containerId;
    String javaName;
}
