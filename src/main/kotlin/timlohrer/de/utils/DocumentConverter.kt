package timlohrer.de.utils

import org.bson.Document
import java.lang.reflect.Modifier

inline fun <reified T : Any> T.toDocument(): Document {
    val document = Document();
    for (property in T::class.java.declaredFields) {
        property.isAccessible = true;
        document.append(property.name, property.get(this));
    }
    return document;
}

inline fun <reified T : Any> Document.toDataClass(
    fieldMappings: Map<String, String> = emptyMap(),
    excludeFields: Set<String> = emptySet()
): T {
    val clazz = T::class.java
    val instance = clazz.getDeclaredConstructor().newInstance()

    for (property in clazz.declaredFields) {
        if (!Modifier.isStatic(property.modifiers)) {
            property.isAccessible = true
            val fieldName = fieldMappings[property.name] ?: property.name

            if (!excludeFields.contains(fieldName)) {
                val rawValue = this[fieldName]
                val value = when (property.type) {
                    String::class.java -> rawValue?.toString() ?: ""
                    else -> rawValue
                }
                property.set(instance, value)
            }
        }
    }

    return instance
}
