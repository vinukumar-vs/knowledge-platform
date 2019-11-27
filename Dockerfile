FROM openjdk:8-jre-alpine
RUN apk update \
    && apk add  unzip \
    && apk add curl \
    && adduser -u 1001 -h /home/sunbird/ -D sunbird \
    && mkdir -p /home/sunbird
RUN chown -R sunbird:sunbird /home/sunbird
USER sunbird
COPY ./learning-api/learning-service/target/learning-service-1.0-SNAPSHOT-dist.zip /home/sunbird/
RUN unzip /home/sunbird/learning-service-1.0-SNAPSHOT-dist.zip -d /home/sunbird/
RUN rm /home/sunbird/learning-service-1.0-SNAPSHOT-dist.zip
WORKDIR /home/sunbird/
CMD java  -cp '/home/sunbird/learning-service-1.0-SNAPSHOT/lib/*' -Dconfig.file=/home/sunbird/learning-service-1.0-SNAPSHOT/application.conf play.core.server.ProdServerStart /home/sunbird/learning-service-1.0-SNAPSHOT