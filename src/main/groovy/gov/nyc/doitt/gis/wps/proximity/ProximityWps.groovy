package gov.nyc.doitt.gis.wps.proximity

import java.util.List

import geoserver.GeoServer

import geoscript.geom.Point
import geoscript.layer.Layer
import geoscript.filter.Filter
import geoscript.layer.io.GeoJSONWriter
import geoscript.geom.io.WktWriter
import geoscript.feature.*
import geoscript.proj.Projection

/* Begin GeoServer WPS metadata */

title = 'Proximity'
description = 'Proximity'

inputs = [
	srs: [name: 'srs', title: 'srs', type: String.class],
	x: [name: 'x', title: 'x', type: Double.class],
	y: [name: 'y', title: 'y', type: Double.class],
	layer: [name: 'layer', title: 'layer', type: String.class],
	distance: [name: 'distance', title: 'distance', type: Double.class],
	units: [name: 'units', title: 'units', type: String.class]
]

outputs = [
	result: [name: 'result', title: 'result', type: Layer.class]
]

/* End GeoServer WPS metadata */

def run(input){
	def layer = getLayer(input.layer)
	def distance = input.distance
	def units = input.units
	def requestedPrj = input.srs
	def point = new Point(input.x, input.y)
	def filter = getFilter(layer, distance, units, requestedPrj, point)
	def features = layer.getFeatures(filter)
	def result = addDistance(layer, features, point, requestedPrj, units)
	return [result: result]
}

def convertUnits(distance, fromUnits, toUnits){
	def result = distance	
	if (fromUnits != toUnits){
		if (fromUnits == 'meters' && toUnits == 'feet'){
			result = distance * 3.28084
		}else if (fromUnits == 'feet' && toUnits == 'meters'){
			result = distance * 0.3048
		}
	}	
	return result
}

def getLayerUnits(layerPrj){
	def crs = layerPrj.crs.getCoordinateSystem()
	def axis = crs.getAxis(crs.getDimension() - 1)
	def layerUnits = getUnits(axis.getUnit().toString())
	if (layerUnits == null){
		throw new Exception('Cannot query data whose units are not meters or feet')
	}
	return layerUnits
}

def getFilter(layer, distance, units, requestedPrj, point){
	def layerPrj = layer.getProj()
	def layerUnits = getLayerUnits(layerPrj)
	def qDistance = convertUnits(distance, units, layerUnits)
	def geomCol = layer.schema.geom.name
	def prjPoint = Projection.transform(point, requestedPrj, "epsg:${layerPrj.getEpsg()}")
	prjPoint = new WktWriter().write(prjPoint)
	return "DWITHIN(${geomCol}, ${prjPoint}, ${qDistance}, ${layerUnits})"
}

def getNewSchema(layer, requestedPrj){
	List fields = []
	def geomField = layer.schema.geom
	layer.schema.fields.each { field ->
		if (field == geomField){
			fields.add(new Field(field.name, field.typ, requestedPrj))
		}else{
			fields.add(field)
		}
	}
	fields.add(new Field('distance', 'Double'))
	return new Schema(layer.name, fields)
}

def addDistance(layer, features, point, requestedPrj, units){
	def layerPrj = layer.getProj()
	def layerUnits = getLayerUnits(layerPrj)
	Schema schema = getNewSchema(layer, requestedPrj)
	List outFeatures = []

	features.each { inFeature ->
		def inGeom = inFeature.getGeom()
		def outGeom = Projection.transform(inGeom, "epsg:${layerPrj.getEpsg()}", requestedPrj)
		def prjPoint = Projection.transform(point, requestedPrj, "epsg:${layerPrj.getEpsg()}")
		
		def distance = inGeom.distance(prjPoint)
		distance = convertUnits(distance, layerUnits, units)
		
		def attrs = inFeature.getAttributes()
		attrs.put('distance', distance)
		
		Feature outFeature = new Feature(attrs, inFeature.getId(), schema)
		outFeature.setGeom(outGeom)
		outFeatures.add(outFeature)
	}
	
	Layer result = new Layer(layer.name, schema)
	outFeatures.sort{it.get('distance')}
	outFeatures.each { outFeature ->
		result.add(outFeature)
	}
	
	return result
}

def getLayer(layer){
	return getCatalog().getLayer(layer).getGeoScriptLayer()
}

def getCatalog(){
	return getGeoServer().catalog
}

def getGeoServer(){
	return new GeoServer()
}

def getUnits(units){
	return [foot_survey_us: 'feet', m: 'meters'].get(units)
}

//http://localhost:8080/geoserver/wps?service=WPS&version=1.0.0&request=Execute&identifier=groovy:ProximityWps&DataInputs=distance=1000;units=feet;srs=epsg:2263;x=993020;y=222220;layer=test:boro&RawDataOutput=result=format@mimetype=application/json