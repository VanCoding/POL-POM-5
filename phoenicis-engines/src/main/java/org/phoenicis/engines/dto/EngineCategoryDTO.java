/*
 * Copyright (C) 2015-2017 PÂRIS Quentin
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.phoenicis.engines.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a category of application
 */
@JsonDeserialize(builder = EngineCategoryDTO.Builder.class)
public class EngineCategoryDTO {
    private final String name;
    private final String description;
    private final List<EngineSubCategoryDTO> subCategories;
    private String icon;

    private EngineCategoryDTO(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.subCategories = Collections.unmodifiableList(builder.subCategories);
        this.icon = builder.icon;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<EngineSubCategoryDTO> getSubCategories() {
        return subCategories;
    }

    public String getIcon() {
        return icon;
    }

    public static Comparator<EngineCategoryDTO> nameComparator() {
        return (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());
    }

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
    public static class Builder {
        private String name;
        private String description;
        private List<EngineSubCategoryDTO> subCategories = new ArrayList<>();
        private String icon;

        public Builder() {
            // Default constructor
        }

        public Builder(EngineCategoryDTO categoryDTO) {
            this.withName(categoryDTO.getName())
                    .withDescription(categoryDTO.getDescription())
                    .withSubCategories(categoryDTO.getSubCategories())
                    .withIcon(categoryDTO.getIcon());
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withSubCategories(List<EngineSubCategoryDTO> subCategories) {
            this.subCategories = subCategories;
            return this;
        }

        public Builder withIcon(String iconPath) {
            this.icon = iconPath;
            return this;
        }

        public EngineCategoryDTO build() {
            return new EngineCategoryDTO(this);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(name)
                .toString();
    }

}
