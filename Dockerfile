FROM vodes/styx-baseimage:latest

COPY ./app.jar .

ENTRYPOINT ["java", "-jar", "app.jar"]