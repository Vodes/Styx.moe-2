Web App for managing the Styx ecosystem.<br>
Powered by [Vaadin](https://vaadin.com/) (24+), [karibu-dsl](https://github.com/mvysny/karibu-dsl)
and [vaadin-boot](https://github.com/mvysny/vaadin-boot) (using Jetty instead of Spring Boot).

## A few commands to get you started

### Run/Debug the thing

```./gradlew run```

### Build a fatjar

```./gradlew clean shadowJar```

### Build a fatjar (production mode)

```./gradlew clean shadowJar "-Pvaadin.productionMode"```
