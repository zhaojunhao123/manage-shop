package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.service.ShopElasticsearchService;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.JSONUtil;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @ClassName ShopElasticsearchServiceImpl
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/9/16
 * @Version V1.0
 **/
@RestController
public class ShopElasticsearchServiceImpl extends BaseApiService implements ShopElasticsearchService {

    @Resource
    private GoodsFeign goodsFeign;

    @Override
    public Result<JSONObject> esGoodsInfo() {

        SpuDTO spuDTO = new SpuDTO();
        spuDTO.setPage(1);
        spuDTO.setRows(5);
        Result<List<SpuDTO>> list = goodsFeign.list(spuDTO);

        if(list.getCode() == HTTPStatus.OK){
            List<SpuDTO> spuDTOList = list.getData();
            spuDTOList.stream().forEach(spu -> {
                Result<List<SkuDTO>> skuBySpuId = goodsFeign.getSkuBySpuId(spu.getId());

                if(skuBySpuId.getCode() == HTTPStatus.OK){
                    List<SkuDTO> skuList = skuBySpuId.getData();
                    String skuJson = JSONUtil.toJsonString(skuList);

                    System.out.println(skuJson);
                    System.out.println(skuList);
                }
            });

        }

        return this.setResultSuccess();
    }
}
