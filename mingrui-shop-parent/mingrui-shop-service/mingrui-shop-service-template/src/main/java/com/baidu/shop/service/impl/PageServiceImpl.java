package com.baidu.shop.service.impl;


import com.baidu.shop.base.Result;
import com.baidu.shop.dto.*;
import com.baidu.shop.entity.*;
import com.baidu.shop.feign.BrandFeign;
import com.baidu.shop.feign.CategoryFeign;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.feign.SpecificationFeign;
import com.baidu.shop.service.PageService;
import com.baidu.shop.utils.BeanUtil;
import com.github.pagehelper.PageInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName PageServiceImpl
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/9/23
 * @Version V1.0
 **/
//@Service
public class PageServiceImpl implements PageService {

    //@Resource
    private GoodsFeign goodsFeign;

    //@Resource
    private BrandFeign brandFeign;

   // @Resource
    private CategoryFeign categoryFeign;

   // @Resource
    private SpecificationFeign specificationFeign;


    @Override
    public Map<String, Object> getPageInfoBySpuId(Integer spuId) {

        Map<String, Object> map = new HashMap<>();

        SpuDTO spuDTO = new SpuDTO();
        spuDTO.setId(spuId);
        Result<List<SpuDTO>> spuList = goodsFeign.list(spuDTO);

        if(spuList.getCode() == 200){

            if(spuList.getData().size() == 1){
                //spu信息
                SpuDTO spuInfo = spuList.getData().get(0);
                map.put("spuInfo",spuInfo);

                //品牌信息
                BrandDTO brandDTO = new BrandDTO();
                brandDTO.setId(spuInfo.getBrandId());
                Result<PageInfo<BrandEntity>> brandInfo = brandFeign.list(brandDTO);

                if(brandInfo.getCode() == 200){

                    PageInfo<BrandEntity> data = brandInfo.getData();
                    List<BrandEntity> list = data.getList();
                    if(list.size() == 1){
                        map.put("brandInfo",list.get(0));
                    }

                }

                //分类信息
                Result<List<CategoryEntity>>  categoryResult = categoryFeign.getByCategoryIds(String.join(",", Arrays.asList(spuInfo.getCid1() + "", spuInfo.getCid2() + "", spuInfo.getCid3() + "")));
                if (categoryResult.getCode() == 200) {
                    List<CategoryEntity> categoryList = categoryResult.getData();
                    map.put("categoryList",categoryList);
                }

                //skus
                //通过spuId查询sku集合
                Result<List<SkuDTO>>  skusResult = goodsFeign.getSkuBySpuId(spuInfo.getId());
                if (skusResult.getCode() == 200) {
                    List<SkuDTO> skuList = skusResult.getData();
                    map.put("skuList",skuList);
                }

                //特有规格参数
                SpecParamDTO specParamDTO = new SpecParamDTO();
                specParamDTO.setCid(spuInfo.getCid3());
                specParamDTO.setGeneric(false);
                Result<List<SpecParamEntity>> specParamInfoResult = specificationFeign.listParam(specParamDTO);
                if (specParamInfoResult.getCode() == 200) {
                    List<SpecParamEntity> specParamList = specParamInfoResult.getData();

                    Map<Integer, String> specParamMap = new HashMap<>();
                    specParamList.stream().forEach(param -> {
                        specParamMap.put(param.getId(),param.getName());
                    });
                    map.put("specParamMap",specParamMap);
                }

                //skuDetail
                Result<SpuDetailEntity> detailResult = goodsFeign.getDetailBySpuId(spuInfo.getId());
                if (detailResult.getCode() == 200) {
                    SpuDetailEntity spuDetailEntity = detailResult.getData();
                    map.put("spuDetailEntity",spuDetailEntity);
                }

                //规格
                SpecGroupDTO specGroupDTO = new SpecGroupDTO();
                specGroupDTO.setCid(spuInfo.getCid3());
                Result<List<SpecGroupEntity>> specGroupInfoResult = specificationFeign.list(specGroupDTO);

                if (specGroupInfoResult.getCode() == 200) {

                    List<SpecGroupEntity> specGroupEntityList = specGroupInfoResult.getData();

                    List<SpecGroupDTO> specGroupDTOList = specGroupEntityList.stream().map(specGroupEntity -> {
                        SpecGroupDTO specGroup = BeanUtil.copyProperties(specGroupEntity, SpecGroupDTO.class);
                        SpecParamDTO paramDTO = new SpecParamDTO();

                        paramDTO.setGroupId(specGroup.getId());
                        paramDTO.setGeneric(true);
                        Result<List<SpecParamEntity>> specParamResult = specificationFeign.listParam(paramDTO);

                        if (specParamResult.getCode() == 200) {
                            specGroup.setParamList(specParamResult.getData());
                        }
                        return specGroup;
                    }).collect(Collectors.toList());

                    map.put("specGroupDTOList",specGroupDTOList);
                }
            }
        }

        return map;
    }
}
