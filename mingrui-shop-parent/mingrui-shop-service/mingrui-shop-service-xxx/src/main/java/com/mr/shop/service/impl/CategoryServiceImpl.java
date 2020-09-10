package com.mr.shop.service.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.entity.CategoryBrandEntity;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.entity.SpecGroupEntity;
import com.baidu.shop.entity.SpuEntity;
import com.baidu.shop.service.CategoryService;
import com.baidu.shop.utils.ObjectUtil;
import com.google.gson.JsonObject;
import com.mr.shop.mapper.CategoryBrandMapper;
import com.mr.shop.mapper.CategoryMapper;
import com.mr.shop.mapper.SpecGroupMapper;
import com.mr.shop.mapper.SpuMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;
import javax.annotation.Resource;
import java.util.List;

/**
 * @ClassName CategoryServiceImpl
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/8/27
 * @Version V1.0
 **/
@RestController
public class CategoryServiceImpl extends BaseApiService implements CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private SpecGroupMapper specGroupMapper;

    @Resource
    private CategoryBrandMapper categoryBrandMapper;

    @Resource
    private SpuMapper spuMapper;

    @Override
    public Result<List<CategoryEntity>> getCategoryByPid(Integer pid) {

        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setParentId(pid);
        List<CategoryEntity> list = categoryMapper.select(categoryEntity);
        return this.setResultSuccess(list);
    }

    @Transactional
    @Override
    public Result<JsonObject> save(CategoryEntity entity) {

        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(entity.getParentId());
        categoryEntity.setIsParent(1);
        categoryMapper.updateByPrimaryKeySelective(categoryEntity);


        categoryMapper.insertSelective(entity);

        return this.setResultSuccess();
    }

    @Transactional
    @Override
    public Result<JsonObject> edit(CategoryEntity entity) {

        categoryMapper.updateByPrimaryKeySelective(entity);

        return this.setResultSuccess();
    }

    @Transactional
    @Override
    public Result<JsonObject> delete(Integer id) {

        CategoryEntity categoryEntity = categoryMapper.selectByPrimaryKey(id);

        if (ObjectUtil.isNull(id)) return this.setResultError("当前id不存在");

        if (categoryEntity.getIsParent() == 1) return this.setResultError("父节点不能被删除");

        Example example = new Example(CategoryEntity.class);
        example.createCriteria().andEqualTo("parentId",categoryEntity.getParentId());
        List<CategoryEntity> list = categoryMapper.selectByExample(example);


        Example example3 = new Example(SpuEntity.class);
        example3.createCriteria().andEqualTo("cid3",id);
        List<SpuEntity> spuEntities = spuMapper.selectByExample(example3);

        if(spuEntities.size() > 0) return this.setResultError("此分类下有商品不能被删除");


        Example example2 = new Example(CategoryBrandEntity.class);
        example2.createCriteria().andEqualTo("categoryId", id);
        List<CategoryBrandEntity> list2 = categoryBrandMapper.selectByExample(example2);

        if(list2.size() != 0 ) return this.setResultError("此分类已被品牌绑定不能删除");


        Example example1 = new Example(SpecGroupEntity.class);
        example1.createCriteria().andEqualTo("cid", id);
        List<SpecGroupEntity> list1 = specGroupMapper.selectByExample(example1);

        if(list1.size() > 0) return this.setResultError("此分类下有规格不能被删除");


        if (!list.isEmpty() && list.size() == 1) {
            CategoryEntity entity = new CategoryEntity();
            entity.setId(categoryEntity.getParentId());
            entity.setIsParent(0);
            categoryMapper.updateByPrimaryKeySelective(entity);
        }

        categoryMapper.deleteByPrimaryKey(id);

        return this.setResultSuccess();
    }

    @Override
    public Result<List<CategoryEntity>> getByBrand(Integer brandId) {

        List<CategoryEntity> byBrandId = categoryMapper.getByBrandId(brandId);

        return this.setResultSuccess(byBrandId);
    }

}
