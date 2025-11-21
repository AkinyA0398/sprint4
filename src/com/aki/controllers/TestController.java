package com.aki.controllers;

import annotation.AnnotationController;
import annotation.GetMethode;

@AnnotationController("test")
public class TestController {

    @GetMethode("hello")
    public String hello() {
        return "<h1>Hello from TestController!</h1><p>URL: /test/hello, Method: GET, Class: TestController</p>";
    }

    @GetMethode("list")
    public String list() {
        return "<h1>List from TestController!</h1><p>URL: /test/list, Method: GET, Class: TestController</p>";
    }

    @GetMethode("test-return1")
    public String getString() {
        System.out.println("Returning a String: Hello World");
        return "<h1>String Method</h1><p>Returning: Hello World</p>";
    }

    @GetMethode("test-return2")
    public boolean getBoolean() {
        System.out.println("Returning a boolean: true");
        return true;
    }
}
