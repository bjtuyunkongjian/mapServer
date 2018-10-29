package com.map.svc;

import com.map.tileManager.TileDownLoadService;
import com.wdtinc.mapbox_vector_tile.util.ParameterParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;

public class ServiceFactory {

    private static Log log = LogFactory.getLog(ServiceFactory.class);

    public static Service getService(HttpServletRequest req) {

        log.info(req.getRequestURI());
        System.out.println(req.getRequestURI() + "===" +
        req.getQueryString());

        String test = ParameterParser.getString(req, "test", "400");
        if(test.equals("200")){
            return new TileDownLoadService();
        }
        return null;
    }
}
