package timlohrer.de.config

class Config {
    val port: Int = 8080;
    val mongoUri: String = "";  // insert mongo uri here
    val JWTSecret: String = ""; // insert private jwt secret here
    val defaultTokenLifespand: Long = 1;
    val rememberMeTokenLifespand: Long = 30;
}