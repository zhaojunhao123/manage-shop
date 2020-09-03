package com.mr.shop.service.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.SpecGroupDTO;
import com.baidu.shop.entity.SpecGroupEntity;
import com.baidu.shop.service.SpecificationService;
import com.baidu.shop.utils.BeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.google.gson.JsonObject;
import com.mr.shop.mapper.SpecGroupMapper;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.List;

/**
 * @ClassName SpecificationServiceImpl
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/9/3
 * @Version V1.0
 **/
@RestController
public class SpecificationServiceImpl extends BaseApiService implements SpecificationService {

    @Resource
    private SpecGroupMapper specGroupMapper;

    @Override
    public Result<List<SpecGroupEntity>> list(SpecGroupDTO specGroupDTO) {

        Example example = new Example(SpecGroupEntity.class);
        if(ObjectUtil.isNotNull(specGroupDTO.getCid())) example.createCriteria()
                .andEqualTo("cid", specGroupDTO.getCid());

        List<SpecGroupEntity> list = specGroupMapper.selectByExample(example);
        return this.setResultSuccess(list);
    }

    @Override
    public Result<JsonObject> add(SpecGroupDTO specGroupDTO) {

        specGroupMapper.insertSelective(BeanUtil.copyProperties(specGroupDTO,SpecGroupEntity.class));
        return this.setResultSuccess();
    }

    @Override
    public Result<JsonObject> edit(SpecGroupDTO specGroupDTO) {

        specGroupMapper.updateByPrimaryKeySelective(BeanUtil.copyProperties(specGroupDTO,SpecGroupEntity.class));
        return this.setResultSuccess();
    }

    @Override
    public Result<JsonObject> delete(Integer id) {

        specGroupMapper.deleteByPrimaryKey(id);
        return this.setResultSuccess();
    }


}
