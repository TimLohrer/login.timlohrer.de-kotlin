package timlohrer.de.database

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import timlohrer.de.Config

class MongoManager {
    var mongoClient: MongoClient? = null;
    var database: MongoDatabase? = null;

    fun connect(config: Config) {
        mongoClient = MongoClients.create(config.mongoUri);
        database = mongoClient?.getDatabase("database")
    }

    fun getCollection(collectionName: String): MongoDatabase? {
        val collection: MongoDatabase? = mongoClient?.getDatabase(collectionName);
        return collection;
    }
}