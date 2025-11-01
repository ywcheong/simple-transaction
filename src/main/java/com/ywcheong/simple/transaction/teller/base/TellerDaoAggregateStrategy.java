package com.ywcheong.simple.transaction.teller.base;

import com.ywcheong.simple.transaction.teller.base.infra.TellerEntity;
import com.ywcheong.simple.transaction.teller.base.infra.TellerPermissionEntity;
import org.seasar.doma.AggregateStrategy;
import org.seasar.doma.AssociationLinker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@AggregateStrategy(root = TellerEntity.class, tableAlias = "t")
public interface TellerDaoAggregateStrategy {
    @AssociationLinker(propertyPath = "permissions", tableAlias = "p")
    BiFunction<TellerEntity, TellerPermissionEntity, TellerEntity> tellerPermLinker = (teller, perm) ->
    {
        List<TellerPermissionEntity> newPermissions = new ArrayList<>(teller.getPermissions());
        newPermissions.add(perm);
        return new TellerEntity(
                teller.getId(),
                teller.getType(),
                teller.getActive(),
                teller.getPublicKey(),
                newPermissions,
                teller.getVersion()
        );
    };
}