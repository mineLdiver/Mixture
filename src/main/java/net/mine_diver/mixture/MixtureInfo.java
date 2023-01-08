package net.mine_diver.mixture;

import lombok.experimental.SuperBuilder;
import net.mine_diver.mixture.inject.HandlerInfo;

import java.util.Set;

@SuperBuilder
public class MixtureInfo {

    public final Class<?> mixtureClass;
    public final Set<HandlerInfo> handlers;
}
