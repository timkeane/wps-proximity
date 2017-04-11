package gov.nyc.doitt.gis.app.proximity

import org.restlet.data.*
import geoserver.*

import geoscript.process.Process
import geoscript.geom.Point
import geoscript.layer.Layer
import geoscript.filter.Filter
import geoscript.layer.io.GeoJSONWriter

import geoscript.feature.*

def run(request, response){
	def params = getParams(request)
	def layer = getLayer(params)
	def filtered = new Layer(layer.name, layer.schema)
	def filter = getFilter(layer, params)
	def features = layer.getFeatures(filter)
	filtered.add(features)
	def result = addDistance(filtered, params)
	response.setEntity(new GeoJSONWriter().write(result), MediaType.APPLICATION_JSON)
}

def getParams(request){
	return request.getResourceRef().getQueryAsForm()
}

def getLayer(params){
	def layerName = params.getFirstValue('layer')
	def gsLayer = new GeoServer().catalog.getLayer(layerName)
	return gsLayer.geoScriptLayer
}

def getFilter(layer, params){
	def geomCol = layer.schema.geom.name
	def point = params.getFirstValue('point')
	def distance = params.getFirstValue('distance')
	def units = params.getFirstValue('units')
	return "DWITHIN(${geomCol}, POINT(${point}), ${distance}, ${units})"
}

def addDistance(inLayer, params){
	def coord = params.getFirstValue('point').split(' ')
	def x = new Double(coord[0])
	def y = new Double(coord[1])
	def inPoint = new Point(x, y) 
	List fields = inLayer.schema.fields
	fields.add(new Field('distance', 'Double'))
	Schema schema = new Schema(inLayer.name, fields)
	List outFeatures = []

	inLayer.eachFeature { inFeature ->
		def geom = inFeature.getGeom()
		def distance = geom.distance(inPoint)
		def attrs = inFeature.getAttributes()
		attrs.put('distance', distance)
		Feature outFeature = new Feature(attrs, inFeature.getId())
		outFeatures.add(outFeature)
	}
	
	Layer result = new Layer(inLayer.name, schema)
	outFeatures.sort{it.get('distance')}
	outFeatures.each { outFeature ->
		result.add(outFeature)
	}
	
	return result
}





//http://localhost:8080/geoserver/script/apps/proximity/?point=993020 222220&layer=test:boro&distance=1000&units=feet