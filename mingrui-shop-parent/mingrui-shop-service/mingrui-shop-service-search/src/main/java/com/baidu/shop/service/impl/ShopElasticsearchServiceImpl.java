package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.document.GoodsDoc;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpecParamDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.entity.SpecParamEntity;
import com.baidu.shop.entity.SpuDetailEntity;
import com.baidu.shop.feign.BrandFeign;
import com.baidu.shop.feign.CategoryFeign;
import com.baidu.shop.feign.SpecificationFeign;
import com.baidu.shop.response.GoodsResponse;
import com.baidu.shop.service.ShopElasticsearchService;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.ESHighLightUtil;
import com.baidu.shop.utils.JSONUtil;
import com.baidu.shop.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName ShopElasticsearchServiceImpl
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/9/16
 * @Version V1.0
 **/
@RestController
@Slf4j
public class ShopElasticsearchServiceImpl extends BaseApiService implements ShopElasticsearchService {

    @Resource
    private GoodsFeign goodsFeign;

    @Resource
    private SpecificationFeign specificationFeign;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private BrandFeign brandFeign;

    @Resource
    private CategoryFeign categoryFeign;

    private NativeSearchQueryBuilder getNativeSearchQueryBuilder(String search, Integer page){

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.multiMatchQuery(search,"brandName","categoryName","title"));

        //高亮
        queryBuilder.withHighlightBuilder(ESHighLightUtil.getHighlightBuilder("title"));


        queryBuilder.addAggregation(AggregationBuilders.terms("brand_agg").field("brandId"));
        queryBuilder.addAggregation(AggregationBuilders.terms("cid_agg").field("cid3"));

        //分页
        queryBuilder.withPageable(PageRequest.of(page-1,10));


        return queryBuilder;
    }

    private List<CategoryEntity> getCategoryList(Aggregations aggregations){

        Terms cid_agg = aggregations.get("cid_agg");

        List<? extends Terms.Bucket> cidAggBuckets = cid_agg.getBuckets();
        List<String> cidList = cidAggBuckets.stream().map(cidAggBucket -> cidAggBucket.getKeyAsNumber().intValue() + ""
        ).collect(Collectors.toList());

        //通过分类id集合去查询数据,将List集合转换成,分隔的string字符串
        String cidJoin = String.join(",", cidList);
        Result<List<CategoryEntity>> categoryIds = categoryFeign.getByCategoryIds(cidJoin);
        return categoryIds.getData();
    }

    private List<BrandEntity> getBrandList(Aggregations aggregations){

        Terms brand_agg = aggregations.get("brand_agg");
        List<String> brandList = brand_agg.getBuckets().stream().map(brandBucket -> brandBucket.getKeyAsNumber().intValue() + "").collect(Collectors.toList());

        Result<List<BrandEntity>> brandByIds = brandFeign.getBrandByIds(String.join(",", brandList));

        return brandByIds.getData();
    }

    @Override
    public GoodsResponse search(String search, Integer page) {

        //判断搜索内容不能为空
        if(StringUtil.isEmpty(search)) throw new RuntimeException("查询内容不能为空");

        NativeSearchQueryBuilder queryBuilder = this.getNativeSearchQueryBuilder(search, page);

        SearchHits<GoodsDoc> searchHits = elasticsearchRestTemplate.search(queryBuilder.build(), GoodsDoc.class);
        List<SearchHit<GoodsDoc>> highLightHit = ESHighLightUtil.getHighLightHit(searchHits.getSearchHits());
        List<GoodsDoc> docList = highLightHit.stream().map(hit -> hit.getContent()).collect(Collectors.toList());

        long total = searchHits.getTotalHits();

        long totalPage = Double.valueOf(Math.ceil(Long.valueOf(total).doubleValue() / 10)).longValue();

        Map<String, Long> longMap = new HashMap<>();
        longMap.put("total", total);
        longMap.put("totalPage",totalPage);
        longMap.toString();  //传到前台是一个json字符串

        Aggregations aggregations = searchHits.getAggregations();

        List<CategoryEntity> categoryList = this.getCategoryList(aggregations);
        List<BrandEntity> brandList = this.getBrandList(aggregations);

        GoodsResponse goodsResponse = new GoodsResponse(total, totalPage, brandList, categoryList, docList);

        return goodsResponse;
    }

    @Override
    public Result<JSONObject> initGoodsEsData() {
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsDoc.class);
        if(!indexOperations.exists()){
            indexOperations.create();
            log.info("索引创建成功");
            indexOperations.createMapping();
            log.info("映射创建成功");
        }

        //批量新增数据
        List<GoodsDoc> goodsDocs = this.esGoodsInfo();
        elasticsearchRestTemplate.save(goodsDocs);

        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> clearGoodsEsData() {
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsDoc.class);
        if(indexOperations.exists()){
            indexOperations.delete();
            log.info("索引删除成功");
        }

        return this.setResultSuccess();
    }

    private List<GoodsDoc> esGoodsInfo() {

        SpuDTO spuDTO = new SpuDTO();
        Result<List<SpuDTO>> spuInfo = goodsFeign.list(spuDTO);

        //查询出来多个spu
        List<GoodsDoc> goodsDocs = new ArrayList<>();

        if(spuInfo.getCode() == HTTPStatus.OK){

            //spu数据
            List<SpuDTO> spuDTOList = spuInfo.getData();

            spuDTOList.stream().forEach(spu -> {

                GoodsDoc goodsDoc = new GoodsDoc();

                goodsDoc.setId(spu.getId().longValue());
                goodsDoc.setTitle(spu.getTitle());
                goodsDoc.setSubTitle(spu.getSubTitle());
                goodsDoc.setBrandName(spu.getBrandName());
                goodsDoc.setCategoryName(spu.getCategoryName());
                goodsDoc.setBrandId(spu.getBrandId().longValue());
                goodsDoc.setCid1(spu.getCid1().longValue());
                goodsDoc.setCid2(spu.getCid2().longValue());
                goodsDoc.setCid3(spu.getCid3().longValue());
                goodsDoc.setCreateTime(spu.getCreateTime());

                //通过spuId查询skuList
                Result<List<SkuDTO>> skuBySpuId = goodsFeign.getSkuBySpuId(spu.getId());

                List<Long> priceList = new ArrayList<>();

                List<Map<String,Object>> skuMap = null;

                Map<String, Object> specMap = new HashMap<>();

                if(skuBySpuId.getCode() == HTTPStatus.OK){

                    List<SkuDTO> skuList = skuBySpuId.getData();
                    skuMap = skuList.stream().map(sku -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", sku.getId());
                        map.put("title", sku.getTitle());
                        map.put("images", sku.getImages());
                        map.put("price", sku.getPrice());

                        priceList.add(sku.getPrice().longValue());

                        return map;
                    }).collect(Collectors.toList());
                    goodsDoc.setPrice(priceList);
                    goodsDoc.setSkus(JSONUtil.toJsonString(skuMap));
                }

                SpecParamDTO specParamDTO = new SpecParamDTO();
                specParamDTO.setCid(spu.getCid3());

                Result<List<SpecParamEntity>> specParamResult = specificationFeign.listParam(specParamDTO);


                if(specParamResult.getCode() == HTTPStatus.OK){
                    //规格参数的ID和规格参数的名字
                    List<SpecParamEntity> paramList = specParamResult.getData();

                    //通过spuId查询spuDetail,detail有通用和特殊规格的值
                    Result<SpuDetailEntity> spuDetailResult = goodsFeign.getDetailBySpuId(spu.getId());

                    if(spuDetailResult.getCode() == HTTPStatus.OK){
                        SpuDetailEntity spuDetailInfo = spuDetailResult.getData();

                        //通用规格参数的值
                        String genericSpec = spuDetailInfo.getGenericSpec();
                        Map<String, String> genericSpecMap = JSONUtil.toMapValueString(genericSpec);

                        //特有规格参数的值
                        String specialSpec = spuDetailInfo.getSpecialSpec();
                        Map<String, List<String>> specialSpecMap = JSONUtil.toMapValueStrList(specialSpec);

                        paramList.stream().forEach(param -> {
                            if(param.getGeneric()){

                                if(param.getNumeric() && param.getSearching()){
                                    specMap.put(param.getName(), this.chooseSegment(genericSpecMap.get(param.getId() + ""),param.getSegments(),param.getUnit()));
                                }else {
                                    specMap.put(param.getName(), genericSpecMap.get(param.getId() + ""));
                                }

                            }else {
                                specMap.put(param.getName(),specialSpecMap.get(param.getId() + ""));
                            }
                        });

                    }

                }
                goodsDoc.setSpecs(specMap);
                goodsDocs.add(goodsDoc);
            });

        }

        return goodsDocs;
    }

    /**
     * 把具体的值转换成区间-->不做范围查询
     * @param value
     * @param segments
     * @param unit
     * @return
     */
    private String chooseSegment(String value, String segments, String unit) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : segments.split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + unit + "以上";
                }else if(begin == 0){
                    result = segs[1] + unit + "以下";
                }else{
                    result = segment + unit;
                }
                break;
            }
        }
        return result;
    }
}
