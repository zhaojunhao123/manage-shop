package com.mr.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.*;
import com.baidu.shop.service.BrandService;
import com.baidu.shop.service.GoodsService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.mr.shop.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName GoodsServiceImpl
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/9/7
 * @Version V1.0
 **/
@RestController
public class GoodsServiceImpl extends BaseApiService implements GoodsService {

    @Resource
    private SpuMapper spuMapper;

    @Autowired
    private BrandService brandService;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private SpuDetailMapper spuDetailMapper;

    @Resource
    private SkuMapper skuMapper;

    @Resource
    private StockMapper stockMapper;

    @Override
    public Result<SpuDetailEntity> getDetailBySpuId(Integer spuId) {

        SpuDetailEntity spuDetailEntity = spuDetailMapper.selectByPrimaryKey(spuId);
        return this.setResultSuccess(spuDetailEntity);
    }

    @Override
    public Result<List<SkuDTO>> getSkuBySpuId(Integer spuId) {
        List<SkuDTO> list = skuMapper.selectSkuAndStockBySpuId(spuId);
        return this.setResultSuccess(list);
    }

    private void addSpuAndStocks(List<SkuDTO> skus, Integer spuId, Date date) {
        //新增sku和stock数据
        skus.stream().forEach(skuDTO -> {
            //新增sku数据
            SkuEntity skuEntity = BeanUtil.copyProperties(skuDTO, SkuEntity.class);
            skuEntity.setSpuId(spuId);
            skuEntity.setCreateTime(date);
            skuEntity.setLastUpdateTime(date);
            skuMapper.insertSelective(skuEntity);

            //新增stock
            StockEntity stockEntity = new StockEntity();
            stockEntity.setSkuId(skuEntity.getId());
            stockEntity.setStock(skuDTO.getStock());
            stockMapper.insertSelective(stockEntity);
        });
    }

    @Override
    public Result<Map<String, Object>> list(SpuDTO spuDTO) {

        //分页
        if (ObjectUtil.isNotNull(spuDTO.getPage()) && ObjectUtil.isNotNull(spuDTO.getRows()))
            PageHelper.startPage(spuDTO.getPage(), spuDTO.getRows());

        Example example = new Example(SpuEntity.class);

        Example.Criteria criteria = example.createCriteria();
        if (StringUtil.isNotEmpty(spuDTO.getTitle()))
            criteria.andLike("title", "%" + spuDTO.getTitle() + "%");
        if (ObjectUtil.isNotNull(spuDTO.getSaleable()) && spuDTO.getSaleable() != 2)
            criteria.andEqualTo("saleable", spuDTO.getSaleable());

        if (ObjectUtil.isNotNull(spuDTO.getSort()))
            example.setOrderByClause(spuDTO.getOrderByClause());

        List<SpuEntity> list = spuMapper.selectByExample(example);

        List<SpuDTO> spuDTOList = list.stream().map(spuEntity -> {
            SpuDTO spuDTO1 = BeanUtil.copyProperties(spuEntity, SpuDTO.class);

            //设置品牌名称
            BrandDTO brandDTO = new BrandDTO();
            brandDTO.setId(spuDTO1.getBrandId());
            Result<PageInfo<BrandEntity>> brandInfo = brandService.list(brandDTO);

            if (ObjectUtil.isNotNull(brandInfo)) {
                PageInfo<BrandEntity> data = brandInfo.getData();
                List<BrandEntity> dataList = data.getList();

                if (!dataList.isEmpty() && dataList.size() == 1)
                    spuDTO1.setBrandName(dataList.get(0).getName());
            }


            //分类名称
            String caterogyName = categoryMapper.selectByIdList(
                    Arrays.asList(spuDTO1.getCid1(), spuDTO1.getCid2(), spuDTO1.getCid3()))
                    .stream().map(category -> category.getName())
                    .collect(Collectors.joining("/"));

            spuDTO1.setCategoryName(caterogyName);

            return spuDTO1;

        }).collect(Collectors.toList());


        PageInfo<SpuEntity> info = new PageInfo<>(list);

        return this.setResult(HTTPStatus.OK, info.getTotal() + "", spuDTOList);
    }

    @Transactional
    @Override
    public Result<JSONObject> save(SpuDTO spuDTO) {

        Date date = new Date();

        SpuEntity spuEntity = BeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setSaleable(1);
        spuEntity.setValid(1);
        spuEntity.setCreateTime(date);
        spuEntity.setLastUpdateTime(date);

        spuMapper.insertSelective(spuEntity);//新增spu数据

        Integer spuId = spuEntity.getId();

        //新增spudetail数据
        SpuDetailEntity spuDetailEntity = BeanUtil.copyProperties(spuDTO.getSpuDetail(), SpuDetailEntity.class);
        spuDetailEntity.setSpuId(spuId);
        spuDetailMapper.insertSelective(spuDetailEntity);

        //新增sku和stock数据
        this.addSpuAndStocks(spuDTO.getSkus(),spuId,date);

        return this.setResultSuccess();
    }

    @Transactional
    @Override
    public Result<JSONObject> edit(SpuDTO spuDTO) {
        Date date = new Date();

        //修改spu信息
        SpuEntity spuEntity = BeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setLastUpdateTime(date);

        spuMapper.updateByPrimaryKeySelective(spuEntity);

        //修改spuDetail信息
        SpuDetailEntity spuDetailEntity = BeanUtil.copyProperties(spuDTO.getSpuDetail(), SpuDetailEntity.class);
        spuDetailMapper.updateByPrimaryKeySelective(spuDetailEntity);

        //通过spuId查询出来将要被删除的sku数据
        this.getSkuIdArrBySpuId(spuDTO.getId());

        //新增sku和stock数据
        this.addSpuAndStocks(spuDTO.getSkus(), spuEntity.getId(), date);

        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> delete(Integer spuId) {
        //删除spu
        spuMapper.deleteByPrimaryKey(spuId);

        //删除spuDetail
        spuDetailMapper.deleteByPrimaryKey(spuId);

        this.getSkuIdArrBySpuId(spuId);

        return this.setResultSuccess();
    }

    private void getSkuIdArrBySpuId(Integer spuId) {
        Example example = new Example(SkuEntity.class);
        example.createCriteria().andEqualTo("spuId", spuId);

        //通过spuId查询出来将要被删除的sku数据
        List<Long> list = skuMapper.selectByExample(example).stream()
                .map(sku -> sku.getId()).collect(Collectors.toList());

        if (list.size() > 0) {
            //删除skus
            skuMapper.deleteByIdList(list);

            //删除stock,与修改时的逻辑一样,先查询出所有将要修改skuId然后批量删除
            stockMapper.deleteByIdList(list);
        }

    }
}

