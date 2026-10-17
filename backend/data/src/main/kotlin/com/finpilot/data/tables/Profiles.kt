package com.finpilot.data.tables

import org.jetbrains.exposed.v1.core.Table

object Profiles : Table("profiles") {
    val id = uuid("id")
    val email = text("email").nullable()
    val displayName = text("display_name").nullable()
    val currency = char("currency", CURRENCY_LENGTH)
    val timezone = text("timezone")

    override val primaryKey = PrimaryKey(id)

    private const val CURRENCY_LENGTH = 3
}
