package gov.nyc.doitt.gis.wps.proximity

import spock.lang.*

import geoserver.GeoServer
import geoserver.catalog.Catalog
import geoserver.catalog.Layer

import java.util.List

import geoscript.feature.*
import geoscript.layer.Layer
import geoscript.geom.*

class ProximityWpsTest extends Specification {
	ProximityWps wps
	
	def 'test getUnits'(){
		when:
			def wps = new ProximityWps()
		then:
			assert wps != null
			assert wps.getUnits('foot_survey_us') == 'feet'
			assert wps.getUnits('m') == 'meters'
			assert wps.getUnits('°') == 'degrees'
	}

	def 'test getGeoServer'(){
		when:
			geoserver.catalog.Layer layerMock = Mock {}
			Catalog catalogMock = Mock {}
			
			GeoServer geoserverMock = Mock {
				1 * getCatalog() >> catalogMock
			}
			ProximityWps.metaClass.getGeoServer = {geoserverMock}
 			def wps = new ProximityWps()
		then:
			assert wps.getGeoServer() == geoserverMock
	}

}