package com.fuguteams.fugureviveme.config;

import net.minecraft.resources.ResourceLocation;

public final class ResourceIdValidator {
    private ResourceIdValidator() {
    }

    public static boolean isValid(Object value) {
        if (!(value instanceof String resourceId) || resourceId.indexOf(':') <= 0) {
            return false;
        }

        ResourceLocation parsed = ResourceLocation.tryParse(resourceId);
        return parsed != null && resourceId.equals(parsed.toString());
    }
}
