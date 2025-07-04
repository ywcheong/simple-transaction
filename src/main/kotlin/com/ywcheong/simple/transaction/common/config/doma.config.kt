package com.ywcheong.simple.transaction.common.config

import org.seasar.doma.jdbc.Config
import org.seasar.doma.jdbc.dialect.Dialect
import org.seasar.doma.jdbc.dialect.MysqlDialect
import org.seasar.doma.jdbc.tx.LocalTransactionDataSource
import org.seasar.doma.jdbc.tx.LocalTransactionManager
import org.seasar.doma.jdbc.tx.TransactionManager
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource



@Configuration
class DomaConfig(
    val beanDataSource: DataSource
) : Config {
    private val source = LocalTransactionDataSource(beanDataSource)
    private val manager = LocalTransactionManager(source.getLocalTransaction(jdbcLogger))

    override fun getDataSource(): DataSource = source
    override fun getTransactionManager(): TransactionManager = manager
    override fun getDialect(): Dialect = MysqlDialect(MysqlDialect.MySqlVersion.V8)
}