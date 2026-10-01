FROM vodes/styx-baseimage:v2

COPY ./app.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]