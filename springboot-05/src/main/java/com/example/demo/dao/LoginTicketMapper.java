package com.example.demo.dao;

import com.example.demo.entity.LoginTicket;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;

@Mapper
@Deprecated
public interface LoginTicketMapper {

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired})"
    })
    @Options(useGeneratedKeys = true,keyProperty = "id")
    int insertLoginInsert(LoginTicket loginTicket);

    @Select({
            "select id,user_id,ticket,status,expired from login_ticket where ticket = #{ticket}"
    })
    LoginTicket selectByTicket(@Param("ticket") String ticket);

    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket}",
            "<if test=\"ticket!=null\">","and 1=1","</if>",
            "</script>"
    })
    int  updateStatus(String ticket,String status);

}
