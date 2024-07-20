FROM vodes/styx-baseimage:latest

COPY ./app.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]