package com.baidu.shop.service;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.validate.group.MingruiOperation;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName BrandService
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/8/31
 * @Version V1.0
 **/
@Api(tags = "商品接口")
public interface BrandService{

    @GetMapping(value = "brand/list")
    @ApiOperation(value = "查询商品信息")
    Result<PageInfo<BrandEntity>> list(BrandDTO brandDTO);

    @PostMapping(value = "brand/save")
    @ApiOperation(value = "新增品牌信息")
    Result<JsonObject> saveBrand(@Validated({MingruiOperation.Add.class}) @RequestBody BrandDTO brandDTO);

    @PutMapping(value = "brand/save")
    @ApiOperation(value = "修改品牌信息")
    Result<JsonObject> editBrand(@Validated({MingruiOperation.Add.class}) @RequestBody BrandDTO brandDTO);

    @DeleteMapping(value = "brand/delete")
    @ApiOperation(value = "通过id删除品牌信息")
    Result<JsonObject> deleteBrand(Integer id);

    @GetMapping(value = "brand/getBrandByCate")
    @ApiOperation(value = "通过分类id查询品牌信息")
    Result<List<BrandEntity>> getBrandByCate(Integer cid);

    @ApiOperation(value="通过品牌id集合获取品牌")
    @GetMapping(value = "brand/getBrandByIds")
    Result<List<BrandEntity>> getBrandByIds(@RequestParam String brandIds);
}
