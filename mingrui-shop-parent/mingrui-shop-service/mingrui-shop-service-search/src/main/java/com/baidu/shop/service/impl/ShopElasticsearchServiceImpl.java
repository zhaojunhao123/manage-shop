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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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
import java.util.*;
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

    @Override
    public Result<JSONObject> saveData(Integer spuId) {

        //通过spuId查询数据
        SpuDTO spuDTO = new SpuDTO();
        spuDTO.setId(spuId);

        List<GoodsDoc> goodsDocs = this.esGoodsInfo(spuDTO);

        elasticsearchRestTemplate.save(goodsDocs.get(0));

        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> delData(Integer spuId) {
        return null;
    }

    private Map<String, List<String>> getspecParam(Integer hotCid, String search){

        SpecParamDTO specParamDTO = new SpecParamDTO();
        specParamDTO.setCid(hotCid);
        specParamDTO.setSearching(true);//查询搜索属性为true的规格参数

        Result<List<SpecParamEntity>> specParamResult = specificationFeign.listParam(specParamDTO);
        if(specParamResult.getCode() == HTTPStatus.OK){
            List<SpecParamEntity> specParamList = specParamResult.getData();

            //聚合查询
            NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();
            searchQueryBuilder.withQuery(QueryBuilders.multiMatchQuery(search,"brandName","categoryName","title"));

            //分页查询必须得到至少一条数据
            searchQueryBuilder.withPageable(PageRequest.of(0,1));

            specParamList.stream().forEach(specParam -> {
                searchQueryBuilder.addAggregation(AggregationBuilders.terms(specParam.getName()).field("specs." + specParam.getName() + ".keyword"));
            });

            SearchHits<GoodsDoc> searchHits = elasticsearchRestTemplate.search(searchQueryBuilder.build(), GoodsDoc.class);

            Map<String, List<String>> map = new HashMap<>();
            Aggregations aggregations = searchHits.getAggregations();

            specParamList.stream().forEach(specParam ->{
                Terms terms = aggregations.get(specParam.getName());
                List<? extends Terms.Bucket> buckets = terms.getBuckets();
                List<String> valueList = buckets.stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());

                map.put(specParam.getName(),valueList);
            });
            return map;
        }
        return null;
    }

    private NativeSearchQueryBuilder getNativeSearchQueryBuilder(String search, Integer page,String filter){

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        if(StringUtil.isNotEmpty(filter) && filter.length() > 2){
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            Map<String, String> filterMap = JSONUtil.toMapValueString(filter);

            filterMap.forEach((key,value) -> {
                MatchQueryBuilder matchQueryBuilder = null;

                //分类品牌和规格参数的查询方式不一样
                if(key.equals("cid3") || key.equals("brandId")){
                    matchQueryBuilder = QueryBuilders.matchQuery(key, value);
                }else{
                    matchQueryBuilder = QueryBuilders.matchQuery("specs." + key + ".keyword",value);
                }
                boolQueryBuilder.must(matchQueryBuilder);
            });
            queryBuilder.withFilter(boolQueryBuilder);
        }

        queryBuilder.withQuery(QueryBuilders.multiMatchQuery(search,"brandName","categoryName","title"));

        queryBuilder.addAggregation(AggregationBuilders.terms("brand_agg").field("brandId"));
        queryBuilder.addAggregation(AggregationBuilders.terms("cid_agg").field("cid3"));

        //高亮
        queryBuilder.withHighlightBuilder(ESHighLightUtil.getHighlightBuilder("title"));

        //分页
        queryBuilder.withPageable(PageRequest.of(page-1,10));


        return queryBuilder;
    }

    private Map<Integer,List<CategoryEntity>> getCategoryList(Aggregations aggregations){

        Terms cid_agg = aggregations.get("cid_agg");
        List<? extends Terms.Bucket> cidAggBuckets = cid_agg.getBuckets();

        List<Integer> hotCidArr = Arrays.asList(0);
        List<Long> maxArr = Arrays.asList(0L);
        HashMap<Integer, List<CategoryEntity>> map = new HashMap<>();



        List<String> cidList = cidAggBuckets.stream().map(cidAggBucket -> {
            Number keyAsNumber = cidAggBucket.getKeyAsNumber();

            if(cidAggBucket.getDocCount() > maxArr.get(0)){
                maxArr.set(0,cidAggBucket.getDocCount());
                hotCidArr.set(0,keyAsNumber.intValue());
            }

            return keyAsNumber.intValue() + "";
        }
        ).collect(Collectors.toList());

        Result<List<CategoryEntity>> categoryIds = categoryFeign.getByCategoryIds(String.join(",", cidList));

        map.put(hotCidArr.get(0),categoryIds.getData());
        return map;
    }

    private List<BrandEntity> getBrandList(Aggregations aggregations){

        Terms brand_agg = aggregations.get("brand_agg");
        List<String> brandList = brand_agg.getBuckets().stream().map(brandBucket -> brandBucket.getKeyAsNumber().intValue() + "").collect(Collectors.toList());

        Result<List<BrandEntity>> brandByIds = brandFeign.getBrandByIds(String.join(",", brandList));

        return brandByIds.getData();
    }

    @Override
    public GoodsResponse search(String search, Integer page,String filter) {

        //判断搜索内容不能为空
        if(StringUtil.isEmpty(search)) throw new RuntimeException("查询内容不能为空");

        SearchHits<GoodsDoc> searchHits = elasticsearchRestTemplate.search(this.getNativeSearchQueryBuilder(search, page,filter).build(), GoodsDoc.class);
        List<SearchHit<GoodsDoc>> highLightHit = ESHighLightUtil.getHighLightHit(searchHits.getSearchHits());
        //返回商品的集合
        List<GoodsDoc> docList = highLightHit.stream().map(hit -> hit.getContent()).collect(Collectors.toList());

        long total = searchHits.getTotalHits();
        long totalPage = Double.valueOf(Math.ceil(Long.valueOf(total).doubleValue() / 10)).longValue();

        Aggregations aggregations = searchHits.getAggregations();

        Map<Integer, List<CategoryEntity>> map = this.getCategoryList(aggregations);

        List<CategoryEntity> categoryList = null;
        Integer hotCid = 0;

        //遍历map集合的方式?????
        for(Map.Entry<Integer,List<CategoryEntity>> mapEntry : map.entrySet()){
            hotCid = mapEntry.getKey();
            categoryList = mapEntry.getValue();
        }

        Map<String, List<String>> specParamValueMap = this.getspecParam(hotCid, search);

        List<BrandEntity> brandList = this.getBrandList(aggregations);

        GoodsResponse goodsResponse = new GoodsResponse(total, totalPage, brandList, categoryList, docList,specParamValueMap);

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
        List<GoodsDoc> goodsDocs = this.esGoodsInfo(new SpuDTO());
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

    private List<GoodsDoc> esGoodsInfo(SpuDTO spuDTO) {

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
