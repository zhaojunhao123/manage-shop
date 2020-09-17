package com.mr.controller;

import com.mr.entity.StudentEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;

/**
 * @ClassName TestController
 * @Description: TODO
 * @Author zhaojunhao
 * @Date 2020/9/15
 * @Version V1.0
 **/
@Controller
public class TestController {

    @GetMapping(value = "test")
    public String test(ModelMap modelMap){

        modelMap.put("name", "高一超");

        return "test";
    }

    @GetMapping(value = "stu")
    public String stu(ModelMap modelMap){

        StudentEntity studentEntity = new StudentEntity();
        studentEntity.setCode("高一超");
        studentEntity.setPass("洞拐洞拐洞洞拐");
        studentEntity.setAge(20);
        studentEntity.setLikeColor("<font color='yellow'>搞黄色</font>");

        modelMap.put("stu", studentEntity);

        return "student";
    }

    @GetMapping("list")
    public String list(ModelMap map){
        StudentEntity s1=new StudentEntity("001","111",18,"red");
        StudentEntity s2=new StudentEntity("002","222",19,"red");
        StudentEntity s3=new StudentEntity("003","333",16,"blue");
        StudentEntity s4=new StudentEntity("004","444",28,"blue");
        StudentEntity s5=new StudentEntity("005","555",68,"blue");

        //转为List
        map.put("stuList", Arrays.asList(s1,s2,s3,s4,s5));
        return "list";
    }
}
