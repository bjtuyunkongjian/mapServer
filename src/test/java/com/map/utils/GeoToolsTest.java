package com.map.utils;

import com.map.entity.ShpGeometry;
import com.vividsolutions.jts.geom.*;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.junit.Test;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;

public class GeoToolsTest {

    @Test
    public void testReadShp() throws Exception {

        String path0 = "E:\\mapbox\\data\\mapData\\binzhou\\binzhou1\\BinZhou1_GAGNPT.shp";
        String path1 = "E:\\mapbox\\data\\zjdemo_shp\\HYD_LN.shp";
        String path = "E:\\mapbox\\data\\zjdemo_shp\\HYD_PY.shp";
        String path2 = "E:\\mapbox\\data\\mapData\\shandong\\1024\\highway0.001\\GROALN_GAOGUO_0.001.shp";
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        try {
            ShapefileDataStore sds = (ShapefileDataStore)dataStoreFactory.createDataStore(new File(path2).toURI().toURL());
            sds.setCharset(Charset.forName("UTF-8"));
            SimpleFeatureSource featureSource = sds.getFeatureSource();
            SimpleFeatureIterator itertor = featureSource.getFeatures().features();

            while(itertor.hasNext()) {
                SimpleFeature feature = itertor.next();
                List<Object> list = feature.getAttributes();
                Object obj = list.get(0);
                if(obj instanceof Point || obj instanceof MultiPoint){
                    Point pt = (Point) obj;
                    Coordinate c = pt.getCoordinate();
                    double[] xy = WGS_Encrypt.WGS2Mars(c.y, c.x);
                    c.setCoordinate(new Coordinate(xy[1], xy[0]));
                }
                if(obj instanceof LineString || obj instanceof MultiLineString){
                    Geometry line = (Geometry) obj;
                    int parts = line.getNumGeometries();
                    for(int i = 0; i < parts; i++){
                        LineString l = (LineString) line.getGeometryN(i);
                        for(int j = 0, num = l.getNumPoints();j < num; j++){
                            Coordinate coor = l.getCoordinateN(j);
                            double[] xy = WGS_Encrypt.WGS2Mars(coor.y, coor.x);
                            Coordinate newc = new Coordinate(xy[1], xy[0]);
                            coor.setCoordinate(newc);
                        }
                    }
                }
                if(obj instanceof  Polygon || obj instanceof MultiPolygon){
                    Geometry polygon = (Geometry) obj;
                    int p = polygon.getNumGeometries();
                    for(int m = 0; m < p; m++){
                        Polygon pol = (Polygon) polygon.getGeometryN(m);
                        int num = pol.getNumPoints();
                        System.out.println(pol.toString());
                        for(int n = 0; n < num; n++){
                            Coordinate[] c = pol.getCoordinates();
                        }
                    }

                }
                /*Iterator<Property> it = feature.getProperties().iterator();
                while(it.hasNext()) {
                    Property pro = it.next();
                    System.out.println(pro);
                }*/
            }
            itertor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
