package com.xiaodingsiren.beanutilshelper;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;

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


    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException {
        Person person = new Person();
        person.setAge(18);
        person.setName("Nick");
        person.setEmail("a@b.com");
        person.setPhone("123456789");
        person.setAddress("Shenzhen");

        Human human = new Human();
        human.setAge(18);
        human.setName("Rose");
        BeanUtil.beanToMap(person, "xx");
        BeanUtil.copyProperties(person, human);
        BeanUtil.copyProperties(person, Human.class);
        BeanUtil.copyProperties(person, Human.class, "name");
        BeanUtil.copyProperties(person, human, "name", "age");
        // Test.Human(name=Nick, age=18)
        System.out.println(human);
        Employee employee = new Employee();
        // 多个共有属性自动转换为块注释
        BeanUtil.copyProperties(person, employee);
        BeanUtil.copyProperties(person, Employee.class);
        org.apache.commons.beanutils.BeanUtils.copyProperties(person, employee);
        org.springframework.beans.BeanUtils.copyProperties(person, employee);
        // 从 Person 对象中复制属性: name,phone 到 Employee 对象中
        Employee target = new Employee();
        target.setName(person.getName());
        target.setPhone(person.getPhone());
        // Test.Employee(name=Nick, age=18, email=a@b.com, phone=123456789, address=Shenzhen, dept=null, salary=null)
        System.out.println(employee);
    }
}
