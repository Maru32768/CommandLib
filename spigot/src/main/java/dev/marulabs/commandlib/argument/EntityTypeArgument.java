package dev.marulabs.commandlib.argument;

import org.bukkit.entity.EntityType;

public class EntityTypeArgument extends EnumArgument<EntityType> {
    public EntityTypeArgument(String name) {
        super(name, EntityType.class);
    }
}
