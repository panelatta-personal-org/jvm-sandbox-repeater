.PHONY: build-console run-console-h2 debug-console-h2 run-console debug-console

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
