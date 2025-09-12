.PHONY: build-console run-console-h2 debug-console-h2

build-console:
	mvn -pl repeater-console/repeater-console-start -am clean package -DskipTests

run-console-h2:
	java -jar repeater-console/repeater-console-start/target/repeater-console.jar --spring.profiles.active=h2

debug-console-h2:
	java -jar repeater-console/repeater-console-start/target/repeater-console.jar --spring.profiles.active=h2 --debug
