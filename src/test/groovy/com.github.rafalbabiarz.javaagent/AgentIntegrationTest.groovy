package com.github.rafalbabiarz.javaagent

import com.jayway.awaitility.Awaitility
import groovy.util.logging.Log4j
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

import java.util.concurrent.Callable

import static com.jayway.awaitility.Duration.FIVE_MINUTES
import static java.lang.ProcessBuilder.Redirect.INHERIT

@Log4j
class AgentIntegrationTest extends Specification {

    private static final int SUCESS_RESULT_CODE = 0
    private static final String MODIFIED_RESPONSE = "modified response"

    def cleanup() {
        new RestTemplate().postForLocation("http://localhost:8080/shutdown", "")
    }

    def "agent changes response when application is run with javaagent option"() {
        given:
        "javaagent jar is built"()
        "web application is built"()
        "web application is started in background with javaagent"()

        when:
        def result = new RestTemplate().getForObject("http://localhost:8080/", String.class)

        then:
        result == MODIFIED_RESPONSE
    }

    def "javaagent jar is built"() {
        packageMavenProject("pom.xml")
    }

    private packageMavenProject(String pom) {
        def mvnProcess = new ProcessBuilder("mvn", "-f${pom}", "package", "-DskipTests").redirectOutput(INHERIT).start()
        verifyProcessExitedWithSuccess(mvnProcess)
    }

    private verifyProcessExitedWithSuccess(Process mvnProcess) {
        def resultCode = mvnProcess.waitFor()
        assert resultCode == SUCESS_RESULT_CODE
    }

    def "web application is built"() {
        packageMavenProject("testapp/pom.xml")
    }

    def "web application is started in background with javaagent"() {
        log.info "Starting application"
        def application = new ProcessBuilder("java -jar -javaagent:target/javaagent-1.0-jar-with-dependencies.jar testapp/target/testapp-1.0.jar".split(" "))
                .redirectOutput(INHERIT).start()
        awaitApplicationIsRunning()
        assert application.isAlive()

    }

    def awaitApplicationIsRunning() {
        Awaitility.await().atMost(FIVE_MINUTES).until(new Callable() {
            @Override
            Boolean call() throws Exception {
                try {
                    return new RestTemplate().getForObject("http://localhost:8080/", String.class) != null
                }
                catch(Exception e) {
                    return false
                }
            }
        })
    }
}
