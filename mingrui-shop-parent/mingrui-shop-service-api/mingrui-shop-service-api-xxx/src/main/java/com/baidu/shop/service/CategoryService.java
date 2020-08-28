package com.baidu.shop.service;

import com.baidu.shop.base.Result;
import com.baidu.shop.entity.CategoryEntity;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "商品分类接口")
public interface CategoryService {

    @ApiOperation(value = "通过查询商品分类")
    @GetMapping(value = "category/list")
    public Result<List<CategoryEntity>> getCategoryByPid(Integer pid);

    @ApiOperation(value = "分类新增")
    @PostMapping(value = "category/add")
    Result<JsonObject> save(@RequestBody CategoryEntity entity);

    @ApiOperation(value = "分类修改")
    @PutMapping(value = "category/edit")
    Result<JsonObject> edit(@RequestBody CategoryEntity entity);

    @ApiOperation(value = "分类删除")
    @DeleteMapping(value = "category/delete")
    Result<JsonObject> delete(Integer id);

}
