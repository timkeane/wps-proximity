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
		when:
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
		 then:
		 	assert wps.getFilter(layerMock, 3, 'feet', 'epsg:2263', point) == 'DWITHIN(the_geom, POINT (1 2), 3, feet)'
		 
	}

	def 'test getFilter when point (2263) and layer (3857) are different proj and distance units (meters) are same as layer (meters)'(){
		when:
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
		 then:
		 	assert wps.getFilter(layerMock, 3, 'meters', 'epsg:2263', point) == 'DWITHIN(the_geom, POINT (-8629440.293092024 4882288.084173), 3, meters)'
			 
	}

	def 'test getFilter when point (2263) and layer (3857) are different proj and distance units (feet) are different from layer (meters)'(){
		when:
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
		 then:
		 	assert wps.getFilter(layerMock, 3, 'feet', 'epsg:2263', point) == 'DWITHIN(the_geom, POINT (-8629440.293092024 4882288.084173), 0.9144, meters)'
	}
	
	def 'test getFilter when point (2263) and layer (2263) are same proj and distance units (meters) are different from layer (feet)'(){
		when:
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
		 then:
		 	assert wps.getFilter(layerMock, 3, 'meters', 'epsg:2263', point) == 'DWITHIN(the_geom, POINT (1 2), 9.84252, feet)'
	}
	
	def 'test getNewSchema where requested proj (2263) is same as layer proj (2263)'(){
		when:
			def wps = new ProximityWps()
			Schema schema = new Schema("testlayer","geom:Point:srid=2263,name:String,address:String")
			geoscript.layer.Layer layerMock = Mock {
				 2 * getSchema() >> schema
				 1 * getName() >> 'testLayer'
			}
		then:
			assert wps.getNewSchema(layerMock, 'epsg:2263').toString() == 'testLayer geom: Point(EPSG:2263), name: String, address: String, distance: Double'
	}

	def 'test getNewSchema where requested proj (3857) is different as layer proj (2263)'(){
		when:
			def wps = new ProximityWps()
			Schema schema = new Schema("testlayer","geom:Point:srid=2263,name:String,address:String")
			geoscript.layer.Layer layerMock = Mock {
				 2 * getSchema() >> schema
				 1 * getName() >> 'testLayer'
			}
		then:
			assert wps.getNewSchema(layerMock, 'epsg:3857').toString() == 'testLayer geom: Point(EPSG:3857), name: String, address: String, distance: Double'

	}	
	
}