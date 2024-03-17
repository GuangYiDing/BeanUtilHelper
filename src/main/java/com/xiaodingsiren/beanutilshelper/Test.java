package com.xiaodingsiren.beanutilshelper;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;

/**
 * @author Rose Ding
 * @since 2024/1/30 14:18
 */
public class Test {


    @Data
    public static class Person {
        String name;
        Integer age;
        String email;
        String phone;
        String address;
    }

    @Data
    public static class Human {
        String name;
        Integer age;
    }

    @Data
    public static class Employee {
        String name;
        Long age;
        String email;
        String phone;
        String address;
        String dept;
        String salary;
    }


    public static void main(String[] args) {
        Person person = new Person();
        person.setAge(18);
        person.setName("Nick");
        person.setEmail("a@b.com");
        person.setPhone("123456789");
        person.setAddress("Shenzhen");

        Human human = new Human();
        human.setAge(18);
        human.setName("Rose");
        // 行注释生成
        // 从 Person 对象中复制属性: age,name 到 Human 对象中
        /*
        差异对比
        Person ➡️ Human
        String name ✅ String name
        Integer age ✅ Integer age
        String email ❌
        String phone ❌
        String address ❌
        */
        BeanUtil.copyProperties(person, human);
        // 从 Person 对象中复制属性: age,name 到 Human 对象中
        /*
        差异对比
        Person ➡️ Human
        String name ✅ String name
        Integer age ✅ Integer age
        String email ❌
        String phone ❌
        String address ❌
        */
        BeanUtil.copyProperties(person, Human.class);
        // 从 Person 对象中复制属性: age 到 Human 对象中
        BeanUtil.copyProperties(person, Human.class,"name");
        /*
        差异对比
        Person ➡️ Human
        ~String name~ 🚫 ~String name~
        ~Integer age~ 🚫 ~Integer age~
        String email ❌
        String phone ❌
        String address ❌
        */
        BeanUtil.copyProperties(person, human,"name","age");
        // Test.Human(name=Nick, age=18)
        System.out.println(human);
        Employee employee = new Employee();
        // 多个共有属性自动转换为块注释
        /*
            从 Person 对象中复制属性:
        		address,
        		age,
        		email,
        		name,
        		phone
            到 Employee 对象中
        */
        BeanUtil.copyProperties(person, employee);
        BeanUtil.copyProperties(person, Employee.class);
        // 从 Person 对象中复制属性: name,phone 到 Employee 对象中
        // 从 Person 对象中复制属性: name,phone 到 Employee 对象中
        BeanUtil.copyProperties(person, Employee.class,"address","age","email");
        // Test.Employee(name=Nick, age=18, email=a@b.com, phone=123456789, address=Shenzhen, dept=null, salary=null)
        System.out.println(employee);
    }
}
