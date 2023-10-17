package timlohrer.de.config

class Config {
    val port: Int = 80;
    val mongoUri: String = "mongodb://192.168.178.99:4002";
    val JWTSecret: String = "h%tW@fRe2@z#TEp9ATdBus#zRa79h!^3#2E24hY%";
    val defaultTokenLifespand: Long = 1;
    val rememberMeTokenLifespand: Long = 30;
}