# Kotlin Backend for login.timlohrer.de

## Note: This was my first head on dive into kotlin / really anything java related. I would have wanted to design the API in another way but to make it easier I chose to rebuild a existing backend to a frontend of mine. Thats why I was limited in adjusting stuff to my new standarts ^^

Default Port is 8080.

### Setup

Enter your mongo uri and private jwt secret in the Config class before building the project with "./gradlew build".

### Using Docker

When using Docker you still have to do the steps in #Setup.
Now create the Docker image using "docker build -t login-backend ."
