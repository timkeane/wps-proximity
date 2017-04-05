package gov.nyc.doitt.gis.wps.proximity

import spock.lang.*

import java.util.List

import geoscript.feature.*
import geoscript.layer.Layer
import geoscript.geom.*

class ProximityTest extends Specification {
	ProximityWps wps
	Schema pointSchema = new Schema('test-schema', [['geom', 'Point'], ['name', 'string'], ['address', 'string']])
	Layer pointLayer
	List inputPoints = [
			[geom: new Point(1, 10), name: 'New DoITT', address: '2 Metrotech'],
			[geom: new Point(10, 20), name: 'Old DoITT', address: '59 Maiden'],
			[geom: new Point(20, 2), name: 'City Hall', address: 'City Hall'],
			[geom: new Point(20, 2), name: 'Mayor', address: 'City Hall']
		]
	
	Schema polygonSchema = new Schema('test-schema', [['geom', 'Polygon'], ['name', 'string'], ['address', 'string']])
	Layer polygonLayer
	List inputPolygons = [
			[geom: new Polygon([0, 0], [10, 0], [10, 10], [0, 10], [0, 0]), name: 'Old DoITT', address: '59 Maiden'],
		]
	
	def setup() {
		wps = new ProximityWps()
		pointLayer = new Layer('test-points', pointSchema)
		inputPoints.eachWithIndex { attrs, id ->
			Feature f = new Feature(attrs, "f.${id}", pointSchema)
			pointLayer.add(f)
		}
		polygonLayer = new Layer('test-polygons', polygonSchema)
		inputPolygons.eachWithIndex { attrs, id ->
			Feature f = new Feature(attrs, "f.${id}", polygonSchema)
			polygonLayer.add(f)
		}
	}
	
	Feature pointFeatureByName(name){
		def feature
		pointLayer.eachFeature {
			if (it.get('name') == name){
				feature = it
			}
		}
		return feature
	}
	
	def 'test that resulting features have distance attribute and are sorted by it'(){
		given:
			Point point = new Point(1, 2)
			def prevDist = -1
		when:
			def output = wps.run([point: point, layer: pointLayer])
			def outputFeatures = output.result.getFeatures()
		then:
			assert outputFeatures.size() == 4
			outputFeatures.eachWithIndex { f, i ->
				def inputFeature = pointFeatureByName(f.get('name'))
				//assert f.getId() == inputFeature.getId()
				assert f.getGeom() == inputFeature.getGeom()
				assert f.get('name') == inputFeature.get('name')
				assert f.get('address') == inputFeature.get('address')
				assert f.get('distance') == point.distance(f.getGeom())
				assert f.get('distance') >= prevDist //sorted by distance
				prevDist = f.get('distance')
			}
	}

	def 'test that distance to polygon is to nearest edge, not to centroid'(){
		given:
			Point point = new Point(-5, 5)
		when:
			def output = wps.run([point: point, layer: polygonLayer])
			def outputFeatures = output.result.getFeatures()
		then:
			assert outputFeatures.size() == 1
			assert outputFeatures[0].get('distance') == 5
	}

}