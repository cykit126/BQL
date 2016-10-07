# 概述
BQL是Baxian Query Language的缩写。

为什么要实现BQL？

1. SQL的语法在`WHERE`条件动态变化时很难使用，比如我们需要提供一个用户账号查询后台页面，会根据`userid`、`age`、`email`等条件组合，那么我们必须根据条件的不同拼接不同的SQL语句。

    `SELECT name,email FROM user WHERE userid = 1`

    `SELECT name,email FROM user WHERE email='example@qq.com' and age > 10`

2. 我们需要一种比接口调用更方便的查询方式提供给第三方使用。如查询订单详情，每次查询不一定需要返回所有字段，采用接口调用的方式不够简洁。并且要提供多个查询接口，如
    
    `http://api.baxian.io/user/1234?fields=name,email`

    `http://api.baxian.io/order/123?fileds=title,price`
    
    采用BQL我们可以只提供一个查询接口，如
    
    `http://api.baxian.io/query?bql=select id,name from user where id=123`
    
# 特性
支持`SELECT`, `UPDATE`, `INSERT`, `DELETE`, `INSERT OR UPDATE`. 

# 注意
* INSERT OR UPDATE不支持多行插入
* 不支持`SELECT *`，因为这是个不好的习惯，浪费IO。

    `SELECT * FROM table`
