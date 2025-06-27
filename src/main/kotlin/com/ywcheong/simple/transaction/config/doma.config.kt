package com.ywcheong.simple.transaction.config

import org.seasar.doma.jdbc.Config
import org.seasar.doma.jdbc.dialect.Dialect
import org.seasar.doma.jdbc.dialect.MysqlDialect
import org.seasar.doma.jdbc.tx.LocalTransactionDataSource
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource



@Configuration
class DomaConfig(
    val beanDataSource: DataSource
) : Config {
    override fun getDataSource(): DataSource = LocalTransactionDataSource(beanDataSource)
    override fun getDialect(): Dialect = MysqlDialect(MysqlDialect.MySqlVersion.V8)
}