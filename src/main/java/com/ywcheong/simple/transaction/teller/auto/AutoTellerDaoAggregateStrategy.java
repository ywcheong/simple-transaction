package com.ywcheong.simple.transaction.teller.auto;

import com.ywcheong.simple.transaction.teller.auto.infra.AutoTellerEntity;
import com.ywcheong.simple.transaction.teller.base.TellerDaoAggregateStrategy;
import com.ywcheong.simple.transaction.teller.base.infra.TellerEntity;
import com.ywcheong.simple.transaction.teller.base.infra.TellerPermissionEntity;
import org.seasar.doma.AggregateStrategy;
import org.seasar.doma.AssociationLinker;

import java.util.function.BiFunction;

@AggregateStrategy(root = AutoTellerEntity.class, tableAlias = "a")
public interface AutoTellerDaoAggregateStrategy {
    @AssociationLinker(propertyPath = "teller", tableAlias = "t")
    BiFunction<AutoTellerEntity, TellerEntity, AutoTellerEntity> tellerLinker = (autoTellerEntity, tellerEntity) ->
    {
        return new AutoTellerEntity(
                tellerEntity.getId(),
                autoTellerEntity.getVault(),
                tellerEntity,
                autoTellerEntity.getVersion()
        );
    };

    // 옆집에서 쓰는 링커랑 기능이 완전히 동일하므로 차용
    @AssociationLinker(propertyPath = "teller.permissions", tableAlias = "p")
    BiFunction<TellerEntity, TellerPermissionEntity, TellerEntity> tellerPermLinker = TellerDaoAggregateStrategy.tellerPermLinker;
}
