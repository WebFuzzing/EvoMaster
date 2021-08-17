package com.foo.spring.rest.mysql.mybatis.mapper

import com.foo.spring.rest.mysql.mybatis.entity.Foo
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface FooMapper {

    @Select("SELECT * FROM test.FOO WHERE name = #{name}")
    fun findByName(@Param("name") name: String?): Foo?
}