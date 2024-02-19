package com.xiaodingsiren.beanutilshelper;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Rose Ding
 * @since 2024/1/30 14:18
 */
public class Test {


    @Data
    public static class A {
        String name;
        Integer age;
        String c;
        String d;
        String e;
    }

    @Data
    public static class B {
        String name;
        String age;
        String d;
        String c;
        String e;
    }

    public static void main0(String[] args) {
        A a = new A();
        a.setAge(18);
        a.setName("A");
        a.setC("c");
        B b = new B();
        BeanUtil.copyProperties(a, b);
        System.out.println(b);
    }

    public static void main(String[] args) {
        String linePrefix = "        ";
        String commentText = Stream.of("name", "age")
                .map(file->linePrefix +"\t\t"+file)
                .collect(Collectors.joining( ",\n",
                String.format("""
                                /*
                                %s    从 %s 对象中复制属性:
                                """, linePrefix,"a"),
                String.format("""
                                
                                %s    到 %s 对象中
                                %s*/
                                """, linePrefix,"b", linePrefix)));
        // 将注释与原代码的缩进对齐
        String commentWithIndent = linePrefix + commentText + '\n';
        System.out.println(commentWithIndent);
    }
}
