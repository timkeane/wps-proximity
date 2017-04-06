package gov.nyc.doitt.gis.app.proximity

import org.restlet.data.*
import geoserver.*

import geoscript.process.Process
import geoscript.geom.Point
import geoscript.layer.Layer
import geoscript.filter.Filter
import geoscript.layer.io.GeoJSONWriter

def run(request, response){
	def params = getParams(request)
	def layer = getLayer(params)
	//def filter = getFilter(layer, params)
            
	//def result = layer.filter(filter, layer.name)
	
	def proc = new Process("gs:Query")
	def result = proc.execute([features: layer, filter: getFilter(layer, params)])
	
	GeoJSONWriter writer = new GeoJSONWriter()

	response.setEntity(writer.write(result), MediaType.APPLICATION_JSON)
}

def getParams(request){
	return request.getResourceRef().getQueryAsForm()
}

def getFilter(layer, params){
	def geomCol = layer.schema.geom.name
	def point = params.getFirstValue('point')
	
	return "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"><ogc:DWithin><ogc:PropertyName>${geomCol}<ogc:/PropertyName><gml:Point srsName=\"http://www.opengis.net/gml/srs/epsg.xml#2263\"xmlns:gml=\"http://www.opengis.net/gml\"><gml:coordinates decimal=\".\" cs=\",\" ts=\" \">${point}</gml:coordinates></gml:Point><ogc:Distance units=\"feet\">7000<ogc:/Distance><ogc:/DWithin><ogc:/Filter>"

	/*
	def point = getPoint(params)
	def distance = getDistance(params)
	def geomCol = layer.schema.geom.name
	return Filter.dwithin(geomCol, point, distance, 'feet')
	*/
}

def getPoint(params){
	def pointStr = params.getFirstValue('point')
	def coord = pointStr.split(',')
	def x = new Double(coord[0])
	def y = new Double(coord[1])
	return new Point(x, y)
}

def getDistance(params){
	return new Double(params.getFirstValue('distance'))
}

def getLayer(params){
	def layerName = params.getFirstValue('layer')
	def gsLayer = new GeoServer().catalog.getLayer(layerName)
	return gsLayer.geoScriptLayer

}

//http://localhost:8080/geoserver/script/apps/proximity/?point=993020,222220&layer=test:boro&distance=1000