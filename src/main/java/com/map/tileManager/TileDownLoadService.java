package com.map.tileManager;

import com.map.svc.Service;
import com.map.utils.GeometryCreator;
import com.map.utils.WGS_Encrypt;
import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.adapt.jts.*;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsLayer;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsMvt;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;
import com.wdtinc.mapbox_vector_tile.util.JdkUtils;
import com.wdtinc.mapbox_vector_tile.util.ParameterParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class TileDownLoadService extends Service {

    private static Log log = LogFactory.getLog(TileDownLoadService.class);

    private static String TEST_LAYER_NAME = "layerNameHere";

    /**
     * Fixed randomization with arbitrary seed value.
     */
    private static final long SEED = 487125064L;

    /**
     * Fixed random.
     */
    private static final Random RANDOM = new Random(SEED);

    /**
     * Example world is 100x100 box.
     */
    private static final double WORLD_SIZE = 100D;

    /**
     * Do not filter tile geometry.
     */
    private static final IGeometryFilter ACCEPT_ALL_FILTER = geometry -> true;

    /**
     * Default MVT parameters.
     */
    private static final MvtLayerParams DEFAULT_MVT_PARAMS = new MvtLayerParams();

    /**
     * Generate Geometries with this default specification.
     */
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Override
    public String doService(HttpServletRequest req, HttpServletResponse res) throws IOException {

        String test = ParameterParser.getString(req, "test", "400");
        byte[] b = new byte[0];

        /*Coordinate[] coordinates1 = new Coordinate[]{
                new Coordinate(117.2,34.8),new Coordinate(122.2,34.8),
                new Coordinate(122.2,31.5),new Coordinate(117.2,31.5),new Coordinate(117.2,34.8)
        };
        Collection<Geometry> geometries =getPoints();

        JtsLayer layer = new JtsLayer("square", geometries);
        JtsMvt mvt = new JtsMvt(singletonList(layer));*/


        b = testPolygon();
        if (res.getBufferSize() < b.length)
            res.setBufferSize(b.length);
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setContentType("binary/octet-stream");
        res.setContentLength(b.length);
        res.getOutputStream().write(b);
        return "finish";
    }

    private static Polygon buildPolygon(Random random, int pointCount, GeometryFactory geomFactory) {
        if(pointCount < 3) {
            throw new RuntimeException("Need 3 or more points to be a polygon");
        }
        final CoordinateSequence coordSeq = getCoordSeq(random, pointCount, geomFactory);
        final ConvexHull convexHull = new ConvexHull(coordSeq.toCoordinateArray(), geomFactory);
        final Geometry hullGeom = convexHull.getConvexHull();
        return (Polygon) hullGeom;
    }

    private static CoordinateSequence getCoordSeq(Random random, int pointCount, GeometryFactory geomFactory) {
        final CoordinateSequence coordSeq = geomFactory.getCoordinateSequenceFactory().create(pointCount, 2);
        for(int i = 0; i < pointCount; ++i) {
            final Coordinate coord = coordSeq.getCoordinate(i);
            coord.setOrdinate(0, random.nextDouble() * WORLD_SIZE);
            coord.setOrdinate(1, random.nextDouble() * WORLD_SIZE);
        }
        return coordSeq;
    }

    private byte[] testPolygon(){
        // Create input geometry
        final GeometryFactory geomFactory = new GeometryFactory();
        final Geometry inputGeom = buildPolygon(RANDOM, 200, geomFactory);


        // Build tile envelope - 1 quadrant of the world
        final Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

        // Build MVT tile geometry
        final TileGeomResult tileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, geomFactory,
                DEFAULT_MVT_PARAMS, ACCEPT_ALL_FILTER);

        // Create MVT layer
        final VectorTile.Tile mvt = encodeMvt(DEFAULT_MVT_PARAMS, tileGeom);

        return mvt.toByteArray();
    }

    public byte[] testLines() throws IOException {

        // Create input geometry
        final GeometryFactory geomFactory = new GeometryFactory();
        //final Geometry inputGeom = buildLineString(RANDOM, 1, geomFactory);
        //final Geometry inputGeom = createLineString(geomFactory);
        final Geometry inputGeom = buildLineString(RANDOM, 10, geomFactory);
        // Build tile envelope - 1 quadrant of the world
        final Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

        // Build MVT tile geometry
        final TileGeomResult tileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, geomFactory,
                DEFAULT_MVT_PARAMS, ACCEPT_ALL_FILTER);

        // Create MVT layer
        final VectorTile.Tile mvt = encodeMvt(DEFAULT_MVT_PARAMS, tileGeom);

        // MVT Bytes
        final byte[] bytes = mvt.toByteArray();
        return bytes;
    }

    public byte[] testPointsInLayers() throws IOException {
        Point point1 = createPoint();
        Point point2 = createPoint();
        Point point3 = createPoint();

        String layer1Name = "Layer 1";
        String layer2Name = "Layer 2";

        byte[] bytes = new MvtWriter.Builder()
                .setLayer(layer1Name)
                .add(point1)
                .add(point2)
                .setLayer(layer2Name)
                .add(point3)
                .build();

        return bytes;
    }

    private static LineString buildLineString(Random random, int pointCount, GeometryFactory geomFactory) {
        final CoordinateSequence coordSeq = getCoordSeq(random, pointCount, geomFactory);
        return new LineString(coordSeq, geomFactory);
    }

    public Coordinate[] createCoor(){
        String path = "E:\\mapbox\\data\\zjdemo_shp\\HYD_LN.shp";

        List<Coordinate> coorLst = new ArrayList<>();
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        try {
            ShapefileDataStore sds = (ShapefileDataStore)dataStoreFactory.createDataStore(new File(path).toURI().toURL());
            sds.setCharset(Charset.forName("UTF-8"));
            SimpleFeatureSource featureSource = sds.getFeatureSource();
            SimpleFeatureIterator itertor = featureSource.getFeatures().features();

            int count = 0;
            while(itertor.hasNext()) {
                SimpleFeature feature = itertor.next();
                List<Object> list = feature.getAttributes();
                Object obj = list.get(0);
                if(obj instanceof com.vividsolutions.jts.geom.Point || obj instanceof com.vividsolutions.jts.geom.MultiPoint){
                    com.vividsolutions.jts.geom.Point pt = (com.vividsolutions.jts.geom.Point) obj;
                    com.vividsolutions.jts.geom.Coordinate c = pt.getCoordinate();
                    double[] xy = WGS_Encrypt.WGS2Mars(c.y, c.x);
                    c.setCoordinate(new com.vividsolutions.jts.geom.Coordinate(xy[1], xy[0]));
                }
                if(obj instanceof com.vividsolutions.jts.geom.LineString || obj instanceof com.vividsolutions.jts.geom.MultiLineString){
                    com.vividsolutions.jts.geom.Geometry line = (com.vividsolutions.jts.geom.Geometry) obj;
                    int parts = line.getNumGeometries();
                    for(int i = 0; i < parts; i++){
                        com.vividsolutions.jts.geom.LineString l = (com.vividsolutions.jts.geom.LineString) line.getGeometryN(i);
                        for(int j = 0, num = l.getNumPoints();j < num; j++){
                            com.vividsolutions.jts.geom.Coordinate coor = l.getCoordinateN(j);
                            double[] xy = WGS_Encrypt.WGS2Mars(coor.y, coor.x);
                            Coordinate newc = new Coordinate(xy[1], xy[0]);
                            coorLst.add(newc);
                        }
                    }
                }

            }
            itertor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Coordinate[] coors = new Coordinate[coorLst.size()];
        for(int k = 0; k < coorLst.size(); k++){
            coors[k] = coorLst.get(k);
        }

        return coors;
    }

    public LineString createLineString(GeometryFactory geomFactory){
        Coordinate[] coors = createCoor();
        final CoordinateSequence coordSeq = geomFactory.getCoordinateSequenceFactory().create(coors);

        return new LineString(coordSeq, geomFactory);
    }

    public Polygon createPolygon(){
        String path = "E:\\mapbox\\data\\zjdemo_shp\\HYD_PY.shp";
        Polygon pol = null;

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        try {
            ShapefileDataStore sds = (ShapefileDataStore)dataStoreFactory.createDataStore(new File(path).toURI().toURL());
            sds.setCharset(Charset.forName("UTF-8"));
            SimpleFeatureSource featureSource = sds.getFeatureSource();
            SimpleFeatureIterator itertor = featureSource.getFeatures().features();
            while(itertor.hasNext()) {
                SimpleFeature feature = itertor.next();
                List<Object> list = feature.getAttributes();
                Object obj = list.get(0);

                if(obj instanceof com.vividsolutions.jts.geom.Polygon || obj instanceof com.vividsolutions.jts.geom.MultiPolygon){
                    org.locationtech.jts.geom.GeometryFactory geomFactory = new org.locationtech.jts.geom.GeometryFactory();
                    com.vividsolutions.jts.geom.MultiPolygon polygon = (com.vividsolutions.jts.geom.MultiPolygon) obj;
                    com.vividsolutions.jts.geom.Coordinate[] c = polygon.getCoordinates();
                    CoordinateSequence coordSeq = geomFactory.getCoordinateSequenceFactory().create(c.length, 2);
                    for(int i = 0; i < c.length; i++){
                        Coordinate newC = new Coordinate(c[i].x, c[i].y);
                        coordSeq.getCoordinate(i).setCoordinate(newC);
                    }
                    final ConvexHull convexHull = new ConvexHull(coordSeq.toCoordinateArray(), geomFactory);
                    final Geometry hullGeom = convexHull.getConvexHull();
                    return (Polygon) hullGeom;
                }
            }
            itertor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pol;
    }

    private byte[] testPoints(){

        // Create input geometry
        final GeometryFactory geomFactory = new GeometryFactory();
        final Geometry inputGeom = buildMultiPoint(RANDOM, 20000, geomFactory);

        // Build tile envelope - 1 quadrant of the world
        final Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

        // Build MVT tile geometry
        final TileGeomResult tileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, geomFactory,
                DEFAULT_MVT_PARAMS, ACCEPT_ALL_FILTER);

        final VectorTile.Tile mvt = encodeMvt(DEFAULT_MVT_PARAMS, tileGeom);

        // MVT Bytes
        final byte[] bytes = mvt.toByteArray();

        return bytes;
    }

    public byte[] singleLayer() throws IOException {
        Collection<Geometry> geometries = australia();

        JtsLayer layer = new JtsLayer("animals", geometries);
        JtsMvt mvt = new JtsMvt(singletonList(layer));

        final byte[] encoded = MvtEncoder.encode(mvt);
        return encoded;
    }

    private static Collection<Geometry> australia() {
        return getPoints(
                createPoint1("Koala"),
                createPoint1("Wombat"),
                createPoint1("Platypus"),
                createPoint1("Dingo"),
                createPoint1("Croc"));
    }

    private static Point createPoint1(String name) {
        Coordinate coord = new Coordinate(RANDOM.nextInt(4096), RANDOM.nextInt(4096));
        Point point = GEOMETRY_FACTORY.createPoint(coord);

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("id", name.hashCode());
        attributes.put("name", name);
        point.setUserData(attributes);

        return point;
    }

    private static MultiPoint buildMultiPoint(Random random, int pointCount, GeometryFactory geomFactory) {
        final CoordinateSequence coordSeq = getCoordSeq(random, pointCount, geomFactory);
        return geomFactory.createMultiPoint(coordSeq);
    }

    private static VectorTile.Tile encodeMvt(MvtLayerParams mvtParams, TileGeomResult tileGeom) {

        // Build MVT
        final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();

        // Create MVT layer
        final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(TEST_LAYER_NAME, mvtParams);
        final MvtLayerProps layerProps = new MvtLayerProps();
        final UserDataIgnoreConverter ignoreUserData = new UserDataIgnoreConverter();

        // MVT tile geometry to MVT features
        final List<VectorTile.Tile.Feature> features = JtsAdapter.toFeatures(tileGeom.mvtGeoms, layerProps, ignoreUserData);
        layerBuilder.addAllFeatures(features);
        MvtLayerBuild.writeProps(layerBuilder, layerProps);

        // Build MVT layer
        final VectorTile.Tile.Layer layer = layerBuilder.build();

        // Add built layer to MVT
        tileBuilder.addLayers(layer);

        /// Build MVT
        return tileBuilder.build();
    }

    private static Collection<Geometry> getPoints(Point... points) {
        return asList(points);
    }

    private static Point createPoint(String name, double x, double y) {
        Coordinate coord = new Coordinate(x, y);
        Point point = GEOMETRY_FACTORY.createPoint(coord);

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("id", name.hashCode());
        attributes.put("name", name);
        point.setUserData(attributes);

        return point;
    }

    private Point createPoint() {
        Coordinate coord = new Coordinate(RANDOM.nextInt(4096), RANDOM.nextInt(4096));
        Point point = GEOMETRY_FACTORY.createPoint(coord);

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("id", RANDOM.nextDouble());
        attributes.put("name", String.format("name %f : %f", coord.x, coord.y));
        point.setUserData(attributes);

        return point;
    }

    private static Collection<Geometry> getPoints() {
        return getPoints(
                createPoint("a",117.2,34.8),
                createPoint("b", 122.2, 34.8),
                createPoint("c", 122.2, 31.5),
                createPoint("d", 117.2, 31.5),
                createPoint("e", 117.2, 34.8));
    }

    private static class MvtWriter {

        static class Builder {
            // Default MVT parameters
            private static final MvtLayerParams DEFAULT_MVT_PARAMS = new MvtLayerParams();

            private String activeLayer = "default";

            private Map<String, List<Geometry>> layers = new HashMap<>();

            Builder() {}

            Builder setLayer(String layerName) {
                JdkUtils.requireNonNull(layerName);
                activeLayer = layerName;
                return this;
            }

            Builder add(Geometry geometry) {
                JdkUtils.requireNonNull(geometry);
                getActiveLayer().add(geometry);
                return this;
            }

            byte[] build() {
                // Build MVT
                final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();

                for (Map.Entry<String, List<Geometry>> layer : layers.entrySet()) {
                    // Layer
                    String name = layer.getKey();
                    List<Geometry> geometries = layer.getValue();

                    // Create MVT layer
                    final VectorTile.Tile.Layer.Builder layerBuilder =
                            MvtLayerBuild.newLayerBuilder(name, DEFAULT_MVT_PARAMS);

                    final MvtLayerProps layerProps = new MvtLayerProps();

                    // MVT tile geometry to MVT features
                    final List<VectorTile.Tile.Feature> features =
                            JtsAdapter.toFeatures(geometries, layerProps,
                                    new UserDataKeyValueMapConverter());

                    layerBuilder.addAllFeatures(features);
                    MvtLayerBuild.writeProps(layerBuilder, layerProps);

                    // Build MVT layer
                    final VectorTile.Tile.Layer mvtLayer = layerBuilder.build();
                    tileBuilder.addLayers(mvtLayer);
                }

                // Build MVT
                return tileBuilder.build().toByteArray();
            }

            private List<Geometry> getActiveLayer() {
                boolean isDefined = layers.containsKey(activeLayer);
                if (!isDefined) {
                    layers.put(activeLayer, new ArrayList<>());
                }
                return layers.get(activeLayer);
            }
        }
    }

}
