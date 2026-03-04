package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

// 前端提交的employee属性，和实体类的属性不完全一样时，采用DTO来封装数据
@Data
public class EmployeeDTO implements Serializable {

    private Long id;

    private String username;

    private String name;

    private String phone;

    private String sex;

    private String idNumber;

}
