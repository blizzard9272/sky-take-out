package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单的任务
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimedOutOrders() {
        log.info("定时处理订单状态的任务执行了");

        // 获取时间
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        // select * from orders where status = ? and order_time < now() - 15min
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);

        // 先判断订单列表是否为空，如果不为空，则遍历订单列表，修改订单状态为已取消
        if (ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时未支付，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直处于配送中的订单的任务
     * 每天凌晨1点触发一次就行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusHours(-1); // 获取1小时前的时间，即为昨天的还处于配送中的订单

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);  // 设置订单状态为已完成
                orderMapper.update(orders);
            }
        }
    }

}
