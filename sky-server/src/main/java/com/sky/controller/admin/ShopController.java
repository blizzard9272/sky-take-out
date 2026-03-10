package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

// 因为Bean的名字不能重复，所以在这里给这个控制器指定一个名字，避免和用户端的ShopController冲突
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    // 注入RedisTemplate对象
    @Autowired
    private RedisTemplate redisTemplate;
    // 定义一个店铺状态的字符串常量
    public static final String KEY = "SHOP_STATUS";

    /**
     * 设置店铺的营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺的营业状态为：{}",status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }

    /**
     * 获取店铺的营业状态
     * @return
     */
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取店铺的营业状态：{}",status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
