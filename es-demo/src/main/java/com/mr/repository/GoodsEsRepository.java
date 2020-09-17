package com.mr.repository;

import com.mr.entity.GoodsEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * @ClassName GoodsEsRepository
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/9/14
 * @Version V1.0
 **/
public interface GoodsEsRepository extends ElasticsearchRepository<GoodsEntity,Long> {
    List<GoodsEntity> findAllByAndTitle(String title);

    List<GoodsEntity> findByAndPriceBetween(Double start,Double end);
}
