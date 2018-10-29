package com.map.svc;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class Service {

    public abstract String doService(HttpServletRequest req, HttpServletResponse res) throws IOException;

}