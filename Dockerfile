FROM dockerfile/java
MAINTAINER Odd Möller <odd.moller@gmail.com>
ADD /app/solum-assembly-0.1.jar /app/solum-assembly-0.1.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/solum-assembly-0.1.jar"]