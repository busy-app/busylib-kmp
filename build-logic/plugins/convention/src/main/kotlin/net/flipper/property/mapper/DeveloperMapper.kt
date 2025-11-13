package net.flipper.property.mapper

import net.flipper.property.model.Developer
import org.gradle.api.GradleException

object DeveloperMapper {
    fun parseDeveloper(value: String): Developer {
        val items: List<String> = value.split("|").map(String::trim)
        if (items.size != 3) {
            throw GradleException(
                "Developer profile should have 3 parts with | delimiter. " +
                    "For example: makeevrserg|Makeev Roman|makeevrserg@gmail.com"
            )
        }
        return Developer(
            id = items[0],
            name = items[1],
            email = items[2]
        )
    }

    fun parseDevelopers(value: String): List<Developer> {
        return value.split(",").map(::parseDeveloper)
    }
}
