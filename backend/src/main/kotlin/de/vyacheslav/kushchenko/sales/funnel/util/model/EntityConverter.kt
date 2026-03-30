package de.vyacheslav.kushchenko.sales.funnel.util.model

interface EntityConverter<K, V> {
    fun V.asModel(): K

    fun K.asEntity(): V
}
