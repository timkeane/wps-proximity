package gov.nyc.doitt.gis.wps.proximity

import java.util.List

import geoserver.GeoServer

import geoscript.process.Process
import geoscript.geom.Point
import geoscript.layer.Layer
import geoscript.filter.Filter
import geoscript.layer.io.GeoJSONWriter
import geoscript.geom.io.WktWriter
import geoscript.feature.*
import geoscript.proj.Projection

import java.time.LocalDateTime

CATALOG = new GeoServer().catalog
WKT = new WktWriter()

LOG = new File('e:/log.txt')

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

def run(input){
	LOG.append(LocalDateTime.now().toString() + ' begin wps\n')
	def layer = CATALOG.getLayer(input.layer).geoScriptLayer
	def distance = input.distance
	def units = input.units
	def requestedPrj = input.srs
	def point = new Point(input.x, input.y)
	LOG.append(LocalDateTime.now().toString() + ' begin get filter\n')
	def filter = getFilter(layer, distance, units, requestedPrj, point)
	LOG.append(LocalDateTime.now().toString() + ' end get filter\n')
	LOG.append(LocalDateTime.now().toString() + ' begin query\n')
	def features = layer.getFeatures(filter)
	LOG.append(LocalDateTime.now().toString() + ' end query\n')
	LOG.append(LocalDateTime.now().toString() + ' begin distance\n')
	def result = addDistance(layer, features, point, requestedPrj)
	LOG.append(LocalDateTime.now().toString() + ' end distance\n\n')
	return [result: result]
}

def getFilter(layer, distance, units, requestedPrj, point){
	def layerPrj = layer.getProj().getEpsg()
	def geomCol = layer.schema.geom.name
	point = Projection.transform(point, requestedPrj, "epsg:${layerPrj}")
	point = WKT.write(point)
	return "DWITHIN(${geomCol}, ${point}, ${distance}, ${units})"
}

def getSNewSchema(layer, requestedPrj){
	List fields = []
	layer.schema.fields.each { field ->
		if (field == layer.schema.geom){
			fields.add(new Field(field.name, field.typ, requestedPrj))
		}else{
			fields.add(field)
		}
	}
	fields.add(new Field('distance', 'Double'))
	return new Schema(layer.name, fields)
}

def addDistance(layer, features, point, requestedPrj){
	def layerPrj = layer.getProj().getEpsg()
	Schema schema = getSNewSchema(layer, requestedPrj)
	List outFeatures = []

	features.each { inFeature ->
		def geom = Projection.transform(inFeature.getGeom(), "epsg:${layerPrj}", requestedPrj)
		def distance = geom.distance(point)
		def attrs = inFeature.getAttributes()
		attrs.put('distance', distance)
		Feature outFeature = new Feature(attrs, inFeature.getId())
		outFeature.setGeom(geom)
		outFeatures.add(outFeature)
	}
	
	Layer result = new Layer(layer.name, schema)
	outFeatures.sort{it.get('distance')}
	outFeatures.each { outFeature ->
		result.add(outFeature)
	}
	
	return result
}

//http://localhost:8080/geoserver/wps?service=WPS&version=1.0.0&request=Execute&identifier=groovy:ProximityWps&DataInputs=distance=1000;units=feet;srs=epsg:2263;x=993020;y=222220;layer=test:boro&RawDataOutput=result=format@mimetype=application/json