package gov.nyc.doitt.gis.app.proximity

import org.restlet.data.*
import geoserver.*

import geoscript.geom.Point
import geoscript.layer.Layer
import geoscript.filter.Filter
import geoscript.layer.io.GeoJSONWriter

def run(request, response){
	def requestParams = request.getResourceRef().getQueryAsForm()
	def pointStr = requestParams.getFirstValue('point')
	def coord = pointStr.split(',')
	def x = new Double(coord[0])
	def y = new Double(coord[1])
	def point = new Point(x, y)
	def distance = new Double(requestParams.getFirstValue('distance'))
	def layerName = requestParams.getFirstValue('layer')
	def gsLayer = new GeoServer().catalog.getLayer(layerName)
	def scriptLayer = gsLayer.geoScriptLayer
	def geomColumn = scriptLayer.schema.geom.name
	def filter = Filter.dwithin(geomColumn, point, distance, 'feet');
            
	def result = scriptLayer.filter(filter, 'wtf')
	
	 GeoJSONWriter writer = new GeoJSONWriter()

	//response.setEntity(writer.write(result), MediaType.APPLICATION_JSON)
	response.setEntity(result.count()  + '', MediaType.TEXT_HTML)
}