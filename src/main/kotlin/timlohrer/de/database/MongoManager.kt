package timlohrer.de.database

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import timlohrer.de.config.Config

class MongoManager {
    var mongoClient: MongoClient? = null;
    var database: MongoDatabase? = null;

    fun connect(config: Config) {
        mongoClient = MongoClients.create(config.mongoUri);
        database = mongoClient?.getDatabase("database")
    }

    fun getCollection(collectionName: String): MongoCollection<Document> {
        val collection: MongoCollection<Document> =
            database?.getCollection(collectionName) ?: return throw Exception("Failed to get collection $collectionName");
        return collection;
    }
}