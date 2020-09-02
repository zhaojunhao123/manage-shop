package com.baidu.shop.service;

import com.baidu.shop.base.Result;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.validate.group.MingruiOperation;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Api(tags = "商品分类接口")
public interface CategoryService {

    @ApiOperation(value = "通过查询商品分类")
    @GetMapping(value = "category/list")
    public Result<List<CategoryEntity>> getCategoryByPid(Integer pid);

    @ApiOperation(value = "分类新增")
    @PostMapping(value = "category/add")
    Result<JsonObject> save(@Validated({MingruiOperation.Add.class}) @RequestBody CategoryEntity entity);

    @ApiOperation(value = "分类修改")
    @PutMapping(value = "category/edit")
    Result<JsonObject> edit(@Validated({MingruiOperation.Update.class}) @RequestBody CategoryEntity entity);

    @ApiOperation(value = "分类删除")
    @DeleteMapping(value = "category/delete")
    Result<JsonObject> delete(Integer id);

    @ApiOperation(value = "通过品牌id查询分类")
    @GetMapping(value = "category/getByBrand")
    public Result<List<CategoryEntity>> getByBrand(Integer brandId);

}
