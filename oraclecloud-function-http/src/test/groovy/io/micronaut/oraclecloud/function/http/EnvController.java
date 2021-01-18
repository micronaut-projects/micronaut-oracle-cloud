package io.micronaut.oraclecloud.function.http;

import io.micronaut.context.env.Environment;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.Set;


@Controller("/env")
public class EnvController {
    private final Environment environment;

    public EnvController(Environment environment) {
        this.environment = environment;
    }

    @Get
    Set<String> index() {
        return environment.getActiveNames();
    }
}

