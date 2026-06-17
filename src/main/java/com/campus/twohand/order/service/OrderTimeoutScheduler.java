package com.campus.twohand.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000L)
    public void cancelExpiredUnpaidOrders() {
        int count = orderService.cancelAllExpiredUnpaidOrders(LocalDateTime.now());
        if (count > 0) {
            log.info("Auto cancelled {} expired unpaid orders", count);
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void autoFinishPaidOrders() {
        int count = orderService.autoFinishAllExpiredPaidOrders(LocalDateTime.now());
        if (count > 0) {
            log.info("Auto finished {} paid orders", count);
        }
    }
}
