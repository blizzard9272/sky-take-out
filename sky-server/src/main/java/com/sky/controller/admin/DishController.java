package com.sky.controller.admin;

import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Api(tags="菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    /**
     * 新增口味和对应的口味
     * @param dishDTO
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询：{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * （批量）删除菜品
     * 业务规则：
     *  可以一次删除一个菜品，也可以批量删除菜品
     *  起售中的菜品不能删除
     *  被套餐关联的菜品不能删除
     *  删除菜品后，关联的口味数据也需要删除掉
     * @param ids 添加@RequestParam注解会将请求参数交给SpringMVC来处理，将ids从String转换成对应的List<Long>类型
     * @return
     */
    @DeleteMapping
    @ApiOperation("（批量）删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除菜品id列表：{}",ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询菜品数据
     * 该接口可以用于修改菜品数据时的前端回显设置
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品：{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品基本信息和菜品口味信息
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("菜品起售停售：{}，id：{}",status,id);
        dishService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        List<DishVO> list = dishService.listWithFlavor(dish);

        return Result.success(list);
    }
}
