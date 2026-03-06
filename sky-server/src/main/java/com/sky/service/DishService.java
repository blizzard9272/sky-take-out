package com.sky.service;

import com.sky.dto.DishDTO;
import org.springframework.beans.factory.annotation.Autowired;

public interface DishService {

    /**
     * 新增口味和对应的口味
     * @param dishDTO
     */
    void saveWithFlavor(DishDTO dishDTO);
}
