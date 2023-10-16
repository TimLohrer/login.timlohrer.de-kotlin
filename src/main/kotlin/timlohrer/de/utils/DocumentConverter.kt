package timlohrer.de.utils

import org.bson.Document

inline fun <reified T : Any> T.toDocument(): Document {
    val document = Document();
    for (property in T::class.java.declaredFields) {
        property.isAccessible = true;
        document.append(property.name, property.get(this));
    }
    return document;
}