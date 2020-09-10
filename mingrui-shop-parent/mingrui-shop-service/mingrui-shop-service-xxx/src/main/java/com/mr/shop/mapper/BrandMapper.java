package com.mr.shop.mapper;

import com.baidu.shop.entity.BrandEntity;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @ClassName BrandMapper
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/8/31
 * @Version V1.0
 **/
public interface BrandMapper extends Mapper<BrandEntity> {

    @Select(value = "select * from tb_brand b where b.id in(select cb.brand_id from tb_category_brand cb where cb.category_id=#{cid})")
    List<BrandEntity> getBrandByCateId(Integer cid);
}
