package com.baidu.shop.service.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.CategoryBrandEntity;
import com.baidu.shop.entity.SpuEntity;
import com.baidu.shop.service.BrandService;
import com.baidu.shop.utils.BeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.PinyinUtil;
import com.baidu.shop.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import com.baidu.shop.mapper.BrandMapper;
import com.baidu.shop.mapper.CategoryBrandMapper;
import com.baidu.shop.mapper.SpuMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName BrandServiceImpl
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/8/31
 * @Version V1.0
 **/
@RestController
public class BrandServiceImpl extends BaseApiService implements BrandService {

    @Resource
    private BrandMapper brandMapper;

    @Resource
    private CategoryBrandMapper categoryBrandMapper;

    @Resource
    private SpuMapper spuMapper;

    private void insertCategoryAndBrand(BrandDTO brandDTO, BrandEntity brandEntity){
        if(brandDTO.getCategory().contains(",")){

            List<CategoryBrandEntity> categoryBrandEntities = Arrays.asList(brandDTO.getCategory().split(","))
                    .stream().map(cid -> {
                        CategoryBrandEntity entity = new CategoryBrandEntity();
                        entity.setCategoryId(StringUtil.toInteger(cid));
                        entity.setBrandId(brandEntity.getId());

                        return entity;
                    }).collect(Collectors.toList());

            categoryBrandMapper.insertList(categoryBrandEntities);
        }else{

            CategoryBrandEntity entity = new CategoryBrandEntity();
            entity.setCategoryId(StringUtil.toInteger(brandDTO.getCategory()));
            entity.setBrandId(brandEntity.getId());

            categoryBrandMapper.insertSelective(entity);
        }
    }

    private void deleteCategoryAndBrand(Integer id){

        Example example = new Example(CategoryBrandEntity.class);
        example.createCriteria().andEqualTo("brandId",id);
        categoryBrandMapper.deleteByExample(example);
    }

    @Override
    public Result<List<BrandEntity>> getBrandByCate(Integer cid) {

        List<BrandEntity> brandByCateId = brandMapper.getBrandByCateId(cid);

        return this.setResultSuccess(brandByCateId);
    }

    @Override
    public Result<PageInfo<BrandEntity>> list(BrandDTO brandDTO) {

        if(ObjectUtil.isNotNull(brandDTO.getPage()) && ObjectUtil.isNotNull(brandDTO.getRows()))
            PageHelper.startPage(brandDTO.getPage(),brandDTO.getRows());

        Example example = new Example(BrandEntity.class);

        if(StringUtil.isNotEmpty(brandDTO.getSort())) example.setOrderByClause(brandDTO.getOrderByClause());

        Example.Criteria criteria = example.createCriteria();
        if(ObjectUtil.isNotNull(brandDTO.getId())) criteria.andEqualTo("id",brandDTO.getId());

        if (StringUtil.isNotEmpty(brandDTO.getName()))
            criteria.andLike("name","%" + brandDTO.getName() + "%");

        List<BrandEntity> list = brandMapper.selectByExample(example);

        PageInfo<BrandEntity> pageInfo = new PageInfo<>(list);

        return this.setResultSuccess(pageInfo);
    }

    @Transactional
    @Override
    public Result<JsonObject> saveBrand(BrandDTO brandDTO) {

        //统一转为大写
        BrandEntity brandEntity = BeanUtil.copyProperties(brandDTO, BrandEntity.class);

        /*String name = brandEntity.getName();//获取到品牌名称
        char c = name.charAt(0);//获取到品牌名称第一个字符
        //将第一个字符转换为pinyin
        String upperCase = PinyinUtil.getUpperCase(String.valueOf(c), PinyinUtil.TO_FIRST_CHAR_PINYIN);
        brandEntity.setLetter(upperCase.charAt(0));//获取拼音的首字母*/

        brandEntity.setLetter(PinyinUtil.getUpperCase(String.valueOf(brandEntity.getName().charAt(0)), PinyinUtil.TO_FIRST_CHAR_PINYIN).charAt(0));

        brandMapper.insertSelective(brandEntity);

        this.insertCategoryAndBrand(brandDTO,brandEntity);

        return this.setResultSuccess();
    }

    @Transactional
    @Override
    public Result<JsonObject> editBrand(BrandDTO brandDTO) {

        BrandEntity brandEntity = BeanUtil.copyProperties(brandDTO, BrandEntity.class);

        brandEntity.setLetter(PinyinUtil.getUpperCase(String.valueOf(brandEntity.getName().charAt(0)),
                PinyinUtil.TO_FIRST_CHAR_PINYIN).charAt(0));

        brandMapper.updateByPrimaryKeySelective(brandEntity);

        this.deleteCategoryAndBrand(brandEntity.getId());

        this.insertCategoryAndBrand(brandDTO,brandEntity);


        return this.setResultSuccess();
    }

    @Transactional
    @Override
    public Result<JsonObject> deleteBrand(Integer id) {

        //品牌绑定商品删除
        Example example = new Example(SpuEntity.class);
        example.createCriteria().andEqualTo("brandId", id);
        List<SpuEntity> spuEntities = spuMapper.selectByExample(example);

        if(spuEntities.size() > 0) return this.setResultError("此品牌下有商品不能删除");

        //品牌删除
        brandMapper.deleteByPrimaryKey(id);

        this.deleteCategoryAndBrand(id);

        return this.setResultSuccess();
    }

    @Override
    public Result<List<BrandEntity>> getBrandByIds(String brandIds) {
        List<Integer> brandIdsArr = Arrays.asList(brandIds.split(","))
                .stream().map(idStr -> Integer.parseInt(idStr)).collect(Collectors.toList());
        List<BrandEntity> list = brandMapper.selectByIdList(brandIdsArr);
        return this.setResultSuccess(list);
    }
}
