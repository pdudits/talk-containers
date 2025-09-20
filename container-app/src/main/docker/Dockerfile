FROM payara/micro:6.2025.9-jdk21
COPY maven/container-app-*.war /opt/payara/deployments/

RUN java -jar payara-micro.jar --rootDir /opt/payara/micro --warmup

ENTRYPOINT exec java -XX:MaxRAMPercentage=${MEM_MAX_RAM_PERCENTAGE} -Xss${MEM_XSS} -XX:+UseContainerSupport -jar payara-micro.jar --rootDir /opt/payara/micro --nohazelcast "$0" $@
CMD ["--deploymentDir", "/opt/payara/deployments", "--contextRoot", "/"]