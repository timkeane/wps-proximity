package gov.nyc.doitt.gis.wps.proximity

import spock.lang.*

import geoserver.GeoServer
import geoserver.catalog.Catalog
import geoserver.catalog.Layer

import java.util.List

import geoscript.feature.*
import geoscript.layer.Layer
import geoscript.geom.*
import geoscript.proj.Projection

class ProximityWpsTest extends Specification {
	ProximityWps wps

	def 'test getUnits'(){
		when:
			def wps = new ProximityWps()
		then:
			assert wps != null
			assert wps.getUnits('foot_survey_us') == 'feet'
			assert wps.getUnits('m') == 'meters'
			assert wps.getUnits('°') == null
	}

	def 'test getLayer'(){
		when:
			Layer geoScriptLayer = new Layer('testLayer')
			geoserver.catalog.Layer layerMock = Mock {
				1 * getGeoScriptLayer() >> geoScriptLayer
			}
			Catalog catalogMock = Mock {
				1 * getLayer('testLayer') >> layerMock
			}
			GeoServer geoserverMock = Mock {
				1 * getCatalog() >> catalogMock
			}
			ProximityWps.metaClass.getGeoServer = {geoserverMock}
 			def wps = new ProximityWps()
		then:
			assert wps.getLayer('testLayer') == geoScriptLayer
	}

	def 'test convertUnits from feet to meters'(){
		when:
 			def wps = new ProximityWps()
		then:
			assert wps.convertUnits(1, 'feet', 'meters') == 0.3048
			assert wps.convertUnits(123.456, 'feet', 'meters') == 37.6293888
	}

	def 'test convertUnits from meters to feet'(){
		when:
			 def wps = new ProximityWps()
		then:
			assert wps.convertUnits(1, 'meters', 'feet') == 3.28084
			assert wps.convertUnits(654.321,'meters', 'feet') == 2146.72250964
	}

	def 'test convertUnits where units are same'(){
		when: 
			def wps = new ProximityWps()
		then: 
			assert wps.convertUnits(1, 'meters', 'meters') == 1
			assert wps.convertUnits(46,'feet', 'feet') == 46
	}
	
	def 'test getLayerUnits epsg:2263'(){
		when:
			 def wps = new ProximityWps()
			 def proj = new Projection('epsg:2263')
		then:
			assert wps.getLayerUnits(proj) == 'feet'
	}

	def 'test getLayerUnits epsg:3857'(){
		when:
			 def wps = new ProximityWps()
			 def proj = new Projection('epsg:3857')
		then:
			assert wps.getLayerUnits(proj) == 'meters'
	}

	def 'test getLayerUnits epsg:3857'(){
		when:
			 def wps = new ProximityWps()
			 def proj = new Projection('epsg:3857')
		then:
			assert wps.getLayerUnits(proj) == 'meters'
	}

	def 'test getLayerUnits epsg:4326 throws Exception'(){
		when:
			 def wps = new ProximityWps()
			 def proj = new Projection('epsg:4326')
			 wps.getLayerUnits(proj)
		 then:
		 	thrown Exception
	}

	def 'test getFilter when point (2263) and layer (2263) are same proj and distance units (feet) are same as layer(feet)'(){
		given:
			 def wps = new ProximityWps()
			 def point = new Point(1, 2)
			 Field fieldMock = Mock {
				 1 * getName() >> 'the_geom'
			 } 
			 Schema schemaMock = Mock {
				 1 * getGeom() >> fieldMock
			 }
			 Layer layerMock = Mock {
				 1 * getProj() >> new Projection('epsg:2263')
				 1 * getSchema() >> schemaMock
				 
			 }
		 when:
		 	def filter = wps.getFilter(layerMock, 3, 'feet', 'epsg:2263', point)
		 then:
		 	assert filter == 'DWITHIN(the_geom, POINT (1 2), 3, feet)'
	}

	def 'test getFilter when point (2263) and layer (3857) are different proj and distance units (meters) are same as layer (meters)'(){
		given:
			 def wps = new ProximityWps()
			 def point = new Point(1, 2)
			 Field fieldMock = Mock {
				 1 * getName() >> 'the_geom'
			 } 
			 Schema schemaMock = Mock {
				 1 * getGeom() >> fieldMock
			 }
			 Layer layerMock = Mock {
				 1 * getProj() >> new Projection('epsg:3857')
				 1 * getSchema() >> schemaMock
			 }
		 when:
		 	def filter = wps.getFilter(layerMock, 3, 'meters', 'epsg:2263', point)
		 then:
		 	assert filter == 'DWITHIN(the_geom, POINT (-8629440.293092024 4882288.084173), 3, meters)'
	}

	def 'test getFilter when point (2263) and layer (3857) are different proj and distance units (feet) are different from layer (meters)'(){
		given:
			 def wps = new ProximityWps()
			 def point = new Point(1, 2)
			 Field fieldMock = Mock {
				 1 * getName() >> 'the_geom'
			 } 
			 Schema schemaMock = Mock {
				 1 * getGeom() >> fieldMock
			 }
			 Layer layerMock = Mock {
				 1 * getProj() >> new Projection('epsg:3857')
				 1 * getSchema() >> schemaMock
			 }			 
		 when:
		 	def filter = wps.getFilter(layerMock, 3, 'feet', 'epsg:2263', point)
		 then:
		 	assert filter == 'DWITHIN(the_geom, POINT (-8629440.293092024 4882288.084173), 0.9144, meters)'
	}
	
	def 'test getFilter when point (2263) and layer (2263) are same proj and distance units (meters) are different from layer (feet)'(){
		given:
			 def wps = new ProximityWps()
			 def point = new Point(1, 2)
			 Field fieldMock = Mock {
				 1 * getName() >> 'the_geom'
			 } 
			 Schema schemaMock = Mock {
				 1 * getGeom() >> fieldMock
			 }
			 Layer layerMock = Mock {
				 1 * getProj() >> new Projection('epsg:2263')
				 1 * getSchema() >> schemaMock
			 }
		 when:
		 	def filter = wps.getFilter(layerMock, 3, 'meters', 'epsg:2263', point)
		 then:
		 	assert filter == 'DWITHIN(the_geom, POINT (1 2), 9.84252, feet)'
	}
	
	def 'test getNewSchema where requested proj (2263) is same as layer proj (2263)'(){
		given:
			def wps = new ProximityWps()
			Schema schema = new Schema("testlayer","geom:Point:srid=2263,name:String,address:String")
			geoscript.layer.Layer layerMock = Mock {
				 2 * getSchema() >> schema
				 1 * getName() >> 'testLayer'
			}
		when:
			def newSchema = wps.getNewSchema(layerMock, 'epsg:2263')
		then:
			assert newSchema.toString() == 'testLayer geom: Point(EPSG:2263), name: String, address: String, distance: Double'
	}

	def 'test getNewSchema where requested proj (3857) is different as layer proj (2263)'(){
		given:
			def wps = new ProximityWps()
			Schema schema = new Schema("testlayer","geom:Point:srid=2263,name:String,address:String")
			geoscript.layer.Layer layerMock = Mock {
				 2 * getSchema() >> schema
				 1 * getName() >> 'testLayer'
			}
		when:
			def newSchema = wps.getNewSchema(layerMock, 'epsg:3857')
		then:
			assert newSchema.toString() == 'testLayer geom: Point(EPSG:3857), name: String, address: String, distance: Double'
	}	
	
	def 'test addDistance when point (2263) and layer (2263) are same proj and distance units (feet) are same as layer (feet)' (){
		given: 
			def wps = new ProximityWps()			
			def testLayer = createTestLayerWithFeatures(2263)
			def prevDist = -1
			def point = new Point(1, 2)
			def inputFeatures = testLayer.getFeatures()
		
		when:
			def result = wps.addDistance(testLayer, inputFeatures, point, 'epsg:2263', 'feet')
			def outputFeatures = result.getFeatures()
		
		then:
			assert result.schema.toString() == 'testLayer geom: Point(EPSG:2263), name: String, address: String, distance: Double'
			assert outputFeatures.size() == 4
			outputFeatures.each {
				def inputFeature = featureByName(testLayer, it.get('name'))
				assert it.getGeom() == inputFeature.getGeom()
				assert it.get('name') == inputFeature.get('name')
				assert it.get('address') == inputFeature.get('address')
				assert it.get('distance') == point.distance(it.getGeom())
				assert it.get('distance') >= prevDist //sorted by distance
				prevDist = it.get('distance')
			}
			
	}
	
	def 'test addDistance when point (2263) and layer (2263) are same proj and distance units (meters) are different as layer (feet)' (){
		given: 
			def wps = new ProximityWps()			
			def testLayer = createTestLayerWithFeatures(2263)
			def prevDist = -1
			def point = new Point(1, 2)
			def inputFeatures = testLayer.getFeatures()
		
		when:
			def result = wps.addDistance(testLayer, inputFeatures, point, 'epsg:2263', 'meters')
			def outputFeatures = result.getFeatures()
		
		then:
			assert result.schema.toString() == 'testLayer geom: Point(EPSG:2263), name: String, address: String, distance: Double'
			assert outputFeatures.size() == 4
			outputFeatures.each {
				def inputFeature = featureByName(testLayer, it.get('name'))
				assert it.getGeom() == inputFeature.getGeom()
				assert it.get('name') == inputFeature.get('name')
				assert it.get('address') == inputFeature.get('address')

				def actualDistance = it.get('distance')
				def expectedDistance = point.distance(it.getGeom()) * 0.3048
				
				assert actualDistance == expectedDistance
				
				assert it.get('distance') >= prevDist //sorted by distance
				prevDist = it.get('distance')
			}
			
	}

	def 'test addDistance when point (2263) and layer (3857) are different proj and distance units (meters) are same as layer (meters)' (){
		given: 
			def wps = new ProximityWps()			
			def testLayer = createTestLayerWithFeatures(3857)
			def prevDist = -1
			def point = new Point(1, 2)
			def inputFeatures = testLayer.getFeatures()
			def prjPoint = Projection.transform(point, 'epsg:2263', 'epsg:3857') //project point to layer proj
		
		when:
			def result = wps.addDistance(testLayer, inputFeatures, prjPoint, 'epsg:3857', 'meters')
			def outputFeatures = result.getFeatures()
			
		
		then:
			assert result.schema.toString() == 'testLayer geom: Point(EPSG:3857), name: String, address: String, distance: Double'
			assert outputFeatures.size() == 4
			outputFeatures.each {
				def inputFeature = featureByName(testLayer, it.get('name'))
				assert it.getGeom() == inputFeature.getGeom()
				assert it.get('name') == inputFeature.get('name')
				assert it.get('address') == inputFeature.get('address')
				
				assert it.get('distance') == prjPoint.distance(it.getGeom())
				
				assert it.get('distance') >= prevDist //sorted by distance
				prevDist = it.get('distance')
			}
			
	}
	
	def 'test addDistance when point (2263) and layer (3857) are different proj and distance units (feet) are different from layer (meters)' (){
		given: 
			def wps = new ProximityWps()			
			def testLayer = createTestLayerWithFeatures(3857)
			def prevDist = -1
			def point = new Point(1, 2)
			def inputFeatures = testLayer.getFeatures()
			def prjPoint = Projection.transform(point, 'epsg:2263', 'epsg:3857') //project point to layer proj
		
		when:
			def result = wps.addDistance(testLayer, inputFeatures, point, 'epsg:2263', 'feet')
			def outputFeatures = result.getFeatures()

		then:
			assert result.schema.toString() == 'testLayer geom: Point(EPSG:2263), name: String, address: String, distance: Double'
			assert outputFeatures.size() == 4
			outputFeatures.each {
				def inputFeature = featureByName(testLayer, it.get('name'))
				
				assert it.getGeom() == Projection.transform(inputFeature.getGeom(), 'epsg:3857', 'epsg:2263')
				assert it.get('name') == inputFeature.get('name')
				assert it.get('address') == inputFeature.get('address')
				
				assert it.get('distance') == prjPoint.distance(inputFeature.getGeom()) * 3.28084 // meters to feet
				
				assert it.get('distance') >= prevDist //sorted by distance
				prevDist = it.get('distance')
			}
				
	}
	
	def 'test run when point (2263) and layer (2263) are same proj and distance units (feet) are same as layer (feet)' () {
		given:
			Layer testLayer = createTestLayerWithFeatures(2263)
			geoserver.catalog.Layer layerMock = Mock {
				1 * getGeoScriptLayer() >> testLayer
			}
			Catalog catalogMock = Mock {
				1 * getLayer('testLayer') >> layerMock
			}
			GeoServer geoserverMock = Mock {
				1 * getCatalog() >> catalogMock
			}
			ProximityWps.metaClass.getGeoServer = {geoserverMock}
 			
			def inputs = [
					srs: 'epsg:2263',
					x: 1,
					y: 2,
					layer: 'testLayer',
					distance: 20,
					units: 'feet'
				]
			def wps = new ProximityWps()
			def point = new Point(inputs.x, inputs.y)
			def prevDist = -1
		
		when: 
			def result = wps.run(inputs)
			def outputFeatures = result.result.getFeatures()
		
		then:
			assert outputFeatures.size() == 3
			outputFeatures.each {
				def inputFeature = featureByName(testLayer, it.get('name'))
				assert it.getGeom() == inputFeature.getGeom()
				assert it.get('name') == inputFeature.get('name')
				assert it.get('address') == inputFeature.get('address')
				assert it.get('distance') == point.distance(it.getGeom())
				assert it.get('distance') >= prevDist //sorted by distance
				prevDist = it.get('distance')
			}
	}
	
	def 'test run when point (2263) and layer (2263) are same proj and distance units (meters) are different from layer (feet)' () {
		given:
			Layer testLayer = createTestLayerWithFeatures(2263)
			geoserver.catalog.Layer layerMock = Mock {
				1 * getGeoScriptLayer() >> testLayer
			}
			Catalog catalogMock = Mock {
				1 * getLayer('testLayer') >> layerMock
			}
			GeoServer geoserverMock = Mock {
				1 * getCatalog() >> catalogMock
			}
			ProximityWps.metaClass.getGeoServer = {geoserverMock}
 			
			def inputs = [
					srs: 'epsg:2263',
					x: 1,
					y: 2,
					layer: 'testLayer',
					distance: 6,
					units: 'meters'
				]
			def wps = new ProximityWps()
			def point = new Point(inputs.x, inputs.y)
			def prevDist = -1
		
		when: 
			def result = wps.run(inputs)
			def outputFeatures = result.result.getFeatures()
		
		then:
			assert outputFeatures.size() == 3
			outputFeatures.each {
				def inputFeature = featureByName(testLayer, it.get('name'))
				assert it.getGeom() == inputFeature.getGeom()
				assert it.get('name') == inputFeature.get('name')
				assert it.get('address') == inputFeature.get('address')
				assert it.get('distance') == point.distance(inputFeature.getGeom()) * 0.3048 // feet to meters
				assert it.get('distance') >= prevDist //sorted by distance
				prevDist = it.get('distance')
			}
	}
	
	def 'test run when point (2263) and layer (3857) are different proj and distance units (feet) are different from layer (meters)' () {
		given:
			Layer testLayer = createTestLayerWithFeatures(3857)
			geoserver.catalog.Layer layerMock = Mock {
				1 * getGeoScriptLayer() >> testLayer
			}
			Catalog catalogMock = Mock {
				1 * getLayer('testLayer') >> layerMock
			}
			GeoServer geoserverMock = Mock {
				1 * getCatalog() >> catalogMock
			}
			ProximityWps.metaClass.getGeoServer = {geoserverMock}
 		
			
			def inputs = [
					srs: 'epsg:2263',
					x: 31130690,
					y:  -2279922,
					layer: 'testLayer',
					distance: 60,
					units: 'feet'
				]
			def wps = new ProximityWps()
			def point = new Point (inputs.x, inputs.y)
			def prevDist = -1
			def prjPoint = Projection.transform(point, 'epsg:2263', 'epsg:3857') //project point to layer proj
			
		when: 
			def result = wps.run(inputs)
			def outputFeatures = result.result.getFeatures()
		then:			
			assert outputFeatures.size() == 2
			outputFeatures.each {
				def inputFeature = featureByName(testLayer, it.get('name'))
				assert it.getGeom() == Projection.transform(inputFeature.getGeom(), 'epsg:3857', 'epsg:2263')
				assert it.get('name') == inputFeature.get('name')
				assert it.get('address') == inputFeature.get('address')
				assert it.get('distance') == prjPoint.distance(inputFeature.getGeom()) * 3.28084 // meters to feet
				assert it.get('distance') >= prevDist //sorted by distance
				prevDist = it.get('distance')
			}
	}
	
		def 'test run when point (3857) and layer (2263) are different proj and distance units (feet) are different from layer (feet)' () {
		given:
			Layer testLayer = createTestLayerWithFeatures(2263)
			geoserver.catalog.Layer layerMock = Mock {
				1 * getGeoScriptLayer() >> testLayer
			}
			Catalog catalogMock = Mock {
				1 * getLayer('testLayer') >> layerMock
			}
			GeoServer geoserverMock = Mock {
				1 * getCatalog() >> catalogMock
			}
			ProximityWps.metaClass.getGeoServer = {geoserverMock}
 		
			def inputs = [
					srs: 'epsg:3857',
					x: -12684158,
					y: 6011883,
					layer: 'testLayer',
					distance: 9914835,
					units: 'feet'
				]
			def wps = new ProximityWps()
			def point = new Point (inputs.x, inputs.y)
			def prevDist = -1
			def prjPoint = Projection.transform(point, 'epsg:3857', 'epsg:2263') //project point to layer proj
		when: 
			def result = wps.run(inputs)
			def outputFeatures = result.result.getFeatures()
		then:			
			assert outputFeatures.size() == 2
			outputFeatures.each {
				def inputFeature = featureByName(testLayer, it.get('name'))
				assert it.getGeom() == Projection.transform(inputFeature.getGeom(), 'epsg:2263', 'epsg:3857' )
				assert it.get('name') == inputFeature.get('name')
				assert it.get('address') == inputFeature.get('address')
				assert it.get('distance') == prjPoint.distance(inputFeature.getGeom())
				assert it.get('distance') >= prevDist //sorted by distance
				prevDist = it.get('distance')
			}
	}
	
	Feature featureByName(layer, name){
		def feature
		layer.eachFeature {
			if (it.get('name') == name){
				feature = it
			}
		}
		return feature
	}
	
	def createTestLayerWithFeatures(projection){
		Schema schema = new Schema("testLayer","geom:Point:srid=${projection},name:String,address:String")

		List inputPoints = [
			[geom: new Point(1, 10), name: 'New DoITT', address: '2 Metrotech'],
			[geom: new Point(10, 20), name: 'Old DoITT', address: '59 Maiden'],
			[geom: new Point(20, 2), name: 'City Hall', address: 'City Hall'],
			[geom: new Point(20, 2), name: 'Mayor', address: 'City Hall']
		]
	
		def testLayer = new Layer('testLayer', schema)
		inputPoints.eachWithIndex { attrs, id ->
			Feature f = new Feature(attrs, "f.${id}", schema)
			testLayer.add(f)
		}
		return testLayer
	}
}