package com.baidu.shop.utils;

import org.springframework.beans.BeanUtils;

/**
 * @ClassName BeanUtil
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/9/1
 * @Version V1.0
 **/
public class BeanUtil<T1,T2> {
    public static <T2> T2 copyProperties(Object source, Class<T2> clazz){

        if(null == source){
            return null;
        }
        if(null == clazz){
            return null;
        }

        try {
            T2 t2 = clazz.newInstance();
            BeanUtils.copyProperties(source,t2);
            return t2;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
