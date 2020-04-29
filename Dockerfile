FROM openjdk:8u181-alpine3.8

WORKDIR /

COPY target/trialzk-0.1.0.jar trialzk.jar

ENTRYPOINT ["java"]
CMD ["-jar" "trialzk.jar"]