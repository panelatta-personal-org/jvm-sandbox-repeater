.PHONY: build-console run-console-h2 debug-console-h2 run-console debug-console docker-build docker-push

build-console:
	mvn -pl repeater-console/repeater-console-start -am clean package -DskipTests

run-console-h2:
	java -jar repeater-console/repeater-console-start/target/repeater-console.jar --spring.profiles.active=h2

run-console:
	java -jar repeater-console/repeater-console-start/target/repeater-console.jar

debug-console-h2:
	java -jar repeater-console/repeater-console-start/target/repeater-console.jar --spring.profiles.active=h2 --debug

debug-console:
	java -jar repeater-console/repeater-console-start/target/repeater-console.jar --debug

image_version ?= 1.0.0

docker-build:
	docker build -f repeater-console/repeater-console-start/Dockerfile --build-arg JAR_FILE=repeater-console/repeater-console-start/target/repeater-console.jar -t gxxtplink/repeater-console:$(image_version) .

docker-push:
	docker push gxxtplink/repeater-console:$(image_version)
