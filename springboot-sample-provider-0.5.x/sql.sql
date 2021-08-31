#provider项目：
#创建数据库inst01
CREATE TABLE tb_account_one
(
    acct_id varchar(16),
    amount  double(10, 2),
    frozen  double(10, 2),
    PRIMARY KEY (acct_id)
) ENGINE = InnoDB;

insert into tb_account_one (acct_id, amount, frozen)
values ('1001', 10000.00, 0.00);

CREATE TABLE `bytejta`
(
    `xid`   varchar(32) NOT NULL,
    `gxid`  varchar(40) DEFAULT NULL,
    `bxid`  varchar(40) DEFAULT NULL,
    `ctime` bigint(20)  DEFAULT NULL,
    PRIMARY KEY (`xid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
